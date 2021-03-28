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

package com.bennero.states;

import com.bennero.networking.ConnectionInformation;
import com.bennero.common.TransitionType;
import com.bennero.pages.ConnectionListPage;
import javafx.scene.Node;

import java.util.List;

/**
 * State data for the ConnectionListPage. ConnectionListStateData is a subclass of StateData, it stores information
 * about the current state of the application so that the GUI can be created or destroyed at any time (meaning that the
 * graphical user interface does not have to be loaded into memory if it is not in use - it can be loaded or destroyed
 * at any time)
 *
 * @see         ConnectionListPage
 * @see         StateData
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ConnectionListStateData extends StateData
{
    private static final String NAME = "CONNECTION_LIST";
    private final List<ConnectionInformation> connections;

    public ConnectionListStateData(List<ConnectionInformation> connections)
    {
        super(NAME, TransitionType.FADE);
        this.connections = connections;
    }

    @Override
    public Node createGUI()
    {
        ConnectionListPage connectionListPage = new ConnectionListPage();
        connectionListPage.setAvailableConnectionsList(connections);
        return connectionListPage;
    }
}