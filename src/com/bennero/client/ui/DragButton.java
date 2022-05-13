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
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public class DragButton extends Button {
    private boolean dragging;

    public DragButton(Cursor cursor,
                      EventHandler<MouseEvent> mousePressedEvent,
                      EventHandler<MouseEvent> dragEvent,
                      EventHandler<MouseEvent> finishDrag) {
        dragging = false;

        setOnMousePressed(mouseEvent ->
        {
            dragging = true;
            setCursor(cursor);

            if (mousePressedEvent != null) {
                mousePressedEvent.handle(mouseEvent);
            }
        });

        setOnMouseDragged(mouseEvent ->
        {
            if (dragging && dragEvent != null) {
                dragEvent.handle(mouseEvent);
            }
        });

        setOnMouseReleased(mouseEvent ->
        {
            dragging = false;
            setCursor(Cursor.DEFAULT);

            if (finishDrag != null) {
                finishDrag.handle(mouseEvent);
            }
        });
    }

    public DragButton(Cursor cursor,
                      EventHandler<MouseEvent> dragEvent,
                      EventHandler<MouseEvent> finishDrag) {
        this(cursor,
                null,
                dragEvent,
                finishDrag);
    }

    public DragButton(Cursor cursor,
                      EventHandler<MouseEvent> dragEvent) {
        this(cursor,
                null,
                dragEvent,
                null);
    }

    public boolean isDragging() {
        return dragging;
    }
}
