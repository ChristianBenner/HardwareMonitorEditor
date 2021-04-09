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

import com.bennero.common.messages.MessageType;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.readLong;

/**
 * HeartbeatListener is a thread that runs while connected to a hardware monitor. It listens for heartbeats sent out by
 * the monitor so it can determine if it is still connected to it. If no heartbeat is received within a specific timeout
 * interval, an event will be passed back. If connection is lost unexpectedly, an event will also be passed back.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class HeartbeatListener implements Runnable
{
    private int heartbeatTimeoutMilliseconds;
    private EventHandler noHeartbeatReceived;
    private EventHandler lostConnection;
    private boolean run;
    private ServerSocket serverSocket;

    public HeartbeatListener(int heartbeatTimeoutMilliseconds,
                             EventHandler noHeartbeatReceived,
                             EventHandler lostConnection) throws IOException
    {
        serverSocket = new ServerSocket(HEARTBEAT_PORT);
        this.heartbeatTimeoutMilliseconds = heartbeatTimeoutMilliseconds;
        this.noHeartbeatReceived = noHeartbeatReceived;
        this.lostConnection = lostConnection;
        run = true;
    }

    @Override
    public void run()
    {
        try
        {
            Socket socket = serverSocket.accept();
            serverSocket.setSoTimeout(heartbeatTimeoutMilliseconds);
            socket.setSoTimeout(heartbeatTimeoutMilliseconds);
            InputStream is = socket.getInputStream();
            while (run)
            {
                try
                {
                    byte[] bytes;
                    bytes = new byte[MESSAGE_NUM_BYTES];
                    is.read(bytes, 0, MESSAGE_NUM_BYTES);
                    readMessage(bytes);
                }
                catch (SocketTimeoutException se)
                {
                    se.printStackTrace();
                    run = false;
                    noHeartbeatReceived.handle(new Event(null));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    run = false;
                    lostConnection.handle(new Event(null));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void readMessage(byte[] bytes)
    {
        if (bytes[MESSAGE_TYPE_POS] == MessageType.HEARTBEAT_MESSAGE)
        {
            final long hwMonitorSystemUniqueConnectionId = readLong(bytes, HW_HEARTBEAT_VALIDATION_NUMBER_POS);

            // Ensures that the message came from a hardware monitor and not a random device on the network
            if (hwMonitorSystemUniqueConnectionId == HW_HEARTBEAT_VALIDATION_NUMBER)
            {
                System.out.println("Received a heartbeat from Hardware Monitor");
            }
            else
            {
                System.out.println("Received an invalid heartbeat from Hardware Monitor");
            }
        }
    }
}