package com.bennero.client.serial;

import com.bennero.client.Version;
import com.bennero.client.core.ApplicationCore;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static com.bennero.common.Constants.*;

public class SerialClient {
    enum ConnectionState {
        CONNECTED,
        COMMUNICATION_FAILURE,

    }

    private static final String LOGGER_TAG = SerialClient.class.getSimpleName();
    private static final int MAX_ATTEMPTS = 4;

    // First wait is 0ms, then 25ms, then 125ms, then 600ms, then 3000ms then fail completely
    private static final double ATTEMPT_WAIT_MS = 25;

    private static SerialClient instance = null;

    private SerialPort serialPort;
    private boolean connected;

    private UUID connectedUUID;
    private long lastMessageSendMs;

    private SerialClient() {
        connected = false;
        this.connectedUUID = null;

//        this.programConfigManager = ProgramConfigManager.getInstance();
//        this.connected = false;
    }

    public static SerialClient getInstance() {
        if (instance == null) {
            instance = new SerialClient();
        }

        return instance;
    }

    public boolean isConnected() {
        return connected;
    }

    private Semaphore messageQueueLock = new Semaphore(1);
    private Queue<byte[]> messageQueue = new LinkedList<>();

    // Write runs its own thread, picking up the queue of messages that need to be sent out
    private void runWriteThread() {
        Thread t = new Thread(() -> {
            while(serialPort.isOpen()) {
                try {
                    if(!write()) {
                        // Comms failure occurred

                    }
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    // true if successful, false if comms failure
    private boolean write() throws InterruptedException {
        // Try to obtain queue lock
        messageQueueLock.acquire();

        LinkedList<byte[]> messages = new LinkedList<>();
        while(!messageQueue.isEmpty()) {
            messages.add(messageQueue.poll());
        }

        // Release lock at the earliest possible time so the queue can be added to again
        messageQueueLock.release();

        long timeSinceLastSendMs = System.currentTimeMillis() - lastMessageSendMs;

        // Send a heartbeat if we have not sent a message since the heartbeat timeout
        if (messages.isEmpty() && timeSinceLastSendMs >= HEARTBEAT_FREQUENCY_MS) {
            HeartbeatMessage out = new HeartbeatMessage(ApplicationCore.s_getUUID(), true);
            messages.add(out.write());
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "No message sent for " + HEARTBEAT_FREQUENCY_MS + "ms. Queueing heartbeat message");
        }

        // Send all the messages
        for(byte[] message : messages) {
            // todo: implement a write and read block but with timeouts. If timeout expires report disconnect
            int attempt = 0;
            for(; attempt < MAX_ATTEMPTS; attempt++) {
                serialPort.writeBytes(message, Message.NUM_BYTES);
                Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent message of type: " + Message.getType(message) + " attempt: " + attempt);

                lastMessageSendMs = System.currentTimeMillis();

                byte[] messageReceiveStatus = new byte[Message.NUM_BYTES];
                serialPort.readBytes(messageReceiveStatus, Message.NUM_BYTES);
                HeartbeatMessage heartbeatMessage = new HeartbeatMessage(messageReceiveStatus);

                // todo: what if the first message received back is bad? need to add checksum to heartbeat
                if (connectedUUID == null) {
                    connectedUUID = heartbeatMessage.getSenderUuid();
                    Logger.log(LogLevel.DEBUG, LOGGER_TAG, "New connected monitor ID: " + connectedUUID.toString());

                    continue;
                }

                if (!connectedUUID.equals(heartbeatMessage.getSenderUuid()) && heartbeatMessage.getStatus() == Message.STATUS_OK) {
                    Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Received bad heartbeat from monitor");

                    // Message did not receive correctly, try to send again
                    // If that was the first attempt try again immediately
                    if(attempt == 0) {
                        continue;
                    }

                    // Bad comm, retry send but wait before
                    int sleepMs = (int)ATTEMPT_WAIT_MS * (int)Math.pow(5, attempt);
                    Thread.sleep(sleepMs);
                } else {
                    Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Received good message confirmation from monitor");

                    // Message was received correctly, do not re-attempt send
                    break;
                }
            }

            if (attempt == MAX_ATTEMPTS) {
                // Exit due to com failure
                return false;
            }
        }

        return true;
    }

    private void queueMessage(byte[] ... messages) {
        try {
            messageQueueLock.acquire();
            for(byte[] message : messages) {
                messageQueue.add(message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        messageQueueLock.release();
    }

    public boolean connect(SerialPort serialPort) {
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Attempting Serial Connection: " + serialPort.getSystemPortName());

        this.serialPort = serialPort;
        connected = serialPort.openPort();

        if(!connected) {
            return false;
        }

        Logger.log(LogLevel.INFO, LOGGER_TAG, "Connected to device: " + serialPort.getSystemPortName());
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.EVEN_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

        // todo: move the connection establish code to its own function and use the write thread instead, need to report
        //  back using handlers
        // Send a message to determine version parity
        VersionParityMessage out = new VersionParityMessage(ApplicationCore.s_getUUID(), true,
                Version.VERSION_MAJOR, Version.VERSION_MINOR, Version.VERSION_PATCH);
        byte[] bytes = out.write();
        serialPort.writeBytes(bytes, Message.NUM_BYTES);

        // Expect immediate response
        byte[] readBuffer = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(readBuffer, Message.NUM_BYTES);
        if (numRead < Message.NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
            return false;
        }

        if(Message.getType(readBuffer) != MessageType.VERSION_PARITY_RESPONSE) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected message type in response to version parity request: " +  readBuffer[0]);
            return false;
        }

        VersionParityResponseMessage in = new VersionParityResponseMessage(readBuffer);
        if(!in.isAccepted()) {
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor refused connection because of version mismatch: v" + in.getVersionMajor() + "." + in.getVersionMinor() + "." + in.getVersionPatch());
            return false;
        }

        serialPort.setComPortTimeouts(0, 0, 0);

        runWriteThread();

        return true;
    }

    public boolean writeMessage(Message message) {
        if (!connected || !serialPort.isOpen()) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Failed to send message (type: %d) because serial port is not connected", message.getType());
        }

        byte[] bytes = message.write();
        queueMessage(bytes);
        Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Sent message (type: %d)", message.getType());
        return true;
    }

    public void writeFileMessage(FileTransferMessage message, byte[] fileBytes) {
       // int size, String name, byte[] fileBytes, byte type
        if (connected && serialPort.isOpen()) {
            // Transfer inbound message
            queueMessage(message.write(), fileBytes);
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send File message because serial port is not connected");
        }
    }
}
