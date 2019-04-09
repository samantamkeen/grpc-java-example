package io.grpc.examples.routeguide.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author saman.tamkeen
 * 05/04/19
 */
public class GrpcServer {

    private final Server server;

    public GrpcServer(BindableService service) throws IOException {
        this(getFreePort(), service);
    }

    public GrpcServer(int port, BindableService service) {
        this.server = ServerBuilder.forPort(port).addService(service).build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("*** gRPC server started on port " + server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShutdown()) {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                shutdown();
            }
        }));
    }

    public boolean isShutdown() {
        return server.isShutdown();
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
            System.out.println("*** gRPC server shut down");
        }
    }

    public void awaitTermination() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void startAndAwaitTermination() throws IOException, InterruptedException {
        start();
        awaitTermination();
    }

    public void terminate() throws IOException, InterruptedException {
        shutdown();
        awaitTermination();
    }

    public int getPort() {
        if(server != null)
            return server.getPort();
        throw new IllegalStateException("gRPC server is not started");
    }

    private static int getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
