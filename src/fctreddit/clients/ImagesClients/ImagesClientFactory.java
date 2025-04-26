package fctreddit.clients.ImagesClients;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Result;
import fctreddit.clients.UsersClients.GrpcUsersClients;
import fctreddit.clients.UsersClients.RestUsersClient;
import fctreddit.clients.UsersClients.UsersClient;
import fctreddit.server.Discovery;
import fctreddit.server.rest.UsersServer;

import java.net.URI;

public class ImagesClientFactory {
    private static Discovery discovery;

    public ImagesClient createClient() {
        URI serverURI;
        try{
            discovery = new Discovery(Discovery.DISCOVERY_ADDR);
            discovery.start();

            serverURI = discovery.knownUrisOf(UsersServer.SERVICE, 1)[0];
        }catch( Exception e) {
            throw new RuntimeException(e);
        }

        String[] split = serverURI.toString().split("/");
        String type = split[split.length - 1];

        if(type.equals("rest")){
            return new RestImagesClient(serverURI);
        }
        else if(type.equals("grpc")){
            return new GrpcImagesClient(serverURI);
        }
        else {
            throw new IllegalArgumentException("Unknown client type: " + type);
        }
    }


}
