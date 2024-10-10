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

package com.bennero.client.pages;

import com.bennero.client.core.ApplicationCore;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.network.NetworkScanner;
import com.bennero.client.serial.ConnectionInfo;
import com.bennero.client.serial.ConnectionState;
import com.bennero.client.serial.SerialClient;
import com.bennero.client.serial.SerialScanner;
import com.bennero.client.states.ConnectionListStateData;
import com.bennero.client.states.InformationStateData;
import com.bennero.client.states.LoadingStateData;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.HEARTBEAT_TIMEOUT_MS;
import static com.bennero.common.networking.NetworkUtils.ip4AddressToString;

/**
 * Displays a list of connections that the user can select from in order to connect to a specific hardware monitor
 * device
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class SerialConnectionListPage extends StackPane {
    private static final String CLASS_NAME = SerialConnectionListPage.class.getSimpleName();

    // SerialPortDisplay class is just used to control the information displayed on the list of available devices (via
    // a toString override)
    class SerialPortDisplay {
        public SerialPort serialPort;

        public SerialPortDisplay(SerialPort serialPort) {
            this.serialPort = serialPort;
        }

        @Override
        public String toString() {
            return serialPort.getDescriptivePortName();
        }
    }

    private ListView connectionListView;
    private SerialPortDisplay[] serialDevicesDisplay;

    public SerialConnectionListPage() {
        super.setId("default-pane");

        serialDevicesDisplay = new SerialPortDisplay[]{};

        BorderPane titleAndConnectionList = new BorderPane();
        titleAndConnectionList.setId("standard-pane");

        Label title = new Label("Select Device");
        title.setId("pane-title");
        titleAndConnectionList.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);

        BorderPane footerPane = new BorderPane();
        Button rescanButton = new Button("Refresh");
        rescanButton.setOnAction(actionEvent -> SerialScanner.handleScan());
        rescanButton.setId("hw-default-button");
        footerPane.setLeft(rescanButton);

        Button selectButton = new Button("Select");
        selectButton.setId("hw-default-button");
        footerPane.setRight(selectButton);

        connectionListView = new ListView<>();
        selectButton.setOnMouseClicked(mouseEvent ->
        {
            if (connectionListView.getSelectionModel().getSelectedItem() != null) {
                SerialPortDisplay selectedPortInformation = (SerialPortDisplay) connectionListView.getSelectionModel().getSelectedItem();

                Alert info = new Alert(Alert.AlertType.CONFIRMATION, "Confirmation", ButtonType.OK);
                info.setContentText("Ensure that the correct device is selected");
                info.showAndWait();

                ApplicationCore.getInstance().setApplicationState(new InformationStateData("Connecting...", "WIP", "", null));

                Thread establishConnectionThread = new Thread(() -> {
                    ConnectionInfo connectionInfo = SerialClient.getInstance().connect(selectedPortInformation.serialPort);
                    switch (connectionInfo.getConnectionState()) {
                        case CONNECTED:
                            Platform.runLater(() -> {
                                ApplicationCore.getInstance().onConnected();
                            });
                            break;
                        case PORT_FAILED_OPEN:
                        case READ_TIMEOUT:
                        case BAD_RESPONSE_WRONG_MESSAGE:
                        case BAD_RESPONSE_INVALID_CHECKSUM:
                        case REJECTED_CONNECTION:
                        default:
                            // todo: implement failed connection page
                            Platform.runLater(() -> {
                                ApplicationCore.getInstance().setApplicationState(new InformationStateData("Failed to connect", "WIP", "", null));
                            });
                            break;
                    }
                });

                establishConnectionThread.start();
            }
        });

        BorderPane pageOverview = new BorderPane();
        pageOverview.setId("standard-pane");
        pageOverview.setTop(titleAndConnectionList);
        pageOverview.setCenter(connectionListView);
        pageOverview.setBottom(footerPane);

        super.getChildren().add(pageOverview);
    }

    public void setAvailableDevicesList(SerialPort[] serialDevices) {
        serialDevicesDisplay = new SerialPortDisplay[serialDevices.length];
        for(int i = 0; i < serialDevices.length; i++) {
            serialDevicesDisplay[i] = new SerialPortDisplay(serialDevices[i]);
        }

        connectionListView.getItems().addAll(serialDevicesDisplay);
    }
}
