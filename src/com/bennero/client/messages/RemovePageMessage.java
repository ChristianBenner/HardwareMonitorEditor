package com.bennero.client.messages;

import com.bennero.common.messages.MessageType;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;

public class RemovePageMessage {
    public static byte[] create(byte pageId) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.REMOVE_PAGE;
        message[MESSAGE_TYPE_POS + 1] = pageId;
        return message;
    }
}
