package io.grpc.examples.routeguide;

import io.grpc.examples.routeguide.client.RouteServiceClient;
import io.grpc.examples.routeguide.utils.RouteGuideUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author saman.tamkeen
 * 08/04/19
 */
public class TestClient {

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 2) {
            String host = args[0];
            int port = Integer.valueOf(args[1]);

            List<Feature> features;
            try {
                features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            RouteServiceClient client = new RouteServiceClient(host, port);
            try {
                // Looking for a valid feature
                client.getFeature(409146138, -746188906);

                // Feature missing.
                client.getFeature(0, 0);

                // Looking for features between 40, -75 and 42, -73.
                client.listFeatures(400000000, -750000000, 420000000, -730000000);

                // Record a few randomly selected points from the features file.
                client.recordRoute(features, 10);

                // Send and receive some notes.
                CountDownLatch finishLatch = client.routeChat();

                if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                    client.warning("routeChat can not finish within 1 minutes");
                }
            } finally {
                client.terminate();
            }
        }
    }
}
