/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.config;

import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import javafx.scene.paint.Color;
import org.xml.sax.Attributes;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.util.ArrayList;

import static com.bennero.common.Constants.SENSOR_POLL_RATE_MS;

/**
 * SaveData is the class for reading and writing save configuration files. One save file per save data object. This is
 * to provide a save system for the user created pages in the application. It saves the pages customised features such
 * as layout, colours, titles, sensors and transitions.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SaveData extends ConfigurationSaveHandler
{
    private static final String SAVE_ELEMENT_TAG = "save";
    private static final String SAVE_SENSOR_UPDATE_TIME_TAG = "sensorUpdateTimeMs";
    private static final String SAVE_SENSOR_ANIMATION_DURATION_TAG = "sensorAnimationDurationMs";

    private static final String PAGE_ELEMENT_TAG = "page";
    private static final String PAGE_ID_ELEMENT_TAG = "id";
    private static final String PAGE_BACKGROUND_COLOUR_ELEMENT_TAG = "backgroundColour";
    private static final String PAGE_TITLE_COLOUR_ELEMENT_TAG = "titleColour";
    private static final String PAGE_SUBTITLE_COLOUR_ELEMENT_TAG = "subtitleColour";
    private static final String PAGE_ROWS_ELEMENT_TAG = "rows";
    private static final String PAGE_COLUMNS_ELEMENT_TAG = "columns";
    private static final String PAGE_NEXT_PAGE_ID_ELEMENT_TAG = "nextPageId";
    private static final String PAGE_TRANSITION_ID_ELEMENT_TAG = "transitionId";
    private static final String PAGE_TRANSITION_TIME_ID_ELEMENT_TAG = "transitionTime";
    private static final String PAGE_DURATION_MS_ELEMENT_TAG = "durationMs";
    private static final String PAGE_TITLE_ELEMENT_TAG = "title";
    private static final String PAGE_TITLE_ENABLED_ELEMENT_TAG = "titleEnabled";
    private static final String PAGE_TITLE_ALIGNMENT_ELEMENT_TAG = "titleAlignment";
    private static final String PAGE_SUBTITLE_ELEMENT_TAG = "subtitle";
    private static final String PAGE_SUBTITLE_ENABLED_ELEMENT_TAG = "subtitleEnabled";
    private static final String PAGE_SUBTITLE_ALIGNMENT_ELEMENT_TAG = "subtitleAlignment";

    private static final String SENSOR_ELEMENT_TAG = "sensor";
    private static final String SENSOR_ID_ELEMENT_TAG = "id";
    private static final String SENSOR_ROW_ELEMENT_TAG = "row";
    private static final String SENSOR_COLUMN_ELEMENT_TAG = "column";
    private static final String SENSOR_TYPE_ELEMENT_TAG = "type";
    private static final String SENSOR_SKIN_ELEMENT_TAG = "skin";
    private static final String SENSOR_MAX_ELEMENT_TAG = "max";
    private static final String SENSOR_THRESHOLD_ELEMENT_TAG = "threshold";
    private static final String SENSOR_ORIGINAL_NAME_ELEMENT_TAG = "originalName";
    private static final String SENSOR_TITLE_ELEMENT_TAG = "title";
    private static final String SENSOR_HARDWARE_TYPE_ELEMENT_TAG = "hardwareType";
    private static final String SENSOR_AVERAGE_ENABLED_ELEMENT_TAG = "averageEnabled";
    private static final String SENSOR_AVERAGING_PERIOD_ELEMENT_TAG = "averagingPeriod";
    private static final String SENSOR_ROW_SPAN_ELEMENT_TAG = "rowSpan";
    private static final String SENSOR_COLUMN_SPAN_ELEMENT_TAG = "columnSpan";
    private static final String SENSOR_AVERAGE_COLOUR_ELEMENT_TAG = "averageColour";
    private static final String SENSOR_NEEDLE_COLOUR_ELEMENT_TAG = "needleColour";
    private static final String SENSOR_VALUE_COLOUR_ELEMENT_TAG = "valueColour";
    private static final String SENSOR_UNIT_COLOUR_ELEMENT_TAG = "unitColour";
    private static final String SENSOR_KNOB_COLOUR_ELEMENT_TAG = "knobColour";
    private static final String SENSOR_BAR_COLOUR_ELEMENT_TAG = "barColour";
    private static final String SENSOR_THRESHOLD_COLOUR_ELEMENT_TAG = "thresholdColour";
    private static final String SENSOR_TITLE_COLOUR_ELEMENT_TAG = "titleColour";
    private static final String SENSOR_BAR_BACKGROUND_COLOUR_ELEMENT_TAG = "barBackgroundColour";
    private static final String SENSOR_FOREGROUND_COLOUR_ELEMENT_TAG = "foregroundColour";
    private static final String SENSOR_TICK_LABEL_COLOUR_ELEMENT_TAG = "tickLabelColour";
    private static final String SENSOR_TICK_MARK_COLOUR_ELEMENT_TAG = "tickMarkColour";

    private int sensorUpdateTime;
    private int sensorAnimationDuration;
    private ArrayList<PageData> pageDataList;
    private PageData currentPageData;

    public SaveData(File file)
    {
        super(file);
        sensorUpdateTime = SENSOR_POLL_RATE_MS;
        sensorAnimationDuration = 1000;
        pageDataList = new ArrayList<>();
        currentPageData = null;
        super.read();
    }

    public int getSensorUpdateTime()
    {
        return sensorUpdateTime;
    }

    public void setSensorUpdateTime(int sensorUpdateTime)
    {
        this.sensorUpdateTime = sensorUpdateTime;
    }

    public int getSensorAnimationDuration()
    {
        return sensorAnimationDuration;
    }

    public void setSensorAnimationDuration(int sensorAnimationDuration)
    {
        this.sensorAnimationDuration = sensorAnimationDuration;
    }

    public final ArrayList<PageData> getPageDataList()
    {
        return pageDataList;
    }

    public void setPageDataList(ArrayList<PageData> pageDataList)
    {
        this.pageDataList = pageDataList;
        save();
    }

    public void addPageData(PageData pageData)
    {
        this.pageDataList.add(pageData);
        save();
    }

    public void removePageData(PageData pageData)
    {
        this.pageDataList.remove(pageData);
        save();
    }

    @Override
    protected void read(String uri, String localName, String qName, Attributes attributes)
    {
        System.out.println("Start Element: " + qName);

        switch (qName)
        {
            case SAVE_ELEMENT_TAG:
                parseSaveData(attributes);
                break;
            case PAGE_ELEMENT_TAG:
                parsePageData(attributes);
                break;
            case SENSOR_ELEMENT_TAG:
                parseSensorData(attributes);
                break;
        }
    }

    private void parseSaveData(Attributes attributes)
    {
        System.out.print("\tSave:");
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String attributeName = attributes.getQName(i);
            String attributeValue = attributes.getValue(i);
            System.out.print(" '" + attributeName + "':" + attributeValue);

            // Parse attributes
            if (attributeName.compareTo(SAVE_SENSOR_UPDATE_TIME_TAG) == 0)
            {
                sensorUpdateTime = Integer.parseInt(attributeValue);
            }

            if (attributeName.compareTo(SAVE_SENSOR_ANIMATION_DURATION_TAG) == 0)
            {
                sensorAnimationDuration = Integer.parseInt(attributeValue);
            }
        }
        System.out.println();
    }

    @Override
    protected void save(XMLStreamWriter streamWriter) throws XMLStreamException
    {
        int depth = 0;
        streamWriter.writeStartDocument("UTF-8", "1.0");
        writeIndentation(streamWriter, depth, true);
        streamWriter.writeStartElement(SAVE_ELEMENT_TAG);
        // Write the sensor update time
        streamWriter.writeAttribute(SAVE_SENSOR_UPDATE_TIME_TAG, Integer.toString(sensorUpdateTime));
        // Write the sensor animation duration
        streamWriter.writeAttribute(SAVE_SENSOR_ANIMATION_DURATION_TAG, Integer.toString(sensorAnimationDuration));
        writeIndentation(streamWriter, ++depth, true);

        // Save all of the pages
        for (int p = 0; p < pageDataList.size(); p++)
        {
            PageData temp = pageDataList.get(p);
            streamWriter.writeStartElement(PAGE_ELEMENT_TAG);
            streamWriter.writeAttribute(PAGE_ID_ELEMENT_TAG, Integer.toString(temp.getUniqueId()));
            streamWriter.writeAttribute(PAGE_BACKGROUND_COLOUR_ELEMENT_TAG, temp.getColour().toString());
            streamWriter.writeAttribute(PAGE_TITLE_COLOUR_ELEMENT_TAG, temp.getTitleColour().toString());
            streamWriter.writeAttribute(PAGE_SUBTITLE_COLOUR_ELEMENT_TAG, temp.getSubtitleColour().toString());
            streamWriter.writeAttribute(PAGE_ROWS_ELEMENT_TAG, Integer.toString(temp.getRows()));
            streamWriter.writeAttribute(PAGE_COLUMNS_ELEMENT_TAG, Integer.toString(temp.getColumns()));
            streamWriter.writeAttribute(PAGE_NEXT_PAGE_ID_ELEMENT_TAG, Integer.toString(temp.getNextPageId()));
            streamWriter.writeAttribute(PAGE_TRANSITION_ID_ELEMENT_TAG, Integer.toString(temp.getTransitionType()));
            streamWriter.writeAttribute(PAGE_TRANSITION_TIME_ID_ELEMENT_TAG, Integer.toString(temp.getTransitionTime()));
            streamWriter.writeAttribute(PAGE_DURATION_MS_ELEMENT_TAG, Integer.toString(temp.getDurationMs()));
            streamWriter.writeAttribute(PAGE_TITLE_ELEMENT_TAG, temp.getTitle());
            streamWriter.writeAttribute(PAGE_TITLE_ENABLED_ELEMENT_TAG, Boolean.toString(temp.isTitleEnabled()));
            streamWriter.writeAttribute(PAGE_TITLE_ALIGNMENT_ELEMENT_TAG, Integer.toString(temp.getTitleAlignment()));
            streamWriter.writeAttribute(PAGE_SUBTITLE_ELEMENT_TAG, temp.getSubtitle());
            streamWriter.writeAttribute(PAGE_SUBTITLE_ENABLED_ELEMENT_TAG, Boolean.toString(temp.isSubtitleEnabled()));
            streamWriter.writeAttribute(PAGE_SUBTITLE_ALIGNMENT_ELEMENT_TAG, Integer.toString(temp.getSubtitleAlignment()));

            if (!temp.getSensorList().isEmpty())
            {
                writeIndentation(streamWriter, ++depth, true);
            }

            // Write sensor data
            for (int s = 0; s < temp.getSensorList().size(); s++)
            {
                Sensor sensor = temp.getSensorList().get(s);

                streamWriter.writeStartElement(SENSOR_ELEMENT_TAG);
                streamWriter.writeAttribute(SENSOR_ID_ELEMENT_TAG, Integer.toString(sensor.getUniqueId()));
                streamWriter.writeAttribute(SENSOR_ROW_ELEMENT_TAG, Integer.toString(sensor.getRow()));
                streamWriter.writeAttribute(SENSOR_COLUMN_ELEMENT_TAG, Integer.toString(sensor.getColumn()));
                streamWriter.writeAttribute(SENSOR_TYPE_ELEMENT_TAG, Integer.toString(sensor.getType()));
                streamWriter.writeAttribute(SENSOR_SKIN_ELEMENT_TAG, Byte.toString(sensor.getSkin()));
                streamWriter.writeAttribute(SENSOR_MAX_ELEMENT_TAG, Float.toString(sensor.getMax()));
                streamWriter.writeAttribute(SENSOR_THRESHOLD_ELEMENT_TAG, Float.toString(sensor.getThreshold()));
                streamWriter.writeAttribute(SENSOR_ORIGINAL_NAME_ELEMENT_TAG, sensor.getOriginalName());
                streamWriter.writeAttribute(SENSOR_TITLE_ELEMENT_TAG, sensor.getTitle());
                streamWriter.writeAttribute(SENSOR_HARDWARE_TYPE_ELEMENT_TAG, sensor.getHardwareType());
                streamWriter.writeAttribute(SENSOR_AVERAGE_ENABLED_ELEMENT_TAG, Boolean.toString(sensor.isAverageEnabled()));
                streamWriter.writeAttribute(SENSOR_AVERAGING_PERIOD_ELEMENT_TAG, Integer.toString(sensor.getAveragingPeriod()));
                streamWriter.writeAttribute(SENSOR_ROW_SPAN_ELEMENT_TAG, Integer.toString(sensor.getRowSpan()));
                streamWriter.writeAttribute(SENSOR_COLUMN_SPAN_ELEMENT_TAG, Integer.toString(sensor.getColumnSpan()));

                if (sensor.getAverageColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_AVERAGE_COLOUR_ELEMENT_TAG, sensor.getAverageColour().toString());
                }

                if (sensor.getNeedleColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_NEEDLE_COLOUR_ELEMENT_TAG, sensor.getNeedleColour().toString());
                }

                if (sensor.getValueColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_VALUE_COLOUR_ELEMENT_TAG, sensor.getValueColour().toString());
                }

                if (sensor.getUnitColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_UNIT_COLOUR_ELEMENT_TAG, sensor.getUnitColour().toString());
                }

                if (sensor.getKnobColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_KNOB_COLOUR_ELEMENT_TAG, sensor.getKnobColour().toString());
                }

                if (sensor.getBarColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_BAR_COLOUR_ELEMENT_TAG, sensor.getBarColour().toString());
                }

                if (sensor.getThresholdColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_THRESHOLD_COLOUR_ELEMENT_TAG, sensor.getThresholdColour().toString());
                }

                if (sensor.getTitleColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_TITLE_COLOUR_ELEMENT_TAG, sensor.getTitleColour().toString());
                }

                if (sensor.getBarBackgroundColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_BAR_BACKGROUND_COLOUR_ELEMENT_TAG, sensor.getBarBackgroundColour().toString());
                }

                if (sensor.getForegroundColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_FOREGROUND_COLOUR_ELEMENT_TAG, sensor.getForegroundColour().toString());
                }

                if (sensor.getTickLabelColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_TICK_LABEL_COLOUR_ELEMENT_TAG, sensor.getTickLabelColour().toString());
                }

                if (sensor.getTickMarkColour() != null)
                {
                    streamWriter.writeAttribute(SENSOR_TICK_MARK_COLOUR_ELEMENT_TAG, sensor.getTickMarkColour().toString());
                }

                streamWriter.writeEndElement();

                if (s == temp.getSensorList().size() - 1)
                {
                    depth--;
                }

                writeIndentation(streamWriter, depth, true);
            }


            streamWriter.writeEndElement();

            if (p == pageDataList.size() - 1)
            {
                depth--;
            }

            writeIndentation(streamWriter, depth, true);
        }

        streamWriter.writeEndElement();
        writeIndentation(streamWriter, --depth, true);
        streamWriter.writeEndDocument();
        streamWriter.flush();
    }

    private void parsePageData(Attributes attributes)
    {
        int id = 0;
        Color backgroundColour = null;
        Color titleColour = null;
        Color subtitleColour = null;
        int rows = 0;
        int columns = 0;
        int nextPageId = 0;
        int transitionId = 0;
        int transitionTime = 0;
        int durationMs = 0;
        String title = null;
        boolean titleEnabled = false;
        int titleAlignment = 0;
        boolean subtitleEnabled = false;
        String subtitle = null;
        int subtitleAlignment = 0;

        System.out.print("\tPage:");
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String attributeName = attributes.getQName(i);
            String attributeValue = attributes.getValue(i);
            System.out.print(" '" + attributeName + "':" + attributeValue);

            // Parse attributes
            if (attributeName.compareTo(PAGE_ID_ELEMENT_TAG) == 0)
            {
                id = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_BACKGROUND_COLOUR_ELEMENT_TAG) == 0)
            {
                backgroundColour = Color.web(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_TITLE_COLOUR_ELEMENT_TAG) == 0)
            {
                titleColour = Color.web(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_SUBTITLE_COLOUR_ELEMENT_TAG) == 0)
            {
                subtitleColour = Color.web(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_ROWS_ELEMENT_TAG) == 0)
            {
                rows = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_COLUMNS_ELEMENT_TAG) == 0)
            {
                columns = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_NEXT_PAGE_ID_ELEMENT_TAG) == 0)
            {
                nextPageId = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_TRANSITION_ID_ELEMENT_TAG) == 0)
            {
                transitionId = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_TRANSITION_TIME_ID_ELEMENT_TAG) == 0)
            {
                transitionTime = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_DURATION_MS_ELEMENT_TAG) == 0)
            {
                durationMs = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_TITLE_ELEMENT_TAG) == 0)
            {
                title = attributeValue;
            }
            else if (attributeName.compareTo(PAGE_TITLE_ENABLED_ELEMENT_TAG) == 0)
            {
                titleEnabled = Boolean.parseBoolean(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_TITLE_ALIGNMENT_ELEMENT_TAG) == 0)
            {
                titleAlignment = Integer.parseInt(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_SUBTITLE_ELEMENT_TAG) == 0)
            {
                subtitle = attributeValue;
            }
            else if (attributeName.compareTo(PAGE_SUBTITLE_ENABLED_ELEMENT_TAG) == 0)
            {
                subtitleEnabled = Boolean.parseBoolean(attributeValue);
            }
            else if (attributeName.compareTo(PAGE_SUBTITLE_ALIGNMENT_ELEMENT_TAG) == 0)
            {
                subtitleAlignment = Integer.parseInt(attributeValue);
            }
        }

        currentPageData = new PageData(id, backgroundColour, titleColour, subtitleColour, rows, columns, nextPageId,
                transitionId, transitionTime, durationMs, title, titleEnabled, titleAlignment, subtitle,
                subtitleEnabled, subtitleAlignment);
        pageDataList.add(currentPageData);

        System.out.println();
    }

    public void parseSensorData(Attributes attributes)
    {
        if (currentPageData != null)
        {
            int id = 0;
            int row = 0;
            int column = 0;
            int type = 0;
            byte skin = 0;
            float max = 0.0f;
            float threshold = 0.0f;
            String originalName = null;
            String title = null;
            String hardwareType = null;
            boolean averageEnabled = false;
            int averagingPeriod = 10000;
            int rowSpan = 1;
            int columnSpan = 1;

            Color averageColour = null;
            Color needleColour = null;
            Color valueColour = null;
            Color unitColour = null;
            Color knobColour = null;
            Color barColour = null;
            Color thresholdColour = null;
            Color titleColour = null;
            Color barBackgroundColour = null;
            Color foregroundColour = null;
            Color tickLabelColour = null;
            Color tickMarkColour = null;

            System.out.print("\tSensor:");
            for (int i = 0; i < attributes.getLength(); i++)
            {
                System.out.print(" '" + attributes.getQName(i) + "':" + attributes.getValue(i));

                String attributeName = attributes.getQName(i);
                String attributeValue = attributes.getValue(i);
                System.out.print(" '" + attributeName + "':" + attributeValue);

                // Parse attributes
                if (attributeName.compareTo(SENSOR_ID_ELEMENT_TAG) == 0)
                {
                    id = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_ROW_ELEMENT_TAG) == 0)
                {
                    row = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_COLUMN_ELEMENT_TAG) == 0)
                {
                    column = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_TYPE_ELEMENT_TAG) == 0)
                {
                    type = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_SKIN_ELEMENT_TAG) == 0)
                {
                    skin = Byte.parseByte(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_MAX_ELEMENT_TAG) == 0)
                {
                    max = Float.parseFloat(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_THRESHOLD_ELEMENT_TAG) == 0)
                {
                    threshold = Float.parseFloat(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_ORIGINAL_NAME_ELEMENT_TAG) == 0)
                {
                    originalName = attributeValue;
                }
                else if (attributeName.compareTo(SENSOR_TITLE_ELEMENT_TAG) == 0)
                {
                    title = attributeValue;
                }
                else if (attributeName.compareTo(SENSOR_HARDWARE_TYPE_ELEMENT_TAG) == 0)
                {
                    hardwareType = attributeValue;
                }
                else if (attributeName.compareTo(SENSOR_AVERAGE_ENABLED_ELEMENT_TAG) == 0)
                {
                    averageEnabled = Boolean.parseBoolean(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_AVERAGING_PERIOD_ELEMENT_TAG) == 0)
                {
                    averagingPeriod = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_ROW_SPAN_ELEMENT_TAG) == 0)
                {
                    rowSpan = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_COLUMN_SPAN_ELEMENT_TAG) == 0)
                {
                    columnSpan = Integer.parseInt(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_AVERAGE_COLOUR_ELEMENT_TAG) == 0)
                {
                    averageColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_NEEDLE_COLOUR_ELEMENT_TAG) == 0)
                {
                    needleColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_VALUE_COLOUR_ELEMENT_TAG) == 0)
                {
                    valueColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_UNIT_COLOUR_ELEMENT_TAG) == 0)
                {
                    unitColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_KNOB_COLOUR_ELEMENT_TAG) == 0)
                {
                    knobColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_BAR_COLOUR_ELEMENT_TAG) == 0)
                {
                    barColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_THRESHOLD_COLOUR_ELEMENT_TAG) == 0)
                {
                    thresholdColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_TITLE_COLOUR_ELEMENT_TAG) == 0)
                {
                    titleColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_BAR_BACKGROUND_COLOUR_ELEMENT_TAG) == 0)
                {
                    barBackgroundColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_FOREGROUND_COLOUR_ELEMENT_TAG) == 0)
                {
                    foregroundColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_TICK_LABEL_COLOUR_ELEMENT_TAG) == 0)
                {
                    tickLabelColour = Color.web(attributeValue);
                }
                else if (attributeName.compareTo(SENSOR_TICK_MARK_COLOUR_ELEMENT_TAG) == 0)
                {
                    tickMarkColour = Color.web(attributeValue);
                }
            }

            Sensor sensor = new Sensor(id, row, column, (byte) type, skin, max, threshold, originalName, title, averageEnabled,
                    averagingPeriod, rowSpan, columnSpan);
            sensor.setHardwareType(hardwareType);

            if (foregroundColour != null)
            {
                sensor.setForegroundColour(foregroundColour);
            }

            if (averageColour != null)
            {
                sensor.setAverageColour(averageColour);
            }

            if (needleColour != null)
            {
                sensor.setNeedleColour(needleColour);
            }

            if (valueColour != null)
            {
                sensor.setValueColour(valueColour);
            }

            if (unitColour != null)
            {
                sensor.setUnitColour(unitColour);
            }

            if (knobColour != null)
            {
                sensor.setKnobColour(knobColour);
            }

            if (barColour != null)
            {
                sensor.setBarColour(barColour);
            }

            if (thresholdColour != null)
            {
                sensor.setThresholdColour(thresholdColour);
            }

            if (titleColour != null)
            {
                sensor.setTitleColour(titleColour);
            }

            if (barBackgroundColour != null)
            {
                sensor.setBarBackgroundColour(barBackgroundColour);
            }

            if (tickLabelColour != null)
            {
                sensor.setTickLabelColour(tickLabelColour);
            }

            if (tickMarkColour != null)
            {
                sensor.setTickMarkColour(tickMarkColour);
            }

            currentPageData.addSensor(sensor);

            System.out.println();
        }
        else
        {
            System.err.println("Error reading save");
        }
    }
}