package fctreddit.server.grpc;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;

import java.net.InetAddress;
import java.util.logging.Logger;

public class ImageServer {
    public static int PORT = 9000;

    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    public static void main( String[] args ) throws Exception {

        ServerCredentials cred = InsecureServerCredentials.create();
        String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostName(), PORT, GRPC_CTX);
        GrpcImageServerStub stub = new GrpcImageServerStub(serverURI);
        Server server = Grpc.newServerBuilderForPort(PORT, cred).addService( stub ).build();

        Log.info( String.format("Users gRPC Server ready @ %s\n", serverURI) );
        server.start().awaitTermination();

    }
}
