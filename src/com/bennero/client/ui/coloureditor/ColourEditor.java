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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Provides an pop-up that allows the user to select or configure a colour. The pop-up comprises of three different tab
 * options so that the user has multiple ways of achieving their desired colour. These tabs are:
 * - Preset Colours
 * - RGB Sliders
 * - HSV Sliders
 *
 * todo: HEX value entry tab
 * todo: Text field entries on the RGB and HSV tabs
 *
 * @see         Stage
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ColourEditor extends Stage
{
    private PresetTab presetsTab;
    private RGBTab rgbTab;
    private HSVTab hsvTab;

    private Color colourIn;
    private ChangeListener<Color> handler;

    public ColourEditor()
    {
        super.setTitle("Colour Editor");
        super.initModality(Modality.APPLICATION_MODAL);
        super.setOnCloseRequest(windowEvent -> close(false));

        BorderPane basePane = new BorderPane();
        basePane.setId("standard-pane");

        Label title = new Label("Select Colour");
        title.setId("colour-editor-title");
        BorderPane.setAlignment(title, Pos.CENTER);

        // on CLOSE = subject.setColour(colourIn);

        TabPane tabPane = new TabPane();
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, t1) ->
                {
                    if (handler != null && colourIn != null)
                    {
                        handler.changed(null, null, colourIn);
                        presetsTab.setColourData(colourIn);
                        rgbTab.setColourData(colourIn);
                        hsvTab.setColourData(colourIn);
                    }
                }
        );

        presetsTab = new PresetTab();
        rgbTab = new RGBTab();
        hsvTab = new HSVTab();

        tabPane.getStyleClass().add("floating");
        tabPane.setId("standard-pane");

        tabPane.getTabs().addAll(presetsTab, rgbTab, hsvTab);

        Button doneButton = new Button("Done");
        doneButton.setId("hw-default-button");
        doneButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        doneButton.setOnAction(actionEvent -> close(true));

        basePane.setTop(title);
        basePane.setCenter(tabPane);
        basePane.setBottom(doneButton);

        Scene dialogScene = new Scene(basePane, 300, 300);
        dialogScene.getStylesheets().add("stylesheet.css");
        super.setScene(dialogScene);
    }

    public void setHandler(Color colourIn, ChangeListener<Color> handler)
    {
        this.colourIn = colourIn;
        this.handler = handler;

        presetsTab.setHandler(handler, colourIn);
        rgbTab.setHandler(handler, colourIn);
        hsvTab.setHandler(handler, colourIn);
        super.show();
    }

    public void close(boolean done)
    {
        // Revert colour back if not done
        if (!done)
        {
            handler.changed(null, null, colourIn);
            presetsTab.setColourData(colourIn);
            rgbTab.setColourData(colourIn);
            hsvTab.setColourData(colourIn);
        }

        presetsTab.removeHandler();
        rgbTab.removeHandler();
        hsvTab.removeHandler();
        super.hide();
    }
}
