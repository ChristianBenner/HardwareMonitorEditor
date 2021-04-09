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

package com.bennero.client.pages;

import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.SkinHelper;
import com.bennero.client.config.SaveManager;
import com.bennero.client.core.ApplicationCore;
import com.bennero.client.states.PageEditorStateData;
import com.bennero.client.states.StateData;
import com.bennero.client.ui.coloureditor.ColourEditor;
import com.bennero.client.ui.UIHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.bennero.common.Skin.*;
import static com.bennero.client.ui.UIHelper.*;

/**
 * SensorEditor is a system for customising the look of how a sensor presents hardware data. The user can select from
 * many different types of gauges and then customise each attribute of the selected gauge type. Attributes such as
 * colour and text can be customised.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SensorEditor extends BorderPane
{
    private ColourEditor colourEditor;
    private VBox editOptions;
    private VBox interchangeableEditOptions;
    private BorderPane centerPane;
    private BorderPane gaugePane;

    private SaveManager saveManager;

    public SensorEditor(PageData pageData, Sensor sensor, StateData backButtonState)
    {
        this.saveManager = SaveManager.getInstance();

        super.setBackground(new Background(new BackgroundFill(Color.DARKGREY, CornerRadii.EMPTY, Insets.EMPTY)));
        super.setPadding(new Insets(10));

        editOptions = new VBox();
        editOptions.setId("default-pane");
        editOptions.setPadding(new Insets(5));

        interchangeableEditOptions = new VBox();
        colourEditor = new ColourEditor();

        Label pageTitle = new Label("Customising '" + sensor.getTitle() + "' Sensor");
        pageTitle.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(pageTitle, Pos.CENTER);
        pageTitle.setId("pane-title");
        super.setTop(pageTitle);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane skinSelection = UIHelper.createComboBoxOption("Skin", SkinHelper.getString(sensor.getSkin()),
                SkinHelper.getNames(),
                (observableValue, s, selected) ->
                {
                    if (SkinHelper.containsSkin(selected))
                    {
                        sensor.setSkin(SkinHelper.getByteCode(selected));
                    }

                    loadSkinSpecificUI(selected, sensor);
                });

        /////////// NAME PANE
        Label nameLabel = new Label("Name:");
        nameLabel.setId("sensor-editor-label");

        TextField nameTextField = new TextField(sensor.getTitle());
        nameTextField.setId("hw-text-field");
        nameTextField.textProperty().addListener((observableValue, s, t1) -> sensor.setTitle(t1));

        BorderPane nameBox = new BorderPane();
        nameBox.setPadding(new Insets(5));
        BorderPane.setAlignment(nameBox, Pos.CENTER_LEFT);
        nameBox.setLeft(nameLabel);
        nameBox.setRight(nameTextField);

        editOptions.getChildren().add(nameBox);
        editOptions.getChildren().add(skinSelection);

        editOptions.getChildren().add(createIntSpinnerOption("Row Span", 1, 1, pageData.getRows() - sensor.getRow(),
                (observableValue, integer, t1) -> sensor.setRowSpan(t1)));
        editOptions.getChildren().add(createIntSpinnerOption("Column Span", 1, 1, pageData.getColumns() - sensor.getColumn(),
                (observableValue, integer, t1) -> sensor.setColumnSpan(t1)));
        editOptions.setAlignment(Pos.CENTER_LEFT);

        /////////// CUSTOM MAX VALUE PANE
        BorderPane maxValueGroup = createIntSpinnerOption("Max Value", (int) sensor.getMax(), 0,
                Integer.MAX_VALUE, (observableValue, integer, t1) -> sensor.setThreshold(t1));
        editOptions.getChildren().add(maxValueGroup);

        // Populate the edit options
        loadSkinSpecificUI(sensor.getSkin(), sensor);

        scrollPane.setFitToWidth(true);
        scrollPane.setContent(editOptions);
        centerPane = new BorderPane();
        centerPane.setPadding(new Insets(10.0, 0.0, 10.0, 0.0));
        centerPane.setCenter(scrollPane);

        gaugePane = new BorderPane();
        gaugePane.setPadding(new Insets(0.0, 0.0, 0.0, 20.0));
        gaugePane.setCenter(sensor);
        centerPane.setRight(gaugePane);

        super.setCenter(centerPane);

        BorderPane footerPane = new BorderPane();
        Button backButton = new Button("Back");
        backButton.setOnAction(actionEvent -> ApplicationCore.s_setApplicationState(backButtonState));

        backButton.setId("hw-default-button");
        footerPane.setLeft(backButton);

        Button doneButton = new Button("Done");
        doneButton.setId("hw-default-button");
        footerPane.setRight(doneButton);
        doneButton.setOnAction(actionEvent ->
        {
            if (!pageData.containsSensor(sensor))
            {
                // Attempting to add new sensor to page, check space first
                if (pageData.isSpaceFree(sensor))
                {
                    pageData.addSensor(sensor);
                    saveManager.getSaveData().save();
                    ApplicationCore.s_setApplicationState(new PageEditorStateData(pageData));
                }
                else
                {
                    // Error pop-up because it will not fit
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot Fit", ButtonType.OK);
                    alert.setContentText("The sensor will not fit with the specified row and column spans, another sensor is in the way. Please adjust the values");
                    alert.showAndWait();
                }
            }
            else
            {
                // This is just a sensor edit, save and display page editor
                saveManager.getSaveData().save();
                ApplicationCore.s_setApplicationState(new PageEditorStateData(pageData));
            }
        });

        super.setBottom(footerPane);

        super.setId("standard-pane");
        pageTitle.setId("pane-title");
    }

    private void loadSkinSpecificUI(byte skin, Sensor sensor)
    {
        editOptions.getChildren().remove(interchangeableEditOptions);
        interchangeableEditOptions = new VBox();
        editOptions.getChildren().add(interchangeableEditOptions);

        // Add UI for supported customisations only
        if (SkinHelper.checkSupport(skin, FOREGROUND_BASE_COLOUR_SUPPORTED))
        {
            addForegroundBaseColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, TITLE_COLOUR_SUPPORTED))
        {
            addTitleColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, VALUE_COLOUR_SUPPORTED))
        {
            addValueColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, UNIT_COLOUR_SUPPORTED))
        {
            addUnitColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, BAR_COLOUR_SUPPORTED))
        {
            addBarColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, AVERAGE_COLOUR_SUPPORTED))
        {
            addAverageUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, NEEDLE_COLOUR_SUPPORTED))
        {
            addNeedleColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, KNOB_COLOUR_SUPPORTED))
        {
            addKnobColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, THRESHOLD_COLOUR_SUPPORTED))
        {
            addThresholdUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, BAR_BACKGROUND_COLOUR_SUPPORTED))
        {
            addBarBackgroundColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, TICK_LABEL_COLOUR_SUPPORTED))
        {
            addTickLabelColourUI(sensor);
        }

        if (SkinHelper.checkSupport(skin, TICK_MARK_COLOUR_SUPPORTED))
        {
            addTickMarkColourUI(sensor);
        }
    }

    private void loadSkinSpecificUI(String skin, Sensor sensor)
    {
        loadSkinSpecificUI(SkinHelper.getByteCode(skin), sensor);
    }

    private void addAverageUI(Sensor sensor)
    {
        final boolean ENABLED_BY_DEFAULT = false;

        BorderPane colourOptions = createColourOption(colourEditor, "Average Colour", sensor.getAverageColour(),
                (observableValue, color, t1) -> sensor.setAverageColour(t1));
        colourOptions.setDisable(!ENABLED_BY_DEFAULT);

        BorderPane averagePeriodGroup = createIntSpinnerOption("Average Period",
                sensor.getAveragingPeriod(), 1000,
                Integer.MAX_VALUE,
                (observableValue, integer, t1) -> sensor.setAveragingPeriod(t1));
        averagePeriodGroup.setDisable(!ENABLED_BY_DEFAULT);

        BorderPane averageOption = createCheckboxOption("Show Average", ENABLED_BY_DEFAULT, (observableValue, aBoolean, t1) ->
        {
            sensor.setAverageEnabled(t1);
            colourOptions.setDisable(!t1);
            averagePeriodGroup.setDisable(!t1);
        });

        interchangeableEditOptions.getChildren().add(averageOption);
        interchangeableEditOptions.getChildren().add(colourOptions);
        interchangeableEditOptions.getChildren().add(averagePeriodGroup);
    }

    private void addNeedleColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Needle Colour", sensor.getNeedleColour(),
                (observableValue, color, t1) -> sensor.setNeedleColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addValueColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Value Colour", sensor.getValueColour(),
                (observableValue, color, t1) -> sensor.setValueColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addUnitColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Unit Colour", sensor.getUnitColour(),
                (observableValue, color, t1) -> sensor.setUnitColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addKnobColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Knob Colour", sensor.getKnobColour(),
                (observableValue, color, t1) -> sensor.setKnobColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addBarColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Bar Colour", sensor.getBarColour(),
                (observableValue, color, t1) -> sensor.setBarColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addThresholdUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Threshold Colour", sensor.getThresholdColour(),
                (observableValue, color, t1) -> sensor.setThresholdColour(t1));

        BorderPane thresholdValueGroup = createIntSpinnerOption("Threshold Value",
                (int) sensor.getThreshold(), (int) (sensor.getMax() * 0.25),
                (int) sensor.getMax(),
                (observableValue, integer, t1) -> sensor.setThreshold(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
        interchangeableEditOptions.getChildren().add(thresholdValueGroup);
    }

    private void addTitleColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Title Colour", sensor.getTitleColour(),
                (observableValue, color, t1) -> sensor.setTitleColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addBarBackgroundColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Bar Background Colour", sensor.getBarBackgroundColour(),
                (observableValue, color, t1) -> sensor.setBarBackgroundColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addForegroundBaseColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Foreground Colour", sensor.getTitleColour(),
                (observableValue, color, t1) ->
                {
                    sensor.setForegroundColour(t1);

                    // These reset the colours to show correctly on the graphic
                    if (sensor.getNeedleColour() != null)
                    {
                        sensor.setNeedleColour(sensor.getNeedleColour());
                    }

                    if (sensor.getValueColour() != null)
                    {
                        sensor.setValueColour(sensor.getValueColour());
                    }

                    if (sensor.getUnitColour() != null)
                    {
                        sensor.setUnitColour(sensor.getUnitColour());
                    }

                    if (sensor.getKnobColour() != null)
                    {
                        sensor.setKnobColour(sensor.getKnobColour());
                    }

                    if (sensor.getBarColour() != null)
                    {
                        sensor.setBarColour(sensor.getBarColour());
                    }

                    if (sensor.getThresholdColour() != null)
                    {
                        sensor.setThresholdColour(sensor.getThresholdColour());
                    }

                    if (sensor.getTitleColour() != null)
                    {
                        sensor.setTitleColour(sensor.getTitleColour());
                    }

                    if (sensor.getBarBackgroundColour() != null)
                    {
                        sensor.setBarBackgroundColour(sensor.getBarBackgroundColour());
                    }
                });

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addTickLabelColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Tick Label Colour", sensor.getTickLabelColour(),
                (observableValue, color, t1) -> sensor.setTickLabelColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }

    private void addTickMarkColourUI(Sensor sensor)
    {
        BorderPane colourOptions = createColourOption(colourEditor, "Tick Label Colour", sensor.getTickMarkColour(),
                (observableValue, color, t1) -> sensor.setTickMarkColour(t1));

        interchangeableEditOptions.getChildren().add(colourOptions);
    }
}
