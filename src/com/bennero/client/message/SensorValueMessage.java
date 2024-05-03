package com.bennero.client.message;

import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.SensorValueDataPositions;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;
import static com.bennero.common.messages.MessageUtils.writeToMessage;

public class SensorValueMessage {
    public static byte[] create(int sensorId, float value) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.DATA;
        message[SensorValueDataPositions.ID_POS] = (byte) sensorId;
        writeToMessage(message, SensorValueDataPositions.VALUE_POS, value);
        return message;
    }
}
