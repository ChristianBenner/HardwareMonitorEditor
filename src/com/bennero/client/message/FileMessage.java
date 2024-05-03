package com.bennero.client.message;

import com.bennero.common.messages.MessageType;

import static com.bennero.common.Constants.*;
import static com.bennero.common.messages.FileDataPositions.*;
import static com.bennero.common.messages.MessageUtils.writeStringToMessage;
import static com.bennero.common.messages.MessageUtils.writeToMessage;

public class FileMessage {
    public static byte[] create(int sizeBytes, String name, byte type) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.FILE_MESSAGE;
        writeToMessage(message, SIZE_POS, sizeBytes);
        message[TYPE_POS] = type;
        writeStringToMessage(message, NAME_POS, name, BACKGROUND_IMAGE_STRING_NUM_BYTES);
        return message;
    }
}
