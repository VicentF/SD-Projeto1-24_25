package fctreddit.server.grpc;

import java.net.InetAddress;
import java.util.logging.Logger;

import fctreddit.server.Discovery;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;

public class ContentServer {

    public static int PORT = 9000;

    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";
    private static Discovery discovery;
    private static Logger Log = Logger.getLogger(UsersServer.class.getName());
    private static final String SERVICE = "Content";

    public static void main( String[] args ) throws Exception {

        ServerCredentials cred = InsecureServerCredentials.create();

        String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);
        GrpcContentServerStub stub = new GrpcContentServerStub(serverURI);
        Server server = Grpc.newServerBuilderForPort(PORT, cred).addService( stub ).build();

        Log.info( String.format("Users gRPC Server ready @ %s\n", serverURI) );

        discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
			
		discovery.start();
        server.start().awaitTermination();

    }
}
