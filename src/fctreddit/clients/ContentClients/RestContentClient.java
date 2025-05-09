package fctreddit.clients.ContentClients;

import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.rest.RestContent;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestContentClient	extends ContentClient {
    private static final Logger Log = Logger.getLogger(RestContentClient.class.getName());

    private static final int READ_TIMEOUT = 5000;
	private static final int CONNECT_TIMEOUT = 5000;

	private final int MAX_RETRIES = 10;
	private static final int RETRY_SLEEP = 5000;

	private final URI serverURI;

	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestContentClient(URI serverURI) {
			this.config = new ClientConfig();
			
			config.property( ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
			config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

			
			this.client = ClientBuilder.newClient(config);

			target = client.target( serverURI ).path( RestContent.PATH );

			this.serverURI = serverURI;
	}

    public Result<Void> deleteAuthor(String userId, String userPassword) {
		Log.info("RestContentClient :: deleteAuthor() called with userId: " + userId + " and userPassword: " + userPassword);
        Response r = executeOperationDelete(target.queryParam(RestContent.USERID, userId).queryParam(RestContent.PASSWORD, userPassword).request());
        if(r == null) {
			Log.info("RestContentClient :: deleteAuthor() - Response is null.");
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			Log.info("RestContentClient :: deleteAuthor() - Status: " + status + " - serverUri: " + serverURI);
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
