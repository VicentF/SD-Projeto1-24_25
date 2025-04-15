package fctreddit.clients.UsersClients;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.rest.RestUsers;
import fctreddit.server.Discovery;
import fctreddit.server.rest.UsersServer;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
//import fctreddit.clients.java.UsersClient;

public class RestUsersClient extends UsersClient {
	private static Logger Log = Logger.getLogger(RestUsersClient.class.getName());

    private static final int READ_TIMEOUT = 5000;
	private static final int CONNECT_TIMEOUT = 5000;

	private final int MAX_RETRIES = 10;
	private static final int RETRY_SLEEP = 5000;
	
	final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestUsersClient() {
		try{
			Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR);
			discovery.start();

			this.serverURI = discovery.knownUrisOf(UsersServer.SERVICE, 1)[0];
		}catch( Exception e) {
			Log.info( "Failed to retrieve Users Server URI.");
			throw new RuntimeException(e);
		}
			this.config = new ClientConfig();
			
			config.property( ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
			config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

			
			this.client = ClientBuilder.newClient(config);

			target = client.target( serverURI ).path( RestUsers.PATH );
	}

	//post
	public Result<String> createUser(User user) {
		Response r = executeOperationPost( target.request()
				.accept(MediaType.APPLICATION_JSON), Entity.json(user) );
		if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok(r.readEntity(String.class));
		}
	}

	//get
	public Result<User> getUser(String userId, String pwd) {
		Response r = executeOperationGet(target.path(userId)
			.queryParam(RestUsers.PASSWORD, pwd).request()
			.accept(MediaType.APPLICATION_JSON));
		if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok(r.readEntity(User.class));
		}
	}
	
	
	//put
	public Result<User> updateUser(String userId, String password, User user) {
		Response r = executeOperationPut(target.path(userId)
			.queryParam(RestUsers.PASSWORD, password).request().accept(MediaType.APPLICATION_JSON), Entity.json(user));
		if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok(r.readEntity(User.class));
		}
	}

	//delete
	public Result<User> deleteUser(String userId, String password) {
		Response r = executeOperationDelete(target.path(userId)
			.queryParam(RestUsers.PASSWORD, password).request()
			.accept(MediaType.APPLICATION_JSON));
		if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok(r.readEntity(User.class));
		}
	}

	//get
	public Result<List<User>> searchUsers(String pattern) {
		Response r = executeOperationGet(target.queryParam(RestUsers.QUERY, pattern).request()
			.accept(MediaType.APPLICATION_JSON));
		if(r == null) {
			return Result.error(ErrorCode.TIMEOUT);
		}
		int status = r.getStatus();
		if(status != Status.OK.getStatusCode()) {
			return Result.error( getErrorCodeFrom(status));
		}
		else {
			return Result.ok(r.readEntity(new GenericType<List<User>>() {}));
		}
	}

	private Response executeOperationPost(Builder req, Entity<?> e){
		for(int i = 0; i < MAX_RETRIES; i++){
			try {
				return req.post(e);
			} catch (ProcessingException x) {
				Log.info(x.getMessage());
				try {
					Thread.sleep(RETRY_SLEEP);
				} catch (InterruptedException y) {
					//Nothing to be done here.
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
		return null;
	}

	private Response executeOperationGet(Builder req){
		for(int i = 0; i < MAX_RETRIES; i++){
			try {
				return req.get();
			} catch (ProcessingException x) {
				Log.info(x.getMessage());
				try {
					Thread.sleep(RETRY_SLEEP);
				} catch (InterruptedException y) {
					//Nothing to be done here.
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
		return null;
	}

	private Response executeOperationPut(Builder req, Entity<?> e){
		for(int i = 0; i < MAX_RETRIES; i++){
			try{
				return req.put(e);
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

