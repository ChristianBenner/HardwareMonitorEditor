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

package com.bennero.client.core;

import com.bennero.common.Sensor;
import com.bennero.common.SensorType;

import java.util.ArrayList;

public class SensorData
{
    private final int id;
    private final String name;
    private final float max;
    private final byte type;
    private final String hardwareType;
    private final float initialValue;

    private ArrayList<Sensor> sensorList;

    public SensorData(int id,
                      String name,
                      float max,
                      byte type,
                      String hardwareType,
                      float initialValue)
    {
        this.id = id;
        this.name = name + " (" + SensorType.getSuffix(type) + ")";
        this.max = max;
        this.type = type;
        this.hardwareType = hardwareType;
        this.initialValue = initialValue;
        sensorList = new ArrayList<>();
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

    public byte getType()
    {
        return type;
    }

    public String getHardwareType()
    {
        return hardwareType;
    }

    public float getInitialValue()
    {
        return initialValue;
    }

    public void addSensor(Sensor sensor)
    {
        this.sensorList.add(sensor);
    }

    public void setValue(float value)
    {
        for(int i = 0; i < sensorList.size(); i++)
        {
            sensorList.get(i).setValue(value);
        }
    }
}
