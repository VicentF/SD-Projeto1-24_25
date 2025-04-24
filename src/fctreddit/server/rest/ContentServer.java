package fctreddit.server.rest;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import fctreddit.impl.rest.ContentResource;
import fctreddit.server.Discovery;

public class ContentServer {

	private static Logger Log = Logger.getLogger(UsersServer.class.getName());
	private static Discovery discovery;

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	public static final String SERVICE = "Content";
	private static final String SERVER_URI_FMT = "http://%s:%s/rest";
	
	public static void main(String[] args) {
		try {
			
		ResourceConfig config = new ResourceConfig();

		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
		config.register(new ContentResource(serverURI));
		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
	
		Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
		
		discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
		discovery.start();

		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}	
}
