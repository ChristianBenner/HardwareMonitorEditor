package com.bennero.client.message;

import com.bennero.common.messages.ConnectionRequestDataPositions;
import com.bennero.common.messages.VersionParityResponseDataPositions;

public class VersionParityResponseMessage {
    private final byte majorVersion;
    private final byte minorVersion;
    private final byte patchVersion;
    private final boolean accepted;

    private VersionParityResponseMessage(byte majorVersion, byte minorVersion, byte patchVersion,
                                         boolean accepted) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.accepted = accepted;
    }

    public static VersionParityResponseMessage processConnectionRequestMessageData(byte[] bytes) {
        final int majorVersion = bytes[VersionParityResponseDataPositions.MAJOR_VERSION_POS] & 0xFF;
        final int minorVersion = bytes[VersionParityResponseDataPositions.MINOR_VERSION_POS] & 0xFF;
        final int patchVersion = bytes[VersionParityResponseDataPositions.PATCH_VERSION_POS] & 0xFF;
        final boolean accepted = (bytes[VersionParityResponseDataPositions.ACCEPTED_POS] & 0xFF) == 1;

        return new VersionParityResponseMessage((byte) majorVersion, (byte) minorVersion, (byte) patchVersion, accepted);
    }

    public byte getMajorVersion() {
        return majorVersion;
    }

    public byte getMinorVersion() {
        return minorVersion;
    }

    public byte getPatchVersion() {
        return patchVersion;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
