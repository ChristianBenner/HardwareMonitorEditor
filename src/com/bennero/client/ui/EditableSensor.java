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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class EditableSensor extends StackPane
{
    private final Border HOVER_BORDER;

    private ResizeButton topLeftDragButton;
    private ResizeButton topDragButton;
    private ResizeButton topRightDragButton;
    private ResizeButton rightDragButton;
    private ResizeButton bottomRightDragButton;
    private ResizeButton bottomDragButton;
    private ResizeButton bottomLeftDragButton;
    private ResizeButton leftDragButton;
    private Group hoverButtonGroup;
    private boolean mouseInside;

    public class DragEvent extends Event
    {
        private MouseEvent mouseEvent;
        private boolean topExpansion;
        private boolean rightExpansion;
        private boolean bottomExpansion;
        private boolean leftExpansion;

        public DragEvent(MouseEvent mouseEvent, boolean topExpansion, boolean rightExpansion, boolean bottomExpansion,
                         boolean leftExpansion)
        {
            super(null);
            this.mouseEvent = mouseEvent;
            this.topExpansion = topExpansion;
            this.rightExpansion = rightExpansion;
            this.bottomExpansion = bottomExpansion;
            this.leftExpansion = leftExpansion;
        }

        public MouseEvent getMouseEvent()
        {
            return mouseEvent;
        }

        public boolean isTopExpandable()
        {
            return topExpansion;
        }

        public boolean isRightExpandable()
        {
            return rightExpansion;
        }

        public boolean isBottomExpandable()
        {
            return bottomExpansion;
        }

        public boolean isLeftExpandable()
        {
            return leftExpansion;
        }
    }

    private EventHandler<DragEvent> dragEvent;

    public EditableSensor(Color highlightColour, Image editIcon, Image removeIcon, Sensor sensor,
                          EventHandler<ActionEvent> editEvent,
                          EventHandler<ActionEvent> removeEvent)
    {
        mouseInside = false;

        // Check if the sensor manager has the sensor we are trying to add
        if(SensorManager.getInstance().isAvailable(sensor))
        {
            super.getChildren().add(sensor);
        }
        else
        {
            // Put a warning sign because the sensor was not found in the list provided by bootstrapper
            super.getChildren().add(new NoSensor(sensor.getOriginalName()));
        }

        HOVER_BORDER = new Border(new BorderStroke(highlightColour, BorderStrokeStyle.DASHED, CornerRadii.EMPTY,
                new BorderWidths(3.0)));

        // Create the UI used for resizing the sensors
        topLeftDragButton = new ResizeButton(Pos.TOP_LEFT, highlightColour, Cursor.NW_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, false, false, true)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        topDragButton = new ResizeButton(Pos.TOP_CENTER, highlightColour, Cursor.N_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, false, false, false)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        topRightDragButton = new ResizeButton(Pos.TOP_RIGHT, highlightColour, Cursor.NE_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, true, true, false, false)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        rightDragButton = new ResizeButton(Pos.CENTER_RIGHT, highlightColour, Cursor.E_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, true, false, false)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        bottomRightDragButton = new ResizeButton(Pos.BOTTOM_RIGHT, highlightColour, Cursor.SE_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, true, true, false)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        bottomDragButton = new ResizeButton(Pos.BOTTOM_CENTER, highlightColour, Cursor.S_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, true, false)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        bottomLeftDragButton = new ResizeButton(Pos.BOTTOM_LEFT, highlightColour, Cursor.SW_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, true, true)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
                        hideEditUI();
                    }
                });

        leftDragButton = new ResizeButton(Pos.CENTER_LEFT, highlightColour, Cursor.W_RESIZE,
                mouseEvent -> dragEvent.handle(new DragEvent(mouseEvent, false, false, false, true)),
                mouseEvent -> {
                    if(!mouseInside)
                    {
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
            if(!isDragging)
            {
                hideEditUI();
            }
        });

        HBox hBox = new HBox();

        Button edit = new Button();
        edit.setCursor(Cursor.HAND);
        edit.setPrefSize(32, 32);
        edit.setBackground(new Background(new BackgroundImage(editIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        edit.setOnAction(editEvent);

        hBox.getChildren().add(edit);
        Button delete = new Button();
        delete.setCursor(Cursor.HAND);
        delete.setPrefSize(32, 32);
        delete.setBackground(new Background(new BackgroundImage(removeIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        delete.setOnAction(removeEvent);

        hBox.getChildren().add(delete);
        hoverButtonGroup.getChildren().add(hBox);
        super.getChildren().add(hoverButtonGroup);
    }

    public void setDragEvent(EventHandler<DragEvent> dragEvent)
    {
        this.dragEvent = dragEvent;
    }

    public void showEditUI()
    {
        hoverButtonGroup.setVisible(true);
        topLeftDragButton.setVisible(true);
        topDragButton.setVisible(true);
        topRightDragButton.setVisible(true);
        rightDragButton.setVisible(true);
        bottomRightDragButton.setVisible(true);
        bottomDragButton.setVisible(true);
        bottomLeftDragButton.setVisible(true);
        leftDragButton.setVisible(true);
       // super.setBackground(HOVER_BACKGROUND);
        super.setBorder(HOVER_BORDER);
    }

    public void hideEditUI()
    {
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
      // super.setBackground(Background.EMPTY);
        super.setBorder(Border.EMPTY);
    }
}
