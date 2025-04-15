package fctreddit.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}

	//range: 224.0.0.1 - 239.255.255.255
	static final public InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;
	static final int DISCOVERY_RETRY_TIMEOUT = 5000;
	static final int MAX_DATAGRAM_SIZE = 65536;

	private static final String DELIMITER = "\t";

	private final InetSocketAddress addr;
	private final String serviceName;
	private final String serviceURI;
	private final MulticastSocket ms;

	private final Map<String, Set<URI>> serviceURIS;

	public Discovery(InetSocketAddress addr, String serviceName, String serviceURI) throws SocketException, UnknownHostException, IOException {
		this.addr = addr;
		this.serviceName = serviceName;
		this.serviceURI = serviceURI;
		this.serviceURIS = new HashMap<>();

		if (this.addr == null) {
			throw new RuntimeException("A multinet address has to be provided.");
		} 
		
		this.ms = new MulticastSocket(addr.getPort());
		this.ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
	}

	public Discovery(InetSocketAddress addr) throws SocketException, UnknownHostException, IOException {
		this(addr, null, null);
	}

	public void start() {
		if (this.serviceName != null && this.serviceURI != null) {

			Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName,
					serviceURI));

			byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
			DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

			try {
				new Thread(() -> {
					for (;;) {
						try {
							ms.send(announcePkt);
							Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
						} catch (Exception e) {
							e.printStackTrace();
							// do nothing
						}
					}
				}).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		new Thread(() -> {
			DatagramPacket pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
			for (;;) {
				try {
					pkt.setLength(MAX_DATAGRAM_SIZE);
					ms.receive(pkt);
					String msg = new String(pkt.getData(), 0, pkt.getLength());
					String[] msgElems = msg.split(DELIMITER);
					if (msgElems.length == 2) {

						synchronized (serviceURIS) {
							serviceURIS.computeIfAbsent(msgElems[0], k -> new HashSet<>()).add(URI.create(msgElems[1]));
							serviceURIS.notifyAll();
						}
					}
				} catch (IOException e) {
					// do nothing
				}
			}
		}).start();
	}

	public URI[] knownUrisOf(String serviceName, int minReplies) {
		while (true) {
			try {
				synchronized (serviceURIS) {
					Set<URI> found = serviceURIS.get(serviceName);
					if (found != null && found.size() >= minReplies) {
						return found.toArray(new URI[0]);
					}
					serviceURIS.wait(DISCOVERY_RETRY_TIMEOUT);
				}
			} catch (InterruptedException | NullPointerException e) {
				throw new RuntimeException(e);
			}
        }
	}

	public static void main(String[] args) throws Exception {
		Discovery discovery = new Discovery(DISCOVERY_ADDR, "test",
				"http://" + InetAddress.getLocalHost().getHostAddress());
		discovery.start();
	}
}