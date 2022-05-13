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
import com.bennero.client.network.NetworkClient;
import com.bennero.client.states.PageEditorStateData;
import com.bennero.client.states.PageOverviewStateData;
import com.bennero.client.states.SensorEditorStateData;
import com.bennero.client.states.SensorSelectionStateData;
import com.bennero.client.ui.ColouredTextField;
import com.bennero.client.ui.EditableSensor;
import com.bennero.client.ui.PageOptions;
import com.bennero.client.ui.TextFieldEditPane;
import com.bennero.client.util.GridUtils;
import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;

/**
 * PageEditor class is the user interface that lets users customize the page holding sensors (the appearance of a page
 * on the hardware monitor). It is similar to the hardware monitor page display however it also provides some options to
 * edit some components.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class PageEditor extends StackPane {
    private final static Insets PAGE_PADDING = new Insets(10, 10, 10, 10);
    private final Background HOVER_BACKGROUND;
    private final Color HIGHLIGHT_COLOUR_TRANSPARENT;
    private final Color HIGHLIGHT_COLOUR;

    private PageData pageData;

    private GridPane sensorPane;
    private Node[][] gridArray;
    private VBox headerPane;
    private BorderPane borderPane;
    private ArrayList<Sensor> placedSensors;
    private ArrayList<Button> addSensorButtons;

    private StackPane titleStackPane;
    private StackPane subtitleStackPane;
    private ColouredTextField titleTextField;
    private ColouredTextField subtitleTextField;

    private SaveManager saveManager;
    private NetworkClient networkClient;

    public PageEditor(PageData pageData) {
        this.pageData = pageData;
        this.saveManager = SaveManager.getInstance();
        this.networkClient = NetworkClient.getInstance();
        this.addSensorButtons = new ArrayList<>();

        HIGHLIGHT_COLOUR = Color.color(pageData.getTitleColour().getRed(), pageData.getTitleColour().getGreen(),
                pageData.getTitleColour().getBlue());
        HIGHLIGHT_COLOUR_TRANSPARENT = Color.color(pageData.getTitleColour().getRed(), pageData.getTitleColour().getGreen(),
                pageData.getTitleColour().getBlue(), 0.2);
        HOVER_BACKGROUND = new Background(new BackgroundFill(HIGHLIGHT_COLOUR_TRANSPARENT, new CornerRadii(20), Insets.EMPTY));

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
                        if (!t1) {
                            headerPane.getChildren().remove(titleStackPane);
                        } else {
                            initTitle();

                            // To make sure that the sub-title appears below the title
                            if (pageData.isSubtitleEnabled()) {
                                if (subtitleStackPane != null) {
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
                        if (!t1) {
                            headerPane.getChildren().remove(subtitleStackPane);
                        } else {
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

    protected void initGrid() {
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
        for (int y = 0; y < pageData.getRows(); y++) {
            sensorPane.getRowConstraints().add(rc);
        }

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100d / pageData.getColumns());
        for (int x = 0; x < pageData.getColumns(); x++) {
            sensorPane.getColumnConstraints().add(cc);
        }

        gridArray = new Node[pageData.getRows()][pageData.getColumns()];
        placeSensors(pageData);
        placeAddSensorButtons();

        borderPane.setCenter(sensorPane);
    }

    private double getGridWidth() {
        double width = 0.0;
        ObservableList<Node> nodes = sensorPane.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (GridPane.getRowIndex(node) == 0) {
                width += node.getBoundsInParent().getWidth();
            }
        }


        width += sensorPane.getHgap() * sensorPane.getColumnCount() + 1;
        return width;

        /*
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }*/
    }


    private Node findNode(int row, int column) {
        ObservableList<Node> nodes = sensorPane.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == column) {
                return node;
            }
        }

        return null;
    }


    protected void placeSensors(PageData pageData) {
        this.pageData = pageData;

        Image editIcon = new Image(getClass().getClassLoader().getResourceAsStream("edit_icon.png"));
        Image removeIcon = new Image(getClass().getClassLoader().getResourceAsStream("remove_icon.png"));
        Image moveIcon = new Image(getClass().getClassLoader().getResourceAsStream("move_icon.png"));

        // Add sensors to page
        for (Sensor sensor : pageData.getSensorList()) {
            if (!GridUtils.isSpaceTaken(placedSensors, sensor)) {
                EditableSensor editableSensor = new EditableSensor(HIGHLIGHT_COLOUR, editIcon, removeIcon, moveIcon,
                        sensor,
                        actionEvent ->
                        {
                            // Edit button has been selected
                            ApplicationCore.s_setApplicationState(
                                    new SensorEditorStateData(pageData, sensor, new PageEditorStateData(pageData)));
                        },
                        actionEvent ->
                        {
                            // Delete button has been selected
                            // Ask before deleting
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Sensor?", ButtonType.YES, ButtonType.NO);
                            alert.setContentText("Are you sure you want to remove this sensor from the page?");
                            alert.showAndWait();

                            if (alert.getResult() == ButtonType.YES) {
                                pageData.removeSensor(sensor);

                                borderPane.getChildren().remove(sensorPane);
                                initGrid();
                                placeSensors(pageData);
                                saveManager.getSaveData().save();

                                // Send network message to remove the sensor
                                NetworkClient.getInstance().writeRemoveSensorMessage((byte) sensor.getUniqueId(), (byte) pageData.getUniqueId());
                            }
                        });

                editableSensor.setMoveButtonDragEvent(moveEvent ->
                {
                    int newCol = moveEvent.getNewColumn();
                    int newRow = moveEvent.getNewRow();
                    int prevCol = moveEvent.getPreviousColumn();
                    int prevRow = moveEvent.getPreviousRow();

                    if (newRow + sensor.getRowSpan() <= pageData.getRows() &&
                            newCol + sensor.getColumnSpan() <= pageData.getColumns() &&
                            newRow >= 0 &&
                            newCol >= 0 &&
                            !GridUtils.isSpaceTaken(placedSensors, newCol, newRow, sensor.getColumnSpan(), sensor.getRowSpan(), sensor)) {
                        int rowSpan = sensor.getRowSpan();
                        int columnSpan = sensor.getColumnSpan();

                        // Remove all nodes where sensor will go but do not free the sensor itself
                        freeSpaceExcluding(newRow, newRow + rowSpan, newCol,
                                newCol + columnSpan, prevRow,
                                prevRow + rowSpan, prevCol,
                                prevCol + columnSpan);

                        // Set where the sensor was dragged from to null
                        gridArray[prevRow][prevCol] = null;

                        // Move the sensor to the left column and expand the right side
                        sensor.setPosition(newRow, newCol);
                        gridArray[newRow][newCol] = editableSensor;

                        GridPane.setColumnIndex(editableSensor, newCol);
                        GridPane.setRowIndex(editableSensor, newRow);

                        // Re-calculate the bounds
                        layout();

                        // When populating freed space, we must also exclude the space which the sensor is in
                        populateFreedSpaceExcluding(prevRow,
                                prevRow + sensor.getRowSpan(),
                                prevCol, prevCol + sensor.getColumnSpan(),
                                newRow, newRow + sensor.getRowSpan(),
                                newCol,
                                newCol + sensor.getColumnSpan());

                        saveManager.getSaveData().save();

                        // Send network message to remove the sensor
                        NetworkClient.getInstance().writeSensorTransformationMessage(sensor,
                                (byte) pageData.getUniqueId());
                    }
                });

                editableSensor.setDragEvent(dragEvent ->
                {
                    // Get the mouse event
                    MouseEvent mouseEvent = dragEvent.getMouseEvent();

                    boolean resized = false;
                    if (dragEvent.isRightExpandable()) {
                        resized = resizeRight(mouseEvent, sensor, editableSensor) || resized;
                    }

                    if (dragEvent.isBottomExpandable()) {
                        resized = resizeBottom(mouseEvent, sensor, editableSensor) || resized;
                    }

                    if (dragEvent.isLeftExpandable()) {
                        resized = resizeLeft(mouseEvent, sensor, editableSensor) || resized;
                    }

                    if (dragEvent.isTopExpandable()) {
                        resized = resizeTop(mouseEvent, sensor, editableSensor) || resized;
                    }

                    // If resized, update the hardware monitor to display it and also save the layout
                    if (resized) {
                        saveManager.getSaveData().save();

                        // Send network message to remove the sensor
                        NetworkClient.getInstance().writeSensorTransformationMessage(sensor,
                                (byte) pageData.getUniqueId());
                    }
                });

                GridPane.setRowSpan(editableSensor, sensor.getRowSpan());
                GridPane.setColumnSpan(editableSensor, sensor.getColumnSpan());

                placedSensors.add(sensor);
                sensorPane.add(editableSensor, sensor.getColumn(), sensor.getRow());
                gridArray[sensor.getRow()][sensor.getColumn()] = editableSensor;
            }
        }
    }

    protected void initTitle() {
        if (pageData.isTitleEnabled()) {
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
                        if (alert.getResult() == ButtonType.YES) {
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

    protected void initSubtitle() {
        if (pageData.isSubtitleEnabled()) {
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
                        if (alert.getResult() == ButtonType.YES) {
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

    // returns null if no sensor in space
    private Sensor getSensorByLocation(int column, int row) {
        // Check if a placed sensor spans across the column and row provided

        boolean taken = false;
        Sensor placedSensor = null;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
            placedSensor = placedSensors.get(i);
            int placedStartColumn = placedSensor.getColumn();
            int placedEndColumn = placedStartColumn + placedSensor.getColumnSpan();
            int placedStartRow = placedSensor.getRow();
            int placedEndRow = placedStartRow + placedSensor.getRowSpan();
            boolean withinRow = (row >= placedStartRow && row < placedEndRow);
            boolean withinColumn = (column >= placedStartColumn && column < placedEndColumn);

            if (withinRow && withinColumn) {
                taken = true;
            }
        }

        if (taken) {
            return placedSensor;
        } else {
            return null;
        }
    }

    private void placeAddSensorButtons() {

        for (int column = 0; column < pageData.getColumns(); column++) {
            for (int row = 0; row < pageData.getRows(); row++) {
                if (!GridUtils.isSpaceTaken(placedSensors, column, row, 1, 1)) {
                    placeAddSensorButton(row, column);
                }
            }
        }
    }

    private void placeAddSensorButton(int row, int column) {
        Button addSensorButton = new Button("+");
        addSensorButton.setId("add-button");
        addSensorButton.setBackground(new Background(new BackgroundFill(pageData.getColour().invert(),
                new CornerRadii(10), Insets.EMPTY)));
        // addSensorButton.setMinSize(150.0, 150.0);
        addSensorButton.setOnMouseClicked(mouseEvent -> ApplicationCore.s_setApplicationState(
                new SensorSelectionStateData(pageData, GridPane.getRowIndex(
                        addSensorButton), GridPane.getColumnIndex(addSensorButton))));
        sensorPane.add(addSensorButton, column, row);
        gridArray[row][column] = addSensorButton;

        GridPane.setHalignment(addSensorButton, HPos.CENTER);
        GridPane.setValignment(addSensorButton, VPos.CENTER);
        GridPane.setFillHeight(addSensorButton, true);
        GridPane.setFillWidth(addSensorButton, true);
        GridPane.setHgrow(addSensorButton, Priority.ALWAYS);
        GridPane.setVgrow(addSensorButton, Priority.ALWAYS);
    }

    // Returns true if the sensor has been resized
    private boolean resizeRight(MouseEvent mouseEvent, Sensor sensor, EditableSensor editableSensor) {
        boolean resized = false;

        double currentX = mouseEvent.getSceneX();
        int sensorRow = sensor.getRow();
        int sensorColumn = sensor.getColumn();
        int sensorColumnSpan = sensor.getColumnSpan();
        int nextAvailableColumn = sensorColumn + sensorColumnSpan;

        if (sensorColumnSpan > 1) {
            Bounds sensorBounds = editableSensor.localToScene(editableSensor.getBoundsInLocal());
            double startX = sensorBounds.getMinX();
            double partWidths = sensorBounds.getWidth() / sensorColumnSpan;
            int newColumnSpan = sensorColumnSpan - 1;
            double lastAvailableColumnEndX = startX + (partWidths * newColumnSpan);

            if (currentX < lastAvailableColumnEndX) {
                sensor.setColumnSpan(newColumnSpan);
                GridPane.setColumnSpan(editableSensor, newColumnSpan);

                // Place sensors in every free spot from the freed up column space
                int freeColStart = sensorColumn + newColumnSpan;
                int freeColEnd = sensorColumn + sensorColumnSpan;
                populateFreedSpace(sensorRow, sensorRow + sensor.getRowSpan(), freeColStart, freeColEnd);

                resized = true;
            }
        }

        if (nextAvailableColumn < pageData.getColumns()) {
            Node nextNode = gridArray[sensorRow][nextAvailableColumn];
            Bounds nextNodeBounds = nextNode.localToScene(nextNode.getBoundsInLocal());
            double nextNodeEndX = nextNodeBounds.getMaxX();

            int newColumnSpan = nextAvailableColumn - sensorColumn + 1;

            // This checks if the mouse is passed the next nodes right side, it also checks that there is no sensor
            // covering that space
            if (currentX > nextNodeEndX && !GridUtils.isRegionTaken(placedSensors,
                    sensorColumn + sensorColumnSpan, sensorRow, sensorColumn + newColumnSpan,
                    sensorRow + sensor.getRowSpan())) {
                sensor.setColumnSpan(newColumnSpan);
                GridPane.setColumnSpan(editableSensor, newColumnSpan);
                removeUnusedNodesAfterExpansion(sensor);

                resized = true;
            }
        }

        return resized;
    }

    // Returns true if the sensor has been resized
    private boolean resizeBottom(MouseEvent mouseEvent, Sensor sensor, EditableSensor editableSensor) {
        boolean resized = false;

        double currentY = mouseEvent.getSceneY();
        int sensorRow = sensor.getRow();
        int sensorColumn = sensor.getColumn();
        int sensorRowSpan = sensor.getRowSpan();
        int nextAvailableRow = sensorRow + sensorRowSpan;

        if (sensorRowSpan > 1) {
            Bounds sensorBounds = editableSensor.localToScene(editableSensor.getBoundsInLocal());
            double startY = sensorBounds.getMinY();
            double partHeights = sensorBounds.getHeight() / sensorRowSpan;
            int newRowSpan = sensorRowSpan - 1;
            double lastAvailableRowEndY = startY + (partHeights * newRowSpan);

            if (currentY < lastAvailableRowEndY) {
                sensor.setRowSpan(newRowSpan);
                GridPane.setRowSpan(editableSensor, newRowSpan);

                // Place sensors in every free spot from the freed up row space
                int freeRowStart = sensorRow + newRowSpan;
                int freeRowEnd = sensorRow + sensorRowSpan;
                populateFreedSpace(freeRowStart, freeRowEnd, sensorColumn, sensorColumn + sensor.getColumnSpan());

                resized = true;
            }
        }

        if (nextAvailableRow < pageData.getRows()) {
            Node nextNode = gridArray[nextAvailableRow][sensorColumn];
            Bounds nextNodeBounds = nextNode.localToScene(nextNode.getBoundsInLocal());
            double nextNodeEndY = nextNodeBounds.getMaxY();

            int newRowSpan = nextAvailableRow - sensorRow + 1;

            // This checks if the mouse is passed the next nodes bottom side, it also checks that there is no sensor
            // covering that space
            if (currentY > nextNodeEndY && !GridUtils.isRegionTaken(placedSensors, sensorColumn,
                    sensorRow + sensorRowSpan, sensorColumn + sensor.getColumnSpan(),
                    sensorRow + newRowSpan)) {
                sensor.setRowSpan(newRowSpan);
                GridPane.setRowSpan(editableSensor, newRowSpan);

                removeUnusedNodesAfterExpansion(sensor);

                resized = true;
            }
        }

        return resized;
    }

    // Returns true if the sensor has been resized
    private boolean resizeLeft(MouseEvent mouseEvent, Sensor sensor, EditableSensor editableSensor) {
        boolean resized = false;

        double currentX = mouseEvent.getSceneX();
        int sensorColumn = sensor.getColumn();
        int sensorColumnSpan = sensor.getColumnSpan();
        int sensorRow = sensor.getRow();

        if (sensorColumnSpan > 1) {
            Bounds sensorBounds = editableSensor.localToScene(editableSensor.getBoundsInLocal());
            double startX = sensorBounds.getMinX();
            double partWidths = sensorBounds.getWidth() / sensorColumnSpan;
            double secondPartStartX = startX + partWidths;
            if (currentX > secondPartStartX) {
                // Move the sensor to the right and decrease the column span by one
                int newColumnSpan = sensor.getColumnSpan() - 1;

                sensor.setPosition(sensorRow, sensorColumn + 1);
                sensor.setColumnSpan(newColumnSpan);

                GridPane.setColumnIndex(editableSensor, sensorColumn + 1);
                GridPane.setColumnSpan(editableSensor, newColumnSpan);

                gridArray[sensor.getRow()][sensor.getColumn()] = editableSensor;
                gridArray[sensorRow][sensorColumn] = null;

                populateFreedSpace(sensorRow, sensorRow + sensor.getRowSpan(), sensorColumn,
                        sensorColumn + 1);

                resized = true;
            }
        }

        int leftNodeColumnIndex = sensorColumn - 1;

        // Check if any sensor takes up the space on the left of this sensor
        if (leftNodeColumnIndex >= 0 && !GridUtils.isSpaceTaken(placedSensors, leftNodeColumnIndex, sensorRow,
                1, sensor.getRowSpan())) {
            // This means that the space is free
            Node leftNode = gridArray[sensorRow][leftNodeColumnIndex];
            Bounds leftNodeBounds = leftNode.localToScene(leftNode.getBoundsInLocal());
            double leftNodeStartX = leftNodeBounds.getMinX();

            if (currentX < leftNodeStartX) {
                int newColumnSpan = sensor.getColumnSpan() + 1;

                // Remove both the left node and the sensor (sensor will be added back in the left nodes position)
                sensorPane.getChildren().remove(leftNode);
                gridArray[sensorRow][sensorColumn] = null;

                // Move the sensor to the left column and expand the right side
                sensor.setPosition(sensorRow, leftNodeColumnIndex);
                sensor.setColumnSpan(newColumnSpan);

                GridPane.setColumnIndex(editableSensor, leftNodeColumnIndex);
                GridPane.setColumnSpan(editableSensor, newColumnSpan);

                gridArray[sensor.getRow()][sensor.getColumn()] = editableSensor;

                removeUnusedNodesAfterExpansion(sensor);

                resized = true;
            }
        }

        return resized;
    }

    // Returns true if the sensor has been resized
    private boolean resizeTop(MouseEvent mouseEvent, Sensor sensor, EditableSensor editableSensor) {
        boolean resized = false;

        double currentY = mouseEvent.getSceneY();
        int sensorRow = sensor.getRow();
        int sensorRowSpan = sensor.getRowSpan();
        int sensorColumn = sensor.getColumn();

        if (sensorRowSpan > 1) {
            Bounds sensorBounds = editableSensor.localToScene(editableSensor.getBoundsInLocal());
            double startY = sensorBounds.getMinY();
            double partHeights = sensorBounds.getHeight() / sensorRowSpan;
            double secondPartStartY = startY + partHeights;
            if (currentY > secondPartStartY) {
                // Move the sensor down and decrease the row span by one
                int newRowSpan = sensor.getRowSpan() - 1;

                sensor.setPosition(sensorRow + 1, sensorColumn);
                sensor.setRowSpan(newRowSpan);

                GridPane.setRowIndex(editableSensor, sensorRow + 1);
                GridPane.setRowSpan(editableSensor, newRowSpan);

                gridArray[sensor.getRow()][sensor.getColumn()] = editableSensor;
                gridArray[sensorRow][sensorColumn] = null;

                populateFreedSpace(sensorRow, sensorRow + 1, sensorColumn,
                        sensorColumn + sensor.getColumnSpan());

                resized = true;
            }
        }

        int aboveNodeRowIndex = sensorRow - 1;

        //if(leftNodeColumnIndex >= 0 && !isSpaceTaken(leftNodeColumnIndex, sensorRow, 1, sensor.getRowSpan()))
        // Check if any sensor takes up the space above this sensor
        if (aboveNodeRowIndex >= 0 && !GridUtils.isRegionTaken(placedSensors, sensorColumn, aboveNodeRowIndex,
                sensorColumn + sensor.getColumnSpan(), sensorRow)) ;
        {
            // Check if the mouse is at the beginning of the node above
            Node aboveNode = gridArray[aboveNodeRowIndex][sensorColumn];
            Bounds aboveNodeBounds = aboveNode.localToScene(aboveNode.getBoundsInLocal());
            double aboveNodeStartY = aboveNodeBounds.getMinY();

            if (currentY < aboveNodeStartY) {
                int newRowSpan = sensor.getRowSpan() + 1;

                // Remove both the left node and the sensor (sensor will be added back in the left nodes position)
                sensorPane.getChildren().remove(aboveNode);
                gridArray[sensorRow][sensorColumn] = null;

                // Move the sensor to the left column and expand the right side
                sensor.setPosition(aboveNodeRowIndex, sensorColumn);
                sensor.setRowSpan(newRowSpan);

                GridPane.setRowIndex(editableSensor, aboveNodeRowIndex);
                GridPane.setRowSpan(editableSensor, newRowSpan);

                gridArray[sensor.getRow()][sensor.getColumn()] = editableSensor;

                removeUnusedNodesAfterExpansion(sensor);

                resized = true;
            }
        }

        return resized;
    }

    private void removeUnusedNodesAfterExpansion(Sensor sensor) {
        // Remove all nodes in area (except for the sensor itself)
        for (int y = sensor.getRow(); y < sensor.getRow() + sensor.getRowSpan(); y++) {
            for (int x = sensor.getColumn(); x < sensor.getColumn() + sensor.getColumnSpan(); x++) {
                if (!(y == sensor.getRow() && x == sensor.getColumn())) {
                    Node unusedNode = gridArray[y][x];
                    if (unusedNode != null) {
                        sensorPane.getChildren().remove(unusedNode);
                        gridArray[y][x] = null;
                    }
                }
            }
        }
    }

    private void populateFreedSpace(int freeRowStart, int freeRowEnd, int freeColumnStart, int freeColumnEnd) {
        for (int y = Math.max(freeRowStart, 0); y < Math.min(freeRowEnd, pageData.getRows()); y++) {
            for (int x = Math.max(freeColumnStart, 0); x < Math.min(freeColumnEnd, pageData.getColumns()); x++) {
                // Only place an add sensor button if the space is free
                if (gridArray[y][x] == null) {
                    // Place an add sensor button on the cell that has been freed up
                    placeAddSensorButton(y, x);
                }
            }
        }
    }

    private void populateFreedSpaceExcluding(int freeRowStart,
                                             int freeRowEnd,
                                             int freeColumnStart,
                                             int freeColumnEnd,
                                             int excludeRowStart,
                                             int excludeRowEnd,
                                             int excludeColumnStart,
                                             int excludeColumnEnd) {
        for (int y = Math.max(freeRowStart, 0); y < Math.min(freeRowEnd, pageData.getRows()); y++) {
            for (int x = Math.max(freeColumnStart, 0); x < Math.min(freeColumnEnd, pageData.getColumns()); x++) {
                // Check if the x, y co-ordinates are within the excluded space, if they are, do not place the button
                boolean inExcludedRow = y >= excludeRowStart && y < excludeRowEnd;
                boolean inExcludedColumn = x >= excludeColumnStart && x < excludeColumnEnd;
                boolean excludePos = inExcludedRow && inExcludedColumn;

                // Only place an add sensor button if the space is free
                if (!excludePos && gridArray[y][x] == null) {
                    // Place an add sensor button on the cell that has been freed up
                    placeAddSensorButton(y, x);
                }
            }
        }
    }

    private void freeSpaceExcluding(int rowStart,
                                    int rowEnd,
                                    int colStart,
                                    int colEnd,
                                    int excludeRowStart,
                                    int excludeRowEnd,
                                    int excludeColStart,
                                    int excludeColEnd) {
        for (int y = Math.max(rowStart, 0); y < Math.min(rowEnd, pageData.getRows()); y++) {
            for (int x = Math.max(colStart, 0); x < Math.min(colEnd, pageData.getColumns()); x++) {
                // Check if the x, y co-ordinates are within the excluded space, if they are, do not free the space
                boolean inExcludedRow = y >= excludeRowStart && y < excludeRowEnd;
                boolean inExcludedColumn = x >= excludeColStart && x < excludeColEnd;
                boolean excludePos = inExcludedRow && inExcludedColumn;

                if (!excludePos) {
                    Node temp = gridArray[y][x];
                    sensorPane.getChildren().remove(temp);
                    gridArray[y][x] = null;
                }
            }
        }
    }
}
