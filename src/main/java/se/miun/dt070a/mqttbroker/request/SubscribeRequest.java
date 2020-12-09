package se.miun.dt070a.mqttbroker.request;

import io.reactivex.rxjava3.core.Observable;
import se.miun.dt070a.mqttbroker.MQTTLogger;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;
import se.miun.dt070a.mqttbroker.Subscription;
import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SubscribeRequest extends Request {

    private int packetIdentifier;
    private int subscribeFlags;

    private final List<Subscription> subscriptions = new ArrayList<>();

    public SubscribeRequest(Socket socket, int flags) throws IOException {
        super(socket);
        this.subscribeFlags = flags;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public Observable<Subscription> getSubscriptions() {
        return Observable.fromIterable(subscriptions);
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

                int qos = nextByteIfRemainingElseThrow() & 0b11;    //TODO close connection if any other bits are set

                subscriptions.add(new Subscription(getSocket(), topicString, qos));
            }

        } catch (MalformedMQTTRequestError malformedMQTTRequestError) {
            MQTTLogger.log("Malformed mqtt request");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SUBSCRIBE;
    }
}
