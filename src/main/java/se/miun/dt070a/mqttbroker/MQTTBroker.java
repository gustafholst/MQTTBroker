package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import se.miun.dt070a.mqttbroker.error.ConnectError;
import se.miun.dt070a.mqttbroker.request.ConnectRequest;
import se.miun.dt070a.mqttbroker.request.PublishRequest;
import se.miun.dt070a.mqttbroker.request.SubscribeRequest;
import se.miun.dt070a.mqttbroker.response.ConnectResponse;
import se.miun.dt070a.mqttbroker.response.PingResponse;
import se.miun.dt070a.mqttbroker.response.PublishMessage;
import se.miun.dt070a.mqttbroker.response.SubscribeResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;

public class MQTTBroker {

    private boolean acceptConnections = true;
    private ServerSocket serverSocket;
    private Map<Integer, Disposable> disposables;
    private Disposable mainDisposable;
    private Subject<Socket> connections;
    private final List<Session> sessions = new ArrayList<>();

    private final Map<Topic, List<Subscription>> subscriptions = new HashMap<>();
    private final Map<Topic, Subject<PublishMessage>> publications = new HashMap<>();

    private Session findPreviousSessionOrCreateNew(Request request) {
        ConnectRequest connectRequest = (ConnectRequest)request;
        Optional<String> clientId = connectRequest.getClientId();
        final Socket requestSocket = request.getSocket();

        Function<Optional<String>, Session> sessionFunction = cid -> cid.map(id -> new Session(requestSocket, id))
                .orElseGet(() -> new Session(requestSocket));

        Session session = sessionFunction.apply(clientId);
        if (connectRequest.isCleanSession()) {
            //create new session using clientId if present otherwise use Session constructor without args which
            //creates a random id
            MQTTLogger.log("created new session for client id " + session.getClientId());
            sessions.add(session);
            return session;
        } else {
            Optional<Session> previousSession = sessions.stream().filter(s -> s.equals(session)).findFirst();
            previousSession.ifPresent(s -> s.closePreviousSocketAndReplace(request.getSocket()));
            previousSession.ifPresentOrElse(ps -> MQTTLogger.log("resumed previous session for client id " + session.getClientId())
                    , () -> MQTTLogger.log("no previous session found. created new session for client id " + session.getClientId()));
            return previousSession.orElse(session);
        }
    }

    private Optional<Response> handleRequest(Request request) {
        Response response = null;
        switch (request.getMessageType()) {
            case CONNECT:
                Session session = findPreviousSessionOrCreateNew(request);
                response = new ConnectResponse(session);
                break;
            case PINGREQ:
                response = new PingResponse(request.getSocket());
                break;
            case PUBLISH:
                // create new topic or add to existing
                PublishRequest publishRequest = (PublishRequest)request;
                Topic topic = Topic.parseString(publishRequest.getTopic());
                PublishMessage publishMessage = new PublishMessage((PublishRequest) request);
                if (!subscriptions.containsKey(topic)) {
                    //prepare a new list of subscriptions
                    subscriptions.put(topic, new ArrayList<>());  //or ReplaySubject if retain is set
                    MQTTLogger.log("created new topic \"" + topic.topicString + "\"");
                }
                if (!publications.containsKey(topic)) {
                    publications.put(topic, newPublicationStream(publishRequest.isRetain()));
                }
                //send to all subscribers
                getPublicationStream(topic).onNext(publishMessage);
                break;
            case SUBSCRIBE:
                final SubscribeRequest subscribeRequest = (SubscribeRequest)request;
                final Socket subscribeSocket = subscribeRequest.getSocket();

                subscribeRequest.getSubscriptions()
                        .map(Subscription::getTopic)
                        .flatMap(this::getPublicationStream)
                        .doOnNext(pm -> pm.socket = subscribeSocket)
                        .subscribe(PublishMessage::send);

                response = new SubscribeResponse(((SubscribeRequest)request));
                break;
            default:
        }
        return Optional.ofNullable(response);
    }

    public Subject<PublishMessage> newPublicationStream(boolean retain) {
        if (retain) {
            return ReplaySubject.create();
        }
        return PublishSubject.create();
    }

//    public Observable<PublishMessage> getTopics() {
//        Subject topics = PublishSubject.create();
//        publications.keySet().stream(t -> topics.onNext(t));
//    }

    public Subject<PublishMessage> getPublicationStream(Topic topic) {
        if (publications.containsKey(topic)) {
            return publications.get(topic);
        }

        PublishSubject<PublishMessage> error = PublishSubject.create();
        error.onError(new Throwable("no such topic"));
        return error;
    }

    public void run() throws IOException {
        disposables = new HashMap<>();
        connections = PublishSubject.create();

        // listen for requests on separate thread
        Completable.create(emitter -> listenForIncomingConnectionRequests())
                .subscribeOn(Schedulers.single())
                .subscribe();

        mainDisposable = connections
                //.flatMap(s -> Observable.just(s).observeOn(Schedulers.io()))
                .doOnNext(s -> MQTTLogger.log("tcp connection accepted...now waiting for incoming request"))
                .subscribe(this::listenToSocket);
    }

    private void listenForIncomingConnectionRequests() throws IOException {
        serverSocket = new ServerSocket(1883);
            while (acceptConnections) {
                Socket socket = serverSocket.accept();
                Observable.<Socket>create(emitter -> {
                    emitter.onNext(socket);
                }).observeOn(Schedulers.io()).subscribe(connections);
            }
    }

    private void handleError(Throwable error) {
        if (error instanceof ConnectError) {
            // send out last LWT in case needed

            Socket socket = ((ConnectError) error).getSocket();
            Disposable d = disposables.get(socket.hashCode());
            d.dispose();
        }
    }

    private void listenToSocket(Socket socket) {
        Observable.<Optional<Request>>create(emitter -> {
            while (!emitter.isDisposed()) {
                if (socket.isClosed()) {
                    emitter.onError(new ConnectError(socket));
                }
                else {
                    Optional<Request> request = Request.parseRequest(socket);
                    emitter.onNext(request);
                }
            }
        })//TODO handle MalformedMQTTRequestError
                .doOnSubscribe(d -> storeDisposable(d, socket))
                .doOnError(this::handleError)
                .onErrorComplete(err -> err instanceof ConnectError)
                .filter(Optional::isPresent)
                .map(Optional::get)         //request
                .doOnNext(MQTTLogger::logRequest)
                .map(this::handleRequest)
                .filter(Optional::isPresent)
                .map(Optional::get)        //response
                .doOnNext(MQTTLogger::logResponse)
                .subscribe(Response::send
                        , Throwable::printStackTrace
                        , () -> MQTTLogger.log("Socket closed"));
    }

    private void storeDisposable(Disposable d, Socket socket) {
        disposables.put(socket.hashCode(), d);
    }

    public void shutdown() throws IOException {
        acceptConnections = false;
        disposables.values().forEach(Disposable::dispose);  //dispose all socket subscriptions
        disposables.clear();
        mainDisposable.dispose();  //dispose of incoming sockets stream
        serverSocket.close();
    }

    public static void main(String[] args){
        try {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


        MQTTBroker broker = new MQTTBroker();
        Disposable disposable = Observable.just(broker)
                .subscribeOn(Schedulers.single())
                .doOnNext(MQTTBroker::run)
                .doOnDispose(broker::shutdown)
                .doOnSubscribe(d -> System.out.println("Broker is running...press <enter> to stop"))
                .subscribe();

            br.readLine();

            disposable.dispose();
            Schedulers.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Scanner scanner = new Scanner(System.in);
//
//        MQTTBroker broker = new MQTTBroker();
//
//        broker.start();
//
//        System.out.println("Broker is running...\npress <enter> to stop");
//
//        scanner.nextLine();
//
//        try {
//            broker.shutdown();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Exiting main!");
    }
}
