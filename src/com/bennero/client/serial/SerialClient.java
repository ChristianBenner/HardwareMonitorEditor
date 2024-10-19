package com.bennero.client.serial;

import com.bennero.client.Version;
import com.bennero.client.core.ApplicationCore;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.fazecast.jSerialComm.SerialPort;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.bennero.common.Constants.HEARTBEAT_FREQUENCY_MS;

public class SerialClient {
    private static final String LOGGER_TAG = SerialClient.class.getSimpleName();
    private static final int MAX_ATTEMPTS = 4;

    private static final int WRITE_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    // First wait is 0ms, then 25ms, then 125ms, then 600ms, then 3000ms then fail completely
    private static final double ATTEMPT_WAIT_MS = 25;

    private static SerialClient instance = null;

    private SerialPort serialPort;
    private boolean connected;

    private UUID connectedUUID;
    private long lastMessageSendMs;

    private SerialClient() {
        connected = false;
        connectedUUID = null;
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

    private void printSerialPortInfo(SerialPort serialPort) {
        System.out.println("PortInfo:" +
                "\n\tDescriptivePortName:   " + serialPort.getDescriptivePortName() +
                "\n\tSystemPortName:        " + serialPort.getSystemPortName() +
                "\n\tPortDescription:       " + serialPort.getPortDescription() +
                "\n\tManufacturer:          " + serialPort.getManufacturer() +
                "\n\tPortLocation:          " + serialPort.getPortLocation() +
                "\n\tSystemPortPath:        " + serialPort.getSystemPortPath() +
                "\n\tBaudRate:              " + serialPort.getBaudRate() +
                "\n\tCTS:                   " + serialPort.getCTS() +
                "\n\tDCD:                   " + serialPort.getDCD() +
                "\n\tDSR:                   " + serialPort.getDSR() +
                "\n\tDTR:                   " + serialPort.getDTR() +
                "\n\tDeviceReadBufferSize:  " + serialPort.getDeviceReadBufferSize() +
                "\n\tDeviceWriteBufferSize: " + serialPort.getDeviceWriteBufferSize() +
                "\n\tFlowControlSettings:   " + serialPort.getFlowControlSettings() +
                "\n\tProductID:             " + serialPort.getProductID() +
                "\n\tRI:                    " + serialPort.getRI() +
                "\n\tRTS:                   " + serialPort.getRTS() +
                "\n\tVendorID:              " + serialPort.getVendorID());
    }

    public ConnectionInfo connect(SerialPort serialPort) {
        this.serialPort = serialPort;

        Logger.logf(LogLevel.INFO, LOGGER_TAG, "Attempting Serial Connection [Port: %s]", serialPort.getSystemPortName());
        //printSerialPortInfo(serialPort);

        connected = serialPort.openPort();
        if(!connected) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Failed to open port '%s'", serialPort.getSystemPortName());
            return new ConnectionInfo(ConnectionState.PORT_FAILED_OPEN);
        }

        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.EVEN_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, READ_TIMEOUT_MS, WRITE_TIMEOUT_MS);

        // Send a message to determine version parity
        VersionParityMessage out = new VersionParityMessage(ApplicationCore.s_getUUID(), true,
                Version.VERSION_MAJOR, Version.VERSION_MINOR, Version.VERSION_PATCH);

        final int MAX_RESEND_ATTEMPTS = 5;
        for (int i = 0; i < MAX_RESEND_ATTEMPTS; i++) {
            byte[] bytes = out.write();
            int numWrite = serialPort.writeBytes(bytes, Message.NUM_BYTES);
            if (numWrite < Message.NUM_BYTES) {
                Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Time out on write [Port: %s] [Num Bytes Wrote: %d]", serialPort.getSystemPortName(), numWrite);
                return new ConnectionInfo(ConnectionState.WRITE_TIMEOUT);
            } else {
                Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Sent %s message", MessageType.asString(out.getType()));
            }

            byte[] readBuffer = new byte[Message.NUM_BYTES];
            int numRead = serialPort.readBytes(readBuffer, Message.NUM_BYTES);
            if (numRead < Message.NUM_BYTES) {
                Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Time out on read [Port: %s] [Num Bytes Read: %d]", serialPort.getSystemPortName(), numRead);
                return new ConnectionInfo(ConnectionState.READ_TIMEOUT);
            }

            if (!Message.isValid(readBuffer)) {
                Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Corrupted message received: checksum mismatch");
                return new ConnectionInfo(ConnectionState.BAD_RESPONSE_INVALID_CHECKSUM);
            }

            byte messageType = Message.getType(readBuffer);
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Received message [Type: %s]", MessageType.asString(messageType));

            if (messageType == MessageType.CONFIRMATION) {
                ConfirmationMessage in = new ConfirmationMessage(readBuffer);
                if(in.getStatus() == Message.STATUS_BAD_RECEIVE) {
                    // Monitor checksum failed, try again
                    continue;
                } else {
                    return new ConnectionInfo(ConnectionState.BAD_RESPONSE_WRONG_MESSAGE);
                }
            }

            if(messageType != MessageType.VERSION_PARITY_RESPONSE) {
                Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected message type in response to version parity request: " + messageType);
                return new ConnectionInfo(ConnectionState.BAD_RESPONSE_WRONG_MESSAGE);
            }

            VersionParityResponseMessage in = new VersionParityResponseMessage(readBuffer);
            if(!in.isAccepted()) {
                Logger.log(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor refused connection, reason: " + in.getRejectionReason());
                return new ConnectionInfo(ConnectionState.REJECTED_CONNECTION, in.getRejectionReason());
            }

            connectedUUID = in.getSenderUuid();
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Monitor connected: " + connectedUUID.toString());

            runWriteThread();

            return new ConnectionInfo(ConnectionState.CONNECTED);
        }

        // After several attempts, the monitor failed to receive our data correctly
        return new ConnectionInfo(ConnectionState.POOR_COMMUNICATION);
    }

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

    private boolean checkReceived() {
        byte[] bytes = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(bytes, Message.NUM_BYTES);

        if (numRead < Message.NUM_BYTES) {
            Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Time out on read [Port: %s] [Num Bytes Read: %d]", serialPort.getSystemPortName(), numRead);
            return false;
        }

        if (!Message.isValid(bytes)) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Bad checksum for confirmation message");
            return false;
        }

        byte receivedType = Message.getType(bytes);
        if (receivedType == MessageType.VERSION_PARITY_RESPONSE) {
            VersionParityResponseMessage in = new VersionParityResponseMessage(bytes);
            if (!in.isAccepted()) {
                // todo: 'try again' feels bad. maybe highlight a bigger comms issue here
                return false;
            }

            // Read the next message because this one does not matter (if there is many version parity response
            // messages in the serial buffer, it will read all of them until we get to a real message).
            return checkReceived();
        }

        if (receivedType != MessageType.CONFIRMATION) {
            Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Expected message of type: %s, got %s", MessageType.asString(MessageType.CONFIRMATION), MessageType.asString(receivedType));
            return false;
        }

        ConfirmationMessage in = new ConfirmationMessage(bytes);
        if (in.getStatus() != Message.STATUS_OK) {
            Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Monitor reported message not received correctly");
            return false;
        }

        if (!connectedUUID.equals(in.getSenderUuid())) {
            Logger.log(LogLevel.WARNING, LOGGER_TAG, "Received confirmation message from a different monitor instance");
            return false;
        }

        return true;
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
                int numWrite = serialPort.writeBytes(message, Message.NUM_BYTES);
                if (numWrite < Message.NUM_BYTES) {
                    Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Time out on write [Port: %s] [Num Bytes Wrote: %d]", serialPort.getSystemPortName(), numWrite);
                    return false;
                }

                boolean received = checkReceived();
                String logState = received ? "Successfully sent message" : "Failed to send message";
                byte type = Message.getType(message);
                String typeString = MessageType.asString(type);
                if (attempt > 0) {
                    Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "%s [Type: %s] [Attempt: %d]", logState, typeString, attempt);
                } else {
                    //if (type != MessageType.SENSOR_UPDATE) {
                        Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "%s [Type: %s]", logState, typeString, attempt);
                    //}
                }

                // todo: if we have not received a good message within heartbeat period, disconnect and report error
                if (received) {
                    lastMessageSendMs = System.currentTimeMillis();
                    break;
                }

                // Message did not receive correctly, try to send again
                // If that was the first attempt try again immediately
                if(attempt == 0) {
                    continue;
                }

                // Bad comm, retry send but wait before
                int sleepMs = (int)ATTEMPT_WAIT_MS * (int)Math.pow(5, attempt);
                Thread.sleep(sleepMs);
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

    public boolean writeMessage(Message message) {
        if (!connected || !serialPort.isOpen()) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Failed to send message (type: %s) because serial port is not connected", message.getTypeString());
        }

        byte[] bytes = message.write();
        queueMessage(bytes);
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
