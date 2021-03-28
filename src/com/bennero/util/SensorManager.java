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

package com.bennero.util;

import com.bennero.bootstrapper.SensorRequest;
import com.bennero.common.Sensor;
import com.bennero.common.Skin;
import com.bennero.network.NetworkClient;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * A singleton that stores all of the sensors that have been created (not GUI gauges but Sensor objects that hold
 * information on each hardware sensor). The SensorManager can process SensorRequests.
 *
 * @see         Sensor
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SensorManager
{
    private static SensorManager instance = null;
    private List<Sensor> sensorList;

    private SensorManager()
    {
        sensorList = new ArrayList<>();
    }

    public static SensorManager getInstance()
    {
        if (instance == null)
        {
            instance = new SensorManager();
        }

        return instance;
    }

    public List<Sensor> getSensorList()
    {
        return sensorList;
    }

    public Sensor addSensor(SensorRequest sensorRequest)
    {
        boolean exists = false;
        // Try to identify if the sensor already exists before adding it
        for (int i = 0; i < sensorList.size() & !exists; i++)
        {
            Sensor existingSensor = sensorList.get(i);
            if (existingSensor.getOriginalName().compareTo(sensorRequest.getName()) == 0 &&
                    existingSensor.getType() == sensorRequest.getSensorType() &&
                    existingSensor.getHardwareType().compareTo(sensorRequest.getHardwareType()) == 0)
            {
                System.out.println("Sensor Exists: " + sensorRequest.getName());
                return existingSensor;
            }
        }

        Sensor sensor = new Sensor(sensorRequest.getId(), 0, 0, sensorRequest.getSensorType(), Skin.SPACE,
                sensorRequest.getMax(), sensorRequest.getMax() * 0.9f, sensorRequest.getName(),
                sensorRequest.getName(), false, 10000, 1, 1);
        sensor.setHardwareType(sensorRequest.getHardwareType());
        sensor.setValue(sensorRequest.getInitialValue());
        sensor.setValueChangeListener((observableValue, aFloat, t1) -> NetworkClient.getInstance().writeSensorValueMessage(sensor.getUniqueId(), t1));

        Platform.runLater(() ->
        {
            System.out.println("Adding Sensor: " + sensorRequest.getName() + ", ID: " + sensorRequest.getId());
            sensorList.add(sensor);
        });

        return sensor;
    }

    public boolean isAvailable(Sensor sensor)
    {
        boolean foundSensor = false;
        List<Sensor> sensorList = SensorManager.getInstance().getSensorList();
        // Check to see if the sensor exists in the list of found sensors
        for(int i = 0; !foundSensor && i < sensorList.size(); i++)
        {
            if(sensorList.get(i).getOriginalName().compareTo(sensor.getOriginalName()) == 0)
            {
                foundSensor = true;
            }
        }

        return foundSensor;
    }
}
