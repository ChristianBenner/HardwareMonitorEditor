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

package com.bennero.client.states;

import com.bennero.client.pages.NetworkScanInfoPage;
import com.bennero.common.TransitionType;
import com.bennero.common.networking.ConnectionInformation;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * State data for the NetworkScanInfoPage. NetworkScanStateData is a subclass of StateData, it stores information about
 * the current state of the application so that the GUI can be created or destroyed at any time (meaning that the
 * graphical user interface does not have to be loaded into memory if it is not in use - it can be loaded or destroyed
 * at any time)
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see StateData
 * @since 1.0
 */
public class NetworkScanStateData extends StateData {
    private static final String NAME = "NETWORK_SCAN";
    private final String information;
    private NetworkScanInfoPage networkScanInfoPage;
    private int numberOfFoundDevices;
    private EventHandler showOtherDevices;
    private boolean connectToLast;
    private ConnectionInformation lastConnectedDevice;

    public NetworkScanStateData(String information, EventHandler showOtherDevices) {
        super(NAME, TransitionType.FADE);
        this.information = information;
        this.showOtherDevices = showOtherDevices;
        this.connectToLast = false;
        networkScanInfoPage = null;
        numberOfFoundDevices = 0;
        lastConnectedDevice = null;
    }

    @Override
    public Node createGUI() {
        networkScanInfoPage = new NetworkScanInfoPage(information, showOtherDevices);
        networkScanInfoPage.setNumberOfFoundDevices(numberOfFoundDevices);
        return networkScanInfoPage;
    }

    public ConnectionInformation getLastConnectedDevice() {
        return lastConnectedDevice;
    }

    public void setLastConnectedDevice(ConnectionInformation lastConnectedDevice) {
        this.lastConnectedDevice = lastConnectedDevice;
    }

    public void setConnectToLast(boolean state) {
        this.connectToLast = state;
    }

    public boolean shouldConnectToLast() {
        return connectToLast;
    }

    public int getNumberOfFoundDevices() {
        return numberOfFoundDevices;
    }

    public void setNumberOfFoundDevices(int numberOfFoundDevices) {
        this.numberOfFoundDevices = numberOfFoundDevices;

        if (networkScanInfoPage != null) {
            networkScanInfoPage.setNumberOfFoundDevices(numberOfFoundDevices);
        }
    }
}
