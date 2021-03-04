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

package com.bennero.pages;

import com.bennero.networking.ConnectionInformation;
import com.bennero.networking.NetworkUtils;
import com.bennero.core.ApplicationCore;
import com.bennero.network.NetworkClient;
import com.bennero.network.NetworkScanner;
import com.bennero.states.ConnectionListStateData;
import com.bennero.states.InformationStateData;
import com.bennero.states.LoadingStateData;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

import static com.bennero.Version.*;
import static com.bennero.common.Constants.HEARTBEAT_TIMEOUT_MS;
import static com.bennero.networking.NetworkUtils.ip4AddressToString;

/**
 * Displays a list of connections that the user can select from in order to connect to a specific hardware monitor
 * device
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ConnectionListPage extends StackPane
{
    private ListView connectionListView;
    private List<ConnectionInformation> availableConnections;

    public ConnectionListPage()
    {
        super.setId("default-pane");

        availableConnections = new ArrayList<>();

        BorderPane titleAndConnectionList = new BorderPane();
        titleAndConnectionList.setId("standard-pane");

        Label title = new Label("Select Connection");
        title.setId("pane-title");
        titleAndConnectionList.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);

        BorderPane footerPane = new BorderPane();
        Button rescanButton = new Button("Re-Scan");
        rescanButton.setOnAction(actionEvent -> NetworkScanner.handleScan());
        rescanButton.setId("hw-default-button");
        footerPane.setLeft(rescanButton);

        Button selectButton = new Button("Select");
        selectButton.setId("hw-default-button");
        footerPane.setRight(selectButton);

        connectionListView = new ListView<>();
        selectButton.setOnMouseClicked(mouseEvent ->
        {
            if (connectionListView.getSelectionModel().getSelectedItem() != null)
            {
                ConnectionInformation selectedConnectionInformation = (ConnectionInformation) connectionListView.
                        getSelectionModel().getSelectedItem();
                NetworkUtils.Compatibility compatibility = NetworkUtils.isVersionCompatible(VERSION_MAJOR,
                        VERSION_MINOR, selectedConnectionInformation.getMajorVersion(),
                        selectedConnectionInformation.getMinorVersion());
                if (compatibility == NetworkUtils.Compatibility.NEWER)
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Incompatible Monitor Version",
                            ButtonType.OK);
                    alert.setContentText("Cannot connect to Hardware Monitor " + selectedConnectionInformation +
                            " because it is running an older software version than the editor");
                    alert.showAndWait();
                }
                else if (compatibility == NetworkUtils.Compatibility.OLDER)
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Incompatible Monitor Version",
                            ButtonType.OK);
                    alert.setContentText("Cannot connect to Hardware Monitor " + selectedConnectionInformation +
                            " because it is running an newer software version than the editor");
                    alert.showAndWait();
                }
                else
                {
                    NetworkClient.getInstance().connect(selectedConnectionInformation, connectedEvent ->
                    {
                        switch (connectedEvent.getConnectionStatus())
                        {
                            case CONNECTED:
                                ApplicationCore.onConnected();
                                break;
                            case CONNECTING:
                                // Display connecting text
                                ApplicationCore.changeApplicationState(new LoadingStateData("Connecting...",
                                        connectedEvent.getConnectionInformation().getHostname() + " (" +
                                                ip4AddressToString(connectedEvent.getConnectionInformation().
                                                        getIp4Address()) + ")"));
                                break;
                            case FAILED:
                                System.err.println("Failed to connect to device " + connectedEvent.
                                        getConnectionInformation().getHostname() + " (" +
                                        ip4AddressToString(connectedEvent.getConnectionInformation().getIp4Address()) +
                                        ") despite finding it on the network. It may be in use by another editor");

                                // Message pop-up that no connections have been found
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to Connect", ButtonType.OK);
                                alert.setContentText("Failed to connect to the device " +
                                        connectedEvent.getConnectionInformation().getHostname() +
                                        ", it may be in use by another editor");
                                alert.showAndWait();

                                // Re-display the connection list page so the user can select another hardware monitor
                                // to connect to
                                ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                        availableConnections));
                                break;
                            case VERSION_MISMATCH:
                                ApplicationCore.changeApplicationState(new InformationStateData(
                                        "Connection Refused", "Could not connect to " + connectedEvent.
                                        getConnectionInformation().getHostname() + " because the client version (v" +
                                        VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH +
                                        ") is incompatible with the server (v" +
                                        connectedEvent.getMajorServerVersion() + "." +
                                        connectedEvent.getMinorServerVersion() + "." +
                                        connectedEvent.getPatchServerVersion() + ")", "Device List",
                                        event -> ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                                availableConnections))));
                                break;
                            case IN_USE:
                                ApplicationCore.changeApplicationState(new InformationStateData(
                                        "Connection Refused", "Could not connect to " + connectedEvent.
                                        getConnectionInformation().getHostname() +
                                        " because it is in use by another device (" + connectedEvent.
                                        getCurrentlyConnectedHostname() + ")", "Device List",
                                        event -> ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                                availableConnections))));
                                break;
                            case CONNECTION_REFUSED:
                                ApplicationCore.changeApplicationState(new InformationStateData(
                                        "Connection Refused", "Could not connect to " + connectedEvent.
                                        getConnectionInformation().getHostname(), "Device List",
                                        event -> ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                                availableConnections))));
                                break;
                            case HEARTBEAT_TIMEOUT:
                                ApplicationCore.changeApplicationState(new LoadingStateData("Lost Communication",
                                        "No heartbeat message was received from " + connectedEvent.
                                                getConnectionInformation().getHostname() + " for " +
                                                HEARTBEAT_TIMEOUT_MS + "ms. Attempting to reconnect",
                                        "Device List",
                                        event -> ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                                availableConnections))));

                                // Re-display the connection list page so the user can select another hardware monitor
                                // to connect to
                                ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                        availableConnections));
                                break;
                            case UNEXPECTED_DISCONNECT:
                                System.out.println("LOST CONNECTION TO HARDWARE MONITOR");
                                ApplicationCore.changeApplicationState(new LoadingStateData("Lost Communication",
                                        connectedEvent.getConnectionInformation().getHostname() +
                                                " Disconnected unexpectedly. Attempting to reconnect",
                                        "Device List",
                                        event -> ApplicationCore.changeApplicationState(new ConnectionListStateData(
                                                availableConnections))));
                                break;
                        }
                    });
                }
            }
        });

        BorderPane pageOverview = new BorderPane();
        pageOverview.setId("standard-pane");
        pageOverview.setTop(titleAndConnectionList);
        pageOverview.setCenter(connectionListView);
        pageOverview.setBottom(footerPane);

        super.getChildren().add(pageOverview);
    }

    public void setAvailableConnectionsList(List<ConnectionInformation> availableConnections)
    {
        this.availableConnections = availableConnections;
        connectionListView.getItems().addAll(availableConnections);
    }
}
