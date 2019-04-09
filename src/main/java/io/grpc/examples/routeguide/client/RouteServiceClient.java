package io.grpc.examples.routeguide.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteGuideGrpc;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteSummary;
import io.grpc.examples.routeguide.utils.RouteGuideUtil;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author saman.tamkeen
 * 05/04/19
 */
public class RouteServiceClient extends GrpcClient {

    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private final RouteGuideGrpc.RouteGuideStub asyncStub;
    private Random random = new Random();

    public RouteServiceClient(String host, int port) {
        super(host, port);
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public void getFeature(int lat, int lon) {
        System.out.println(String.format("*** GetFeature: lat=%d lon=%d", lat, lon));

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        Feature feature;
        try {
            feature = blockingStub.getFeature(request);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed: " + e.getStatus());
            return;
        }
        if (RouteGuideUtil.exists(feature)) {
            System.out.println(String.format("Found feature called \"%s\" at %f, %f",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation())));
        } else {
            System.out.println(String.format("Found no feature at %f, %f",
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation())));
        }
    }

    /**
     * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
     * response feature as it arrives.
     */
    public void listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        System.out.println(String.format("*** ListFeatures: lowLat=%d lowLon=%d hiLat=%d hiLon=%d",
                lowLat, lowLon, hiLat, hiLon));

        Rectangle request =
                Rectangle.newBuilder()
                        .setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
                        .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build()).build();
        Iterator<Feature> features;
        try {
            features = blockingStub.listFeatures(request);
            for (int i = 1; features.hasNext(); i++) {
                Feature feature = features.next();
                String.format(String.format("Result #%d: %s", i, feature));
            }
        } catch (StatusRuntimeException e) {
            System.err.println(String.format("RPC failed: %s", e.getStatus()));
        }
    }

    /**
     * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
     * features} with a variable delay in between. Prints the statistics when they are sent from the
     * server.
     */
    public void recordRoute(List<Feature> features, int numPoints) throws InterruptedException {
        System.out.println("*** RecordRoute");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteSummary> responseObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary summary) {
                System.out.println(String.format("Finished trip with %d points. Passed %d features. "
                                + "Travelled %d meters. It took %d seconds.", summary.getPointCount(),
                        summary.getFeatureCount(), summary.getDistance(), summary.getElapsedTime()));
            }

            @Override
            public void onError(Throwable t) {
                System.err.println(String.format("RecordRoute Failed: %s", Status.fromThrowable(t)));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
        try {
            // Send numPoints points randomly selected from the features list.
            for (int i = 0; i < numPoints; ++i) {
                int index = random.nextInt(features.size());
                Point point = features.get(index).getLocation();
                System.out.println(String.format("Visiting point %f, %f", RouteGuideUtil.getLatitude(point),
                        RouteGuideUtil.getLongitude(point)));
                requestObserver.onNext(point);
                // Sleep for a bit before sending the next one.
                Thread.sleep(random.nextInt(1000) + 500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before we finished sending.
                    // Sending further requests won't error, but they will just be thrown away.
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.err.println("recordRoute can not finish within 1 minutes");
        }
    }

    /**
     * Bi-directional example, which can only be asynchronous. Send some chat messages, and print any
     * chat messages that are sent from the server.
     */
    public CountDownLatch routeChat() {
        System.out.println("*** RouteChat");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver =
                asyncStub.routeChat(new StreamObserver<RouteNote>() {
                    @Override
                    public void onNext(RouteNote note) {
                        System.out.println(String.format("Got message \"%s\" at %d, %d", note.getMessage(), note.getLocation()
                                .getLatitude(), note.getLocation().getLongitude()));
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println(String.format("RouteChat Failed: %s", Status.fromThrowable(t)));
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Finished RouteChat");
                        finishLatch.countDown();
                    }
                });

        try {
            RouteNote[] requests =
                    {newNote("First message", 0, 0), newNote("Second message", 0, 1),
                            newNote("Third message", 1, 0), newNote("Fourth message", 1, 1)};

            for (RouteNote request : requests) {
                System.out.println(String.format("Sending message \"%s\" at %d, %d", request.getMessage(), request.getLocation()
                        .getLatitude(), request.getLocation().getLongitude()));
                requestObserver.onNext(request);
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // return the latch while receiving happens asynchronously
        return finishLatch;
    }

    public void warning(String msg) {
        System.err.println(msg);
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }
}
