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

package com.bennero.client.util;

import com.bennero.common.Sensor;

import java.util.ArrayList;

public class GridUtils {
    public static boolean isSpaceTaken(ArrayList<Sensor> placedSensors, Sensor sensor) {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
            Sensor placedSensor = placedSensors.get(i);

            if (placedSensor != sensor) {
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

                if (withinRow && withinColumn) {
                    taken = true;
                }
            }
        }

        return taken;
    }

    public static boolean isSpaceTaken(ArrayList<Sensor> placedSensors,
                                       int column,
                                       int row,
                                       int columnSpan,
                                       int rowSpan) {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
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

            if (withinRow && withinColumn) {
                taken = true;
            }
        }

        return taken;
    }

    // Exclude a given sensor from the checks
    public static boolean isSpaceTaken(ArrayList<Sensor> placedSensors,
                                       int column,
                                       int row,
                                       int columnSpan,
                                       int rowSpan,
                                       Sensor excludedSensor) {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
            Sensor placedSensor = placedSensors.get(i);

            if (placedSensor != excludedSensor) {
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

                if (withinRow && withinColumn) {
                    taken = true;
                }
            }
        }

        return taken;
    }

    // Check if a space is taken, if the space is taken within the excluded area then it would be treated as if it was
    // available
    public static boolean isSpaceTaken(ArrayList<Sensor> placedSensors,
                                       int column,
                                       int row,
                                       int columnSpan,
                                       int rowSpan,
                                       int excludeColumn,
                                       int excludeRow,
                                       int excludeColumnSpan,
                                       int excludeRowSpan) {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
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

            if (withinRow && withinColumn) {
                int endExcludedColumn = excludeColumn + excludeColumnSpan;
                int endExcludedRow = excludeRow + excludeRowSpan;

                // Will the new location be within the excluded area
                boolean withinExcludedRow = (row >= excludeRow && row < endExcludedRow) ||
                        (endRow > excludeRow && endRow <= endExcludedRow);
                boolean withinExcludedColumn = (column >= excludeColumn && column < endExcludedColumn) ||
                        (endColumn > excludeColumn && endColumn <= endExcludedColumn);

                // if within the excluded space then it is not taken
                if (!(withinExcludedRow && withinExcludedColumn)) {
                    taken = true;
                }
            }
        }

        return taken;
    }

    public static boolean isRegionTaken(ArrayList<Sensor> placedSensors,
                                        int column,
                                        int row,
                                        int endColumn,
                                        int endRow) {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++) {
            Sensor placedSensor = placedSensors.get(i);

            int placedStartColumn = placedSensor.getColumn();
            int placedEndColumn = placedStartColumn + placedSensor.getColumnSpan();
            int placedStartRow = placedSensor.getRow();
            int placedEndRow = placedStartRow + placedSensor.getRowSpan();

            boolean withinRow = (row >= placedStartRow && row < placedEndRow) ||
                    (endRow > placedStartRow && endRow <= placedEndRow);
            boolean withinColumn = (column >= placedStartColumn && column < placedEndColumn) ||
                    (endColumn > placedStartColumn && endColumn <= placedEndColumn);

            if (withinRow && withinColumn) {
                taken = true;
            }
        }

        return taken;
    }
}
