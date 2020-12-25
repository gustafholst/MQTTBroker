package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.subjects.Subject;
import se.miun.dt070a.mqttbroker.response.PublishMessage;

import java.util.List;
import java.util.Map;

public class MQTTLogger {

    public static void logRequest(Request request) {
        log("Received " + request.getMessageType().name());
    }

    public static void logResponse(Response response) {
        log("Sending " + response.getMessageType().name());
    }

    public static void log(String message) {
        System.out.println(message + " on thread " + Thread.currentThread().getName());
    }

    public static void logTopic(Map<Topic, List<Subscription>> subscriptions, Map<Topic, Subject<PublishMessage>> publications) {

        for (Topic t : subscriptions.keySet()) {
            log("Topic: " + t.topicString);
            System.out.println("*****************************************");
            for (Subscription s : subscriptions.get(t)) {
                log("\tsocket: " + s.getCurrentSocket());
                log("\tdisposed " + s.isDisposed());
                System.out.println("------------------------------");
            }
        }

        for (Topic t : publications.keySet()) {
            log("PUBLICATIONS:");
            log("\t" + publications.get(t).toString());
            log("\thasObservers" + publications.get(t).hasObservers());
        }
    }
}
