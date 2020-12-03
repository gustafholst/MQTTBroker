package se.miun.dt070a.mqttbroker;

import java.io.IOException;
import java.net.Socket;

public class Response {

    private Socket socket;

    protected byte[] header;

    protected Response(Socket socket) {
        this.socket = socket;
    }

    public void send() throws IOException {
        socket.getOutputStream().write(header);
    }

}
