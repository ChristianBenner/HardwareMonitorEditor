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

import com.bennero.common.TransitionType;
import com.bennero.client.pages.InformationPage;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * State data for the InformationPage. InformationStateData is a subclass of StateData, it stores information about the
 * current state of the application so that the GUI can be created or destroyed at any time (meaning that the graphical
 * user interface does not have to be loaded into memory if it is not in use - it can be loaded or destroyed at any
 * time)
 *
 * @see         InformationPage
 * @see         StateData
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class InformationStateData extends StateData
{
    private static final String NAME = "INFORMATION";

    private final String title;
    private final String info;
    private final String buttonText;
    private final EventHandler buttonEvent;

    public InformationStateData(String title, String info, String buttonText, EventHandler buttonEvent)
    {
        super(NAME, TransitionType.FADE);
        this.title = title;
        this.info = info;
        this.buttonText = buttonText;
        this.buttonEvent = buttonEvent;
    }

    @Override
    public Node createGUI()
    {
        return new InformationPage(title, info, buttonText, buttonEvent);
    }
}