package com.bennero.client.serial;

import com.bennero.client.core.ApplicationCore;
import com.bennero.client.network.NetworkScanner;
import com.bennero.client.states.SerialDeviceListStateData;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.fazecast.jSerialComm.SerialPort;

public class SerialScanner {
    private static final String LOGGER_TAG = NetworkScanner.class.getSimpleName();

    public static void handleScan() {
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Scanning USB ports for Hardware Monitors");

        SerialPort[] serialPorts = SerialPort.getCommPorts();

        // todo: FILTER THROUGH AND ONLY ALLOW RASPBERRY PI DEVICES TO HERE
        ApplicationCore.s_setApplicationState(new SerialDeviceListStateData(serialPorts));
    }
}
