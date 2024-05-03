package com.bennero.client.core;

import com.bennero.client.network.NetworkClient;
import com.bennero.client.serial.SerialClient;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;

public class DataClient {
    public static boolean isConnected() {
        if(NetworkClient.getInstance().isConnected() || SerialClient.getInstance().isConnected()) {
            return true;
        }

        return false;
    }

    public static void writeRemovePageMessage(byte pageId) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeRemovePageMessage(pageId);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeRemovePageMessage(pageId);
        }
    }

    public static void writeRemoveSensorMessage(byte sensorId, byte pageId) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeRemoveSensorMessage(sensorId, pageId);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeRemoveSensorMessage(sensorId, pageId);
        }
    }

    public static void writePageMessage(PageData pageData) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writePageMessage(pageData);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writePageMessage(pageData);
        }
    }

    public static void writeSensorSetupMessage(Sensor sensor, byte pageId) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeSensorSetupMessage(sensor, pageId);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeSensorSetupMessage(sensor, pageId);
        }
    }

    public static void writeSensorTransformationMessage(Sensor sensor, byte pageId) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeSensorTransformationMessage(sensor, pageId);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeSensorTransformationMessage(sensor, pageId);
        }
    }

    public static void writeSensorValueMessage(int sensorId, float value) {
        if(NetworkClient.getInstance().isConnected()) {
            NetworkClient.getInstance().writeSensorValueMessage(sensorId, value);
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeSensorValueMessage(sensorId, value);
        }
    }

    public static void writeFileMessage(int size, String name, byte[] bytes, byte type) {
        if(NetworkClient.getInstance().isConnected()) {
            // todo: implement
        } else if(SerialClient.getInstance().isConnected()) {
            SerialClient.getInstance().writeFileMessage(size, name, bytes, type);
        }
    }
}
