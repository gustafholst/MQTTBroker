package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import se.miun.dt070a.mqttbroker.response.PublishMessage;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Subscription {

    public static final int FAILURE = 128;   //0x80

    public final Topic topic;
    public final int QoS;

    private final Socket currentSocket;

    private int responseCode = 0;

    Disposable disposable;

    public Subscription(Socket socket, String topicString, int qos) {
        this.currentSocket = socket;
        this.topic = Topic.parseString(topicString);
        this.QoS = qos;
    }

    public void subscribeToTopic(Observable<PublishMessage> messageStream, Maybe<PublishMessage> retainedMessage) {
        disposable = messageStream
                .startWith(retainedMessage)
                //.doOnNext(pm -> pm.socket = this.currentSocket)
                .delay(10, TimeUnit.MILLISECONDS)
                .doOnNext(MQTTLogger::logResponse)
                .subscribe(pm -> pm.sendToSocket(getCurrentSocket()), err -> setFailure());
    }

    public void unscubscribe() {
        disposable.dispose();
    }

    public boolean isDisposed() {
        return disposable.isDisposed();
    }

    public Topic getTopic() {
        return topic;
    }

    public Socket getCurrentSocket() {
        return currentSocket;
    }

    public int getResponseCode() {
        return responseCode == FAILURE ? responseCode : QoS;
    }

    public void setFailure() {
        responseCode = FAILURE;
    }

}
