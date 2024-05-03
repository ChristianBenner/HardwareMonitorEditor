package com.bennero.client.message;

import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.VersionParityDataPositions;

import static com.bennero.client.Version.*;
import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;

public class VersionParityMessage {
    public static byte[] create() {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.VERSION_PARITY_MESSAGE;
        message[VersionParityDataPositions.MAJOR_VERSION_POS] = VERSION_MAJOR;
        message[VersionParityDataPositions.MINOR_VERSION_POS] = VERSION_MINOR;
        message[VersionParityDataPositions.PATCH_VERSION_POS] = VERSION_PATCH;
        return message;
    }
}
