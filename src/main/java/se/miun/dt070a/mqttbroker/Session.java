package se.miun.dt070a.mqttbroker;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class Session {

    private final String clientId;
    private Socket socket;
    private boolean newSession = true;

    public Session(Socket socket) {
        this(socket, UUID.randomUUID().toString());
    }

    public Session(Socket socket, String clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean isNewSession() {
        return newSession;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Session) {
            return this.clientId.equals(((Session) other).getClientId());
        }
        return false;
    }

    public void closePreviousSocketAndReplace(Socket newSocket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = newSocket;
        newSession = false;
    }
}
