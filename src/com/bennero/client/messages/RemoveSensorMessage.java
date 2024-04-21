package com.bennero.client.messages;

import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.RemoveSensorDataPositions;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;

public class RemoveSensorMessage {
    public static byte[] create(byte sensorId, byte pageId) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.REMOVE_SENSOR;
        message[RemoveSensorDataPositions.SENSOR_ID_POS] = sensorId;
        message[RemoveSensorDataPositions.PAGE_ID_POS] = pageId;
        return message;
    }
}
