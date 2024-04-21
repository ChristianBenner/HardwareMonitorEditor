package com.bennero.client.messages;

import com.bennero.common.Sensor;
import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.SensorTransformationPositions;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;

public class SensorTransformationMessage {
    public static byte[] create(Sensor sensor, byte pageId) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.SENSOR_TRANSFORMATION_MESSAGE;
        message[SensorTransformationPositions.ID_POS] = (byte) sensor.getUniqueId();
        message[SensorTransformationPositions.PAGE_ID_POS] = pageId;
        message[SensorTransformationPositions.ROW_POS] = (byte) sensor.getRow();
        message[SensorTransformationPositions.COLUMN_POS] = (byte) sensor.getColumn();
        message[SensorTransformationPositions.ROW_SPAN_POS] = (byte) sensor.getRowSpan();
        message[SensorTransformationPositions.COLUMN_SPAN_POS] = (byte) sensor.getColumnSpan();
        return message;
    }
}
