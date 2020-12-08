package se.miun.dt070a.mqttbroker.request;

import se.miun.dt070a.mqttbroker.MQTTLogger;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;
import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;

import java.io.IOException;
import java.net.Socket;

public class SubscribeRequest extends Request {

    private int packetIdentifier;
    private int subscribeFlags;

    public SubscribeRequest(Socket socket, int flags) throws IOException {
        super(socket);
        this.subscribeFlags = flags;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public void createFromInputStream() throws IOException, MalformedMQTTRequestError {
        if ((subscribeFlags & 0b01111101) != 0) {      //subscribe flags has to be 10000010
            getSocket().close();
            throw new MalformedMQTTRequestError("Invalid flags");
        }

        if (remaining == 0)
            return;

        try {
            int lengthMSB = nextByteIfRemainingElseThrow();
            int lengthLSB = nextByteIfRemainingElseThrow();

            //move significant bit 8 steps to the left
            packetIdentifier = (lengthMSB << 8) + lengthLSB;
            System.out.println("Variable length header length: " + packetIdentifier);
        } catch (MalformedMQTTRequestError malformedMQTTRequestError) {
            MQTTLogger.log("Malformed mqtt request");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SUBSCRIBE;
    }
}
