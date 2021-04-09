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

import com.bennero.common.networking.AddressInformation;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.Constants;
import com.bennero.common.networking.NetworkUtils;
import com.bennero.client.core.ApplicationCore;
import com.bennero.common.messages.BroadcastAnnouncementDataPositions;
import com.bennero.common.messages.MessageType;
import com.bennero.client.states.ConnectionListStateData;
import com.bennero.client.states.LoadingStateData;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.writeBytesToMessage;
import static com.bennero.common.networking.NetworkUtils.writeToMessage;

/**
 * NetworkScanner class provides functionality for scanning the network for hardware monitors. It will send out a
 * network message on the broadcast address containing the systems IP4 address. All devices on the network will receive
 * this broadcast message. It is then the role of a hardware monitor to reply to the message revealing its connection
 * info such as IP4 and MAC addresses so that it can be listed in the results and selected by the user.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NetworkScanner
{
    private static final long MS_PER_SECOND = 1000;

    private Thread broadcastReplyThread;
    private Thread broadcastSenderThread;
    private boolean sendBroadcastMessages;

    public static void handleScan()
    {
        // Now scan the network for devices
        List<ConnectionInformation> availableConnections = new ArrayList<>();

        // First, try to scan and connect. If that fails, scan and present results on connection list
        // Scan network for device with that MAC address
        NetworkScanner networkScanner = new NetworkScanner();

        ApplicationCore.s_setApplicationState(new LoadingStateData("Scanning network", "For hardware monitor devices"));
        networkScanner.scan(5, scanReplyMessage ->
        {
            // Before adding to available connections, check if a connection in the list with the same
            // MAC address already exists. This is so when multiple broadcast messages are sent out,
            // only one of the same device will show up in the connections list
            boolean found = false;
            for (int i = 0; i < availableConnections.size(); i++)
            {
                if (NetworkUtils.doAddressesMatch(availableConnections.get(i).getMacAddress(),
                        scanReplyMessage.getMacAddress()))
                {
                    found = true;
                }
            }

            if (!found)
            {
                availableConnections.add(scanReplyMessage);
            }
        }, event ->
        {
            if (NetworkClient.getInstance().isConnected())
            {
                System.out.println("END SCAN: CONNECTED!");
            }
            else
            {
                System.out.println("END SCAN: NOT CONNECTED!");
            }

            if (!NetworkClient.getInstance().isConnected())
            {
                ApplicationCore.s_setApplicationState(new ConnectionListStateData(availableConnections));

                if (availableConnections.isEmpty())
                {
                    // Message pop-up that no connections have been found
                    Alert alert = new Alert(Alert.AlertType.WARNING, "No Hardware Monitors Found", ButtonType.OK);
                    alert.setContentText("Scanning the network revealed no hardware monitors, please make sure that hardware monitor devices are online and running and then attempt a scan");
                    alert.showAndWait();
                }
            }
        });
    }

    public NetworkScanner()
    {
        // Nothing to do here
    }

    public void scan(int seconds, EventHandler<ConnectionInformation> broadcastReplyDataEventHandler, EventHandler endScan)
    {
        broadcastReplyThread = new Thread(new BroadcastReplyReceiver(broadcastReplyDataEventHandler, endScan));
        broadcastReplyThread.start();

        sendBroadcastMessages = true;

        broadcastSenderThread = new Thread(() ->
        {
            long timeStart = System.currentTimeMillis();

            while (sendBroadcastMessages &&
                    (seconds == 0 || ((timeStart + (seconds * MS_PER_SECOND)) > System.currentTimeMillis())))
            {
                sendBroadcastMessages();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            try
            {
                broadcastReplyThread.interrupt();
                broadcastReplyThread.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            // Run on JavaFX thread
            Platform.runLater(() -> endScan.handle(new Event(null)));
        });
        broadcastSenderThread.start();
    }

    public void stopScanning()
    {
        sendBroadcastMessages = false;
    }

    private void sendBroadcastMessages()
    {
        List<InetAddress> addresses = new ArrayList<>();
        try
        {
            addresses = discoverBroadcastAddresses();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }

        try
        {
            sendBroadcastMessage(InetAddress.getByName("255.255.255.255"));
            for (int i = 0; i < addresses.size(); i++)
            {
                sendBroadcastMessage(addresses.get(i));
            }
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
    }

    // Get the broadcast addresses of other network devices found on the network
    private List<InetAddress> discoverBroadcastAddresses() throws SocketException
    {
        List<InetAddress> broadcastAddressList = new ArrayList<>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements())
        {
            final NetworkInterface temp = networkInterfaces.nextElement();
            if (temp.isLoopback() || !temp.isUp())
            {
                continue;
            }

            temp.getInterfaceAddresses().stream().map(interfaceAddress -> interfaceAddress.getBroadcast()).
                    filter(Objects::nonNull).forEach(broadcastAddressList::add);
        }
        return broadcastAddressList;
    }

    private void sendBroadcastMessage(InetAddress inetAddress)
    {
        try
        {
            DatagramSocket broadcastSocket = new DatagramSocket();
            broadcastSocket.setBroadcast(true);

            byte[] message = new byte[MESSAGE_NUM_BYTES];
            message[MESSAGE_TYPE_POS] = MessageType.BROADCAST_MESSAGE;

            AddressInformation siteLocalAddress = NetworkUtils.getMyIpAddress();
            writeToMessage(message, BroadcastAnnouncementDataPositions.HW_SYSTEM_IDENTIFIER_POS, HW_EDITOR_SYSTEM_UNIQUE_CONNECTION_ID);
            writeBytesToMessage(message, BroadcastAnnouncementDataPositions.IP4_ADDRESS_POS, siteLocalAddress.getIp4Address(), IP4_ADDRESS_NUM_BYTES);

            DatagramPacket packet = new DatagramPacket(message, MESSAGE_NUM_BYTES, inetAddress, Constants.BROADCAST_RECEIVE_PORT);
            broadcastSocket.send(packet);
            broadcastSocket.close();
            System.out.println("Sent broadcastAvailability message on: " + inetAddress.getHostAddress());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
