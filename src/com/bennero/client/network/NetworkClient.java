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

import com.bennero.client.bootstrapper.Native;
import com.bennero.common.*;
import com.bennero.client.config.ProgramConfigManager;
import com.bennero.common.messages.*;
import com.bennero.common.networking.AddressInformation;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import javafx.application.Platform;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;

import static com.bennero.client.network.ConnectionRequestReplyMessage.processConnectionRequestReplyMessageData;
import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.*;

/**
 * NetworkClient is a thread that handles the connection to a hardware monitor. It is responsible for establishing
 * connection and writing all of the network messages.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NetworkClient extends Thread
{
    private static NetworkClient instance = null;
    private Socket socket;
    private PrintStream socketWriter;
    private ProgramConfigManager programConfigManager;
    private boolean connected;

    public static NetworkClient getInstance()
    {
        if (instance == null)
        {
            instance = new NetworkClient();
        }

        return instance;
    }

    private NetworkClient()
    {
        this.programConfigManager = ProgramConfigManager.getInstance();
        this.connected = false;
    }

    @Override
    public void run()
    {
        while (true)
        {
            // todo is this even used?
            Platform.runLater(() ->
            {
                Native.updateSensors();
            });

            try
            {
                sleep(2000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected()
    {
        if (socket == null || socket.isClosed())
        {
            return false;
        }
        else
        {
            return connected;
        }
    }

    public void setIsConnected(boolean state)
    {
        this.connected = state;
    }

    public void connect(ConnectionInformation connectionInformation,
                        EventHandler<ConnectedEvent> connectionEventHandler)
    {
        Thread thread = new Thread(() ->
        {
            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation,
                    ConnectionStatus.CONNECTING)));

            try
            {
                System.out.println("Attempting Connection: " + programConfigManager.getLastConnectedHostname() + " (" +
                        NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()) + ")");

                // This means that the IP4 and MAC address have just been discovered, so we can start with a direct
                // connection attempt
                socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(connectionInformation.getIp4Address()), PORT), 5000);
                socketWriter = new PrintStream(socket.getOutputStream(), true);

                Thread heartbeatThread = new Thread(new HeartbeatListener(HEARTBEAT_TIMEOUT_MS,
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.HEARTBEAT_TIMEOUT))),
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.UNEXPECTED_DISCONNECT)))));
                heartbeatThread.start();

                if (socket.isConnected())
                {
                    System.out.println("Connected to " + NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));

                    // Update config to include latest network information
                    programConfigManager.setConnectionData(connectionInformation);

                    // Need to handshake with
                    sendHandshakeMessage();

                    byte[] bytes = new byte[MESSAGE_NUM_BYTES];
                    socket.getInputStream().read(bytes, 0, MESSAGE_NUM_BYTES);

                    if (bytes[MESSAGE_TYPE_POS] == MessageType.CONNECTION_REQUEST_RESPONSE_MESSAGE)
                    {
                        ConnectionRequestReplyMessage message = processConnectionRequestReplyMessageData(bytes);
                        if (message.isConnectionAccepted())
                        {
                            Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(
                                    connectionInformation, ConnectionStatus.CONNECTED)));
                            connected = true;
                        }
                        else
                        {
                            connected = false;

                            // Send event for different connection refusal reasons
                            if (message.isVersionMismatch())
                            {
                                ConnectedEvent event = new ConnectedEvent(connectionInformation,
                                        ConnectionStatus.VERSION_MISMATCH);
                                event.setServerVersion(message.getMajorVersion(), message.getMinorVersion(),
                                        message.getPatchVersion());
                                Platform.runLater(() -> connectionEventHandler.handle(event));
                            }
                            else if (message.isCurrentlyInUse())
                            {
                                ConnectedEvent event = new ConnectedEvent(connectionInformation,
                                        ConnectionStatus.IN_USE);
                                event.setCurrentlyConnectedHostname(message.getCurrentClientHostname());
                                Platform.runLater(() -> connectionEventHandler.handle(event));
                            }
                            else
                            {
                                Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(
                                        connectionInformation, ConnectionStatus.CONNECTION_REFUSED)));
                            }
                        }
                    }
                }
                else
                {
                    System.err.println("Failed to connect to " + NetworkUtils.ip4AddressToString(connectionInformation.getIp4Address()));
                    Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.FAILED)));
            }
        });
        thread.start();
    }

    public void removePageMessage(byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.REMOVE_PAGE;
            message[MESSAGE_TYPE_POS + 1] = pageId;
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent removePage message");
        }
        else
        {
            System.err.println("Failed to send remove page message because socket is not connected");
        }
    }

    public void removeSensorMessage(byte sensorId, byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.REMOVE_SENSOR;
            message[RemoveSensorDataPositions.SENSOR_ID_POS] = sensorId;
            message[RemoveSensorDataPositions.PAGE_ID_POS] = pageId;
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent removeSensor message");
        }
        else
        {
            System.err.println("Failed to send remove sensor message because socket is not connected");
        }
    }

    public void writePageMessage(PageData pageData)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];
            writePageSetupMessage(pageData, message);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent pageData message");
        }
        else
        {
            System.err.println("Failed to send page message because socket is not connected");
        }
    }

    public void writeSensorMessage(Sensor sensor, byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];
            writeSensorSetupMessage(sensor, pageId, message);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent sensorSetup message");
        }
        else
        {
            System.err.println("Failed to send sensor message because socket is not connected");
        }
    }

    // Make it so we can write an array of sensor values in one message and if there is too many for one message, write
    // the remaining on another
    public void writeSensorValueMessage(int sensorId, float value)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.DATA;
            message[SensorValueDataPositions.ID_POS] = (byte) sensorId;
            writeToMessage(message, SensorValueDataPositions.VALUE_POS, value);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
        }
        else
        {
            System.err.println("Failed to send sensor value message because socket is not connected");
        }
    }

    private void sendHandshakeMessage() throws SocketException, UnknownHostException
    {
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
        System.out.println("Sent connection request message");
    }

    private void writeSensorSetupMessage(Sensor sensor, byte pageId, byte[] bytes)
    {
        bytes[MESSAGE_TYPE_POS] = MessageType.SENSOR_SETUP;
        bytes[SensorDataPositions.ID_POS] = (byte) sensor.getUniqueId();
        bytes[SensorDataPositions.PAGE_ID_POS] = pageId;
        bytes[SensorDataPositions.ROW_POS] = (byte) sensor.getRow();
        bytes[SensorDataPositions.COLUMN_POS] = (byte) sensor.getColumn();
        bytes[SensorDataPositions.TYPE_POS] = sensor.getType();
        bytes[SensorDataPositions.SKIN_POS] = sensor.getSkin();
        writeToMessage(bytes, SensorDataPositions.MAX_POS, sensor.getMax());
        writeToMessage(bytes, SensorDataPositions.THRESHOLD_POS, sensor.getThreshold());
        bytes[SensorDataPositions.AVERAGE_ENABLED_POS] = sensor.isAverageEnabled() ? (byte) 0x01 : (byte) 0x00;
        writeToMessage(bytes, SensorDataPositions.AVERAGING_PERIOD_POS, sensor.getAveragingPeriod());
        bytes[SensorDataPositions.ROW_SPAN_POS] = (byte) sensor.getRowSpan();
        bytes[SensorDataPositions.COLUMN_SPAN_POS] = (byte) sensor.getColumnSpan();

        if (sensor.getAverageColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.AVERAGE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.AVERAGE_COLOUR_R_POS] = (byte) (sensor.getAverageColour().getRed() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_G_POS] = (byte) (sensor.getAverageColour().getGreen() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_B_POS] = (byte) (sensor.getAverageColour().getBlue() * 255.0);
        }

        if (sensor.getNeedleColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.NEEDLE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.NEEDLE_COLOUR_R_POS] = (byte) (sensor.getNeedleColour().getRed() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_G_POS] = (byte) (sensor.getNeedleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_B_POS] = (byte) (sensor.getNeedleColour().getBlue() * 255.0);
        }

        if (sensor.getValueColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.VALUE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.VALUE_COLOUR_R_POS] = (byte) (sensor.getValueColour().getRed() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_G_POS] = (byte) (sensor.getValueColour().getGreen() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_B_POS] = (byte) (sensor.getValueColour().getBlue() * 255.0);
        }

        if (sensor.getUnitColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.UNIT_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.UNIT_COLOUR_R_POS] = (byte) (sensor.getUnitColour().getRed() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_G_POS] = (byte) (sensor.getUnitColour().getGreen() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_B_POS] = (byte) (sensor.getUnitColour().getBlue() * 255.0);
        }

        if (sensor.getKnobColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.KNOB_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.KNOB_COLOUR_R_POS] = (byte) (sensor.getKnobColour().getRed() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_G_POS] = (byte) (sensor.getKnobColour().getGreen() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_B_POS] = (byte) (sensor.getKnobColour().getBlue() * 255.0);
        }

        if (sensor.getBarColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.BAR_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.BAR_COLOUR_R_POS] = (byte) (sensor.getBarColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_G_POS] = (byte) (sensor.getBarColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_B_POS] = (byte) (sensor.getBarColour().getBlue() * 255.0);
        }

        if (sensor.getThresholdColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.THRESHOLD_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.THRESHOLD_COLOUR_R_POS] = (byte) (sensor.getThresholdColour().getRed() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_G_POS] = (byte) (sensor.getThresholdColour().getGreen() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_B_POS] = (byte) (sensor.getThresholdColour().getBlue() * 255.0);
        }

        if (sensor.getTitleColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TITLE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TITLE_COLOUR_R_POS] = (byte) (sensor.getTitleColour().getRed() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_G_POS] = (byte) (sensor.getTitleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_B_POS] = (byte) (sensor.getTitleColour().getBlue() * 255.0);
        }

        if (sensor.getBarBackgroundColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.BAR_BACKGROUND_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_R_POS] = (byte) (sensor.getBarBackgroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_G_POS] = (byte) (sensor.getBarBackgroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_B_POS] = (byte) (sensor.getBarBackgroundColour().getBlue() * 255.0);
        }

        if (sensor.getForegroundColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.FOREGROUND_BASE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.FOREGROUND_COLOUR_R_POS] = (byte) (sensor.getForegroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_G_POS] = (byte) (sensor.getForegroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_B_POS] = (byte) (sensor.getForegroundColour().getBlue() * 255.0);
        }

        if (sensor.getTickLabelColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TICK_LABEL_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_R_POS] = (byte) (sensor.getTickLabelColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_G_POS] = (byte) (sensor.getTickLabelColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_B_POS] = (byte) (sensor.getTickLabelColour().getBlue() * 255.0);
        }

        if (sensor.getTickMarkColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TICK_MARK_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TICK_MARK_COLOUR_R_POS] = (byte) (sensor.getTickMarkColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_G_POS] = (byte) (sensor.getTickMarkColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_B_POS] = (byte) (sensor.getTickMarkColour().getBlue() * 255.0);
        }

        writeToMessage(bytes, SensorDataPositions.INITIAL_VALUE_POS, sensor.getValue());
        writeStringToMessage(bytes, SensorDataPositions.TITLE_POS, sensor.getTitle(), NAME_STRING_NUM_BYTES);
    }

    // take a Page type in the future
    private void writePageSetupMessage(PageData pageData, byte[] bytes)
    {
        byte pageId = (byte) pageData.getUniqueId();
        byte pageColourR = (byte) (pageData.getColour().getRed() * 255.0);
        byte pageColourG = (byte) (pageData.getColour().getGreen() * 255.0);
        byte pageColourB = (byte) (pageData.getColour().getBlue() * 255.0);
        byte titleColourR = (byte) (pageData.getTitleColour().getRed() * 255.0);
        byte titleColourG = (byte) (pageData.getTitleColour().getGreen() * 255.0);
        byte titleColourB = (byte) (pageData.getTitleColour().getBlue() * 255.0);
        byte subtitleColourR = (byte) (pageData.getSubtitleColour().getRed() * 255.0);
        byte subtitleColourG = (byte) (pageData.getSubtitleColour().getGreen() * 255.0);
        byte subtitleColourB = (byte) (pageData.getSubtitleColour().getBlue() * 255.0);
        byte pageRows = (byte) pageData.getRows();
        byte pageColumns = (byte) pageData.getColumns();
        byte nextPageId = (byte) pageData.getNextPageId();
        byte pageTransitionType = (byte) pageData.getTransitionType();
        int pageTransitionTime = pageData.getTransitionTime();
        int pageDurationMs = pageData.getDurationMs();
        String title = pageData.getTitle();
        byte titleEnabled = pageData.isTitleEnabled() ? (byte) 0x01 : (byte) 0x00;
        byte titleAlignment = (byte) pageData.getTitleAlignment();
        String subtitle = pageData.getSubtitle();
        byte subtitleEnabled = pageData.isSubtitleEnabled() ? (byte) 0x01 : (byte) 0x00;
        byte subtitleAlignment = (byte) pageData.getSubtitleAlignment();

        bytes[MESSAGE_TYPE_POS] = MessageType.PAGE_SETUP;
        bytes[PageDataPositions.ID_POS] = pageId;
        bytes[PageDataPositions.COLOUR_R_POS] = pageColourR;
        bytes[PageDataPositions.COLOUR_G_POS] = pageColourG;
        bytes[PageDataPositions.COLOUR_B_POS] = pageColourB;
        bytes[PageDataPositions.TITLE_COLOUR_R_POS] = titleColourR;
        bytes[PageDataPositions.TITLE_COLOUR_G_POS] = titleColourG;
        bytes[PageDataPositions.TITLE_COLOUR_B_POS] = titleColourB;
        bytes[PageDataPositions.SUBTITLE_COLOUR_R_POS] = subtitleColourR;
        bytes[PageDataPositions.SUBTITLE_COLOUR_G_POS] = subtitleColourG;
        bytes[PageDataPositions.SUBTITLE_COLOUR_B_POS] = subtitleColourB;
        bytes[PageDataPositions.ROWS_POS] = pageRows;
        bytes[PageDataPositions.COLUMNS_POS] = pageColumns;
        bytes[PageDataPositions.NEXT_ID_POS] = nextPageId;
        bytes[PageDataPositions.TRANSITION_TYPE_POS] = pageTransitionType;
        writeToMessage(bytes, PageDataPositions.TRANSITION_TIME_POS, pageTransitionTime);
        writeToMessage(bytes, PageDataPositions.DURATION_MS_POS, pageDurationMs);
        writeStringToMessage(bytes, PageDataPositions.TITLE_POS, title, NAME_STRING_NUM_BYTES);
        bytes[PageDataPositions.TITLE_ENABLED_POS] = titleEnabled;
        bytes[PageDataPositions.TITLE_ALIGNMENT_POS] = titleAlignment;
        writeStringToMessage(bytes, PageDataPositions.SUBTITLE_POS, subtitle, NAME_STRING_NUM_BYTES);
        bytes[PageDataPositions.SUBTITLE_POS_ENABLED_POS] = subtitleEnabled;
        bytes[PageDataPositions.SUBTITLE_POS_ALIGNMENT_POS] = subtitleAlignment;
    }

    private void sendMessage(byte[] message, int offset, int length)
    {
        socketWriter.write(message, offset, length);
        socketWriter.flush();
    }
}