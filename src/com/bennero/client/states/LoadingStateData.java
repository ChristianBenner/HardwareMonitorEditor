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

import com.bennero.client.pages.LoadingInformationPage;
import com.bennero.common.TransitionType;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * State data for the LoadingInformationPage. LoadingStateData is a subclass of StateData, it stores information about
 * the current state of the application so that the GUI can be created or destroyed at any time (meaning that the
 * graphical user interface does not have to be loaded into memory if it is not in use - it can be loaded or destroyed
 * at any time)
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see LoadingInformationPage
 * @see StateData
 * @since 1.0
 */
public class LoadingStateData extends StateData {
    private static final String NAME = "LOADING";

    private final String title;
    private final String subtitle;
    private final String buttonText;
    private final EventHandler eventHandler;

    public LoadingStateData(String title) {
        super(NAME, TransitionType.FADE);
        this.title = title;
        subtitle = null;
        buttonText = null;
        eventHandler = null;
    }

    public LoadingStateData(String title, String subtitle) {
        super(NAME, TransitionType.FADE);
        this.title = title;
        this.subtitle = subtitle;
        buttonText = null;
        eventHandler = null;
    }

    public LoadingStateData(String title, String subtitle, String buttonText, EventHandler eventHandler) {
        super(NAME, TransitionType.FADE);
        this.title = title;
        this.subtitle = subtitle;
        this.buttonText = buttonText;
        this.eventHandler = eventHandler;
    }

    @Override
    public Node createGUI() {
        if (buttonText != null && eventHandler != null && subtitle != null) {
            return new LoadingInformationPage(title, subtitle, buttonText, eventHandler);
        } else if (subtitle != null) {
            return new LoadingInformationPage(title, subtitle);
        } else {
            return new LoadingInformationPage(title);
        }
    }
}