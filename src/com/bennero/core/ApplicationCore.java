/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.core;

import com.bennero.Version;
import com.bennero.bootstrapper.Native;
import com.bennero.bootstrapper.SensorRequest;
import com.bennero.networking.ConnectionInformation;
import com.bennero.networking.NetworkUtils;
import com.bennero.common.SensorType;
import com.bennero.common.TransitionType;
import com.bennero.config.ProgramConfigManager;
import com.bennero.config.SaveManager;
import com.bennero.network.ConnectedEvent;
import com.bennero.network.NetworkClient;
import com.bennero.network.NetworkScanner;
import com.bennero.states.*;
import com.bennero.util.SystemTrayUtils;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.bennero.Version.*;
import static com.bennero.common.Constants.*;
import static com.bennero.networking.NetworkUtils.ip4AddressToString;

/**
 * ApplicationCore controls and starts the application including threads, GUI and sub-systems. The application on launch
 * will attempt to locate the previous save and connect to the previously connected hardware monitor. If there is no
 * previous save or it cannot be located, it will display the welcome screen and get the user to create one. If there is
 * no previously connected device it will scan the network for hardware monitors and list them allowing the user to
 * choose one to connect to.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ApplicationCore extends Application
{
    public static final int WINDOW_WIDTH_PX = 800;
    public static final int WINDOW_HEIGHT_PX = 480;

    private static final String RES_PATH_PARAMETER = "-respath=";
    private static final String WINDOW_TITLE = "Hardware Monitor Editor v" + VERSION_MAJOR + "." + VERSION_MINOR + "." +
            VERSION_PATCH;

    public static ApplicationCore applicationCore = null;
    private static StateData currentStateData;

    private StackPane basePane;
    private Node currentPage;
    private Stage stage;
    private ProgramConfigManager programConfigManager;
    private TrayIcon trayIcon;
    private String titleSaveString;

    public ApplicationCore()
    {
        titleSaveString = "";
        System.out.println("***** APP INIT ******");
        applicationCore = this;
        programConfigManager = ProgramConfigManager.getInstance();
    }

    public static void launchApplication()
    {
        System.out.println("Received native launch request");

        try
        {
            ApplicationCore.launch();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void setTitleSaveString(String saveString)
    {
        applicationCore.titleSaveString = saveString;
        applicationCore.stage.setTitle(WINDOW_TITLE + ": " + saveString);
    }

    public static void changeApplicationState(StateData stateData)
    {
        if (currentStateData == null)
        {
            System.out.println("Setting initial state " + stateData.getName());
        }
        else
        {
            System.out.println("Changing state " + currentStateData.getName() + " --> " + stateData.getName());
        }

        currentStateData = stateData;
        if (applicationCore.stage.isShowing())
        {
            applicationCore.changeGuiState(currentStateData);
        }
    }

    public static void show()
    {
        Platform.runLater(() ->
        {
            if (currentStateData != null)
            {
                applicationCore.initGui();

                // Load GUI that is associated to the current application state
                applicationCore.basePane.getChildren().clear();
                applicationCore.basePane.getChildren().add(currentStateData.createGUI());
                applicationCore.stage.show();
            }
            else
            {
                System.err.println("ERROR: No current state data available, cannot show display");
            }
        });
    }

    public static ReadOnlyDoubleProperty getWidthProperty()
    {
        return applicationCore.stage.widthProperty();
    }

    public static void openBrowser(String url)
    {
        applicationCore.getHostServices().showDocument(url);
    }

    public static File showDirectorySelector()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        return directoryChooser.showDialog(applicationCore.stage);
    }

    public static File showFileSelector()
    {
        ProgramConfigManager programConfigManager = ProgramConfigManager.getInstance();
        FileChooser fileChooser = new FileChooser();

        // If the program config manager contains a file area path, open the file chooser window there
        if (programConfigManager.isFileAreaPathAvailable())
        {
            File saveAreaFile = new File(programConfigManager.getFileAreaPath());
            if (saveAreaFile.exists() && saveAreaFile.isDirectory())
            {
                fileChooser.setInitialDirectory(saveAreaFile);
            }
        }

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Benner Hardware Monitor Save (*.bhwms)", "*.bhwms");
        fileChooser.getExtensionFilters().add(extensionFilter);

        return fileChooser.showOpenDialog(applicationCore.stage);
    }

    public static void onConnected()
    {
        // Load save
        if (!SaveManager.getInstance().loadPreviousSave())
        {
            ApplicationCore.changeApplicationState(new LoadingStateData("Save Error", "Failed to load previous save"));

            System.err.println("Failed to load previous save");

            // Ask before deleting
            ButtonType openSaveButtonType = new ButtonType("Open Save");
            ButtonType newSaveButtonType = new ButtonType("New Save");

            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load previous save", openSaveButtonType, newSaveButtonType);
            alert.setContentText("There was an error loading the previously used save Please locate it, open a different save, or create a new save.");

            boolean selectedOption = false;

            while (!selectedOption)
            {
                alert.showAndWait();

                if (alert.getResult() == openSaveButtonType)
                {
                    selectedOption = SaveManager.displayOpenSaveUI();
                }
                else if (alert.getResult() == newSaveButtonType)
                {
                    selectedOption = SaveManager.displayNewSaveUI();
                }
            }
        }

        // If the save contains no pages, we should open the page editor so that the user can add some
        if (SaveManager.getInstance().getSaveData().getPageDataList().isEmpty())
        {
            ApplicationCore.show();
        }

        ApplicationCore.changeApplicationState(new PageOverviewStateData());
    }

    public static void onConnecting(ConnectedEvent connectedEvent)
    {
        // Display connecting text
        ApplicationCore.changeApplicationState(new LoadingStateData("Connecting...", connectedEvent.
                getConnectionInformation().getHostname() + " (" + ip4AddressToString(connectedEvent.
                getConnectionInformation().getIp4Address()) + ")"));
    }

    public static void onVersionMismatch(ConnectedEvent connectedEvent,
                                         List<ConnectionInformation> availableConnections)
    {
        ApplicationCore.changeApplicationState(new InformationStateData(
                "Connection Refused", "Could not connect to " + connectedEvent.
                getConnectionInformation().getHostname() + " because the client version (v" +
                VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH +
                ") is incompatible with the server (v" +
                connectedEvent.getMajorServerVersion() + "." +
                connectedEvent.getMinorServerVersion() + "." +
                connectedEvent.getPatchServerVersion() + ")", "Device List",
                event -> changeApplicationState(new ConnectionListStateData(availableConnections))));
    }

    public void initGui()
    {
        if (basePane == null)
        {
            basePane = new StackPane();
            Scene uiScene = new Scene(basePane, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);
            uiScene.getStylesheets().add("stylesheet.css");
            basePane.setId("standard-pane");

            if (!titleSaveString.isEmpty())
            {
                stage.setTitle(WINDOW_TITLE + ": " + titleSaveString);
            }
            else
            {
                stage.setTitle(WINDOW_TITLE);
            }

            stage.setScene(uiScene);
            stage.setOnCloseRequest(windowEvent -> destroyGui());
        }
    }

    public void destroyGui()
    {
        stage.hide();
        currentPage = null;
        basePane = null;
    }

    public void changeGuiState(StateData newStateData)
    {
        initGui();

        Node newPage = newStateData.createGUI();
        basePane.getChildren().add(newPage);

        Transition transition = TransitionType.getTransition(newStateData.getTransitionType(), 1000,
                basePane, newPage);
        transition.setOnFinished(actionEvent1 ->
        {
            basePane.getChildren().remove(currentPage);
            currentPage = newPage;
        });
        transition.play();
    }

    @Override
    public void stop() throws Exception
    {
        System.out.println("Stop called");
        SystemTrayUtils.removeFromSystemTray(trayIcon);
        super.stop();
        System.exit(0);
    }

    @Override
    public void start(Stage stage)
    {
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("Michroma.ttf"), 48);

        System.out.println("***** START ******");
        // Process parameters
        List<String> parameterList = super.getParameters().getRaw();
        System.out.println("Parameter List Size: " + parameterList.size());
        for (int i = 0; i < parameterList.size(); i++)
        {
            String parameter = parameterList.get(i);
            if (parameter.startsWith(RES_PATH_PARAMETER))
            {
                String fileArea = parameter.substring(RES_PATH_PARAMETER.length());
                System.out.println("Set resource path from parameter to: " + fileArea);
            }
        }

        Platform.setImplicitExit(false);
        this.stage = stage;
        this.stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        changeApplicationState(new LoadingStateData("Launching Editor"));

        try
        {
            trayIcon = SystemTrayUtils.addToSystemTray();

            // Removes the system tray icon if the application is terminated
            Runtime.getRuntime().addShutdownHook(new Thread(() -> SystemTrayUtils.removeFromSystemTray(trayIcon)));
        }
        catch (Exception e)
        {
            // If adding to system tray fails, we should show the GUI
            e.printStackTrace();
        }

        if (!programConfigManager.doesFileExist() || !programConfigManager.isLastLoadedFilePathAvailable() ||
                !programConfigManager.isFileAreaPathAvailable())
        {
            // Check the program configuration to see if the application has been launched before
            // There is no program config, or there is no file paths set-up. This means this could be the first time the
            // application has been launched on the device
            System.out.println("Program Configuration does not required save location information, displaying " +
                    "welcome page");
            changeApplicationState(new WelcomePageStateData());
            show();
        }
        else if (programConfigManager.doesFileExist() && !programConfigManager.containsCompleteConnectionInfo())
        {
            // If the program configuration exists but there is no previously connected device, the user has configured
            // there file area (its not the first time launching), but they never connected to a device. In this
            // scenario we need to show the GUI and start a scan
            System.out.println("Program Configuration does not contain all of the required connection information, " +
                    "starting network scan");
            NetworkScanner.handleScan();
            show();
        }
        else
        {
            // If the program has been set-up/launched before and has previously connected to a device, run in system
            // tray and attempt to connect to the device
            System.out.println("Program Configuration contains all necessary data, attempting to connect to " +
                    "previously used Hardware Monitor");
            startNetworkClient(true);
        }

        addNativeSensors();

        Thread thread = new Thread(() ->
        {
            try
            {
                while (true)
                {
                    Thread.sleep(SENSOR_POLL_RATE_MS);

                    Platform.runLater(() ->
                    {
                        if (NetworkClient.getInstance().isConnected())
                        {
                            Native.updateSensors();
                        }
                    });
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
                {
                    System.err.println("No NativeAddSensors method. System not launched with the bootstrapper");
                    e.printStackTrace();

                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update sensors");
                    alert.setTitle("Hardware Monitor Error");
                    alert.setHeaderText("Failed to update sensors");
                    alert.setContentText("There was an error updating the sensor data due to failed communication with " +
                            "the native interface provided by the bootstrapper application (ERROR CODE: " +
                            EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED + ")");
                    alert.showAndWait();
                    System.exit(EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED);
                }
                else
                {
                    e.printStackTrace();
                    System.err.println("Failed to update sensors. Native interface not working correctly");
                }
            }
        });

        if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
        {
            thread.start();
        }
        else
        {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No bootstrapper enabled");
            alert.setTitle("Hardware Monitor Warning");
            alert.setHeaderText("No bootstrapper enabled");
            alert.setContentText("The bootstrapper is disabled, meaning that the application will not poll for " +
                    "hardware data and therefor not update any sensors with real data. This warning is for " +
                    "developers only, if you are seeing this as a user, somebody built the software wrong! Set " +
                    "BOOTSTRAPPER_LAUNCH_REQUIRED to false");
            alert.showAndWait();
        }
    }

    private void addNativeSensors()
    {
        try
        {
            Native.addSensors();
        }
        catch (UnsatisfiedLinkError e)
        {
            if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
            {
                System.err.println("No NativeAddSensors method. System not launched with the bootstrapper");
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to retrieve sensor data");
                alert.setTitle("Hardware Monitor Error");
                alert.setHeaderText("Failed to retrieve sensor data");
                alert.setContentText("There was an error loading the sensor data due to a communication problem with " +
                        "the native interface provided by the bootstrapper application. Do not attempt to run the JAR " +
                        "file without the bootstrapper. If you see this error for any other reason please contact " +
                        "Bennero support (ERROR CODE: " + EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED + ")");
                alert.showAndWait();

                System.exit(EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED);
            }
            else
            {
                // Add debug sensors
                for (int i = 0; i < 6; i++)
                {
                    new SensorRequest(i, "Core #" + i + " Temp", 100.0f, SensorType.TEMPERATURE,
                            "DEBUG_CPU", 25.0f);
                }

                new SensorRequest(6, "Core Clock", 1800.0f, SensorType.CLOCK,
                        "DEBUG_CPU", 25.0f);
                new SensorRequest(7, "Memory Clock", 1250.0f, SensorType.CLOCK,
                        "DEBUG_CPU", 1105.0f);
                new SensorRequest(8, "Core Utilisation", 100.0f, SensorType.LOAD,
                        "DEBUG_CPU", 53.0f);
                new SensorRequest(9, "Power Draw", 300.0f, SensorType.POWER,
                        "DEBUG_CPU", 125.0f);
            }
        }
    }

    private void networkClientConnect(NetworkClient networkClient,
                                      ConnectionInformation lastConnectedDevice,
                                      List<ConnectionInformation> availableConnections)
    {
        networkClient.connect(lastConnectedDevice, connectedEvent ->
        {
            switch (connectedEvent.getConnectionStatus())
            {
                case CONNECTING:
                    onConnecting(connectedEvent);
                    break;
                case CONNECTED:
                    // Load save
                    onConnected();
                    break;
                case FAILED:
                    System.err.println("Failed to connect to device " + connectedEvent.
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
                    changeApplicationState(new ConnectionListStateData(availableConnections));
                    break;
                case VERSION_MISMATCH:
                    onVersionMismatch(connectedEvent, availableConnections);
                    break;
                case IN_USE:
                    ApplicationCore.changeApplicationState(new InformationStateData(
                            "Connection Refused", "Could not connect to " + connectedEvent.
                            getConnectionInformation().getHostname() +
                            " because it is in use by another device (" + connectedEvent.
                            getCurrentlyConnectedHostname() + ")", "Device List",
                            event -> changeApplicationState(new ConnectionListStateData(availableConnections))));
                    break;
                case CONNECTION_REFUSED:
                    ApplicationCore.changeApplicationState(new InformationStateData(
                            "Connection Refused", "Could not connect to " + connectedEvent.
                            getConnectionInformation().getHostname(), "Device List",
                            event -> changeApplicationState(new ConnectionListStateData(availableConnections))));
                    break;
                case HEARTBEAT_TIMEOUT:
                    ApplicationCore.changeApplicationState(new LoadingStateData("Lost Communication",
                            "No heartbeat message was received from " + connectedEvent.
                                    getConnectionInformation().getHostname() + " for " +
                                    HEARTBEAT_TIMEOUT_MS + "ms. Attempting to reconnect",
                            "Device List",
                            event -> changeApplicationState(new ConnectionListStateData(availableConnections))));
                    System.out.println("NO HEARTBEAT RECEIVED");

                    // Attempt to reconnect to the device
                    startNetworkClient(false);
                    break;
                case UNEXPECTED_DISCONNECT:
                    System.out.println("LOST CONNECTION TO HARDWARE MONITOR");
                    ApplicationCore.changeApplicationState(new LoadingStateData("Lost Communication",
                            connectedEvent.getConnectionInformation().getHostname() +
                                    " Disconnected unexpectedly. Attempting to reconnect",
                            "Device List",
                            event -> changeApplicationState(new ConnectionListStateData(availableConnections))));

                    // Attempt to reconnect to the device
                    startNetworkClient(false);
                    break;
            }
        });
    }

    private void startNetworkClient(boolean displayScannedDevices)
    {
        ConnectionInformation lastConnectedMonitorInfo = programConfigManager.getConnectionInformation();
        List<ConnectionInformation> availableConnections = new ArrayList<>();

        // First, try to scan and connect. If that fails, scan and present results on connection list
        // Scan network for device with that MAC address
        NetworkScanner networkScanner = new NetworkScanner();

        // Create the network scan state data. If the show other devices button is selected then change state to the
        // connection list
        NetworkScanStateData networkScanStateData = new NetworkScanStateData("For last used device (" +
                programConfigManager.getLastConnectedHostname() + ")", event -> networkScanner.stopScanning());

        if (displayScannedDevices)
        {
            ApplicationCore.changeApplicationState(networkScanStateData);
        }

        networkScanner.scan(0, scanReplyMessage ->
        {
            // Before adding to available connections, check if a connection in the list with the same
            // MAC address already exists. This is so when multiple broadcast messages are sent out,
            // only one of the same device will show up in the connections list
            boolean found = false;
            for (int i = 0; i < availableConnections.size(); i++)
            {
                if (NetworkUtils.doAddressesMatch(availableConnections.get(i).getMacAddress(), scanReplyMessage.getMacAddress()))
                {
                    found = true;
                }
            }

            // Check to see if a response has matched the MAC address that we are looking for
            if (NetworkUtils.doAddressesMatch(scanReplyMessage.getMacAddress(),
                    lastConnectedMonitorInfo.getMacAddress()))
            {
                networkScanStateData.setLastConnectedDevice(scanReplyMessage);
                networkScanStateData.setConnectToLast(true);
                networkScanner.stopScanning();
            }

            // If it is a different one, add it to the list of available connections
            if (!found)
            {
                availableConnections.add(scanReplyMessage);
                Platform.runLater(() -> networkScanStateData.setNumberOfFoundDevices(networkScanStateData.
                        getNumberOfFoundDevices() + 1));
            }
        }, endScan ->
        {
            // Only connect to last on end of scan if it has been enabled (because the last connected device was found
            // during a network scan). Otherwise do nothing because it is up to the user to select what device to
            // connect to via the connection list which will be displayed instead
            if (!networkScanStateData.shouldConnectToLast())
            {
                ApplicationCore.changeApplicationState(new ConnectionListStateData(availableConnections));
            }
            else
            {
                NetworkClient networkClient = NetworkClient.getInstance();
                NetworkUtils.Compatibility compatibility = NetworkUtils.isVersionCompatible(VERSION_MAJOR,
                        VERSION_MINOR, networkScanStateData.getLastConnectedDevice().getMajorVersion(),
                        networkScanStateData.getLastConnectedDevice().getMinorVersion());

                if (compatibility == NetworkUtils.Compatibility.COMPATIBLE)
                {
                    // Connect to new IP4 address of the device
                    networkClientConnect(networkClient, networkScanStateData.getLastConnectedDevice(),
                            availableConnections);
                }
                else
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Incompatible Monitor Version",
                            ButtonType.OK);

                    if (compatibility == NetworkUtils.Compatibility.NEWER)
                    {
                        alert.setContentText("Cannot connect to previously connected Hardware Monitor " +
                                networkScanStateData.getLastConnectedDevice() +
                                " because it is running an older software version than the editor");
                    }
                    else if (compatibility == NetworkUtils.Compatibility.OLDER)
                    {
                        alert.setContentText("Cannot connect to previously connected Hardware Monitor " +
                                networkScanStateData.getLastConnectedDevice() +
                                " because it is running an newer software version than the editor");
                    }

                    alert.showAndWait();

                    // Display everything in the broadcast reply data list on the connection page
                    changeApplicationState(new ConnectionListStateData(availableConnections));
                    show();
                }
            }
        });
    }
}
