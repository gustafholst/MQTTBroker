# MQTTbroker

A simple MQTT broker implemented using RxJava as project assignment in course DT070A IoT Protocols.
Only the most basic functionality is implemented.
On the todo list:
 - Session management
 - QoS > 0
 - topic filtering

## Flow of an incoming connection request
MQTTBroker::listenForIncomingConnectionRequests accepts new conections on the server socket and sends
sockets on separate threads to an observable stream ('connections' of type Subject<Socket> from rxjava).
That stream is subscribed to ("listened to") in MQTT::run where each socket is in turn subscribed to in
the same fashion (on separate threads using rxjava Schedulers).

Each connection is listened to and requests are handled as follows (in MQTTBroker::listenToSocket).
 1. Request::parseRequest (factory method) reads the first byte and creates the corresponding Request object.
 2. The constructor of the specific request reads the next byte (remaining length);
 3. Request::createFromInputStream reads the rest of the message and parses accordingly.
 4. MQTTBroker::handleRequest receives the request object and creates an appropriate response
 5. Response is sent to the socket (which is passed on from the request)

Admittedly it is a bit confusing that bytes are read from the inputstream in three different places.
It would have made sense to read them all at once into a buffer of size 'remaining'. I just wanted to
try to parse the requests more or less byte wise.

### Retain
I had to give up the use of ReplaySubject for handling retain since I could not find a way to make it 
distuingush retained messages from not retained ones. The solution had to be a good old Map storing each
retained message per topic. The retained message is passed as argument to Subscription::subscribeToTopic
and transmitted before any other message.

## Classes
 
#### Topic
The idea was to create a class that could parse a string into subtopics and handle the different wild cards.
For now, it is just a wrapper for the String class (no extra functionality).  

#### Session
In its current state the broker stores client ids and can recognize if a client has connected before but not more than
that. TODO: resubscribe to previous subcriptions

#### Subscription
Represents a subscription, including a current socket and a topic (and a QoS but this is not yet implemented).

#### MessageType
Enum with the names of message types as they appear in the mqqt specification.

#### Request
Base class for request implementations (in the request package)

#### Response
Base class for response implementations (in the response package)


