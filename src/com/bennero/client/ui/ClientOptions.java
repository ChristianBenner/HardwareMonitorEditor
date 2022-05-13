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

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * A pop-up that displays all of the options available for the hardware monitor. This is global options that are applied
 * to all profiles and stored in the main config file such as sensor update interval and sensor animation duration.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see Stage
 * @since 1.0
 */
public class ClientOptions extends Stage {

    public ClientOptions(int currentSensorUpdateInterval,
                         int currentSensorAnimationDuration,
                         ChangeListener<Integer> sensorUpdateIntervalMs,
                         ChangeListener<Integer> sensorAnimationDuration,
                         EventHandler doneEditing) {
        super.setTitle("Page Options");
        super.initModality(Modality.APPLICATION_MODAL);
        super.setOnCloseRequest(windowEvent -> hide());

        BorderPane basePane = new BorderPane();
        basePane.setId("standard-pane");

        Label title = new Label("Save Options");
        title.setId("colour-editor-title");
        BorderPane.setAlignment(title, Pos.CENTER);

        VBox optionsPane = new VBox();
        optionsPane.getChildren().add(UIHelper.createIntSpinnerOption("Sensor Update Interval",
                currentSensorUpdateInterval, 300, Integer.MAX_VALUE, sensorUpdateIntervalMs));
        optionsPane.getChildren().add(UIHelper.createIntSpinnerOption("Sensor Animation Duration",
                currentSensorAnimationDuration, 150, Integer.MAX_VALUE, sensorAnimationDuration));

        Button doneButton = new Button("Done");
        doneButton.setId("hw-default-button");
        doneButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        doneButton.setOnAction(actionEvent ->
        {
            hide();
            doneEditing.handle(actionEvent);
        });

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(optionsPane);

        basePane.setTop(title);
        basePane.setCenter(scrollPane);
        basePane.setBottom(doneButton);

        Scene dialogScene = new Scene(basePane, 500, 200);
        dialogScene.getStylesheets().add("stylesheet.css");
        super.setScene(dialogScene);
    }
}
