package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;
import se.miun.dt070a.mqttbroker.request.PublishRequest;

import java.io.IOException;

public class PublishMessage extends Response {

    private final byte[] packetContent;

    protected PublishMessage(PublishRequest request) {
        super(request.getSocket());
        String payloadString = request.getPayload();
        int fakedFlags = 0b0;

        byte[] topic = request.getTopic().getBytes();
        byte topicLengthMSB = (byte) (topic.length & 0b1111111100000000);  //mask out msb
        byte topicLengthLSB = (byte) (topic.length & 0b11111111);          //mask out lsb

        int topicLengthNumberOfBytes = 2;
        int remainingLength = topicLengthNumberOfBytes + topic.length + payloadString.length();

        header = new byte[]{(byte) (getMessageTypeAsByte() << 4 & fakedFlags) , (byte) remainingLength};
                                                                        // 0011XXXX    PUBLISH
                                                                        // 00000000    Remaining length = 0

                                                                        // XXXXXXXX    variable length header (topic)
                                                                        // XXXXXXXX    payload   (message)
        byte[] variableLengthHeader = concatenate(new byte[]{topicLengthMSB, topicLengthLSB}, topic);
        byte[] payLoadBytes = payloadString.getBytes();

        packetContent = concatenate(concatenate(header, variableLengthHeader), payLoadBytes);

        //TODO implement dup/QoS/retain (fakedFLags) transfer flags from pubreqest in handleRequest
    }

    public byte[] concatenate(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    @Override
    public void send() throws IOException {
        socket.getOutputStream().write(packetContent);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PUBLISH;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte)getMessageType().code;
    }
}
