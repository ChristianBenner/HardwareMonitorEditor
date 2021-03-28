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

package com.bennero.network;

/**
 * ConnectionStatus enumeration contains the different states in which a connected event can be in.
 * CONNECTING:              Current connecting to the hardware monitor
 * CONNECTED:               Connected to the hardware monitor
 * FAILED:                  Unexpected connection failure (e.g. no internet connection)
 * CONNECTION_REFUSED:      Hardware monitor refused connection but did not specify why
 * VERSION_MISMATCH:        Hardware monitor refused connection because the editor version is not compatible
 * IN_USE:                  Hardware monitor refused connection because it is in use by another editor
 * HEARTBEAT_TIMEOUT:       No heartbeat has been received from the hardware monitor within the heartbeat timeout time
 * UNEXPECTED_DISCONNECT:   Unexpected loss in connection with a connected hardware monitor
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public enum ConnectionStatus
{
    CONNECTING,
    CONNECTED,
    FAILED,
    CONNECTION_REFUSED,
    VERSION_MISMATCH,
    IN_USE,
    HEARTBEAT_TIMEOUT,
    UNEXPECTED_DISCONNECT
}