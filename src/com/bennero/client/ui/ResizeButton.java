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

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ResizeButton extends Button
{
    private final double RESIZE_BUTTON_SIZE = 15.0;
    private boolean dragging;
    private double startDragX;
    private double startDragY;
    private double dragX;
    private double dragY;

    public ResizeButton(Pos alignment, Color colour, Cursor cursor, EventHandler<MouseEvent> dragEvent,
                        EventHandler<MouseEvent> finishDrag)
    {
        dragging = false;

        setBorder(Border.EMPTY);
        setBackground(new Background(new BackgroundFill(colour, CornerRadii.EMPTY, Insets.EMPTY)));
        setPrefSize(RESIZE_BUTTON_SIZE, RESIZE_BUTTON_SIZE);
        setMinSize(RESIZE_BUTTON_SIZE, RESIZE_BUTTON_SIZE);
        setMaxSize(RESIZE_BUTTON_SIZE, RESIZE_BUTTON_SIZE);
        setVisible(false);
        StackPane.setAlignment(this, alignment);

        setOnMousePressed(mouseEvent ->
        {
            dragging = true;
            startDragX = mouseEvent.getX();
            startDragY = mouseEvent.getY();
            setCursor(cursor);
        });

        setOnMouseMoved(mouseEvent ->
        {
            if(dragging)
            {
                dragEvent.handle(mouseEvent);
                dragX = mouseEvent.getX();
                dragY = mouseEvent.getY();
            }
        });
        setOnMouseDragged(dragEvent);

        setOnMouseReleased(mouseEvent ->
        {
            dragging = false;
            setCursor(Cursor.DEFAULT);
            finishDrag.handle(mouseEvent);
        });
    }

    public double getStartX()
    {
        return startDragX;
    }

    public double getStartY()
    {
        return startDragY;
    }

    public double getX()
    {
        return dragX;
    }

    public double getY()
    {
        return dragY;
    }

    public boolean isDragging()
    {
        return dragging;
    }
}
