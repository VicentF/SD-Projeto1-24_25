package fctreddit.clients.ContentClients;

import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.rest.RestUsers;
import fctreddit.server.Discovery;
import fctreddit.server.rest.UsersServer;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestContentClient {
    private static final Logger Log = Logger.getLogger(RestContentClient.class.getName());
	private static Discovery discovery;

    private static final int READ_TIMEOUT = 5000;
	private static final int CONNECT_TIMEOUT = 5000;

	private final int MAX_RETRIES = 10;
	private static final int RETRY_SLEEP = 5000;
	
	final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestContentClient() {
		try{
			discovery = new Discovery(Discovery.DISCOVERY_ADDR);
			discovery.start();

			this.serverURI = discovery.knownUrisOf(UsersServer.SERVICE, 1)[0];
		}catch( Exception e) {
			Log.info( "Failed to retrieve Content Server URI.");
			throw new RuntimeException(e);
		}
			this.config = new ClientConfig();
			
			config.property( ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
			config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

			
			this.client = ClientBuilder.newClient(config);

			target = client.target( serverURI ).path( RestUsers.PATH );
	}

    public Result<Void> deleteAuthor(String userId, String userPassword) {
        Response r = executeOperationDelete(target.path("deleteAuthor").path(userId).queryParam("userPassword", userPassword).request());
        if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok();
		}
    }

    private Response executeOperationDelete(Builder req){
		for(int i = 0; i < MAX_RETRIES; i++){
			try{
				return req.delete();
			} catch (ProcessingException x){
				Log.info(x.getMessage());
				try {
					Thread.sleep(RETRY_SLEEP);
				} catch (InterruptedException y) {
					//Nothing to be done here.
				}
			} catch (Exception y) {
				y.printStackTrace();
			}
		}
		return null;
	}

    public static ErrorCode getErrorCodeFrom(int status) {
		return switch (status) {
		case 200, 209 -> ErrorCode.OK;
		case 409 -> ErrorCode.CONFLICT;
		case 403 -> ErrorCode.FORBIDDEN;
		case 404 -> ErrorCode.NOT_FOUND;
		case 400 -> ErrorCode.BAD_REQUEST;
		case 500 -> ErrorCode.INTERNAL_ERROR;
		case 501 -> ErrorCode.NOT_IMPLEMENTED;
		default -> ErrorCode.INTERNAL_ERROR;
		};
	}
}
