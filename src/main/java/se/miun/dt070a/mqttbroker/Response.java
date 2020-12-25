package se.miun.dt070a.mqttbroker;

import java.io.IOException;
import java.net.Socket;

public abstract class Response {

    protected Socket socket;

    protected byte[] header;

    protected Response(Socket socket) {
        this.socket = socket;
    }

    public void send() throws IOException {
        this.socket.getOutputStream().write(header);
    }

    public abstract MessageType getMessageType();
    public abstract byte getMessageTypeAsByte();

    public byte[] concatenate(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
