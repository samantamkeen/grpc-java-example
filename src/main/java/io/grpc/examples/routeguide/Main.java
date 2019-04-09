package io.grpc.examples.routeguide;

import io.grpc.examples.routeguide.client.RouteServiceClient;
import io.grpc.examples.routeguide.server.RouteServiceServer;
import io.grpc.examples.routeguide.utils.RouteGuideUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author saman.tamkeen
 * 05/04/19
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        RouteServiceServer server = new RouteServiceServer();
        server.startAndAwaitTermination();
    }
}
