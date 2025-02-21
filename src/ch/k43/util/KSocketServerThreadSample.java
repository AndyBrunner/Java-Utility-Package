package ch.k43.util;

import java.net.Socket;

/**
 * Simple upper case echo server to show a sample KSocketServerThread implementation.
 * 
 * Notes:<br>
 * - Use "openssl s_client -connect hostname:9998" (linux) on client to test TLS server.<br>
 * - Use "telnet hostname 9999" (windows) or "nc hostname 9999" (linux) on client to test non-TLS server.<br>
 */
public class KSocketServerThreadSample extends KSocketServerThread {

	/**
	 * Thread constructor.<br>
	 * 
	 * @param argSocket		Socket passed by the KSocketServerListener during thread initialization
	 */
	public KSocketServerThreadSample(Socket argSocket) {
		super(argSocket);
	}

	/**
	 * Thread main entry point (called by KSocketServerListener).
	 */
	@Override
	public void run() {
		
		// Declarations
		String clientData	= null;
		
		// Log program start
		KLog.info("Upper-Case-Echo-Server started");
		
		// Send hello message to client
		write((!isSecuredConnection() ? "Non-" : "") + "TLS Upper-Case-Echo-Server ready." + K.LINE_SEPARATOR);

		// Echo each line in upper case characters
		while ((clientData = readLine()) != null) {
			write(clientData.toUpperCase() + K.LINE_SEPARATOR);
		}
		
		// Close connection
		close();

		// Log program termination
		KLog.info("Upper-Case-Echo-Server terminated");
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSocketServerThreadSample []";
	}
}
