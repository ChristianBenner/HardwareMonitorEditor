package com.bennero.client.messages;

import com.bennero.common.Sensor;
import com.bennero.common.Skin;
import com.bennero.common.SkinHelper;
import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.SensorDataPositions;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.writeStringToMessage;
import static com.bennero.common.networking.NetworkUtils.writeToMessage;

public class SensorSetupMessage {
    // Creates a message that contains all information required to create a new sensor
    public static byte[] create(Sensor sensor, byte pageId) {
        byte[] bytes = new byte[MESSAGE_NUM_BYTES];
        bytes[MESSAGE_TYPE_POS] = MessageType.SENSOR_SETUP;
        bytes[SensorDataPositions.ID_POS] = sensor.getUniqueId();
        bytes[SensorDataPositions.PAGE_ID_POS] = pageId;
        bytes[SensorDataPositions.ROW_POS] = (byte) sensor.getRow();
        bytes[SensorDataPositions.COLUMN_POS] = (byte) sensor.getColumn();
        bytes[SensorDataPositions.TYPE_POS] = sensor.getType();
        bytes[SensorDataPositions.SKIN_POS] = sensor.getSkin();
        writeToMessage(bytes, SensorDataPositions.MAX_POS, sensor.getMax());
        writeToMessage(bytes, SensorDataPositions.THRESHOLD_POS, sensor.getThreshold());
        bytes[SensorDataPositions.AVERAGE_ENABLED_POS] = sensor.isAverageEnabled() ? (byte) 0x01 : (byte) 0x00;
        writeToMessage(bytes, SensorDataPositions.AVERAGING_PERIOD_POS, sensor.getAveragingPeriod());
        bytes[SensorDataPositions.ROW_SPAN_POS] = (byte) sensor.getRowSpan();
        bytes[SensorDataPositions.COLUMN_SPAN_POS] = (byte) sensor.getColumnSpan();

        if (sensor.getAverageColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.AVERAGE_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.AVERAGE_COLOUR_R_POS] = (byte) (sensor.getAverageColour().getRed() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_G_POS] = (byte) (sensor.getAverageColour().getGreen() * 255.0);
            bytes[SensorDataPositions.AVERAGE_COLOUR_B_POS] = (byte) (sensor.getAverageColour().getBlue() * 255.0);
        }

        if (sensor.getNeedleColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.NEEDLE_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.NEEDLE_COLOUR_R_POS] = (byte) (sensor.getNeedleColour().getRed() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_G_POS] = (byte) (sensor.getNeedleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.NEEDLE_COLOUR_B_POS] = (byte) (sensor.getNeedleColour().getBlue() * 255.0);
        }

        if (sensor.getValueColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.VALUE_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.VALUE_COLOUR_R_POS] = (byte) (sensor.getValueColour().getRed() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_G_POS] = (byte) (sensor.getValueColour().getGreen() * 255.0);
            bytes[SensorDataPositions.VALUE_COLOUR_B_POS] = (byte) (sensor.getValueColour().getBlue() * 255.0);
        }

        if (sensor.getUnitColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.UNIT_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.UNIT_COLOUR_R_POS] = (byte) (sensor.getUnitColour().getRed() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_G_POS] = (byte) (sensor.getUnitColour().getGreen() * 255.0);
            bytes[SensorDataPositions.UNIT_COLOUR_B_POS] = (byte) (sensor.getUnitColour().getBlue() * 255.0);
        }

        if (sensor.getKnobColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.KNOB_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.KNOB_COLOUR_R_POS] = (byte) (sensor.getKnobColour().getRed() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_G_POS] = (byte) (sensor.getKnobColour().getGreen() * 255.0);
            bytes[SensorDataPositions.KNOB_COLOUR_B_POS] = (byte) (sensor.getKnobColour().getBlue() * 255.0);
        }

        if (sensor.getBarColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.BAR_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.BAR_COLOUR_R_POS] = (byte) (sensor.getBarColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_G_POS] = (byte) (sensor.getBarColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_COLOUR_B_POS] = (byte) (sensor.getBarColour().getBlue() * 255.0);
        }

        if (sensor.getThresholdColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.THRESHOLD_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.THRESHOLD_COLOUR_R_POS] = (byte) (sensor.getThresholdColour().getRed() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_G_POS] = (byte) (sensor.getThresholdColour().getGreen() * 255.0);
            bytes[SensorDataPositions.THRESHOLD_COLOUR_B_POS] = (byte) (sensor.getThresholdColour().getBlue() * 255.0);
        }

        if (sensor.getTitleColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TITLE_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.TITLE_COLOUR_R_POS] = (byte) (sensor.getTitleColour().getRed() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_G_POS] = (byte) (sensor.getTitleColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TITLE_COLOUR_B_POS] = (byte) (sensor.getTitleColour().getBlue() * 255.0);
        }

        if (sensor.getBarBackgroundColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.BAR_BACKGROUND_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_R_POS] = (byte) (sensor.getBarBackgroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_G_POS] = (byte) (sensor.getBarBackgroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_B_POS] = (byte) (sensor.getBarBackgroundColour().getBlue() * 255.0);
        }

        if (sensor.getForegroundColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.FOREGROUND_BASE_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.FOREGROUND_COLOUR_R_POS] = (byte) (sensor.getForegroundColour().getRed() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_G_POS] = (byte) (sensor.getForegroundColour().getGreen() * 255.0);
            bytes[SensorDataPositions.FOREGROUND_COLOUR_B_POS] = (byte) (sensor.getForegroundColour().getBlue() * 255.0);
        }

        if (sensor.getTickLabelColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TICK_LABEL_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_R_POS] = (byte) (sensor.getTickLabelColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_G_POS] = (byte) (sensor.getTickLabelColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_LABEL_COLOUR_B_POS] = (byte) (sensor.getTickLabelColour().getBlue() * 255.0);
        }

        if (sensor.getTickMarkColour() != null && SkinHelper.checkSupport(sensor.getSkin(), Skin.TICK_MARK_COLOUR_SUPPORTED)) {
            bytes[SensorDataPositions.TICK_MARK_COLOUR_R_POS] = (byte) (sensor.getTickMarkColour().getRed() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_G_POS] = (byte) (sensor.getTickMarkColour().getGreen() * 255.0);
            bytes[SensorDataPositions.TICK_MARK_COLOUR_B_POS] = (byte) (sensor.getTickMarkColour().getBlue() * 255.0);
        }

        writeToMessage(bytes, SensorDataPositions.INITIAL_VALUE_POS, sensor.getValue());
        writeStringToMessage(bytes, SensorDataPositions.TITLE_POS, sensor.getTitle(), NAME_STRING_NUM_BYTES);
        return bytes;
    }
}
