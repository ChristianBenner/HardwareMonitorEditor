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

package com.bennero.client.bootstrapper;

import com.bennero.client.core.SensorManager;
import com.bennero.common.SensorData;
import com.bennero.common.SensorType;
import javafx.application.Platform;

/**
 * SensorRequest is how to register a tracked hardware sensor to the application (not add to a page as a graphic). This
 * is used by the bootstrapper to provide and update hardware sensor information
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SensorRequest
{
    private final int id;
    private final String name;
    private final float max;
    private final byte sensorType;
    private final String hardwareType;
    private final float initialValue;

    private SensorData sensorRef;

    public SensorRequest(int id,
                         String name,
                         float max,
                         byte sensorType,
                         String hardwareType,
                         float initialValue)
    {
        System.out.println("Received sensor request: [ID: " + id + "], [Name: " + name + "], [Max: " + max +
                "], [SensorType: " + sensorType + "], [HardwareType: " + hardwareType + "], [InitialValue: " +
                initialValue + "]");
        this.id = id;
        this.name = name + " (" + SensorType.getSuffix(sensorType) + ")";
        this.max = max;
        this.sensorType = sensorType;
        this.hardwareType = hardwareType;
        this.initialValue = initialValue;
        publish();
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public float getMax()
    {
        return max;
    }

    public byte getSensorType()
    {
        return sensorType;
    }

    public String getHardwareType()
    {
        return hardwareType;
    }

    public float getInitialValue()
    {
        return initialValue;
    }

    public void publish()
    {
        sensorRef = SensorManager.getInstance().addSensor(this);
    }

    public void setValue(float value)
    {
        if (sensorRef != null)
        {
            Platform.runLater(() -> sensorRef.setValue(value));
        }
    }
}
