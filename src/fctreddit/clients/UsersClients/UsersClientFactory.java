package fctreddit.clients.UsersClients;

import java.net.URI;
import java.util.logging.Logger;

import fctreddit.server.Discovery;
import fctreddit.server.rest.UsersServer;

public class UsersClientFactory {
    private static final Logger Log = Logger.getLogger(RestUsersClient.class.getName());
	private static Discovery discovery;

    public UsersClientFactory() {
    }

    public UsersClient createClient() {
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
            return new RestUsersClient(serverURI);
        } else if(type.equals("grpc")){
            return new GrpcUsersClients(serverURI);
        } else {
            throw new IllegalArgumentException("Unknown client type: " + type);
        }
    }
    
}
