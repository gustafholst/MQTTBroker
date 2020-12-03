package se.miun.dt070a.mqttbroker;

import java.io.IOException;
import java.net.Socket;

public abstract class Response {

    private Socket socket;

    protected byte[] header;

    protected Response(Socket socket) {
        this.socket = socket;
    }

    public void send() throws IOException {
        socket.getOutputStream().write(header);
    }

    public abstract MessageType getMessageType();
    public abstract byte getMessageTypeAsByte();
}
