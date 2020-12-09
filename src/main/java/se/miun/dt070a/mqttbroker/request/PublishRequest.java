package se.miun.dt070a.mqttbroker.request;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;
import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;

import java.io.IOException;
import java.net.Socket;

public class PublishRequest extends Request {

    private int QoS;
    private boolean duplicate;
    private boolean retain;

    private String topic;
    private String payload;

    public PublishRequest(Socket socket, int flags) throws IOException {
        super(socket);
        parseFlags(flags);
    }

    public int getQoS() {
        return QoS;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public boolean isRetain() {
        return retain;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getPayload() {
        return this.payload;
    }

    private void parseFlags(int flags) {
                                           // mask        move one step right for value
        this.QoS = (flags & 6) >> 1;       // 00000110 -> 000000XX
        this.duplicate = (flags & 8) != 0; // 00001000
        this.retain = (flags & 1) != 0;    // 00000001
    }

    @Override
    public void createFromInputStream() throws IOException {
        if (remaining == 0)
            return;

        try {
            int lengthMSB = nextByteIfRemainingElseThrow();
            int lengthLSB = nextByteIfRemainingElseThrow();

            //move significant bit 8 steps to the left
            int topicNameLength = (lengthMSB << 8) + lengthLSB;
            System.out.println("Variable length header length: " + topicNameLength);

            /*
                The variable header contains the following fields in the order:
                 - Topic Name
                 - Packet Identifier    (only for QoS 2 or 3)
             */
            byte[] bytes = nextNBytesIfRemainingElseThrow(topicNameLength);
            this.topic = new String(bytes);
            System.out.println("Topic name: " + this.topic);

            if (this.QoS > 0) {
                int packetIdMSB = nextByteIfRemainingElseThrow();
                int packetIdLSB = nextByteIfRemainingElseThrow();

                //move significant bit 8 steps to the left
                int packetId = (packetIdMSB << 8) + packetIdLSB;
                System.out.println("PacketId: " + packetId);
            }

            /*
               The Payload contains the Application Message that is being published. The content and
               format of the data is application specific. The length of the payload can be calculated
               by subtracting the length of the variable header from the Remaining Length field that
               is in the Fixed Header. It is valid for a PUBLISH Packet to contain a zero length payload.
            */

            int payloadLength = remaining;

            byte[] payLoadBuffer = nextNBytesIfRemainingElseThrow(payloadLength);
            this.payload = new String(payLoadBuffer);
            System.out.println("payload: " + this.payload);

        } catch (MalformedMQTTRequestError mqttRequestException) {
            System.out.println("Malformed mqtt request");
        }

    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PUBLISH;
    }
}
