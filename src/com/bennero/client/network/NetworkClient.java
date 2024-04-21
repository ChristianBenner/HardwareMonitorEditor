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

package com.bennero.client.network;

import com.bennero.client.config.ProgramConfigManager;
import com.bennero.client.messages.*;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.bennero.common.networking.AddressInformation;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import javafx.application.Platform;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;

import static com.bennero.client.Version.*;
import static com.bennero.client.network.ConnectionRequestReplyMessage.processConnectionRequestReplyMessageData;
import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.*;

/**
 * NetworkClient is a thread that handles the connection to a hardware monitor. It is responsible for establishing
 * connection and writing all of the network messages.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class NetworkClient {
    private static final String LOGGER_TAG = NetworkClient.class.getSimpleName();

    private static NetworkClient instance = null;
    private Socket socket;
    private PrintStream socketWriter;
    private ProgramConfigManager programConfigManager;
    private boolean connected;
    private HeartbeatListener heartbeatListener;
    private Thread connectionThread;

    private NetworkClient() {
        this.programConfigManager = ProgramConfigManager.getInstance();
        this.connected = false;
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }

        return instance;
    }

    public boolean isConnected() {
        if (socket == null || socket.isClosed()) {
            return false;
        } else {
            return connected;
        }
    }

    public void setIsConnected(boolean state) {
        this.connected = state;
    }

    public void connect(ConnectionInformation connectionInformation,
                        EventHandler<ConnectedEvent> connectionEventHandler) {
        connectionThread = new Thread(() ->
        {
            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation,
                    ConnectionStatus.CONNECTING)));

            try {
                Logger.log(LogLevel.INFO, LOGGER_TAG, "Attempting Connection: " +
                        programConfigManager.getLastConnectedHostname() + " (" +
                        NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()) + ")");

                // This means that the IP4 and MAC address have just been discovered, so we can start with a direct
                // connection attempt
                socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(connectionInformation.getIp4Address()), PORT), 5000);
                socketWriter = new PrintStream(socket.getOutputStream(), true);

                heartbeatListener = new HeartbeatListener(HEARTBEAT_TIMEOUT_MS,
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.HEARTBEAT_TIMEOUT))),
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.UNEXPECTED_DISCONNECT))));
                heartbeatListener.start();

                if (socket.isConnected()) {
                    Logger.log(LogLevel.INFO, LOGGER_TAG, "Connected to " +
                            NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));

                    // Update config to include latest network information
                    programConfigManager.setConnectionData(connectionInformation);

                    // Need to handshake with
                    sendHandshakeMessage();

                    byte[] bytes = new byte[MESSAGE_NUM_BYTES];
                    socket.getInputStream().read(bytes, 0, MESSAGE_NUM_BYTES);

                    if (bytes[MESSAGE_TYPE_POS] == MessageType.CONNECTION_REQUEST_RESPONSE_MESSAGE) {
                        Logger.log(LogLevel.INFO, LOGGER_TAG, "Received connection request response");
                        ConnectionRequestReplyMessage message = processConnectionRequestReplyMessageData(bytes);

                        if (message.isConnectionAccepted()) {
                            Logger.log(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor '" +
                                    message.getCurrentClientHostname() + "' (v" + message.getMajorVersion() + "." +
                                    message.getMinorVersion() + "." + message.getPatchVersion() +
                                    ") accepted connection");

                            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(
                                    connectionInformation, ConnectionStatus.CONNECTED)));
                            connected = true;
                        } else {
                            connected = false;

                            // Send event for different connection refusal reasons
                            if (message.isVersionMismatch()) {
                                Logger.log(LogLevel.INFO, LOGGER_TAG,
                                        "Hardware Monitor refused connection because of version mismatch: v" +
                                                message.getMajorVersion() + "." + message.getMinorVersion() + "." +
                                                message.getPatchVersion());

                                ConnectedEvent event = new ConnectedEvent(connectionInformation,
                                        ConnectionStatus.VERSION_MISMATCH);
                                event.setServerVersion(message.getMajorVersion(), message.getMinorVersion(),
                                        message.getPatchVersion());
                                Platform.runLater(() -> connectionEventHandler.handle(event));
                            } else if (message.isCurrentlyInUse()) {
                                Logger.log(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor refused connection " +
                                        "because it is currently in use by '" + message.getCurrentClientHostname() +
                                        "'");

                                ConnectedEvent event = new ConnectedEvent(connectionInformation,
                                        ConnectionStatus.IN_USE);
                                event.setCurrentlyConnectedHostname(message.getCurrentClientHostname());
                                Platform.runLater(() -> connectionEventHandler.handle(event));
                            } else {
                                Logger.log(LogLevel.INFO, LOGGER_TAG,
                                        "Hardware Monitor refused connection");

                                Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(
                                        connectionInformation, ConnectionStatus.CONNECTION_REFUSED)));
                            }
                        }
                    }
                } else {
                    Logger.log(LogLevel.WARNING, LOGGER_TAG, "Failed to connect to " +
                            NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));
                    Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
            }
        });
        connectionThread.start();
    }

    public void disconnect() {
        if (isConnected()) {
            heartbeatListener.stopThread();
            sendDisconnectMessage();
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Disconnected from hardware monitor");
            programConfigManager.clearConnectionData();

            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            connected = false;
        } else {
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Did not disconnect as not currently connected");
        }
    }

    public void writeRemovePageMessage(byte pageId) {
        if (socket != null && socket.isConnected()) {
            byte[] message = RemovePageMessage.create(pageId);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Remove Page Message: [ID: " + pageId + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Remove Page message because socket is not connected");
        }
    }

    public void writeRemoveSensorMessage(byte sensorId, byte pageId) {
        if (socket != null && socket.isConnected()) {
            byte[] message = RemoveSensorMessage.create(sensorId, pageId);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Remove Sensor Message: [ID: " + sensorId + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Remove Sensor message because socket is not connected");
        }
    }

    public void writePageMessage(PageData pageData) {
        if (socket != null && socket.isConnected()) {
            byte[] message = PageSetupMessage.create(pageData);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent PageData Message: [ID: " + pageData.getUniqueId() + "], [TITLE: " + pageData.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send PageData message because socket is not connected");
        }
    }

    public void writeSensorSetupMessage(Sensor sensor, byte pageId) {
        if (socket != null && socket.isConnected()) {
            byte[] message = SensorSetupMessage.create(sensor, pageId);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Sensor set-up Message: [ID: " + sensor.getUniqueId() + "], [TITLE: " + sensor.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor set-up Message because socket is not connected");
        }
    }

    public void writeSensorTransformationMessage(Sensor sensor, byte pageId) {
        if (socket != null && socket.isConnected()) {
            byte[] message = SensorTransformationMessage.create(sensor, pageId);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Sent Sensor Transformation Message: [ID: " + sensor.getUniqueId() + "], [TITLE: " + sensor.getTitle() + "]");
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor Transformation Message because socket is not connected");
        }
    }

    // Make it so we can write an array of sensor values in one message and if there is too many for one message, write
    // the remaining on another
    public void writeSensorValueMessage(int sensorId, float value) {
        if (socket != null && socket.isConnected()) {
            byte[] message = SensorValueMessage.create(sensorId, value);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Sensor value message because socket is not connected");
        }
    }

    private void sendHandshakeMessage() throws SocketException, UnknownHostException {
        final AddressInformation siteLocalAddress = NetworkUtils.getMyIpAddress();
        byte[] message = new byte[MESSAGE_NUM_BYTES];

        message[MESSAGE_TYPE_POS] = MessageType.CONNECTION_REQUEST_MESSAGE;
        message[ConnectionRequestDataPositions.MAJOR_VERSION_POS] = VERSION_MAJOR;
        message[ConnectionRequestDataPositions.MINOR_VERSION_POS] = VERSION_MINOR;
        message[ConnectionRequestDataPositions.PATCH_VERSION_POS] = VERSION_PATCH;

        // Do not want to force connect at this point as it may be good to know who is connected to device
        message[ConnectionRequestDataPositions.FORCE_CONNECT] = 0x00;
        writeBytesToMessage(message, ConnectionRequestDataPositions.IP4_ADDRESS_POS, siteLocalAddress.getIp4Address(), IP4_ADDRESS_NUM_BYTES);
        writeStringToMessage(message, ConnectionRequestDataPositions.HOSTNAME_POS, siteLocalAddress.getHostname(), NAME_STRING_NUM_BYTES);

        sendMessage(message, 0, MESSAGE_NUM_BYTES);
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Sent connection request message");
    }

    private void sendDisconnectMessage() {
        if (socket != null && socket.isConnected()) {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.DISCONNECT_MESSAGE;
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
        } else {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send disconnect message because socket is not connected");
        }
    }

    private void sendMessage(byte[] message, int offset, int length) {
        socketWriter.write(message, offset, length);
        socketWriter.flush();
    }
}