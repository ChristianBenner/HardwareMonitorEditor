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
import com.bennero.common.*;
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
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NetworkClient
{
    private static final String LOGGER_TAG = NetworkClient.class.getSimpleName();

    private static NetworkClient instance = null;
    private Socket socket;
    private PrintStream socketWriter;
    private ProgramConfigManager programConfigManager;
    private boolean connected;
    private HeartbeatListener heartbeatListener;
    private Thread connectionThread;

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
        connectionThread = new Thread(() ->
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

                heartbeatListener = new HeartbeatListener(HEARTBEAT_TIMEOUT_MS,
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.HEARTBEAT_TIMEOUT))),
                        event -> Platform.runLater(() -> connectionEventHandler.handle(new ConnectedEvent(connectionInformation, ConnectionStatus.UNEXPECTED_DISCONNECT))));
                heartbeatListener.start();

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
        connectionThread.start();
    }

    public boolean disconnect()
    {
        boolean success = false;

        if(isConnected())
        {

           // heartbeatListener.join();
            heartbeatListener.stopThread();
            sendDisconnectMessage();
            success = true;
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Disconnected from hardware monitor");
            programConfigManager.clearConnectionData();
            try
            {
                socket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            connected = false;


            //heartbeatListener.stopThread();
            /*try
            {

                socket.shutdownOutput();
                socket.close();
                connected = false;
                heartbeatListener.stopThread();
                heartbeatListener.join();
                Logger.log(LogLevel.INFO, LOGGER_TAG, "Disconnected from hardware monitor");
                success = true;
            }
            catch (IOException | InterruptedException e)
            {
                Logger.log(LogLevel.ERROR, LOGGER_TAG, "Failed to disconnect from hardware monitor: " +
                        e.getMessage());
            }*/
        }
        else
        {
            Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Did not disconnect as not currently connected");
            success = true;
        }

        return success;
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

    public void removeSensorGUIMessage(byte sensorId, byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.REMOVE_SENSOR;
            message[RemoveSensorGUIDataPositions.SENSOR_GUI_ID_POS] = sensorId;
            message[RemoveSensorGUIDataPositions.PAGE_ID_POS] = pageId;
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

    public void writeSensorMessage(SensorData sensorData, SensorGUI sensorGUI, byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];
            writeSensorSetupMessage(sensorData, sensorGUI, pageId, message);
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent sensorSetup message");
        }
        else
        {
            System.err.println("Failed to send sensor message because socket is not connected");
        }
    }

    public void writeSensorRelocationMessage(SensorGUI sensorGUI, byte pageId)
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.SENSOR_RELOCATION_MESSAGE;
            message[SensorDataPositions.ID_POS] = (byte)sensorGUI.getUniqueId();
            message[SensorDataPositions.PAGE_ID_POS] = pageId;
            message[SensorDataPositions.ROW_POS] = (byte)sensorGUI.getRow();
            message[SensorDataPositions.COLUMN_POS] = (byte)sensorGUI.getColumn();
            message[SensorDataPositions.ROW_SPAN_POS] = (byte)sensorGUI.getRowSpan();
            message[SensorDataPositions.COLUMN_SPAN_POS] = (byte)sensorGUI.getColumnSpan();
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
            System.out.println("Sent sensorRelocation message");
        }
        else
        {
            System.err.println("Failed to send sensor relocation message because socket is not connected");
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

    private void writeSensorSetupMessage(SensorData sensorData, SensorGUI sensorGUI, byte pageId, byte[] bytes)
    {
        bytes[MESSAGE_TYPE_POS] = MessageType.SENSOR_SETUP;
        bytes[SensorDataPositions.ID_POS] = (byte) sensorData.getUniqueId();
        bytes[SensorDataPositions.PAGE_ID_POS] = pageId;
        bytes[SensorDataPositions.ROW_POS] = (byte) sensorGUI.getRow();
        bytes[SensorDataPositions.COLUMN_POS] = (byte) sensorGUI.getColumn();
        bytes[SensorDataPositions.TYPE_POS] = sensorData.getType();
        bytes[SensorDataPositions.SKIN_POS] = sensorGUI.getSkin();
        writeToMessage(bytes, SensorDataPositions.MAX_POS, sensorData.getMax());
        writeToMessage(bytes, SensorDataPositions.THRESHOLD_POS, sensorData.getThreshold());
        bytes[SensorDataPositions.AVERAGE_ENABLED_POS] = sensorGUI.isAverageEnabled() ? (byte) 0x01 : (byte) 0x00;
        writeToMessage(bytes, SensorDataPositions.AVERAGING_PERIOD_POS, sensorGUI.getAveragingPeriod());
        bytes[SensorDataPositions.ROW_SPAN_POS] = (byte) sensorGUI.getRowSpan();
        bytes[SensorDataPositions.COLUMN_SPAN_POS] = (byte) sensorGUI.getColumnSpan();

        if (sensorGUI.getAverageColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.AVERAGE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.AVERAGE_COLOUR_R_POS] = (byte) (sensorGUI.getAverageColour().getRed() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_G_POS] = (byte) (sensorGUI.getAverageColour().getGreen() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_B_POS] = (byte) (sensorGUI.getAverageColour().getBlue() * 255.0);
        }

        if (sensorGUI.getNeedleColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.NEEDLE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.NEEDLE_COLOUR_R_POS] = (byte) (sensorGUI.getNeedleColour().getRed() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_G_POS] = (byte) (sensorGUI.getNeedleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_B_POS] = (byte) (sensorGUI.getNeedleColour().getBlue() * 255.0);
        }

        if (sensorGUI.getValueColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.VALUE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.VALUE_COLOUR_R_POS] = (byte) (sensorGUI.getValueColour().getRed() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_G_POS] = (byte) (sensorGUI.getValueColour().getGreen() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_B_POS] = (byte) (sensorGUI.getValueColour().getBlue() * 255.0);
        }

        if (sensorGUI.getUnitColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.UNIT_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.UNIT_COLOUR_R_POS] = (byte) (sensorGUI.getUnitColour().getRed() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_G_POS] = (byte) (sensorGUI.getUnitColour().getGreen() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_B_POS] = (byte) (sensorGUI.getUnitColour().getBlue() * 255.0);
        }

        if (sensorGUI.getKnobColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.KNOB_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.KNOB_COLOUR_R_POS] = (byte) (sensorGUI.getKnobColour().getRed() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_G_POS] = (byte) (sensorGUI.getKnobColour().getGreen() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_B_POS] = (byte) (sensorGUI.getKnobColour().getBlue() * 255.0);
        }

        if (sensorGUI.getBarColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.BAR_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.BAR_COLOUR_R_POS] = (byte) (sensorGUI.getBarColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_G_POS] = (byte) (sensorGUI.getBarColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_B_POS] = (byte) (sensorGUI.getBarColour().getBlue() * 255.0);
        }

        if (sensorGUI.getThresholdColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.THRESHOLD_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.THRESHOLD_COLOUR_R_POS] = (byte) (sensorGUI.getThresholdColour().getRed() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_G_POS] = (byte) (sensorGUI.getThresholdColour().getGreen() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_B_POS] = (byte) (sensorGUI.getThresholdColour().getBlue() * 255.0);
        }

        if (sensorGUI.getTitleColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.TITLE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TITLE_COLOUR_R_POS] = (byte) (sensorGUI.getTitleColour().getRed() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_G_POS] = (byte) (sensorGUI.getTitleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_B_POS] = (byte) (sensorGUI.getTitleColour().getBlue() * 255.0);
        }

        if (sensorGUI.getBarBackgroundColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.BAR_BACKGROUND_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_R_POS] = (byte) (sensorGUI.getBarBackgroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_G_POS] = (byte) (sensorGUI.getBarBackgroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_B_POS] = (byte) (sensorGUI.getBarBackgroundColour().getBlue() * 255.0);
        }

        if (sensorGUI.getForegroundColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.FOREGROUND_BASE_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.FOREGROUND_COLOUR_R_POS] = (byte) (sensorGUI.getForegroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_G_POS] = (byte) (sensorGUI.getForegroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_B_POS] = (byte) (sensorGUI.getForegroundColour().getBlue() * 255.0);
        }

        if (sensorGUI.getTickLabelColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.TICK_LABEL_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_R_POS] = (byte) (sensorGUI.getTickLabelColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_G_POS] = (byte) (sensorGUI.getTickLabelColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_B_POS] = (byte) (sensorGUI.getTickLabelColour().getBlue() * 255.0);
        }

        if (sensorGUI.getTickMarkColour() != null && SkinHelper.checkSupport(sensorGUI.getSkin(), Skin.TICK_MARK_COLOUR_SUPPORTED))
        {
            bytes[SensorDataPositions.TICK_MARK_COLOUR_R_POS] = (byte) (sensorGUI.getTickMarkColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_G_POS] = (byte) (sensorGUI.getTickMarkColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_B_POS] = (byte) (sensorGUI.getTickMarkColour().getBlue() * 255.0);
        }

        writeToMessage(bytes, SensorDataPositions.INITIAL_VALUE_POS, sensorData.getValue());
        writeStringToMessage(bytes, SensorDataPositions.TITLE_POS, sensorGUI.getTitle(), NAME_STRING_NUM_BYTES);
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

    private void sendDisconnectMessage()
    {
        if (socket != null && socket.isConnected())
        {
            byte[] message = new byte[MESSAGE_NUM_BYTES];

            message[MESSAGE_TYPE_POS] = MessageType.DISCONNECT_MESSAGE;
            sendMessage(message, 0, MESSAGE_NUM_BYTES);
        }
        else
        {
            System.err.println("Failed to send disconnect message because socket is not connected");
        }
    }

    private void sendMessage(byte[] message, int offset, int length)
    {
        socketWriter.write(message, offset, length);
        socketWriter.flush();
    }
}