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

import com.bennero.client.Version;
import com.bennero.client.bootstrapper.Native;
import com.bennero.client.bootstrapper.SensorRequest;
import com.bennero.client.network.NetworkClient;
import com.bennero.common.Sensor;
import com.bennero.common.SensorType;
import com.bennero.common.Skin;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

import static com.bennero.common.Constants.*;

/**
 * A singleton that stores all of the sensors that have been created (not GUI gauges but Sensor objects that hold
 * information on each hardware sensor). The SensorManager can process SensorRequests.
 *
 * @see         SensorData
 * @see         Sensor
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SensorManager
{
    private static SensorManager instance = null;
    private static int sensorGuiId = 0;

    private List<SensorData> sensorList;

    public static SensorManager getInstance()
    {
        if (instance == null)
        {
            instance = new SensorManager();
        }

        return instance;
    }

    private SensorManager()
    {
        sensorList = new ArrayList<>();
    }

    public List<SensorData> getSensorList()
    {
        return sensorList;
    }

    public void addSensorData(SensorData sensorData)
    {
        final String name = sensorData.getName();
        final int id = sensorData.getId();

        boolean exists = false;
        // Try to identify if the sensor already exists before adding it
        for (int i = 0; i < sensorList.size() & !exists; i++)
        {
            SensorData existingSensor = sensorList.get(i);
            if (compareSensorData(existingSensor, sensorData))
            {
                System.out.println("Sensor Exists: " + name);
                exists = true;
            }
        }

        if(!exists)
        {
            Platform.runLater(() ->
            {
                System.out.println("Adding Sensor: " + name + ", ID: " + id);
                sensorList.add(sensorData);
            });
        }
    }

    public boolean compareSensorData(SensorData lhs, SensorData rhs)
    {
        if (lhs.getName().compareTo(rhs.getName()) == 0 && lhs.getType() == rhs.getType() &&
                lhs.getHardwareType().compareTo(rhs.getHardwareType()) == 0)
        {
            return true;
        }

        return false;
    }

    public boolean compareSensorGuiToData(SensorData sensorData, Sensor sensorGui)
    {
        if(sensorData.getName().compareTo(sensorGui.getOriginalName()) == 0 &&
                sensorData.getType() == sensorGui.getType() &&
                sensorData.getHardwareType().compareTo(sensorGui.getHardwareType()) == 0)
        {
            return true;
        }

        return false;
    }

    public Sensor createSensorGui(SensorData sensorData,
                                  int row,
                                  int column,
                                  byte skin,
                                  float threshold,
                                  String title,
                                  boolean averagingEnabled,
                                  int averagingPeriod,
                                  int rowSpan,
                                  int columnSpan)
    {
        Sensor sensor = new Sensor(sensorGuiId++, row, column, sensorData.getType(), skin, sensorData.getMax(),
                threshold, sensorData.getName(), title, averagingEnabled, averagingPeriod, rowSpan, columnSpan);
        sensor.setHardwareType(sensorData.getHardwareType());
        sensor.setValue(sensorData.getInitialValue());
        sensor.setValueChangeListener((observableValue, aFloat, t1) -> NetworkClient.getInstance().
                writeSensorValueMessage(sensor.getUniqueId(), t1));
        sensorData.addSensor(sensor);
        return sensor;
    }

    public Sensor createSensorGui(SensorData sensorData, int row, int column)
    {
        return createSensorGui(sensorData, row, column, Skin.SPACE, sensorData.getMax() * 0.9f,
                sensorData.getName(), false, 10000, 1, 1);
    }

    public boolean registerExistingSensor(Sensor sensor)
    {
        boolean foundSensorData = false;

        // Locate the sensor data in the list
        List<SensorData> sensorList = getSensorList();
        for(int i = 0; !foundSensorData && i < sensorList.size(); i++)
        {
            if (compareSensorGuiToData(sensorList.get(i), sensor))
            {
                sensor.setValueChangeListener((observableValue, aFloat, t1) -> NetworkClient.getInstance().
                        writeSensorValueMessage(sensor.getUniqueId(), t1));
                sensorList.get(i).addSensor(sensor);
                foundSensorData = true;
            }
        }

        return foundSensorData;
    }

    public boolean isAvailable(SensorData sensorData)
    {
        boolean foundSensor = false;
        List<SensorData> sensorList = getSensorList();
        // Check to see if the sensor exists in the list of found sensors
        for(int i = 0; !foundSensor && i < sensorList.size(); i++)
        {
            if(compareSensorData(sensorList.get(i), sensorData))
            {
                foundSensor = true;
            }
        }

        return foundSensor;
    }

    public boolean isAvailable(Sensor sensor)
    {
        // Locate the sensor data
        boolean foundSensorData = false;

        List<SensorData> sensorList = getSensorList();
        // Check to see if the sensor exists in the list of found sensors
        for(int i = 0; !foundSensorData && i < sensorList.size(); i++)
        {
            if(compareSensorGuiToData(sensorList.get(i), sensor))
            {
                foundSensorData = true;
            }
        }

        return foundSensorData;
    }

    public void addNativeSensors()
    {
        try
        {
            Native.addSensors();
        }
        catch (UnsatisfiedLinkError e)
        {
            if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
            {
                System.err.println("No NativeAddSensors method. System not launched with the bootstrapper");
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to retrieve sensor data");
                alert.setTitle("Hardware Monitor Error");
                alert.setHeaderText("Failed to retrieve sensor data");
                alert.setContentText("There was an error loading the sensor data due to a communication problem with " +
                        "the native interface provided by the bootstrapper application. Do not attempt to run the JAR " +
                        "file without the bootstrapper. If you see this error for any other reason please contact " +
                        "Bennero support (ERROR CODE: " + EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED + ")");
                alert.showAndWait();

                System.exit(EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED);
            }
            else
            {
                // Bootstrapper has not been found but we are in debug mode so this is not an error, we should instead
                // add some example/debug sensors so that the programmer can use some fake sensors
                addDebugSensors();
            }
        }
    }

    public void startSensorUpdateThread()
    {
        Thread thread = new Thread(() ->
        {
            try
            {
                while (true)
                {
                    Thread.sleep(SENSOR_POLL_RATE_MS);

                    Platform.runLater(() ->
                    {
                        if (NetworkClient.getInstance().isConnected())
                        {
                            Native.updateSensors();
                        }
                    });
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
                {
                    System.err.println("No NativeAddSensors method. System not launched with the bootstrapper");
                    e.printStackTrace();

                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update sensors");
                    alert.setTitle("Hardware Monitor Error");
                    alert.setHeaderText("Failed to update sensors");
                    alert.setContentText("There was an error updating the sensor data due to failed communication with " +
                            "the native interface provided by the bootstrapper application (ERROR CODE: " +
                            EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED + ")");
                    alert.showAndWait();
                    System.exit(EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED);
                }
                else
                {
                    e.printStackTrace();
                    System.err.println("Failed to update sensors. Native interface not working correctly");
                }
            }
        });

        if (Version.BOOTSTRAPPER_LAUNCH_REQUIRED)
        {
            thread.start();
        }
        else
        {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No bootstrapper enabled");
            alert.setTitle("Hardware Monitor Warning");
            alert.setHeaderText("No bootstrapper enabled");
            alert.setContentText("The bootstrapper is disabled, meaning that the application will not poll for " +
                    "hardware data and therefor not update any sensors with real data. This warning is for " +
                    "developers only, if you are seeing this as a user, somebody built the software wrong! Set " +
                    "BOOTSTRAPPER_LAUNCH_REQUIRED to false");
            alert.showAndWait();
        }
    }

    private void addDebugSensors()
    {
        // Add debug sensors
        for (int i = 0; i < 6; i++)
        {
            new SensorRequest(i, "Core #" + i + " Temp", 100.0f, SensorType.TEMPERATURE,
                    "DEBUG_CPU", 25.0f);
        }

        new SensorRequest(6, "Core Clock", 1800.0f, SensorType.CLOCK,
                "DEBUG_CPU", 25.0f);
        new SensorRequest(7, "Memory Clock", 1250.0f, SensorType.CLOCK,
                "DEBUG_CPU", 1105.0f);
        new SensorRequest(8, "Core Utilisation", 100.0f, SensorType.LOAD,
                "DEBUG_CPU", 53.0f);
        new SensorRequest(9, "Power Draw", 300.0f, SensorType.POWER,
                "DEBUG_CPU", 125.0f);
    }
}
