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

import com.bennero.client.config.SaveManager;
import com.bennero.client.core.ApplicationCore;
import com.bennero.client.core.SensorManager;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.states.PageEditorStateData;
import com.bennero.client.states.PageOverviewStateData;
import com.bennero.client.states.SensorEditorStateData;
import com.bennero.client.states.SensorSelectionStateData;
import com.bennero.client.ui.ColouredTextField;
import com.bennero.client.ui.NoSensor;
import com.bennero.client.ui.PageOptions;
import com.bennero.client.ui.TextFieldEditPane;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * PageEditor class is the user interface that lets users customize the page holding sensors (the appearance of a page
 * on the hardware monitor). It is similar to the hardware monitor page display however it also provides some options to
 * edit some components.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PageEditor extends StackPane
{
    private final static Insets PAGE_PADDING = new Insets(10, 10, 10, 10);
    private final static Background HOVER_BACKGROUND = new Background(new BackgroundFill(Color.rgb(50, 50,
            50, 0.2), new CornerRadii(20), Insets.EMPTY));

    private PageData pageData;

    private GridPane sensorPane;
    private VBox headerPane;
    private BorderPane borderPane;
    private ArrayList<Sensor> placedSensors;

    private StackPane titleStackPane;
    private StackPane subtitleStackPane;
    private ColouredTextField titleTextField;
    private ColouredTextField subtitleTextField;

    private SaveManager saveManager;
    private NetworkClient networkClient;

    public PageEditor(PageData pageData)
    {
        this.pageData = pageData;
        this.saveManager = SaveManager.getInstance();
        this.networkClient = NetworkClient.getInstance();
        this.sendPageNetworkData();

        super.setBackground(new Background(new BackgroundFill(pageData.getColour(), CornerRadii.EMPTY, Insets.EMPTY)));

        headerPane = new VBox();
        initTitle();
        initSubtitle();
        BorderPane.setAlignment(headerPane, Pos.CENTER);

        borderPane = new BorderPane();
        initGrid();

        borderPane.setTop(headerPane);
        borderPane.setPadding(PAGE_PADDING);

        super.getChildren().add(borderPane);


        Button backButton = new Button("Back");
        backButton.setOnAction(actionEvent -> ApplicationCore.s_setApplicationState(new PageOverviewStateData()));
        backButton.setId("hw-page-editor-back-button");

        Button optionsButton = new Button("Options");
        optionsButton.setOnAction(actionEvent ->
        {
            PageOptions pageOptions = new PageOptions(pageData, saveManager.getSaveData().getPageDataList(),
                    (observableValue, integer, t1) ->
                    {
                        pageData.setTitleEnabled(t1);
                        if (!t1)
                        {
                            headerPane.getChildren().remove(titleStackPane);
                        }
                        else
                        {
                            initTitle();

                            // To make sure that the sub-title appears below the title
                            if (pageData.isSubtitleEnabled())
                            {
                                if (subtitleStackPane != null)
                                {
                                    headerPane.getChildren().remove(subtitleStackPane);
                                }

                                initSubtitle();
                            }
                        }

                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setSubtitleEnabled(t1);
                        if (!t1)
                        {
                            headerPane.getChildren().remove(subtitleStackPane);
                        }
                        else
                        {
                            initSubtitle();
                        }

                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, color, t1) ->
                    {
                        setBackground(new Background(new BackgroundFill(t1, CornerRadii.EMPTY, Insets.EMPTY)));
                        pageData.setColour(t1);
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setRows(t1);
                        initGrid();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setColumns(t1);
                        initGrid();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, string, t1) ->
                    {
                        pageData.setNextPageId(t1);
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setTransitionType(t1);
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setTransitionTime(t1);
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setDurationMs(t1);
                        networkClient.writePageMessage(pageData);
                    },
                    (EventHandler<Event>) event ->
                    {
                        saveManager.getSaveData().removePageData(pageData);
                        ApplicationCore.s_setApplicationState(new PageOverviewStateData());
                    },
                    event -> saveManager.getSaveData().save());
            pageOptions.show();
        });

        optionsButton.setId("hw-page-editor-options-button");

        StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(optionsButton, Pos.BOTTOM_RIGHT);
        super.getChildren().add(backButton);
        super.getChildren().add(optionsButton);
    }

    protected void initGrid()
    {
        placedSensors = new ArrayList<>();
        sensorPane = new GridPane();
        sensorPane.setPadding(new Insets(15, 15, 15, 15));
        sensorPane.setHgap(10.0f);
        sensorPane.setVgap(10.0f);
        BorderPane.setAlignment(sensorPane, Pos.CENTER);
        sensorPane.setAlignment(Pos.CENTER);

        // Configure the grid pane cells to be of equal size
        RowConstraints rc = new RowConstraints();
        rc.setPercentHeight(100d / pageData.getRows());
        for (int y = 0; y < pageData.getRows(); y++)
        {
            sensorPane.getRowConstraints().add(rc);
        }

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100d / pageData.getColumns());
        for (int x = 0; x < pageData.getColumns(); x++)
        {
            sensorPane.getColumnConstraints().add(cc);
        }

        placeSensors(pageData);
        placeAddSensorButtons();

        borderPane.setCenter(sensorPane);
    }

    protected void placeSensors(PageData pageData)
    {
        this.pageData = pageData;

        Image editIcon = new Image(getClass().getClassLoader().getResourceAsStream("edit_icon.png"));
        Image removeIcon = new Image(getClass().getClassLoader().getResourceAsStream("remove_icon.png"));

        // Add sensors to page
        for (Sensor sensor : pageData.getSensorList())
        {
            if (!isSpaceTaken(sensor))
            {
                StackPane stackPane = new StackPane();

                // Check if the sensor manager has the sensor we are trying to add
                if(SensorManager.getInstance().isAvailable(sensor))
                {
                    stackPane.getChildren().add(sensor);
                }
                else
                {
                    // Put a warning sign because the sensor was not found in the list provided by bootstrapper
                    stackPane.getChildren().add(new NoSensor(sensor.getOriginalName()));
                }

                Group hoverButtonGroup = new Group();
                hoverButtonGroup.setVisible(false);
                stackPane.setOnMouseEntered(mouseEvent ->
                {
                    hoverButtonGroup.setVisible(true);
                    stackPane.setBackground(HOVER_BACKGROUND);
                });

                stackPane.setOnMouseExited(mouseEvent ->
                {
                    hoverButtonGroup.setVisible(false);
                    stackPane.setBackground(Background.EMPTY);
                });

                HBox hBox = new HBox();

                Button edit = new Button();
                edit.setCursor(Cursor.HAND);
                edit.setPrefSize(32, 32);
                edit.setBackground(new Background(new BackgroundImage(editIcon, BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                edit.setOnAction(event -> ApplicationCore.s_setApplicationState(
                        new SensorEditorStateData(pageData, sensor, new PageEditorStateData(pageData))));

                hBox.getChildren().add(edit);
                Button delete = new Button();
                delete.setCursor(Cursor.HAND);
                delete.setPrefSize(32, 32);
                delete.setBackground(new Background(new BackgroundImage(removeIcon, BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                delete.setOnAction(actionEvent ->
                {
                    // Ask before deleting
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Sensor?", ButtonType.YES, ButtonType.NO);
                    alert.setContentText("Are you sure you want to remove this sensor from the page?");
                    alert.showAndWait();

                    if (alert.getResult() == ButtonType.YES)
                    {
                        pageData.removeSensor(sensor);

                        borderPane.getChildren().remove(sensorPane);
                        initGrid();
                        placeSensors(pageData);
                        saveManager.getSaveData().save();

                        // Send network message to remove the sensor
                        NetworkClient.getInstance().removeSensorMessage((byte) sensor.getUniqueId(), (byte) pageData.getUniqueId());
                    }
                });

                hBox.getChildren().add(delete);
                hoverButtonGroup.getChildren().add(hBox);
                stackPane.getChildren().add(hoverButtonGroup);

                GridPane.setRowSpan(stackPane, sensor.getRowSpan());
                GridPane.setColumnSpan(stackPane, sensor.getColumnSpan());

                placedSensors.add(sensor);
                sensorPane.add(stackPane, sensor.getColumn(), sensor.getRow());
            }
        }
    }

    protected void initTitle()
    {
        if (pageData.isTitleEnabled())
        {
            titleStackPane = new StackPane();
            titleTextField = new ColouredTextField(new Font(42), pageData.getTitle(), pageData.getTitleAlignment(),
                    pageData.getTitleColour());
            titleStackPane.getChildren().add(titleTextField);
            titleStackPane.getChildren().add(new TextFieldEditPane(titleTextField, pageData.getTitleAlignment(), HOVER_BACKGROUND,
                    actionEvent ->
                    {
                        // Ask before deleting
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Title?", ButtonType.YES, ButtonType.NO);
                        alert.setContentText("Are you sure you want to remove the title from the page? (You can get it back under page options)");

                        alert.showAndWait();
                        if (alert.getResult() == ButtonType.YES)
                        {
                            headerPane.getChildren().remove(titleStackPane);
                            pageData.setTitleEnabled(false);
                            saveManager.getSaveData().save();
                            networkClient.writePageMessage(pageData);
                        }
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setTitleAlignment(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, color, t1) ->
                    {
                        pageData.setTitleColour(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, s, t1) ->
                    {
                        pageData.setTitle(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    }));

            headerPane.getChildren().add(titleStackPane);
        }
    }

    protected void initSubtitle()
    {
        if (pageData.isSubtitleEnabled())
        {
            subtitleStackPane = new StackPane();
            subtitleTextField = new ColouredTextField(new Font(28), pageData.getSubtitle(),
                    pageData.getSubtitleAlignment(), pageData.getSubtitleColour());
            subtitleStackPane.getChildren().add(subtitleTextField);
            subtitleStackPane.getChildren().add(new TextFieldEditPane(subtitleTextField, pageData.getSubtitleAlignment(), HOVER_BACKGROUND,
                    actionEvent ->
                    {
                        // Ask before deleting
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Subheading?", ButtonType.YES, ButtonType.NO);
                        alert.setContentText("Are you sure you want to remove the subheading from the page? (You can get it back under page options)");

                        alert.showAndWait();
                        if (alert.getResult() == ButtonType.YES)
                        {
                            headerPane.getChildren().remove(subtitleStackPane);
                            pageData.setSubtitleEnabled(false);
                            saveManager.getSaveData().save();
                            networkClient.writePageMessage(pageData);
                        }
                    },
                    (observableValue, integer, t1) ->
                    {
                        pageData.setSubtitleAlignment(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, color, t1) ->
                    {
                        pageData.setSubtitleColour(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    },
                    (observableValue, s, t1) ->
                    {
                        pageData.setSubtitle(t1);
                        saveManager.getSaveData().save();
                        networkClient.writePageMessage(pageData);
                    }));

            headerPane.getChildren().add(subtitleStackPane);
        }
    }

    private void sendPageNetworkData()
    {
        networkClient.writePageMessage(pageData);

        List<Sensor> sensors = pageData.getSensorList();
        for (int i = 0; i < sensors.size(); i++)
        {
            networkClient.writeSensorMessage(sensors.get(i), (byte) pageData.getUniqueId());
        }
    }

    private boolean isSpaceTaken(Sensor sensor)
    {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++)
        {
            Sensor placedSensor = placedSensors.get(i);

            if (placedSensor != sensor)
            {
                int startColumn = sensor.getColumn();
                int endColumn = startColumn + sensor.getColumnSpan();
                int startRow = sensor.getRow();
                int endRow = startRow + sensor.getRowSpan();

                int placedStartColumn = placedSensor.getColumn();
                int placedEndColumn = placedStartColumn + placedSensor.getColumnSpan();
                int placedStartRow = placedSensor.getRow();
                int placedEndRow = placedStartRow + placedSensor.getRowSpan();

                boolean withinRow = (startRow >= placedStartRow && startRow < placedEndRow) ||
                        (endRow > placedStartRow && endRow <= placedEndRow);
                boolean withinColumn = (startColumn >= placedStartColumn && startColumn < placedEndColumn) ||
                        (endColumn > placedStartColumn && endColumn <= placedEndColumn);

                if (withinRow && withinColumn)
                {
                    taken = true;
                }
            }
        }

        return taken;
    }

    private boolean isSpaceTaken(int column, int row, int columnSpan, int rowSpan)
    {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++)
        {
            Sensor placedSensor = placedSensors.get(i);

            int endColumn = column + columnSpan;
            int endRow = row + rowSpan;
            int placedStartColumn = placedSensor.getColumn();
            int placedEndColumn = placedStartColumn + placedSensor.getColumnSpan();
            int placedStartRow = placedSensor.getRow();
            int placedEndRow = placedStartRow + placedSensor.getRowSpan();

            boolean withinRow = (row >= placedStartRow && row < placedEndRow) ||
                    (endRow > placedStartRow && endRow <= placedEndRow);
            boolean withinColumn = (column >= placedStartColumn && column < placedEndColumn) ||
                    (endColumn > placedStartColumn && endColumn <= placedEndColumn);

            if (withinRow && withinColumn)
            {
                taken = true;
            }
        }

        return taken;
    }

    private void placeAddSensorButtons()
    {

        for (int column = 0; column < pageData.getColumns(); column++)
        {
            for (int row = 0; row < pageData.getRows(); row++)
            {
                if (!isSpaceTaken(column, row, 1, 1))
                {
                    Button addSensorButton = new Button("+");
                    addSensorButton.setId("add-button");
                    addSensorButton.setBackground(new Background(new BackgroundFill(pageData.getColour().invert(),
                            new CornerRadii(10), Insets.EMPTY)));
                    // addSensorButton.setMinSize(150.0, 150.0);
                    addSensorButton.setOnMouseClicked(mouseEvent -> ApplicationCore.s_setApplicationState(
                            new SensorSelectionStateData(pageData, GridPane.getRowIndex(
                                    addSensorButton), GridPane.getColumnIndex(addSensorButton))));
                    sensorPane.add(addSensorButton, column, row);

                    GridPane.setHalignment(addSensorButton, HPos.CENTER);
                    GridPane.setValignment(addSensorButton, VPos.CENTER);
                    GridPane.setFillHeight(addSensorButton, true);
                    GridPane.setFillWidth(addSensorButton, true);
                    GridPane.setHgrow(addSensorButton, Priority.ALWAYS);
                    GridPane.setVgrow(addSensorButton, Priority.ALWAYS);
                }
            }
        }
    }
}
