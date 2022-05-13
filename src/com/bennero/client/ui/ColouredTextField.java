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

package com.bennero.client.ui;

import com.bennero.common.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * ColouredTextField is a custom user interface component that provides the ability to set the colour of the text using
 * a Color object. The class extends TextField from JavaFX which does not come with methods for specifically setting the
 * text colour, instead it has to be applied as a CSS style which this class provides.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see Color
 * @since 1.0
 */
public class ColouredTextField extends TextField {
    private Color colour;

    public ColouredTextField(Font font, String text, int alignment, Color textColour) {
        super(text);
        setColour(textColour);
        super.setFont(font);
        super.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY,
                Insets.EMPTY)));
        super.setPadding(new Insets(0));

        // Align the text
        switch (alignment) {
            case Constants.TEXT_ALIGNMENT_LEFT:
                super.setAlignment(Pos.CENTER_LEFT);
                break;
            case Constants.TEXT_ALIGNMENT_CENTER:
                super.setAlignment(Pos.CENTER);
                break;
            case Constants.TEXT_ALIGNMENT_RIGHT:
                super.setAlignment(Pos.CENTER_RIGHT);
                break;
        }
    }

    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        String textColourString = "#" + colour.toString().substring(2);
        super.setStyle("-fx-text-inner-color: " + textColourString + ";");
        this.colour = colour;
    }
}
