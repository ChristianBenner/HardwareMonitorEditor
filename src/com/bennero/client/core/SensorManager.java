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
import com.bennero.common.Sensor;
import com.bennero.common.SensorType;
import com.bennero.common.Skin;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.SensorUpdateInfo;
import com.bennero.common.messages.SensorUpdateMessage;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.bennero.common.Constants.*;

/**
 * A singleton that stores all of the sensors that have been created (not GUI gauges but Sensor objects that hold
 * information on each hardware sensor). The SensorManager can process SensorRequests.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see SensorData
 * @see Sensor
 * @since 1.0
 */
public class SensorManager {
    private static final String LOGGER_TAG = SensorManager.class.getSimpleName();

    private static SensorManager instance = null;
    private static byte sensorGuiId = 0;

    private List<SensorData> sensorList;

    private List<SensorRequest> mockSensors;
    private boolean usingMockSensors;
    private Random debugSensorRandom;

    private SensorManager() {
        sensorList = new ArrayList<>();
    }

    public static SensorManager getInstance() {
        if (instance == null) {
            instance = new SensorManager();
        }

        return instance;
    }

    public List<SensorData> getSensorList() {
        return sensorList;
    }

    public void clearSensorList() {
        sensorList.clear();
    }

    public void addSensorData(SensorData sensorData) {
        final String name = sensorData.getName();

        boolean exists = false;
        // Try to identify if the sensor already exists before adding it
        for (int i = 0; i < sensorList.size() & !exists; i++) {
            SensorData existingSensor = sensorList.get(i);
            if (compareSensorData(existingSensor, sensorData)) {
                Logger.log(LogLevel.WARNING, LOGGER_TAG, "Failed to add sensor because it already exists");
                exists = true;
            }
        }

        if (!exists) {
            Platform.runLater(() ->
            {
                Logger.log(LogLevel.INFO, LOGGER_TAG, "Adding Sensor Data: " + name + ", Hardware Type: " + sensorData.getHardwareType());
                sensorList.add(sensorData);
            });
        }
    }

    public boolean compareSensorData(SensorData lhs, SensorData rhs) {
        if (lhs.getName().compareTo(rhs.getName()) == 0 && lhs.getType() == rhs.getType() &&
                lhs.getHardwareType().compareTo(rhs.getHardwareType()) == 0) {
            return true;
        }

        return false;
    }

    public boolean compareSensorGuiToData(SensorData sensorData, Sensor sensorGui) {
        if (sensorData.getName().compareTo(sensorGui.getOriginalName()) == 0 &&
                sensorData.getType() == sensorGui.getType() &&
                sensorData.getHardwareType().compareTo(sensorGui.getHardwareType()) == 0) {
            return true;
        }

        return false;
    }

    public byte getAvailableId() {
        return ++sensorGuiId;
    }

    public Sensor createSensorGui(SensorData sensorData, int row, int column, byte skin, float threshold, String title,
                                  boolean averagingEnabled, int averagingPeriod, int rowSpan, int columnSpan) {
        Sensor sensor = new Sensor(getAvailableId(), row, column, sensorData.getType(), skin, sensorData.getMax(),
                threshold, sensorData.getName(), title, averagingEnabled, averagingPeriod, rowSpan, columnSpan);
        sensor.setHardwareType(sensorData.getHardwareType());
        sensor.setValue(sensorData.getInitialValue());
        registerSensor(sensor, sensorData);
        return sensor;
    }

    public Sensor createSensorGui(SensorData sensorData, int row, int column) {
        return createSensorGui(sensorData, row, column, Skin.SPACE, sensorData.getMax() * 0.9f,
                sensorData.getName(), false, 10000, 1, 1);
    }

    public void registerSensor(Sensor sensor, SensorData sensorData) {
        sensor.setValueChangeListener((observableValue, aFloat, t1) -> {
            SensorUpdateInfo[] updateInfos = new SensorUpdateInfo[1];
            updateInfos[0] = new SensorUpdateInfo(sensor.getUniqueId(), t1);
            DataClient.writeMessage(new SensorUpdateMessage(ApplicationCore.s_getUUID(), true, updateInfos));
        });
        sensorData.addSensor(sensor);
        Logger.log(LogLevel.DEBUG, LOGGER_TAG, "Registered Sensor: [ID: " + sensor.getUniqueId() + "], [SENSOR_DATA_ID: " + sensorData.getId() + "], [NAME: " + sensor.getTitle() + "]");
    }

    public void registerExistingSensor(Sensor sensor) {
        boolean foundSensorData = false;

        // Locate the sensor data in the list
        List<SensorData> sensorList = getSensorList();
        for (int i = 0; !foundSensorData && i < sensorList.size(); i++) {
            if (compareSensorGuiToData(sensorList.get(i), sensor)) {
                registerSensor(sensor, sensorList.get(i));
                if (sensorGuiId < sensor.getUniqueId()) {
                    sensorGuiId = (byte)(sensor.getUniqueId() + 1);
                }
                foundSensorData = true;
            }
        }
    }

    public boolean isAvailable(SensorData sensorData) {
        boolean foundSensor = false;
        List<SensorData> sensorList = getSensorList();
        // Check to see if the sensor exists in the list of found sensors
        for (int i = 0; !foundSensor && i < sensorList.size(); i++) {
            if (compareSensorData(sensorList.get(i), sensorData)) {
                foundSensor = true;
            }
        }

        return foundSensor;
    }

    public boolean isAvailable(Sensor sensor) {
        // Locate the sensor data
        boolean foundSensorData = false;

        List<SensorData> sensorList = getSensorList();
        // Check to see if the sensor exists in the list of found sensors
        for (int i = 0; !foundSensorData && i < sensorList.size(); i++) {
            if (compareSensorGuiToData(sensorList.get(i), sensor)) {
                foundSensorData = true;
            }
        }

        return foundSensorData;
    }

    public void addNativeSensors(boolean mockSensors) {
        if(mockSensors) {
            usingMockSensors = true;

            // add some example/debug sensors so that the programmer can use some fake sensors
            addDebugSensors();
        } else {
            try {
                Native.addSensors();
            } catch (UnsatisfiedLinkError e) {
                Logger.log(LogLevel.ERROR, LOGGER_TAG, "No NativeAddSensors method. System failed to communicate with the bootstrapper");
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to retrieve sensor data");
                alert.setTitle("Hardware Monitor Error");
                alert.setHeaderText("Failed to retrieve sensor data");
                alert.setContentText("There was an error loading the sensor data due to a communication problem with " +
                        "the native interface provided by the bootstrapper application. Do not attempt to run the JAR " +
                        "file without the bootstrapper. If you see this error for any other reason please contact " +
                        "Bennero support (ERROR CODE: " + EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED + ")");

                Platform.runLater(() -> {
                    ApplicationCore.getInstance().getWindow().setAllowShow(false);
                });
                alert.showAndWait();

                try {
                    ApplicationCore.getInstance().stop();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(EXIT_ERROR_CODE_NATIVE_GET_SENSOR_FAILED);
            }
        }
    }

    public void startSensorUpdateThread() {
        Thread thread = new Thread(() ->
        {
            try {
                while (true) {
                    Thread.sleep(SENSOR_POLL_RATE_MS);

                    Platform.runLater(() ->
                    {
                        if (DataClient.isConnected()) {
                            if (usingMockSensors) {
                                updateDebugSensors();
                            } else {
                                try {
                                    Native.updateSensors();
                                } catch (Exception e) {
                                    Logger.log(LogLevel.ERROR, LOGGER_TAG, "No NativeAddSensors method. System not launched with the bootstrapper");
                                    e.printStackTrace();

                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update sensors");
                                    alert.setTitle("Hardware Monitor Error");
                                    alert.setHeaderText("Failed to update sensors");
                                    alert.setContentText("There was an error updating the sensor data due to failed communication with " +
                                            "the native interface provided by the bootstrapper application (ERROR CODE: " +
                                            EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED + ")");
                                    Platform.runLater(() -> {
                                        ApplicationCore.getInstance().getWindow().setAllowShow(false);
                                    });
                                    alert.showAndWait();

                                    try {
                                        ApplicationCore.getInstance().stop();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    System.exit(EXIT_ERROR_CODE_NATIVE_SENSOR_UPDATE_FAILED);
                                }
                            }
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.start();

        if (usingMockSensors) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No bootstrapper enabled");
            alert.setTitle("Hardware Monitor Warning");
            alert.setHeaderText("No bootstrapper enabled");
            alert.setContentText("The bootstrapper is disabled, meaning that the application will not poll for " +
                    "hardware data and therefore not update any sensors with real data. Remove the -m/--mock-sensors " +
                    "program argument to run with real sensor data");
            alert.showAndWait();
        }
    }

    private void updateDebugSensors() {
        if(mockSensors == null || debugSensorRandom == null) {
            return;
        }

        // Update all sensors with random values between there min and max
        for (SensorRequest sensorRequest : mockSensors) {
            float max = sensorRequest.getMax();
            float randomVal = debugSensorRandom.nextFloat() * max;
            sensorRequest.setValue(randomVal);
        }
    }

    private void addDebugSensors() {
        usingMockSensors = true;
        debugSensorRandom = new Random();
        mockSensors = new ArrayList<>();

        // Add debug CPU sensors
        int id = 0;
        for (int cpuI = 0; cpuI < 6; cpuI++) {
            mockSensors.add(new SensorRequest(id, "Core #" + id + " Temp", 100.0f, SensorType.TEMPERATURE,
                    "DEBUG_CPU", 25.0f));
            id++;
        }

        mockSensors.add(new SensorRequest(id++, "Core Clock", 4000.0f, SensorType.CLOCK,
                "DEBUG_CPU", 25.0f));
        mockSensors.add(new SensorRequest(id++, "Memory Clock", 1250.0f, SensorType.CLOCK,
                "DEBUG_CPU", 1105.0f));
        mockSensors.add(new SensorRequest(id++, "Core Utilisation", 100.0f, SensorType.LOAD,
                "DEBUG_CPU", 53.0f));
        mockSensors.add(new SensorRequest(id++, "Power Draw", 90.0f, SensorType.POWER,
                "DEBUG_CPU", 40.0f));

        // Add debug GPU sensors
        for (int gpuI = 0; gpuI < 6; gpuI++) {
            mockSensors.add(new SensorRequest(id, "Temp", 100.0f, SensorType.TEMPERATURE,
                    "DEBUG_GPU", 25.0f));
            id++;
        }

        mockSensors.add(new SensorRequest(id++, "Core Clock", 1800.0f, SensorType.CLOCK,
                "DEBUG_GPU", 25.0f));
        mockSensors.add(new SensorRequest(id++, "Memory Clock", 1250.0f, SensorType.CLOCK,
                "DEBUG_GPU", 20.0f));
        mockSensors.add(new SensorRequest(id++, "Core Utilisation", 100.0f, SensorType.LOAD,
                "DEBUG_GPU", 15.0f));
        mockSensors.add(new SensorRequest(id++, "Power Draw", 330.0f, SensorType.POWER,
                "DEBUG_GPU", 120.0f));

        // Add debug RAM sensors
        mockSensors.add(new SensorRequest(id++, "Capacity", 100.0f, SensorType.LOAD,
                "DEBUG_MEMORY", 67.0f));

    }
}
