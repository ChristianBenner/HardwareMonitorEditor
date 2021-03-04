/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/**
 * A simple custom button with a plus icon used to add new pages to the PageOverview page
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NewPageButton extends Button
{
    private static final CornerRadii BACKGROUND_CORNER_RADII = new CornerRadii(15.0);
    private static final Background BACKGROUND = new Background(new BackgroundFill(Color.DARKSLATEBLUE,
            BACKGROUND_CORNER_RADII, Insets.EMPTY));
    private static final Background BACKGROUND_HOVER = new Background(new BackgroundFill(Color.LIGHTBLUE,
            BACKGROUND_CORNER_RADII, Insets.EMPTY));

    public NewPageButton(final int width, final int height)
    {
        super("+");
        super.setMinSize(width, height);
        super.setBackground(BACKGROUND);
        super.setOnMouseEntered(mouseEvent -> NewPageButton.super.setBackground(BACKGROUND_HOVER));
        super.setOnMouseExited(mouseEvent -> NewPageButton.super.setBackground(BACKGROUND));
    }
}
