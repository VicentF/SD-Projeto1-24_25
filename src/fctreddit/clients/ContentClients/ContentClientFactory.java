package fctreddit.clients.ContentClients;

import java.net.URI;
import java.util.logging.Logger;

import fctreddit.clients.UsersClients.GrpcUsersClients;
import fctreddit.clients.UsersClients.RestUsersClient;
import fctreddit.server.Discovery;
import fctreddit.server.rest.UsersServer;

public class ContentClientFactory {
    private static final Logger Log = Logger.getLogger(RestUsersClient.class.getName());
	private static Discovery discovery;

    public ContentClientFactory() {
    }

    public ContentClient createClient() {
        URI serverURI;
        try{
			discovery = new Discovery(Discovery.DISCOVERY_ADDR);
			discovery.start();

			serverURI = discovery.knownUrisOf(UsersServer.SERVICE, 1)[0];
		}catch( Exception e) {
			Log.info( "Failed to retrieve Users Server URI.");
			throw new RuntimeException(e);
		}
        String[] split = serverURI.toString().split("/");
        String type = split[split.length - 1];
        if(type.equals("rest")){
            return new RestContentClient(serverURI);
        } else if(type.equals("grpc")){
            return new GrpcContentClient(serverURI);
        } else {
            throw new IllegalArgumentException("Unknown client type: " + type);
        }
    }
}
