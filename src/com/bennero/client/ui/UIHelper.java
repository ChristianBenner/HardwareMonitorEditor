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

import com.bennero.client.ui.coloureditor.ColourEditor;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Collection;

/**
 * Provides some commonly used static functions associated to user interface.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class UIHelper {
    public static BorderPane createColourOption(ColourEditor colourEditor,
                                                String text, Color startColor,
                                                ChangeListener<Color> changeListener) {
        Label label = new Label(text + ":");
        label.setId("sensor-editor-label");

        HBox colourDisplay = new HBox();
        colourDisplay.setPrefWidth(50.0);
        colourDisplay.setBackground(new Background(new BackgroundFill(startColor,
                new CornerRadii(5.0, 0.0, 0.0, 5.0, false), Insets.EMPTY)));

        Button changeColourButton = new Button("Change");
        changeColourButton.setId("hw-sensor-editor-button");
        changeColourButton.heightProperty().addListener((observableValue, number, t1) -> colourDisplay.setPrefHeight((double) t1));

        ChangeListener<Color> colourChangeListener = (observableValue, color, t1) ->
        {
            changeListener.changed(observableValue, color, t1);
            colourDisplay.setBackground(new Background(new BackgroundFill(t1,
                    new CornerRadii(5.0, 0.0, 0.0, 5.0, false), Insets.EMPTY)));
        };

        changeColourButton.setOnAction(thresholdColourChangeActionEvent -> colourEditor.setHandler(startColor, colourChangeListener));

        HBox editOptionsGroup = new HBox();
        editOptionsGroup.getChildren().add(colourDisplay);
        editOptionsGroup.getChildren().add(changeColourButton);

        BorderPane allGroup = new BorderPane();
        allGroup.setPadding(new Insets(5));
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        allGroup.setLeft(label);
        allGroup.setRight(editOptionsGroup);

        return allGroup;
    }

    public static BorderPane createCheckboxOption(String text,
                                                  boolean checkedStartState,
                                                  ChangeListener<Boolean> changeListener) {
        Label label = new Label(text + ":");
        label.setId("sensor-editor-label");

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(checkedStartState);
        checkBox.selectedProperty().addListener(changeListener);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5));
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        BorderPane.setAlignment(checkBox, Pos.CENTER_RIGHT);
        borderPane.setLeft(label);
        borderPane.setRight(checkBox);

        return borderPane;
    }

    public static BorderPane createIntSpinnerOption(String text,
                                                    int startValue,
                                                    int minVal,
                                                    int maxVal,
                                                    ChangeListener<Integer> changeListener) {
        return createIntSpinnerOption(text, startValue, minVal, maxVal, 1, changeListener);
    }

    public static BorderPane createIntSpinnerOption(String text,
                                                    int startValue,
                                                    int minVal,
                                                    int maxVal,
                                                    int changeAmount,
                                                    ChangeListener<Integer> changeListener) {
        Label label = new Label(text + ":");
        label.setId("sensor-editor-label");

        Spinner spinner = new Spinner(new SpinnerValueFactory.IntegerSpinnerValueFactory(minVal, maxVal, startValue, changeAmount));
        spinner.setId("hw-spinner");
        spinner.setEditable(true);

        // Listener that prevents anything but numbers to be entered into the spinner
        spinner.getEditor().textProperty().addListener((observableValue, s, t1) ->
        {
            if (!t1.matches("\\d*")) {
                spinner.getEditor().setText(t1.replaceAll("[^\\d]", ""));
            }

            if (t1.isEmpty() || t1 == null) {
                changeListener.changed(null, null, minVal);
            } else {
                if (Integer.parseInt(t1) < minVal) {
                    changeListener.changed(null, null, minVal);
                } else if (Integer.parseInt(t1) > maxVal) {
                    changeListener.changed(null, null, maxVal);
                } else {
                    changeListener.changed(null, null, Integer.parseInt(t1));
                }
            }
        });

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5));
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        borderPane.setLeft(label);
        borderPane.setRight(spinner);

        return borderPane;
    }

    public static BorderPane createComboBoxOption(String text,
                                                  String selected,
                                                  Collection<String> objectCollection,
                                                  ChangeListener<String> changeListener) {
        Label label = new Label(text + ":");
        label.setId("sensor-editor-label");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(objectCollection);
        comboBox.getSelectionModel().select(selected);
        comboBox.getSelectionModel().selectedItemProperty().addListener(changeListener);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5));
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        borderPane.setLeft(label);
        borderPane.setRight(comboBox);

        return borderPane;
    }

    public static <E> BorderPane createComboBoxOption(String text,
                                                      E selected,
                                                      Collection<E> objectCollection,
                                                      ChangeListener<E> changeListener) {
        Label label = new Label(text + ":");
        label.setId("sensor-editor-label");

        ComboBox<E> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(objectCollection);
        comboBox.getSelectionModel().select(selected);
        comboBox.getSelectionModel().selectedItemProperty().addListener(changeListener);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5));
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        borderPane.setLeft(label);
        borderPane.setRight(comboBox);

        return borderPane;
    }
}
