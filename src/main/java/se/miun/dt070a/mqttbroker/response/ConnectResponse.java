package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.Response;

import java.net.Socket;

public class ConnectResponse extends Response {

    public ConnectResponse(Socket socket) {
        super(socket);
        header = new byte[]{(2 << 4), 2, 0, 0};  // 00110000    CONNACK
                                                 // 00000010    Remaining length = 2
                                                 // 00000000    no session
                                                 // 00000000    return code success
    }
}
