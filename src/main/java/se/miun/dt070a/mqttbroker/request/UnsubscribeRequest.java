package se.miun.dt070a.mqttbroker.request;

import io.reactivex.rxjava3.core.Observable;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;
import se.miun.dt070a.mqttbroker.Subscription;
import se.miun.dt070a.mqttbroker.Topic;
import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UnsubscribeRequest extends Request {

    private int packetIdentifier;

    private final List<String> topicStrings = new ArrayList<>();

    public UnsubscribeRequest(Socket socket) throws IOException {
        super(socket);
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public Observable<Topic> getTopics() {
        return Observable.fromIterable(topicStrings).map(Topic::parseString);
    }

    @Override
    public void createFromInputStream() throws IOException, MalformedMQTTRequestError {
        if (remaining == 0)
            return;

        /* variable length header */
        int packetIdMSB = nextByteIfRemainingElseThrow();
        int packetIdLSB = nextByteIfRemainingElseThrow();

        //move significant bit 8 steps to the left
        packetIdentifier = (packetIdMSB << 8) + packetIdLSB;
        System.out.println("packet identifier: " + packetIdentifier);

        /* payload */
        while (remaining > 0) {
            int topicLengthMSB = nextByteIfRemainingElseThrow();
            int topicLengthLSB = nextByteIfRemainingElseThrow();
            int topicLength = (topicLengthMSB << 8) + topicLengthLSB;

            byte[] topicBytes = nextNBytesIfRemainingElseThrow(topicLength);
            String topicString = new String(topicBytes);
            System.out.println("topic: " + topicString);

            topicStrings.add(topicString);
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UNSCUBSCRIBE;
    }
}
