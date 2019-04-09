package io.grpc.examples.routeguide.server;

import io.grpc.BindableService;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.service.RouteGuideService;
import io.grpc.examples.routeguide.utils.RouteGuideUtil;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author saman.tamkeen
 * 05/04/19
 */
public class RouteServiceServer extends GrpcServer {

    private RouteServiceServer(BindableService service) throws IOException {
        super(service);
    }

    public RouteServiceServer() throws IOException {
        this(getService());
    }

    private static RouteGuideService getService() throws IOException {
        URL featureFile = RouteGuideUtil.getDefaultFeaturesFile();
        List<Feature> features = RouteGuideUtil.parseFeatures(featureFile);
        return new RouteGuideService(features);
    }
}
