package com.bennero.client.serial;

public class ConnectionInfo {
        private final ConnectionState connectionState;
        private final String rejectionReason;

        public ConnectionInfo(ConnectionState state) {
            this.connectionState = state;
            this.rejectionReason = "";
        }

        public ConnectionInfo(ConnectionState state, String rejectionReason) {
            this.connectionState = state;
            this.rejectionReason = rejectionReason;
        }

        public ConnectionState getConnectionState() {
            return connectionState;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }
    }