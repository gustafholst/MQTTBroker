package se.miun.dt070a.mqttbroker.error;

import java.net.Socket;

public class ConnectError extends Throwable {

    private final Socket socket;

    public ConnectError(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
