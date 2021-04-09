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

import com.bennero.common.networking.ConnectionInformation;
import javafx.event.Event;

/**
 * A ConnectedEvent contains connection information and is created when connection has been established with a hardware
 * monitor. It includes some further information such as the status of the connection and the version of the monitor.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ConnectedEvent extends Event
{
    private final ConnectionInformation connectionInformation;
    private final ConnectionStatus connectionStatus;
    private String currentlyConnectedHostname;
    private int majorServerVersion;
    private int minorServerVersion;
    private int patchServerVersion;

    public ConnectedEvent(final ConnectionInformation connectionInformation,
                          final ConnectionStatus connectionStatus)
    {
        super(connectionStatus, null, null);
        this.connectionInformation = connectionInformation;
        this.connectionStatus = connectionStatus;
    }

    public ConnectionStatus getConnectionStatus()
    {
        return connectionStatus;
    }

    public ConnectionInformation getConnectionInformation()
    {
        return connectionInformation;
    }

    public String getCurrentlyConnectedHostname()
    {
        return currentlyConnectedHostname;
    }

    public void setCurrentlyConnectedHostname(String currentlyConnectedHostname)
    {
        this.currentlyConnectedHostname = currentlyConnectedHostname;
    }

    public void setServerVersion(int major, int minor, int patch)
    {
        setMajorServerVersion(major);
        setMinorServerVersion(minor);
        setPatchServerVersion(patch);
    }

    public int getMajorServerVersion()
    {
        return majorServerVersion;
    }

    public void setMajorServerVersion(int majorServerVersion)
    {
        this.majorServerVersion = majorServerVersion;
    }

    public int getMinorServerVersion()
    {
        return minorServerVersion;
    }

    public void setMinorServerVersion(int minorServerVersion)
    {
        this.minorServerVersion = minorServerVersion;
    }

    public int getPatchServerVersion()
    {
        return patchServerVersion;
    }

    public void setPatchServerVersion(int patchServerVersion)
    {
        this.patchServerVersion = patchServerVersion;
    }
}