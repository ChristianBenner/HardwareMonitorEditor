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

import com.bennero.client.core.SensorManager;
import com.bennero.common.Sensor;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class EditableSensor extends StackPane {
    private final Border HOVER_BORDER;

    private DragButton topLeftDragButton;
    private DragButton topDragButton;
    private DragButton topRightDragButton;
    private DragButton rightDragButton;
    private DragButton bottomRightDragButton;
    private DragButton bottomDragButton;
    private DragButton bottomLeftDragButton;
    private DragButton leftDragButton;
    private Group hoverButtonGroup;
    private boolean mouseInside;
    private EventHandler<DragEvent> dragEvent;
    private EventHandler<MoveEvent> moveButtonDragEvent;

    public EditableSensor(Color highlightColour,
                          Image editIcon,
                          Image removeIcon,
                          Image moveIcon,
                          Sensor sensor,
                          EventHandler<ActionEvent> editEvent,
                          EventHandler<ActionEvent> removeEvent) {
        mouseInside = false;

        // Check if the sensor manager has the sensor we are trying to add
        if (SensorManager.getInstance().isAvailable(sensor)) {
            super.getChildren().add(sensor);
        } else {
            // Put a warning sign because the sensor was not found in the list provided by bootstrapper
            super.getChildren().add(new NoSensor(sensor.getOriginalName()));
        }

        HOVER_BORDER = new Border(new BorderStroke(highlightColour, BorderStrokeStyle.DASHED, CornerRadii.EMPTY,
                new BorderWidths(3.0)));

        // Create the UI used for resizing the sensors
        topLeftDragButton = new ResizeSensorButton(Pos.TOP_LEFT, highlightColour, Cursor.NW_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, false, false, true)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        topDragButton = new ResizeSensorButton(Pos.TOP_CENTER, highlightColour, Cursor.N_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, false, false, false)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        topRightDragButton = new ResizeSensorButton(Pos.TOP_RIGHT, highlightColour, Cursor.NE_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, true, false, false)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        rightDragButton = new ResizeSensorButton(Pos.CENTER_RIGHT, highlightColour, Cursor.E_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, true, false, false)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        bottomRightDragButton = new ResizeSensorButton(Pos.BOTTOM_RIGHT, highlightColour, Cursor.SE_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, true, true, false)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        bottomDragButton = new ResizeSensorButton(Pos.BOTTOM_CENTER, highlightColour, Cursor.S_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, true, false)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        bottomLeftDragButton = new ResizeSensorButton(Pos.BOTTOM_LEFT, highlightColour, Cursor.SW_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, true, true)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        leftDragButton = new ResizeSensorButton(Pos.CENTER_LEFT, highlightColour, Cursor.W_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, false, true)),
                mouseEvent -> {
                    if (!mouseInside) {
                        hideEditUI();
                    }
                });

        super.getChildren().add(topLeftDragButton);
        super.getChildren().add(topDragButton);
        super.getChildren().add(topRightDragButton);
        super.getChildren().add(rightDragButton);
        super.getChildren().add(bottomRightDragButton);
        super.getChildren().add(bottomDragButton);
        super.getChildren().add(bottomLeftDragButton);
        super.getChildren().add(leftDragButton);

        hoverButtonGroup = new Group();
        hoverButtonGroup.setVisible(false);
        super.setOnMouseEntered(mouseEvent ->
        {
            mouseInside = true;
            showEditUI();
        });

        super.setOnMouseExited(mouseEvent ->
        {
            mouseInside = false;
            boolean isDragging = topLeftDragButton.isDragging() || topDragButton.isDragging() ||
                    topRightDragButton.isDragging() || rightDragButton.isDragging() ||
                    bottomRightDragButton.isDragging() || bottomDragButton.isDragging() ||
                    bottomLeftDragButton.isDragging() || leftDragButton.isDragging();
            if (!isDragging) {
                hideEditUI();
            }
        });

        // A frame for holding the interaction buttons
        HBox hBox = new HBox();

        // Create an edit button that opens the sensor editor
        Button edit = new Button();
        edit.setCursor(Cursor.HAND);
        edit.setPrefSize(32, 32);
        edit.setBackground(new Background(new BackgroundImage(editIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        edit.setOnAction(editEvent);
        hBox.getChildren().add(edit);

        // Create a delete button that removes the sensor from the page
        Button delete = new Button();
        delete.setCursor(Cursor.HAND);
        delete.setPrefSize(32, 32);
        delete.setBackground(new Background(new BackgroundImage(removeIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        delete.setOnAction(removeEvent);
        hBox.getChildren().add(delete);

        // Create a move icon that allows to the user to drag the sensor to a new location on the page
        DragButton move = new DragButton(Cursor.HAND,
                dragEvent ->
                {
                    Bounds sensorBounds = this.localToScene(this.getBoundsInLocal());
                    double mouseX = dragEvent.getSceneX();
                    double mouseY = dragEvent.getSceneY();
                    double sensorX = sensorBounds.getMinX();
                    double sensorY = sensorBounds.getMinY();
                    double sensorWidth = sensorBounds.getWidth();
                    double sensorHeight = sensorBounds.getHeight();
                    double sensorCenterX = sensorX + (sensorWidth / 2.0);
                    double sensorCenterY = sensorY + (sensorHeight / 2.0);
                    int sensorColumnSpan = sensor.getColumnSpan();
                    int sensorRowSpan = sensor.getRowSpan();
                    double sensorColumnWidth = sensorWidth / sensorColumnSpan;
                    double sensorRowHeight = sensorHeight / sensorRowSpan;
                    double mouseRelativeToSensorX = mouseX - sensorCenterX;
                    double mouseRelativeToSensorY = mouseY - sensorCenterY;
                    double columnDragOffset = mouseRelativeToSensorX / sensorColumnWidth;
                    double rowDragOffset = mouseRelativeToSensorY / sensorRowHeight;
                    double columnDragTotal = columnDragOffset;
                    double rowDragTotal = rowDragOffset;
                    int columnMove = (int) columnDragTotal;
                    int rowMove = (int) rowDragTotal;
                    int currentCol = sensor.getColumn();
                    int currentRow = sensor.getRow();
                    int newCol = currentCol + columnMove;
                    int newRow = currentRow + rowMove;

                    // Find what column/row we are hovering over
                    if (newRow != currentRow || newCol != currentCol) {
                        moveButtonDragEvent.handle(new MoveEvent(newCol, newRow, currentCol, currentRow));
                    }
                });

        move.setCursor(Cursor.HAND);
        move.setPrefSize(32, 32);
        move.setBackground(new Background(new BackgroundImage(moveIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        hBox.getChildren().add(move);

        hoverButtonGroup.getChildren().add(hBox);
        super.getChildren().add(hoverButtonGroup);
    }

    public void setDragEvent(EventHandler<DragEvent> dragEvent) {
        this.dragEvent = dragEvent;
    }

    public void setMoveButtonDragEvent(EventHandler<MoveEvent> moveEvent) {
        this.moveButtonDragEvent = moveEvent;
    }

    public void showEditUI() {
        hoverButtonGroup.setVisible(true);
        topLeftDragButton.setVisible(true);
        topDragButton.setVisible(true);
        topRightDragButton.setVisible(true);
        rightDragButton.setVisible(true);
        bottomRightDragButton.setVisible(true);
        bottomDragButton.setVisible(true);
        bottomLeftDragButton.setVisible(true);
        leftDragButton.setVisible(true);
        super.setBorder(HOVER_BORDER);
    }

    public void hideEditUI() {
        hoverButtonGroup.setVisible(false);
        hoverButtonGroup.setVisible(false);
        topLeftDragButton.setVisible(false);
        topDragButton.setVisible(false);
        topRightDragButton.setVisible(false);
        rightDragButton.setVisible(false);
        bottomRightDragButton.setVisible(false);
        bottomDragButton.setVisible(false);
        bottomLeftDragButton.setVisible(false);
        leftDragButton.setVisible(false);
        super.setBorder(Border.EMPTY);
    }

    public class DragEvent extends Event {
        private MouseEvent mouseEvent;
        private boolean topExpansion;
        private boolean rightExpansion;
        private boolean bottomExpansion;
        private boolean leftExpansion;

        public DragEvent(MouseEvent mouseEvent, boolean topExpansion, boolean rightExpansion, boolean bottomExpansion,
                         boolean leftExpansion) {
            super(null);
            this.mouseEvent = mouseEvent;
            this.topExpansion = topExpansion;
            this.rightExpansion = rightExpansion;
            this.bottomExpansion = bottomExpansion;
            this.leftExpansion = leftExpansion;
        }

        public MouseEvent getMouseEvent() {
            return mouseEvent;
        }

        public boolean isTopExpandable() {
            return topExpansion;
        }

        public boolean isRightExpandable() {
            return rightExpansion;
        }

        public boolean isBottomExpandable() {
            return bottomExpansion;
        }

        public boolean isLeftExpandable() {
            return leftExpansion;
        }
    }

    public class MoveEvent extends Event {
        private int newColumn;
        private int newRow;
        private int previousColumn;
        private int previousRow;

        public MoveEvent(int newColumn, int newRow, int previousColumn, int previousRow) {
            super(null);
            this.newColumn = newColumn;
            this.newRow = newRow;
            this.previousColumn = previousColumn;
            this.previousRow = previousRow;
        }

        public int getNewColumn() {
            return newColumn;
        }

        public int getNewRow() {
            return newRow;
        }

        public int getPreviousColumn() {
            return previousColumn;
        }

        public int getPreviousRow() {
            return previousRow;
        }
    }
}
