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

import com.bennero.client.core.ApplicationCore;
import com.bennero.client.core.SensorData;
import com.bennero.client.core.SensorManager;
import com.bennero.client.states.PageEditorStateData;
import com.bennero.client.states.SensorEditorStateData;
import com.bennero.client.states.SensorSelectionStateData;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SensorSelectionPane is a user interface list of the different hardware sensors that the user can select. This page
 * will display when the user has selected an empty space on a page to put a graphical gauge. It allows the sensors to
 * be listed by hardware type e.g. CPU, GPU and Memory and provides a title such as 'Core Clock #1 (MHz)'. After
 * selecting a sensor from the list, it will take the user to the sensor editor page for customisation of the gauge
 * appearance.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class SensorSelectionPane extends BorderPane {
    private PageData pageData;
    private Map<String, List<SensorData>> sensorByHardwareType = new HashMap<>();
    private BorderPane sensorOverview;
    private ComboBox<String> hardwareTypeComboBox;
    private ListView<SensorItem> sensorListView;
    private Button selectButton;
    private int row;
    private int column;

    public SensorSelectionPane() {
        super.setPadding(new Insets(10));

        sensorOverview = new BorderPane();
        sensorListView = new ListView<>();

        hardwareTypeComboBox = new ComboBox<>();

        // When a hardware type is selected, display the sensors that fall under that hardware type
        EventHandler<ActionEvent> event = actionEvent ->
        {
            sensorListView.getItems().clear();
            List<SensorData> sensors = sensorByHardwareType.get(hardwareTypeComboBox.getValue());
            for (SensorData sensor : sensors) {
                sensorListView.getItems().add(new SensorItem(sensor));
            }
        };

        hardwareTypeComboBox.setOnAction(event);

        // FlowPane headerPane = new FlowPane();
        BorderPane titleAndHardwareCollection = new BorderPane();
        Label title = new Label("Select Sensor");
        titleAndHardwareCollection.setTop(title);

        FlowPane hardwareGroupSelectionPane = new FlowPane();
        Label hardwareGroupSelectionLabel = new Label("Hardware Group: ");
        hardwareGroupSelectionPane.getChildren().add(hardwareGroupSelectionLabel);
        hardwareGroupSelectionPane.getChildren().add(hardwareTypeComboBox);
        titleAndHardwareCollection.setCenter(hardwareGroupSelectionPane);

        BorderPane.setAlignment(title, Pos.CENTER);
        // headerPane.getChildren().add(hardwareCollectionLabel);
        // headerPane.getChildren().add(hardwareTypeComboBox);

        BorderPane footerPane = new BorderPane();
        Button backButton = new Button("Back");
        backButton.setOnAction(actionEvent -> ApplicationCore.s_setApplicationState(new PageEditorStateData(pageData)));
        backButton.setId("hw-default-button");
        footerPane.setLeft(backButton);

        selectButton = new Button("Select");
        selectButton.setId("hw-default-button");
        footerPane.setRight(selectButton);

        selectButton.setOnMouseClicked(mouseEvent ->
        {
            if (sensorListView.getSelectionModel().getSelectedItem() != null) {
                SensorData sensorData = sensorListView.getSelectionModel().getSelectedItem().sensorData;
                Sensor sensor = SensorManager.getInstance().createSensorGui(sensorData, row, column);

                ApplicationCore.s_setApplicationState(new SensorEditorStateData(pageData, sensor,
                        new SensorSelectionStateData(pageData, sensor.getRow(), sensor.getColumn())));
            }
        });

        sensorOverview.setTop(titleAndHardwareCollection);
        sensorOverview.setCenter(sensorListView);
        sensorOverview.setBottom(footerPane);

        super.setCenter(sensorOverview);

        super.setId("standard-pane");
        title.setId("pane-title");
        hardwareGroupSelectionLabel.setId("hardware-group-selection-text");
    }

    // the page that we are editing the sensor on as well as its position on the page
    public void setPage(PageData page, int row, int column) {
        this.pageData = page;
        this.row = row;
        this.column = column;
    }

    public void addSensorData(List<SensorData> sensors) {
        for (SensorData sensorData : sensors) {
            addSensorData(sensorData);
        }
    }

    public void addSensorData(SensorData sensor) {
        // If the combo box does not contain the hardware type, add it
        if (!hardwareTypeComboBox.getItems().contains(sensor.getHardwareType())) {
            hardwareTypeComboBox.getItems().add(sensor.getHardwareType());

            // If there is nothing selected, select the hardware type we just added
            if (hardwareTypeComboBox.getSelectionModel().isEmpty()) {
                hardwareTypeComboBox.getSelectionModel().select(sensor.getHardwareType());
            }
        }

        // If the sensor by hardware type does not contain the hardware type, add it
        if (!sensorByHardwareType.containsKey(sensor.getHardwareType())) {
            sensorByHardwareType.put(sensor.getHardwareType(), new ArrayList<>());
        }

        // Add sensor to the sensor map
        sensorByHardwareType.get(sensor.getHardwareType()).add(sensor);

        // If the currently selected hardware type is the same as the sensor, add the sensor to the UI list
        if (hardwareTypeComboBox.getSelectionModel().getSelectedItem().compareTo(sensor.getHardwareType()) == 0) {
            sensorListView.getItems().add(new SensorItem(sensor));
        }
    }

    class SensorItem {
        private SensorData sensorData;

        public SensorItem(SensorData sensorData) {
            this.sensorData = sensorData;
        }

        @Override
        public String toString() {
            return sensorData.getName();
        }
    }
}
