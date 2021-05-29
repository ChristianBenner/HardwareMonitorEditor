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

import com.bennero.common.messages.BroadcastReplyDataPositions;
import com.bennero.common.messages.MessageType;
import com.bennero.common.networking.ConnectionInformation;
import com.bennero.common.networking.NetworkUtils;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.*;

/**
 * BroadcastReplyReceiver is a thread for receiving messages from hardware monitors. More specifically, the messages are
 * responses to broadcast requests sent by the hardware monitor editor. The messages contain information required by the
 * editor to identify and connect to the monitor, for example MAC address and IP address. This thread is a sub-system to
 * the network scanner.
 *
 * @see         NetworkScanner
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class BroadcastReplyReceiver extends Thread
{
    private EventHandler<ConnectionInformation> receivedBroadcastReply;
    private EventHandler endScan;
    private boolean run;
    private ServerSocketChannel serverSocketChannel;

    public BroadcastReplyReceiver(EventHandler<ConnectionInformation> receivedBroadcastReply, EventHandler endScan)
    {
        this.receivedBroadcastReply = receivedBroadcastReply;
        this.endScan = endScan;
        this.run = false;
    }

    public void stopThread()
    {
        if(serverSocketChannel != null)
        {
            try
            {
                serverSocketChannel.socket().close();
                serverSocketChannel.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        run = false;
    }

    @Override
    public void run()
    {
        try
        {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.socket().setReceiveBufferSize(MESSAGE_NUM_BYTES);
            serverSocketChannel.socket().bind(new InetSocketAddress(BROADCAST_REPLY_PORT));
            this.run = true;

            while (run)
            {
                // Build to accept multiple connections
                SocketChannel socketChannel = serverSocketChannel.accept();
                InputStream is = socketChannel.socket().getInputStream();
                byte[] bytes;

                bytes = new byte[MESSAGE_NUM_BYTES];
                is.read(bytes, 0, MESSAGE_NUM_BYTES);
                readMessage(bytes);

                socketChannel.close();
            }

            serverSocketChannel.close();
            endScan.handle(new Event(null));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void readMessage(byte[] bytes)
    {
        if (bytes[MESSAGE_TYPE_POS] == MessageType.BROADCAST_REPLY_MESSAGE)
        {
            final long hwMonitorSystemUniqueConnectionId = readLong(bytes,
                    BroadcastReplyDataPositions.HW_SYSTEM_IDENTIFIER_POS);

            // Ensures that the message came from a hardware monitor and not a random device on the network
            if (hwMonitorSystemUniqueConnectionId == HW_MONITOR_SYSTEM_UNIQUE_CONNECTION_ID)
            {
                final byte majorVersion = bytes[BroadcastReplyDataPositions.MAJOR_VERSION_POS];
                final byte minorVersion = bytes[BroadcastReplyDataPositions.MINOR_VERSION_POS];
                final byte patchVersion = bytes[BroadcastReplyDataPositions.PATCH_VERSION_POS];
                final byte[] macAddress = readBytes(bytes, BroadcastReplyDataPositions.MAC_ADDRESS_POS,
                        MAC_ADDRESS_NUM_BYTES);
                final byte[] ip4Address = readBytes(bytes, BroadcastReplyDataPositions.IP4_ADDRESS_POS,
                        IP4_ADDRESS_NUM_BYTES);
                String hostName = readString(bytes, BroadcastReplyDataPositions.HOSTNAME_POS, NAME_STRING_NUM_BYTES);
                
                System.out.println("Received a broadcast reply message from a hardware monitor: VERSION[" +
                        majorVersion + "." + minorVersion + "." + patchVersion + "] IP4[" +
                        NetworkUtils.ip4AddressToString(ip4Address) + "], MAC[" +
                        NetworkUtils.macAddressToString(macAddress) + "], HOSTNAME[" + hostName + "]");

                receivedBroadcastReply.handle(new ConnectionInformation(majorVersion, minorVersion, patchVersion,
                        macAddress, ip4Address, hostName));
            }
        }
    }
}