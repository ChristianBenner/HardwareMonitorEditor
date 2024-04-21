package com.bennero.client.serial;

import com.bennero.client.Version;
import com.bennero.client.config.ProgramConfigManager;
import com.bennero.client.messages.VersionParityMessage;
import com.bennero.client.network.NetworkClient;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.networking.NetworkUtils;
import com.fazecast.jSerialComm.SerialPort;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;

public class SerialClient {
    private static final String LOGGER_TAG = SerialClient.class.getSimpleName();

    private static SerialClient instance = null;
    private SerialPort serialPort;
    private boolean connected;

    private SerialClient() {
        connected = false;
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
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 5000, 5000);

        // todo: version parity needs to be performed before allowing connection
        byte[] versionParityMessage = VersionParityMessage.create();
        serialPort.writeBytes(versionParityMessage, MESSAGE_NUM_BYTES);

        // Expect immediate response
        byte[] readBuffer = new byte[MESSAGE_NUM_BYTES];
        int numRead = serialPort.readBytes(readBuffer, MESSAGE_NUM_BYTES);
        if (numRead > 0) {
            System.out.println("Received data: " + new String(readBuffer, 0, numRead));
        }

        return true;
    }
}
