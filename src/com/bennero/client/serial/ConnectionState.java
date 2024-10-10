package com.bennero.client.serial;

public enum ConnectionState {
    CONNECTED,
    PORT_FAILED_OPEN,
    WRITE_TIMEOUT,
    READ_TIMEOUT,
    BAD_RESPONSE_WRONG_MESSAGE,
    BAD_RESPONSE_INVALID_CHECKSUM,
    REJECTED_CONNECTION
}