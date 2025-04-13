package ch.k43.util;

import java.net.Socket;

/**
 * Simple HTTP web server to serve files.
 * 
 * @since 2025.04.12
 */
public class KHTTPServerThreadSample extends KHTTPServerThread {

	/**
	 * Thread constructor.
	 * 
	 * @param argSocket		Socket passed by the KSocketServerListener during thread initialization
	 */
	public KHTTPServerThreadSample(Socket argSocket) {
		super(argSocket);
	}
	
	/**
	 * HTTP GET method: Send file. 
	 */
	@Override
	public void get(String argURL) {
		sendFile(argURL);
	}
}
