package com.bennero.client.serial;

import com.bennero.client.core.ApplicationCore;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.network.NetworkScanner;
import com.bennero.client.states.ConnectionListStateData;
import com.bennero.client.states.LoadingStateData;
import com.bennero.client.states.SerialDeviceListStateData;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import com.fazecast.jSerialComm.SerialPort;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;

public class SerialScanner {
    private static final String LOGGER_TAG = NetworkScanner.class.getSimpleName();

    public static void handleScan() {
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Scanning USB ports for Hardware Monitors");

        SerialPort[] serialPorts = SerialPort.getCommPorts();

        // todo: FILTER THROUGH AND ONLY ALLOW RASPBERRY PI DEVICES TO HERE
        ApplicationCore.s_setApplicationState(new SerialDeviceListStateData(serialPorts));

//
//
//
//        // Now scan the ports for devices
//        List<ConnectionInformation> availableConnections = new ArrayList<>();
//
//        // First, try to scan and connect. If that fails, scan and present results on connection list
//        // Scan network for device with that MAC address
//        NetworkScanner networkScanner = new NetworkScanner();
//
//        ApplicationCore.s_setApplicationState(new LoadingStateData("Scanning network", "For hardware monitor devices"));
//        networkScanner.scan(5, scanReplyMessage ->
//        {
//            // Before adding to available connections, check if a connection in the list with the same
//            // MAC address already exists. This is so when multiple broadcast messages are sent out,
//            // only one of the same device will show up in the connections list
//            boolean found = false;
//            for (int i = 0; i < availableConnections.size(); i++) {
//                if (NetworkUtils.doAddressesMatch(availableConnections.get(i).getMacAddress(),
//                        scanReplyMessage.getMacAddress())) {
//                    found = true;
//                }
//            }
//
//            if (!found) {
//                Logger.log(LogLevel.INFO, LOGGER_TAG,
//                        "Received a broadcast reply message from a hardware monitor: VERSION[" +
//                                scanReplyMessage.getMajorVersion() + "." + scanReplyMessage.getMinorVersion() + "." +
//                                scanReplyMessage.getPatchVersion() + "] IP4[" +
//                                NetworkUtils.ip4AddressToString(scanReplyMessage.getIp4Address()) + "], MAC[" +
//                                NetworkUtils.macAddressToString(scanReplyMessage.getMacAddress()) + "], HOSTNAME[" +
//                                scanReplyMessage.getHostname() + "]");
//                availableConnections.add(scanReplyMessage);
//            }
//        }, event ->
//        {
//            if (!NetworkClient.getInstance().isConnected()) {
//                ApplicationCore.s_setApplicationState(new ConnectionListStateData(availableConnections));
//
//                if (availableConnections.isEmpty()) {
//                    // Message pop-up that no connections have been found
//                    Alert alert = new Alert(Alert.AlertType.WARNING, "No Hardware Monitors Found", ButtonType.OK);
//                    alert.setContentText("Scanning the network revealed no hardware monitors, please make sure that hardware monitor devices are online and running and then attempt a scan");
//                    alert.showAndWait();
//                }
//            }
//        });
    }
}
