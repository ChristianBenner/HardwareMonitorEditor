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

import com.bennero.common.messages.BroadcastReplyMessage;
import com.bennero.common.messages.Message;
import com.bennero.common.messages.MessageType;
import com.bennero.common.networking.ConnectionInformation;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static com.bennero.common.Constants.*;
import static com.bennero.common.messages.MessageUtils.*;

/**
 * BroadcastReplyReceiver is a thread for receiving messages from hardware monitors. More specifically, the messages are
 * responses to broadcast requests sent by the hardware monitor editor. The messages contain information required by the
 * editor to identify and connect to the monitor, for example MAC address and IP address. This thread is a sub-system to
 * the network scanner.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see NetworkScanner
 * @since 1.0
 */
public class BroadcastReplyReceiver extends Thread {
    private EventHandler<ConnectionInformation> receivedBroadcastReply;
    private EventHandler endScan;
    private boolean run;
    private ServerSocketChannel serverSocketChannel;

    public BroadcastReplyReceiver(EventHandler<ConnectionInformation> receivedBroadcastReply, EventHandler endScan) {
        this.receivedBroadcastReply = receivedBroadcastReply;
        this.endScan = endScan;
        this.run = false;
    }

    public void stopThread() {
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.socket().close();
                serverSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        run = false;
    }

    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.socket().setReceiveBufferSize(Message.NUM_BYTES);
            serverSocketChannel.socket().bind(new InetSocketAddress(BROADCAST_REPLY_PORT));
            this.run = true;

            while (run) {
                // Build to accept multiple connections
                SocketChannel socketChannel = serverSocketChannel.accept();
                InputStream is = socketChannel.socket().getInputStream();
                byte[] bytes;

                bytes = new byte[Message.NUM_BYTES];
                is.read(bytes, 0, Message.NUM_BYTES);
                readMessage(bytes);

                socketChannel.close();
            }

            serverSocketChannel.close();
            endScan.handle(new Event(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage(byte[] bytes) {
        if (Message.getType(bytes) == MessageType.BROADCAST_REPLY) {
            BroadcastReplyMessage message = new BroadcastReplyMessage(bytes);

            // Ensures that the message came from a hardware monitor and not a random device on the network
            if (message.getSystemIdentifier() == HW_MONITOR_SYSTEM_UNIQUE_CONNECTION_ID) {
                receivedBroadcastReply.handle(new ConnectionInformation(message.getVersionMajor(),
                        message.getVersionMinor(), message.getVersionPatch(), message.getMacAddress(),
                        message.getIp4Address(), message.getHostName()));
            }
        }
    }
}