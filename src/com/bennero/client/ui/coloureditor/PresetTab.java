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

package com.bennero.client.ui.coloureditor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * PresetTab is a tab for the ColourEditor pop-up. It implements ColourTab and extends Tab (JavaFX). The Preset tab
 * provides many options of pre-defined colours that the user can select from. This allows the user to easily select a
 * colour if they like one that they see.
 *
 * @see         ColourTab
 * @see         Tab
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PresetTab extends Tab implements ColourTab
{
    private PresetColourButton currentlySelected;
    private ChangeListener<Color> handler;
    public PresetTab()
    {
        super("Presets");

        GridPane gridPane = new GridPane();
        gridPane.setId("standard-pane");
        Color[] colorList = new Color[]{
                Color.rgb(255, 128, 128),
                Color.rgb(255, 255, 128),
                Color.rgb(128, 255, 128),
                Color.rgb(0, 255, 128),
                Color.rgb(128, 255, 255),
                Color.rgb(0, 128, 255),
                Color.rgb(255, 128, 192),
                Color.rgb(255, 128, 255),
                Color.rgb(255, 0, 0),
                Color.rgb(255, 255, 0),
                Color.rgb(128, 255, 0),
                Color.rgb(0, 255, 64),
                Color.rgb(0, 255, 255),
                Color.rgb(0, 128, 192),
                Color.rgb(128, 128, 192),
                Color.rgb(255, 0, 255),
                Color.rgb(128, 64, 64),
                Color.rgb(255, 128, 64),
                Color.rgb(0, 255, 0),
                Color.rgb(0, 128, 128),
                Color.rgb(0, 64, 128),
                Color.rgb(128, 128, 255),
                Color.rgb(128, 0, 64),
                Color.rgb(255, 0, 128),
                Color.rgb(128, 0, 0),
                Color.rgb(255, 128, 0),
                Color.rgb(0, 128, 0),
                Color.rgb(0, 128, 64),
                Color.rgb(0, 0, 255),
                Color.rgb(0, 0, 160),
                Color.rgb(128, 0, 128),
                Color.rgb(128, 0, 255),
                Color.rgb(64, 0, 0),
                Color.rgb(128, 64, 0),
                Color.rgb(0, 64, 0),
                Color.rgb(0, 64, 64),
                Color.rgb(0, 0, 128),
                Color.rgb(0, 0, 64),
                Color.rgb(64, 0, 64),
                Color.rgb(64, 0, 128),
                Color.rgb(0, 0, 0),
                Color.rgb(128, 128, 0),
                Color.rgb(128, 128, 64),
                Color.rgb(128, 128, 128),
                Color.rgb(64, 128, 128),
                Color.rgb(192, 192, 192),
                Color.rgb(64, 0, 64),
                Color.rgb(255, 255, 255)
        };

        int row = 0;
        final int PER_ROW = 8;
        for (int i = 0; i < colorList.length; i++)
        {
            PresetColourButton presetColourButton = new PresetColourButton(colorList[i]);
            presetColourButton.setOnAction(actionEvent ->
            {
                setColourData(presetColourButton.getColour());

                if (currentlySelected != null)
                {
                    currentlySelected.deselect();
                }

                presetColourButton.select();
                currentlySelected = presetColourButton;

                if (handler != null)
                {
                    handler.changed(null, null, presetColourButton.getColour());
                }
            });

            row = i / PER_ROW;
            gridPane.add(presetColourButton, i - (PER_ROW * row), row);
        }

        gridPane.setAlignment(Pos.CENTER);

        super.setContent(gridPane);
        super.setClosable(false);
    }

    @Override
    public void setColourData(Color colourData)
    {
        if (currentlySelected != null)
        {
            this.currentlySelected.deselect();
            this.currentlySelected = null;
        }
    }

    @Override
    public void setHandler(ChangeListener<Color> handler, Color colourIn)
    {
        setColourData(colourIn);
        this.handler = handler;
    }

    @Override
    public void removeHandler()
    {
        handler = null;
    }

    class PresetColourButton extends Button
    {
        private Color colour;
        private boolean selected;

        public PresetColourButton(Color colour)
        {
            super("");
            super.setBackground(new Background(new BackgroundFill(colour, CornerRadii.EMPTY, Insets.EMPTY)));
            super.setPrefSize(30.0, 30.0);
            super.setMaxSize(30.0, 30.0);
            super.hoverProperty().addListener((observableValue, aBoolean, hover) ->
            {
                if (!selected)
                {
                    if (hover)
                    {
                        setBorder(new Border(new BorderStroke(colour.darker(), BorderStrokeStyle.SOLID,
                                CornerRadii.EMPTY, new BorderWidths(2))));
                    }
                    else
                    {
                        setBorder(Border.EMPTY);
                    }
                }
            });

            this.colour = colour;
            this.selected = false;
        }

        public Color getColour()
        {
            return colour;
        }

        public void select()
        {
            this.selected = true;
            super.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(2), new Insets(0))));
        }

        public void deselect()
        {
            this.selected = false;
            super.setBorder(Border.EMPTY);
        }
    }
}
