/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Additional terms included with this license are to:
 * - Preserve legal notices and author attributions such as this one. Do not remove the original author license notices
 *   from the program
 * - Preserve the donation button and its link to the original authors donation page (christianbenner35@gmail.com)
 * - Only break the terms if given permission from the original author christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.client.core;

import com.bennero.client.config.ProgramConfigManager;
import com.bennero.client.config.SaveManager;
import com.bennero.client.network.ConnectedEvent;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.network.NetworkScanner;
import com.bennero.client.serial.SerialScanner;
import com.bennero.client.states.*;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.MessageUtils;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.HEARTBEAT_TIMEOUT_MS;
import static com.bennero.common.networking.NetworkUtils.ip4AddressToString;

/**
 * ApplicationCore controls and starts the application including threads, GUI and sub-systems. The application on launch
 * will attempt to locate the previous save and connect to the previously connected hardware monitor. If there is no
 * previous save or it cannot be located, it will display the welcome screen and get the user to create one. If there is
 * no previously connected device it will scan the network for hardware monitors and list them allowing the user to
 * choose one to connect to.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class ApplicationCore extends Application {
    private static final String CLASS_NAME = ApplicationCore.class.getSimpleName();
    private static final String RES_PATH_PARAMETER = "-respath=";

    enum CommunicationMode {
        USB,
        IP,
    }

    private static final CommunicationMode COMMUNICATION_MODE = CommunicationMode.USB;

    public static ApplicationCore applicationCore = null;
    private static StateData currentStateData = null;

    private ProgramConfigManager programConfigManager;
    private Window window;
    private SystemTrayManager systemTrayManager;
    private SensorManager sensorManager;
    private SaveManager saveManager;

    /**
     * Construct the application and initialise core components such as the programs configuration
     *
     * @since 1.0
     */
    public ApplicationCore() {
        applicationCore = this;
        programConfigManager = ProgramConfigManager.getInstance();
        sensorManager = SensorManager.getInstance();
        saveManager = SaveManager.getInstance();

        final LogLevel LOG_LEVEL_ON_BOOTSTRAPPER_LAUNCH = DEBUG_BOOTSTRAPPER ? LogLevel.DEBUG : LogLevel.INFO;
        final LogLevel LOG_LEVEL = BOOTSTRAPPER_LAUNCH_REQUIRED ? LOG_LEVEL_ON_BOOTSTRAPPER_LAUNCH : LogLevel.DEBUG;
        Logger.setLogLevel(LOG_LEVEL);
    }

    /**
     * Get singleton instance of the application core
     *
     * @return The instance of application core
     * @since 1.0
     */
    public static ApplicationCore getInstance() {
        return applicationCore;
    }

    /**
     * Method designed to launch the application natively using JavaFX base class methods. This provides a way for the
     * native C# bootstrapper to request to start the program.
     *
     * @since 1.0
     */
    public static void launchApplication() {
        Logger.log(LogLevel.DEBUG, CLASS_NAME, "Received native launch request");
        try {
            ApplicationCore.launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StateData s_getApplicationState() {
        return getInstance().getApplicationState();
    }

    /**
     * Set the application state. This feature was implemented to allow for user interface to be created on demand,
     * meaning that the application does not need to initialise graphical components that are not in use. This reduces
     * hardware usage such as CPU and memory load.
     *
     * @param stateData The new state for the application
     * @since 1.0
     */
    public static void s_setApplicationState(StateData stateData) {
        getInstance().setApplicationState(stateData);
    }

    @Override

    public void start(Stage stage) {
        // Process parameters
        processParameters();

        systemTrayManager = SystemTrayManager.getInstance();
        systemTrayManager.addToSystemTray();

        Font.loadFont(getClass().getClassLoader().getResourceAsStream("Michroma.ttf"), 48);

        this.window = new Window(stage);
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        setApplicationState(new LoadingStateData("Launching Editor"));

        // Check if the program has been launched before
        if (!programConfigManager.doesFileExist() || !programConfigManager.isLastLoadedFilePathAvailable() ||
                !programConfigManager.isFileAreaPathAvailable()) {
            // Check the program configuration to see if the application has been launched before
            // There is no program config, or there is no file paths set-up. This means this could be the first time the
            // application has been launched on the device
            if(!programConfigManager.doesFileExist()) {
                Logger.log(LogLevel.DEBUG, CLASS_NAME, "Program configuration does not exist");
            } else if (!programConfigManager.isLastLoadedFilePathAvailable()) {
                Logger.log(LogLevel.WARNING, CLASS_NAME, "Program configuration points to a non-existent " +
                        "layout");
            } else if(!programConfigManager.isFileAreaPathAvailable()) {
                Logger.log(LogLevel.WARNING, CLASS_NAME, "Program configuration points to a non-existent " +
                        "layout folder");
            }

            setApplicationState(new WelcomePageStateData());
            window.show();
        } else if (programConfigManager.doesFileExist() && !programConfigManager.containsCompleteConnectionInfo()) {
            // If the program configuration exists but there is no previously connected device, the user has configured
            // there file area (its not the first time launching), but they never connected to a device. In this
            // scenario we need to show the GUI and start a scan

            switch (COMMUNICATION_MODE) {
                case USB:
                    Logger.log(LogLevel.DEBUG, CLASS_NAME, "Program configuration does not contain all of the required serial information, scanning ports for Hardware Monitors");
                    SerialScanner.handleScan();
                    window.show();
                    break;
                case IP:
                    Logger.log(LogLevel.DEBUG, CLASS_NAME, "Program configuration does not contain all of the required connection information, scanning network for Hardware Monitors");
                    NetworkScanner.handleScan();
                    window.show();
                    break;
            }
        } else {
            // If the program has been set-up/launched before and has previously connected to a device, run in system
            // tray and attempt to connect to the device
            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Program configuration contains all necessary data, " +
                    "attempting to connect to previously used Hardware Monitor");
            startNetworkClient(true);

            // If the system tray is not supported then show the window
            if (!SystemTrayManager.isSupported()) {
                window.show();
            }
        }

        // Add the sensors to the application and start the thread that updates them
        sensorManager.addNativeSensors();
        sensorManager.startSensorUpdateThread();
    }

    @Override
    public void stop() throws Exception {
        Logger.log(LogLevel.INFO, CLASS_NAME, "Stopping application");
        systemTrayManager.removeFromSystemTray();
        super.stop();
        System.exit(0);
    }

    public StateData getApplicationState() {
        return this.currentStateData;
    }

    public void setApplicationState(StateData stateData) {
        if (currentStateData == null) {
            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Setting initial state " + stateData.getName());
        } else {
            Logger.log(LogLevel.DEBUG, CLASS_NAME, "Changing state " + currentStateData.getName() + " --> " +
                    stateData.getName());
        }

        currentStateData = stateData;
        if (window.isShowing()) {
            window.changeGuiState(currentStateData);
        }
    }

    public Window getWindow() {
        return window;
    }

    private void processParameters() {
        List<String> parameterList = super.getParameters().getRaw();
        Logger.log(LogLevel.DEBUG, CLASS_NAME, "Parameter List Size: " + parameterList.size());
        for (int i = 0; i < parameterList.size(); i++) {
            String parameter = parameterList.get(i);
            if (parameter.startsWith(RES_PATH_PARAMETER)) {
                String fileArea = parameter.substring(RES_PATH_PARAMETER.length());
                Logger.log(LogLevel.INFO, CLASS_NAME, "Set resource path from parameter to: " + fileArea);
            }
        }
    }

    private void sendCurrentSave() {
        ArrayList<PageData> pageDataList = saveManager.getSaveData().getPageDataList();

        // Send all of the pages to the monitor
        if (DataClient.isConnected()) {
            for (PageData pageData : pageDataList) {
                DataClient.writePageMessage(pageData);

                // Send all sensors contained in the pages to the monitor
                for (Sensor sensor : pageData.getSensorList()) {
                    DataClient.writeSensorSetupMessage(sensor, (byte) pageData.getUniqueId());
                }
            }
        }
    }

    public void onConnected() {
        // If a save is already loaded, send that to the Display, otherwise try to load the previous
        if(saveManager.isSaveLoaded()) {
            sendCurrentSave();
        } else {
            // If previous save failed to be loaded, give the option to open or create a new save
            if(saveManager.loadPreviousSave()) {
                sendCurrentSave();
            } else {
                setApplicationState(new LoadingStateData("Save Error", "Failed to load previous save"));

                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to load previous save");

                // Ask before deleting
                ButtonType openSaveButtonType = new ButtonType("Open Save");
                ButtonType newSaveButtonType = new ButtonType("New Save");

                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load previous save", openSaveButtonType, newSaveButtonType);
                alert.setContentText("There was an error loading the previously used save Please locate it, open a different save, or create a new save.");

                boolean selectedOption = false;

                while (!selectedOption) {
                    alert.showAndWait();

                    if (alert.getResult() == openSaveButtonType) {
                        selectedOption = saveManager.displayOpenSaveUI();
                    } else if (alert.getResult() == newSaveButtonType) {
                        selectedOption = saveManager.displayNewSaveUI();
                    }
                }
            }
        }

        // If the save contains no pages, we should open the page editor so that the user can add some
        if (saveManager.getSaveData().getPageDataList().isEmpty()) {
            window.show();
        }

        setApplicationState(new PageOverviewStateData());
    }

    private void onConnecting(ConnectedEvent connectedEvent) {
        // Display connecting text
        setApplicationState(new LoadingStateData("Connecting...", connectedEvent.
                getConnectionInformation().getHostname() + " (" + ip4AddressToString(connectedEvent.
                getConnectionInformation().getIp4Address()) + ")"));
    }

    private void onVersionMismatch(ConnectedEvent connectedEvent,
                                   List<ConnectionInformation> availableConnections) {
        setApplicationState(new InformationStateData(
                "Connection Refused", "Could not connect to " + connectedEvent.
                getConnectionInformation().getHostname() + " because the client version (v" +
                VERSION_MAJOR + "" + VERSION_MINOR + "." + VERSION_PATCH +
                ") is incompatible with the server (v" +
                connectedEvent.getMajorServerVersion() + "." +
                connectedEvent.getMinorServerVersion() + "." +
                connectedEvent.getPatchServerVersion() + ")", "Device List",
                event -> setApplicationState(new ConnectionListStateData(availableConnections))));
    }

    private void networkClientConnect(NetworkClient networkClient,
                                      ConnectionInformation lastConnectedDevice,
                                      List<ConnectionInformation> availableConnections) {
        networkClient.connect(lastConnectedDevice, connectedEvent ->
        {
            switch (connectedEvent.getConnectionStatus()) {
                case CONNECTING:
                    onConnecting(connectedEvent);
                    break;
                case CONNECTED:
                    // Load save
                    onConnected();
                    break;
                case FAILED:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to connect to device " + connectedEvent.
                            getConnectionInformation().getHostname() + " (" +
                            ip4AddressToString(connectedEvent.getConnectionInformation().getIp4Address()) +
                            ") despite finding it on the network. It may be in use by another editor");

                    // Message pop-up that no connections have been found
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to Connect", ButtonType.OK);
                    alert.setContentText("Failed to connect to device " + connectedEvent.
                            getConnectionInformation().getHostname() + " (" +
                            ip4AddressToString(connectedEvent.getConnectionInformation().getIp4Address()) +
                            ") despite finding it on the network. It may be in use by another editor");
                    alert.showAndWait();

                    // Display everything in the broadcast reply data list on the connection page
                    setApplicationState(new ConnectionListStateData(availableConnections));
                    break;
                case VERSION_MISMATCH:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Connection refused: Could not connect to '" +
                                    connectedEvent.getConnectionInformation().getHostname() +
                                    "' because the client version (v" + VERSION_MAJOR + "" + VERSION_MINOR + "." +
                                    VERSION_PATCH + ") is incompatible with the server (v" +
                                    connectedEvent.getMajorServerVersion() + "." +
                                    connectedEvent.getMinorServerVersion() + "." +
                                    connectedEvent.getPatchServerVersion() + ")");
                    onVersionMismatch(connectedEvent, availableConnections);
                    break;
                case IN_USE:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Connection refused: Could not connect to " +
                            connectedEvent.getConnectionInformation().getHostname() +
                            " because it is in use by another device (" + connectedEvent.getCurrentlyConnectedHostname()
                            + ")");
                    setApplicationState(new InformationStateData(
                            "Connection Refused", "Could not connect to " + connectedEvent.
                            getConnectionInformation().getHostname() +
                            " because it is in use by another device (" + connectedEvent.
                            getCurrentlyConnectedHostname() + ")", "Device List",
                            event -> setApplicationState(new ConnectionListStateData(availableConnections))));
                    break;
                case CONNECTION_REFUSED:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Connection refused: Failed to connect");
                    setApplicationState(new InformationStateData(
                            "Connection Refused", "Could not connect to " + connectedEvent.
                            getConnectionInformation().getHostname(), "Device List",
                            event -> setApplicationState(new ConnectionListStateData(availableConnections))));
                    break;
                case HEARTBEAT_TIMEOUT:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Lost communication: Heartbeat timeout reached (" +
                            HEARTBEAT_TIMEOUT_MS + "ms)");
                    setApplicationState(new LoadingStateData("Lost Communication",
                            "No heartbeat message was received from " + connectedEvent.
                                    getConnectionInformation().getHostname() + " for " +
                                    HEARTBEAT_TIMEOUT_MS + "ms. Attempting to reconnect",
                            "Device List",
                            event -> setApplicationState(new ConnectionListStateData(availableConnections))));

                    // Attempt to reconnect to the device
                    startNetworkClient(false);
                    break;
                case UNEXPECTED_DISCONNECT:
                    Logger.log(LogLevel.ERROR, CLASS_NAME, "Lost communication: Unexpected disconnect");
                    setApplicationState(new LoadingStateData("Lost Communication",
                            connectedEvent.getConnectionInformation().getHostname() +
                                    " Disconnected unexpectedly. Attempting to reconnect",
                            "Device List",
                            event -> setApplicationState(new ConnectionListStateData(availableConnections))));

                    // Attempt to reconnect to the device
                    startNetworkClient(false);
                    break;
            }
        });
    }

    private void startNetworkClient(boolean displayScannedDevices) {
        ConnectionInformation lastConnectedMonitorInfo = programConfigManager.getConnectionInformation();
        List<ConnectionInformation> availableConnections = new ArrayList<>();

        // First, try to scan and connect. If that fails, scan and present results on connection list
        // Scan network for device with that MAC address
        NetworkScanner networkScanner = new NetworkScanner();

        // Create the network scan state data. If the show other devices button is selected then change state to the
        // connection list
        NetworkScanStateData networkScanStateData = new NetworkScanStateData("For last used device (" +
                programConfigManager.getLastConnectedHostname() + ")", event -> networkScanner.stopScanning());

        if (displayScannedDevices) {
            setApplicationState(networkScanStateData);
        }

        networkScanner.scan(0, scanReplyMessage ->
        {
            // Before adding to available connections, check if a connection in the list with the same
            // MAC address already exists. This is so when multiple broadcast messages are sent out,
            // only one of the same device will show up in the connections list
            boolean found = false;
            for (int i = 0; i < availableConnections.size(); i++) {
                if (NetworkUtils.doAddressesMatch(availableConnections.get(i).getMacAddress(), scanReplyMessage.getMacAddress())) {
                    found = true;
                }
            }

            // Check to see if a response has matched the MAC address that we are looking for
            if (NetworkUtils.doAddressesMatch(scanReplyMessage.getMacAddress(),
                    lastConnectedMonitorInfo.getMacAddress())) {
                networkScanStateData.setLastConnectedDevice(scanReplyMessage);
                networkScanStateData.setConnectToLast(true);
                networkScanner.stopScanning();
            }

            // If it is a different one, add it to the list of available connections
            if (!found) {
                availableConnections.add(scanReplyMessage);
                Logger.log(LogLevel.INFO, CLASS_NAME,
                        "Received a broadcast reply message from a hardware monitor: VERSION[" +
                        scanReplyMessage.getMajorVersion() + "." + scanReplyMessage.getMinorVersion() + "." +
                        scanReplyMessage.getPatchVersion() + "] IP4[" +
                        NetworkUtils.ip4AddressToString(scanReplyMessage.getIp4Address()) + "], MAC[" +
                        NetworkUtils.macAddressToString(scanReplyMessage.getMacAddress()) + "], HOSTNAME[" +
                        scanReplyMessage.getHostname() + "]");

                Platform.runLater(() -> networkScanStateData.setNumberOfFoundDevices(networkScanStateData.
                        getNumberOfFoundDevices() + 1));
            }
        }, endScan ->
        {
            // Only connect to last on end of scan if it has been enabled (because the last connected device was found
            // during a network scan). Otherwise do nothing because it is up to the user to select what device to
            // connect to via the connection list which will be displayed instead
            if (!networkScanStateData.shouldConnectToLast()) {
                setApplicationState(new ConnectionListStateData(availableConnections));
            } else {
                NetworkClient networkClient = NetworkClient.getInstance();
                MessageUtils.Compatibility compatibility = MessageUtils.isVersionCompatible(VERSION_MAJOR,
                        VERSION_MINOR, networkScanStateData.getLastConnectedDevice().getMajorVersion(),
                        networkScanStateData.getLastConnectedDevice().getMinorVersion());

                if (compatibility == MessageUtils.Compatibility.COMPATIBLE) {
                    // Connect to new IP4 address of the device
                    networkClientConnect(networkClient, networkScanStateData.getLastConnectedDevice(),
                            availableConnections);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Incompatible Monitor Version",
                            ButtonType.OK);

                    if (compatibility == MessageUtils.Compatibility.NEWER) {
                        alert.setContentText("Cannot connect to previously connected Hardware Monitor " +
                                networkScanStateData.getLastConnectedDevice() +
                                " because it is running an older software version than the editor");
                    } else if (compatibility == MessageUtils.Compatibility.OLDER) {
                        alert.setContentText("Cannot connect to previously connected Hardware Monitor " +
                                networkScanStateData.getLastConnectedDevice() +
                                " because it is running an newer software version than the editor");
                    }

                    alert.showAndWait();

                    // Display everything in the broadcast reply data list on the connection page
                    setApplicationState(new ConnectionListStateData(availableConnections));
                    window.show();
                }
            }
        });
    }
}
