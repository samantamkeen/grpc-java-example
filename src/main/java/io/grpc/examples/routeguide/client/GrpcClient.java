package io.grpc.examples.routeguide.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author saman.tamkeen
 * 05/04/19
 */
public class GrpcClient {

    protected final ManagedChannel channel;

    public GrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        System.out.println(String.format("*** gRPC client connected to host %s port %d", host, port));
    }

    public void shutdown() {
        channel.shutdown();
        System.out.println("*** gRPC client shut down");
    }

    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        channel.awaitTermination(l, timeUnit);
        return channel.isTerminated();
    }

    public void terminate() throws InterruptedException {
        shutdown();
        while (!awaitTermination(10, TimeUnit.MILLISECONDS));
    }

}
