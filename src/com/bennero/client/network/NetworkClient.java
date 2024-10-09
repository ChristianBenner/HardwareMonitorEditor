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

import com.bennero.client.Version;
import com.bennero.client.config.ProgramConfigManager;
import com.bennero.client.core.ApplicationCore;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.HEARTBEAT_TIMEOUT_MS;
import static com.bennero.common.Constants.PORT;

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

    private ConnectedEvent handleConnectionResponse(ConnectionInformation connectionInformation, ConnectionRequestResponseMessage message) {
        connected = message.isConnectionAccepted();
        if (connected) {
            Logger.logf(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor '%s' accepted connection", connectionInformation.getHostname());
            return new ConnectedEvent(connectionInformation, ConnectionStatus.CONNECTED);
        }

        // Send event for different connection refusal reasons
        if (message.isVersionMismatch()) {
            Logger.logf(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor '%s' refused connection because of version mismatch (Us: v%d.%d.%d, Them: v%d.%d.%d)", connectionInformation.getHostname(), Version.VERSION_MAJOR, Version.VERSION_MINOR, Version.VERSION_PATCH, message.getVersionMajor(), message.getVersionMinor(), message.getVersionPatch());
            ConnectedEvent event = new ConnectedEvent(connectionInformation, ConnectionStatus.VERSION_MISMATCH);
            event.setServerVersion(message.getVersionMajor(), message.getVersionMinor(), message.getVersionPatch());
            return event;
        }

        if (message.isCurrentlyInUse()) {
            Logger.logf(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor '%s' refused connection because it is currently in use by another editor '%s'", connectionInformation.getHostname(), message.getCurrentClientHostname());
            ConnectedEvent event = new ConnectedEvent(connectionInformation, ConnectionStatus.IN_USE);
            event.setCurrentlyConnectedHostname(message.getCurrentClientHostname());
            return event;
        }

        Logger.logf(LogLevel.INFO, LOGGER_TAG, "Hardware Monitor '%s' refused connection", connectionInformation.getHostname());
        return new ConnectedEvent(connectionInformation, ConnectionStatus.CONNECTION_REFUSED);
    }

    private void attemptConnection(ConnectionInformation connectionInformation,
                                             EventHandler<ConnectedEvent> connectionEventHandler) throws IOException {
        Logger.logf(LogLevel.INFO, LOGGER_TAG, "Attempting Connection to Hardware Monitor '%s'", programConfigManager.getLastConnectedHostname());

        // This means that the IP4 and MAC address have just been discovered, so we can start with a direct connection attempt
        socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getByAddress(connectionInformation.getIp4Address()), PORT), 5000);
        socketWriter = new PrintStream(socket.getOutputStream(), true);

        heartbeatListener = new HeartbeatListener(HEARTBEAT_TIMEOUT_MS,
                event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.HEARTBEAT_TIMEOUT))),
                event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.UNEXPECTED_DISCONNECT))));
        heartbeatListener.start();

        if (!socket.isConnected()) {
            Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Failed to connect to '%s'", NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));
            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
            return;
        }

        Logger.log(LogLevel.INFO, LOGGER_TAG, "Connected to " +
                NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));

        // Update config to include latest network information
        programConfigManager.setConnectionData(connectionInformation);

        AddressInformation siteLocalAddress = NetworkUtils.getMyIpAddress();

        // Need to handshake with hardware monitor - Do not want to force connect at this point as it may be good to
        // know if/who is connected to device
        ConnectionRequestMessage out = new ConnectionRequestMessage(ApplicationCore.s_getUUID(), true,
                VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, false, siteLocalAddress.getIp4Address(),
                siteLocalAddress.getHostname());
        writeMessage(out);

        byte[] bytes = new byte[Message.NUM_BYTES];
        socket.getInputStream().read(bytes, 0, Message.NUM_BYTES);

        if (Message.getType(bytes) == MessageType.CONNECTION_REQUEST_RESPONSE) {
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Received connection request response");

            ConnectionRequestResponseMessage in = new ConnectionRequestResponseMessage(bytes);

            switch (in.getStatus()) {
                case Message.STATUS_OK:
                    ConnectedEvent event = handleConnectionResponse(connectionInformation, in);
                    Platform.runLater(() -> connectionEventHandler.handle(event));
                    break;
                case Message.STATUS_BAD_RECEIVE:
                    // other end did not receive message properly
                    Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Communication error with '%s'", NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));
                    Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
                    break;
                case Message.STATUS_FAILED_READ:
                    // checksum bad
                    Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Bad data from '%s'", NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));
                    Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
                    break;
            }
        }
    }

    public void connect(ConnectionInformation connectionInformation,
                        EventHandler<ConnectedEvent> connectionEventHandler) {
        connectionThread = new Thread(() ->
        {
            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation,
                    ConnectionStatus.CONNECTING)));
            try {
                attemptConnection(connectionInformation, connectionEventHandler);
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

            DisconnectMessage out = new DisconnectMessage(ApplicationCore.s_getUUID(), true);
            writeMessage(out);

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

    public boolean writeMessage(Message message) {
        if (socket == null || !socket.isConnected()) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to send Remove Page message because socket is not connected");
            return false;
        }

        byte[] bytes = message.write();
        sendMessage(bytes, 0, Message.NUM_BYTES);
        Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Sent message (type: %d)", message.getType());
        return true;
    }

    private void sendMessage(byte[] message, int offset, int length) {
        socketWriter.write(message, offset, length);
        socketWriter.flush();
    }
}