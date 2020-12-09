package se.miun.dt070a.mqttbroker;

public class Topic {

    public final String topicString;

    private Topic(String topic) {
        this.topicString = topic;
    }

    public static Topic parseString(String topicString) {
        return new Topic(topicString);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Topic) {
            return ((Topic)other).topicString.equals(this.topicString);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return topicString.hashCode();
    }
}
