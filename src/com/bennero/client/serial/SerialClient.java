package com.bennero.client.serial;

import com.bennero.client.Version;
import com.bennero.client.core.ApplicationCore;
import com.bennero.client.core.SystemTrayManager;
import com.bennero.client.states.InformationStateData;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.event.EventHandler;

import java.awt.*;
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
    private static final int NOTIFY_CONNECT_FAILURE_MS = 3 * 60 * 1000;

    // First wait is 0ms, then 25ms, then 125ms, then 600ms, then 3000ms then fail completely
    private static final double ATTEMPT_WAIT_MS = 25;

    private static SerialClient instance = null;

    private SerialPort serialPort;
    private boolean connected;

    private UUID connectedUUID;
    private long lastMessageSendMs;

    private Thread writeThread;
    boolean disconnecting;

    private SerialClient() {
        connected = false;
        connectedUUID = null;
        disconnecting = false;
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

    private void clearMessageQueue() {
        try {
            messageQueueLock.acquire();
            messageQueue.clear();
            messageQueueLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (connected && serialPort.isOpen() && writeThread != null && writeThread.isAlive()) {
            // 1. Prevent more messages from being queued
            disconnecting = true;

            try {
                messageQueueLock.acquire();
                // 2. Remove all messages from the message queue
                messageQueue.clear();

                messageQueueLock.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 3. Wait for the write thread to exit (exits when the serial port is closed - on confirmation that the
            // monitor has received the disconnect message)
            try {
                writeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 4. Write message to serial port for disconnect
            DisconnectMessage out = new DisconnectMessage(ApplicationCore.s_getUUID(), true);
            try {
                attemptSend(out.write(), MAX_ATTEMPTS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 5. Close serial port
            serialPort.closePort();
            connected = false;
            disconnecting = false;

            SerialScanner.handleScan();
        }
    }

    public String getConnectionName() {
        if (serialPort == null) {
            return "";
        }

        if (!serialPort.isOpen()) {
            return "";
        }

        return serialPort.getDescriptivePortName();
    }

    private void showFailedToConnect(String info, EventHandler buttonEvent) {
        Platform.runLater(() -> {
            ApplicationCore.getInstance().setApplicationState(new InformationStateData("Failed to connect", info + "\nRetrying...", "Cancel", buttonEvent));
        });
    }

    private boolean handleConnectionState(ConnectionInfo connectionInfo, EventHandler buttonEvent) {
        switch (connectionInfo.getConnectionState()) {
            case CONNECTED:
                Platform.runLater(() -> {
                    ApplicationCore.getInstance().onConnected();
                });
                return true;
            case PORT_FAILED_OPEN:
                showFailedToConnect("Failed to open serial port", buttonEvent);
                return false;
            case WRITE_TIMEOUT:
                showFailedToConnect("Timeout sending request", buttonEvent);
                return false;
            case READ_TIMEOUT:
                showFailedToConnect("Timeout waiting for response", buttonEvent);
                return false;
            case BAD_RESPONSE_WRONG_MESSAGE:
                showFailedToConnect("Received unexpected response", buttonEvent);
                return false;
            case BAD_RESPONSE_INVALID_CHECKSUM:
                showFailedToConnect("Received corrupt response", buttonEvent);
                return false;
            case REJECTED_CONNECTION:
                showFailedToConnect("Monitor rejected connection: " + connectionInfo.getRejectionReason(), buttonEvent);
                return false;
            default:
                showFailedToConnect("Unknown error", buttonEvent);
                return false;
        }
    }

    public void runConnectThread(SerialPort serialPort) {
        Thread establishConnectionThread = new Thread(() -> {
            long connectionAttemptStartTime = System.currentTimeMillis();
            boolean notificationSent = false;

            final boolean[] retry = {true};
            EventHandler cancelRetryEvent = event -> {
                retry[0] = false;
                SerialScanner.handleScan();
            };

            while(retry[0]) {
                ConnectionInfo connectionInfo = SerialClient.getInstance().connect(serialPort);
                boolean success = handleConnectionState(connectionInfo, cancelRetryEvent);
                if(success) {
                    break;
                }

                // After some time of failing to connect, if the program is docked, send a notification to the user
                boolean notifyTimeReached = System.currentTimeMillis() - connectionAttemptStartTime >= NOTIFY_CONNECT_FAILURE_MS;
                if (!notificationSent && notifyTimeReached && SystemTrayManager.isSupported() &&
                        !ApplicationCore.getInstance().getWindow().isShowing()) {
                    SystemTrayManager.getInstance().displayMessage("Failed to connect to Hardware Monitor", TrayIcon.MessageType.WARNING);
                    notificationSent = true;
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        establishConnectionThread.start();
    }

    // Write runs its own thread, picking up the queue of messages that need to be sent out
    private void runWriteThread() {
        if (writeThread != null && writeThread.isAlive()) {
            // The write thread is already running
            return;
        }

        writeThread = new Thread(() -> {
            while(connected && serialPort.isOpen() && !disconnecting) {
                try {
                    if(!write()) {
                        disconnecting = true;

                        // Comms failure occurred
                        DisconnectMessage out = new DisconnectMessage(ApplicationCore.s_getUUID(), true);
                        try {
                            boolean sent = attemptSend(out.write(), MAX_ATTEMPTS);
                            if (!sent) {
                                Logger.log(LogLevel.WARNING, LOGGER_TAG, "Failed graceful disconnect");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // 5. Close serial port
                        serialPort.closePort();
                        connected = false;
                        disconnecting = false;


                        // Display comms error. If docked, send notification
                        Platform.runLater(() -> {
                            ApplicationCore.s_setApplicationState(new InformationStateData("Communication Error", "Lost communication with Hardware Monitor", "Device List", event -> SerialScanner.handleScan()));
                        });

                        if(SystemTrayManager.isSupported() && !ApplicationCore.getInstance().getWindow().isShowing()) {
                            SystemTrayManager.getInstance().displayMessage("Lost communication with Hardware Monitor", TrayIcon.MessageType.WARNING);
                        }

                        runConnectThread(serialPort);

                        // Exit thread
                        break;
                    }
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        writeThread.start();
    }

    private ConnectionState checkReceived() {
        byte[] bytes = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(bytes, Message.NUM_BYTES);

        // Keep reading until we get the latest message (latest 255 bytes)
        if (serialPort.bytesAvailable() > 0) {
            return checkReceived();
        }

        if (numRead < Message.NUM_BYTES) {
            Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Time out on read [Port: %s] [Num Bytes Read: %d]", serialPort.getSystemPortName(), numRead);
            return ConnectionState.READ_TIMEOUT;
        }

        if (!Message.isValid(bytes)) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Bad checksum for confirmation message");
            return ConnectionState.BAD_RESPONSE_INVALID_CHECKSUM;
        }

        byte receivedType = Message.getType(bytes);
        if (receivedType == MessageType.VERSION_PARITY_RESPONSE) {
            VersionParityResponseMessage in = new VersionParityResponseMessage(bytes);
            if (!in.isAccepted()) {
                // todo: 'try again' feels bad. maybe highlight a bigger comms issue here
                return ConnectionState.BAD_RESPONSE_WRONG_MESSAGE;
            }

            // Read the next message because this one does not matter (if there is many version parity response
            // messages in the serial buffer, it will read all of them until we get to a real message).
            return checkReceived();
        }

        if (receivedType != MessageType.CONFIRMATION) {
            Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Expected message of type: %s, got %s", MessageType.asString(MessageType.CONFIRMATION), MessageType.asString(receivedType));
            return ConnectionState.BAD_RESPONSE_WRONG_MESSAGE;
        }

        ConfirmationMessage in = new ConfirmationMessage(bytes);
        if (in.getStatus() == Message.STATUS_BAD_RECEIVE) {
            Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Monitor reported message not received correctly");
            return ConnectionState.POOR_COMMUNICATION;
        }

        if (!connectedUUID.equals(in.getSenderUuid())) {
            Logger.log(LogLevel.WARNING, LOGGER_TAG, "Received confirmation message from a different monitor instance");
            return ConnectionState.BAD_RESPONSE_UNEXPECTED_DEVICE;
        }

        return ConnectionState.CONNECTED;
    }

    private boolean attemptSend(byte[] message, int maxAttempts) throws InterruptedException {
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            int numWrite = serialPort.writeBytes(message, Message.NUM_BYTES);
            if (numWrite < Message.NUM_BYTES) {
                Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Time out on write [Port: %s] [Num Bytes Wrote: %d]", serialPort.getSystemPortName(), numWrite);
                return false;
            }

            ConnectionState received = checkReceived();
            boolean sent;
            switch (received) {
                case CONNECTED:
                    sent = true;
                    break;
                case BAD_RESPONSE_INVALID_CHECKSUM:
                case BAD_RESPONSE_WRONG_MESSAGE:
                    // try again
                    sent = false;
                    break;
                default:
                    // A more serious communication issue occurred, attempt re-connect
                    return false;
            }

            String logState = sent ? "Successfully sent message" : "Failed to send message";
            byte type = Message.getType(message);
            String typeString = MessageType.asString(type);
            if (attempt > 0) {
                Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "%s [Type: %s] [Attempt: %d]", logState, typeString, attempt);
            } else {
                Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "%s [Type: %s]", logState, typeString, attempt);
            }

            // todo: if we have not received a good message within heartbeat period, disconnect and report error
            if (sent) {
                lastMessageSendMs = System.currentTimeMillis();
                return true;
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

        // Exit due to com failure
        return false;
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
           if (!attemptSend(message, MAX_ATTEMPTS)) {
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
        if (disconnecting) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Cannot send message (type: %s) while disconnecting", message.getTypeString());
            return false;
        }

        if (!connected || !serialPort.isOpen()) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Failed to send message (type: %s) because serial port is not connected", message.getTypeString());
            return false;
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
