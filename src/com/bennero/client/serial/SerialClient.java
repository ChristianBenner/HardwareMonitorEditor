package com.bennero.client.serial;

import com.bennero.client.message.*;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.HeartbeatMessage;
import com.bennero.common.messages.MessageType;
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
    private UUID instanceUUID;
    private long lastMessageSendMs;

    private SerialClient() {
        connected = false;
        this.connectedUUID = null;
        instanceUUID = UUID.randomUUID();

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
            messages.add(HeartbeatMessage.create(instanceUUID, true));
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "No message sent for " + HEARTBEAT_FREQUENCY_MS + "ms. Queueing heartbeat message");
        }

        // Send all the messages
        for(byte[] message : messages) {
            // Add a checksum to the end of the message so that the monitor can verify the data was received correctly
            Checksum checksum = new CRC32();
            checksum.update(message, 0, message.length);
            ByteBuffer buffer = ByteBuffer.allocate(message.length + Long.BYTES);
            buffer.put(message);
            buffer.putLong(checksum.getValue());

            // Send message with checksum
            byte[] packetWithChecksum = buffer.array();

            // todo: implement a write and read block but with timeouts. If timeout expires report disconnect
            int attempt = 0;
            for(; attempt < MAX_ATTEMPTS; attempt++) {
                serialPort.writeBytes(packetWithChecksum, packetWithChecksum.length);
                Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent message of type: " + packetWithChecksum[MESSAGE_TYPE_POS] + " attempt: " + attempt);

                lastMessageSendMs = System.currentTimeMillis();

                byte[] messageReceiveStatus = new byte[MESSAGE_NUM_BYTES];
                serialPort.readBytes(messageReceiveStatus, MESSAGE_NUM_BYTES);
                HeartbeatMessage heartbeatMessage = HeartbeatMessage.readHeartbeatMessage(messageReceiveStatus);

                // todo: what if the first message received back is bad? need to add checksum to heartbeat
                if (connectedUUID == null) {
                    connectedUUID = heartbeatMessage.getInstanceUuid();
                    Logger.log(LogLevel.DEBUG, LOGGER_TAG, "New connected monitor ID: " + connectedUUID.toString());

                    continue;
                }

                if (!connectedUUID.equals(heartbeatMessage.getInstanceUuid()) && heartbeatMessage.isOk()) {
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
        byte[] versionParityMessage = VersionParityMessage.create();
        serialPort.writeBytes(versionParityMessage, MESSAGE_NUM_BYTES);

        // Expect immediate response
        byte[] readBuffer = new byte[MESSAGE_NUM_BYTES];
        int numRead = serialPort.readBytes(readBuffer, MESSAGE_NUM_BYTES);
        if (numRead < MESSAGE_NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
            return false;
        }

        if(readBuffer[0] != MessageType.VERSION_PARITY_RESPONSE_MESSAGE) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected message type in response to version parity request: " +  readBuffer[0]);
            return false;
        }

        VersionParityResponseMessage response = VersionParityResponseMessage.processConnectionRequestMessageData(readBuffer);
        if(!response.isAccepted()) {
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor refused connection because of version mismatch: v" + response.getMajorVersion() + "." + response.getMinorVersion() + "." + response.getPatchVersion());
            return false;
        }

        serialPort.setComPortTimeouts(0, 0, 0);

        runWriteThread();

        return true;
    }

    public void writeRemovePageMessage(byte pageId) {
        if (connected && serialPort.isOpen()) {
            byte[] message = RemovePageMessage.create(pageId);
            queueMessage(message);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Remove Page Message: [ID: " + pageId + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Remove Page message because serial port is not connected");
        }
    }

    public void writeRemoveSensorMessage(byte sensorId, byte pageId) {
        if (connected && serialPort.isOpen()) {
            byte[] message = RemoveSensorMessage.create(sensorId, pageId);
            queueMessage(message);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Remove Sensor Message: [ID: " + sensorId + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Remove Sensor message because serial port is not connected");
        }
    }

    public void writePageMessage(PageData pageData) {
        if (connected && serialPort.isOpen()) {
            byte[] message = PageSetupMessage.create(pageData);
            queueMessage(message);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent PageData Message: [ID: " + pageData.getUniqueId() + "], [TITLE: " + pageData.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send PageData message because serial port is not connected");
        }
    }

    public void writeSensorSetupMessage(Sensor sensor, byte pageId) {
        if (connected && serialPort.isOpen()) {
            byte[] message = SensorSetupMessage.create(sensor, pageId);
            queueMessage(message);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Sensor set-up Message: [ID: " + sensor.getUniqueId() + "], [TITLE: " + sensor.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor set-up Message because serial port is not connected");
        }
    }

    public void writeSensorTransformationMessage(Sensor sensor, byte pageId) {
        if (connected && serialPort.isOpen()) {
            byte[] message = SensorTransformationMessage.create(sensor, pageId);
            queueMessage(message);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Sensor Transformation Message: [ID: " + sensor.getUniqueId() + "], [TITLE: " + sensor.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor Transformation Message because serial port is not connected");
        }
    }

    public void writeSensorValueMessage(int sensorId, float value) {
        if (connected && serialPort.isOpen()) {
            byte[] message = SensorValueMessage.create(sensorId, value);
            queueMessage(message);
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor value message because serial port is not connected");
        }
    }

    public void writeFileMessage(int size, String name, byte[] fileBytes, byte type) {
        if (connected && serialPort.isOpen()) {
            // Transfer inbound message
            byte[] filePrepMessage = FileMessage.create(size, name, type);
            queueMessage(filePrepMessage, fileBytes);
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send File message because serial port is not connected");
        }
    }
}
