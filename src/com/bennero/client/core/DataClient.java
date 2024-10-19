package com.bennero.client.core;

import com.bennero.client.network.NetworkClient;
import com.bennero.client.serial.SerialClient;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.messages.FileTransferMessage;
import com.bennero.common.messages.Message;

public class DataClient {
    // todo: write a dataclient base class. each needs:
    // - isConnected
    // - getConnectionName
    // - writeMessage
    // - writeFile


    public static String getConnectionName() {
        if (NetworkClient.getInstance().isConnected()) {
            return NetworkClient.getInstance().getConnectionName();
        }

        if (SerialClient.getInstance().isConnected()) {
            return SerialClient.getInstance().getConnectionName();
        }

        return "";
    }

    public static boolean isConnected() {
        if(NetworkClient.getInstance().isConnected() || SerialClient.getInstance().isConnected()) {
            return true;
        }

        return false;
    }

    public static void disconnect() {
        if (NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().disconnect();
        }

        if (SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().disconnect();
        }
    }

    public static void writeMessage(Message message) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeMessage(message);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeMessage(message);
        }
    }

    // Files work differently to other messages as they do not fit in the regular message size
    public static void writeFile(FileTransferMessage message, byte[] fileBytes) {
        if(NetworkClient.getInstance().isConnected()) {
            // todo: implement
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeFileMessage(message, fileBytes);
        }
    }
}
