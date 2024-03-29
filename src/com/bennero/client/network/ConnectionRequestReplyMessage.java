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

import com.bennero.common.PageData;
import com.bennero.common.messages.ConnectionRequestReplyDataPositions;

import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.NAME_STRING_NUM_BYTES;
import static com.bennero.common.networking.NetworkUtils.readString;

/**
 * ConnectionRequestReplyMessage contains the data of a connection request response from a Hardware Monitor. It can
 * contain useful information such as if the connection was rejected and why, alongside the version of the hardware
 * monitor. This is another step in ensuring that an editors cannot connect to incompatible monitors.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see PageData
 * @since 1.0
 */
public class ConnectionRequestReplyMessage {
    private final byte majorVersion;
    private final byte minorVersion;
    private final byte patchVersion;
    private final boolean connectionAccepted;
    private final boolean versionMismatch;
    private final boolean currentlyInUse;
    private final String currentClientHostname;

    public ConnectionRequestReplyMessage(final byte majorVersion,
                                         final byte minorVersion,
                                         final byte patchVersion,
                                         final boolean connectionAccepted,
                                         final boolean versionMismatch,
                                         final boolean currentlyInUse,
                                         final String currentClientHostname) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.connectionAccepted = connectionAccepted;
        this.versionMismatch = versionMismatch;
        this.currentlyInUse = currentlyInUse;
        this.currentClientHostname = currentClientHostname;
    }

    public static ConnectionRequestReplyMessage processConnectionRequestReplyMessageData(byte[] bytes) {
        final byte majorVersion = bytes[ConnectionRequestReplyDataPositions.MAJOR_VERSION_POS];
        final byte minorVersion = bytes[ConnectionRequestReplyDataPositions.MINOR_VERSION_POS];
        final byte patchVersion = bytes[ConnectionRequestReplyDataPositions.PATCH_VERSION_POS];
        final boolean connectionAccepted = bytes[ConnectionRequestReplyDataPositions.CONNECTION_ACCEPTED] == 0x01;
        final boolean versionMismatch = bytes[ConnectionRequestReplyDataPositions.VERSION_MISMATCH] == 0x01;
        final boolean currentlyInUse = bytes[ConnectionRequestReplyDataPositions.CURRENTLY_IN_USE] == 0x01;
        String currentClientHostname = null;

        if (!connectionAccepted && currentlyInUse) {
            currentClientHostname = readString(bytes, ConnectionRequestReplyDataPositions.CURRENT_CLIENT_HOSTNAME,
                    NAME_STRING_NUM_BYTES);
        }

        return new ConnectionRequestReplyMessage(majorVersion, minorVersion, patchVersion, connectionAccepted,
                versionMismatch, currentlyInUse, currentClientHostname);
    }

    public byte getMajorVersion() {
        return majorVersion;
    }

    public byte getMinorVersion() {
        return minorVersion;
    }

    public byte getPatchVersion() {
        return patchVersion;
    }

    public boolean isConnectionAccepted() {
        return connectionAccepted;
    }

    public boolean isVersionMismatch() {
        return versionMismatch;
    }

    public boolean isCurrentlyInUse() {
        return currentlyInUse;
    }

    public String getCurrentClientHostname() {
        return currentClientHostname;
    }
}
