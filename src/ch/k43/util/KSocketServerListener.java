package ch.k43.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import javax.net.ssl.SSLServerSocket;

/**
 * This class provides support for incoming socket connections.<br>
 * 
 * Notes:<br>
 * - This class is started as a separate thread by KSocketServer.<br>
 * - For each client connection, this class will start a separate user thread (subclass of KSocketServerThread or KHTTPServerThread).<br>
 * 
 * @see KSocketServerThread
 * @see KHTTPServerThread
 * 
 */
class KSocketServerListener extends Thread implements AutoCloseable {

	// Class variables
	private Vector<Socket>	gClientSockets			= null;
	private Class<?>		gUserClass				= null;
	private ServerSocket	gServerSocket			= null;
	private String			gThisClassName			= null;
	private int				gPortNumber				= 0;
	private boolean			gInitialized			= false;
	private boolean			gIsSecuredConnection	= false;
	
	/**
	 * Prohibit default class constructor without arguments.
	 */
	@SuppressWarnings("unused")
	private KSocketServerListener() {
		// Dummy constructor to prevent class instantiation without arguments
	}

	/**
	 * Loads the user class (subclass of KSocketServerThread) and starts it in a separate thread.<br>
	 * 
	 * @param argServerSocket	Server socket passed by KSocketServer
	 * @param argClassName		User class name to be dynamically loaded (passed by KSocketServer)
	 * 
	 * @see close()
	 * @see run
	 */
	public KSocketServerListener(ServerSocket argServerSocket, String argClassName) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argServerSocket), "argServerSocket must not be empty");
		KLog.argException(K.isEmpty(argClassName), "argClassName must not be empty");
		
		// Save passed server socket
		gServerSocket = argServerSocket;
		
		// Save server port number
		gPortNumber = gServerSocket.getLocalPort();
				
		// Get own class name
		gThisClassName = this.getClass().getName();
		
		// Set TLS state
		if (gServerSocket instanceof SSLServerSocket) {
			gIsSecuredConnection = true;
		}
		
		// Load user class handling the client socket requests
		gUserClass = K.loadClass(argClassName);
		
		if (gUserClass == null) {
			KLog.error("Unable to load class {}", argClassName);
			return;
		}
		
    	// Check if user class extends correct parent class
		String superClassName1 = KSocketServerThread.class.getName();
		String superClassName2 = KHTTPServerThread.class.getName();
		
		if (!gUserClass.getSuperclass().getName().equals(superClassName1) && 
		    !gUserClass.getSuperclass().getName().equals(superClassName2)) {
			KLog.error("{} must not extend class {}", argClassName, gUserClass.getSuperclass().getName());
    		return;
    	}
		
    	// Create vector to hold connected client sockets
    	gClientSockets = new Vector<>();
    	
    	// Mark initialization successful
    	gInitialized = true;
	}
	
	/**
	 * Remove inactive client sockets from vector.
	 */
	private synchronized void cleanupSockets() {
		
		// Declarations
		int	socketsCleared = 0;
		
		for (int index = 0; index < gClientSockets.size(); index++) {
		
			Socket clientSocket = gClientSockets.get(index);

			if ((clientSocket != null) && (clientSocket.isClosed())) {
				gClientSockets.remove(index);
				socketsCleared++;
			}
		}
		
		if (socketsCleared > 0) {
			KLog.debug("Inactive client connections removed: {}", socketsCleared);
		}
	}
	
	/**
	 * Close all client sockets to force KSocketServerThread threads termination.<p>
	 * 
	 * @see KSocketServerThread
	 */
	public synchronized void close() {
		
		// Check if any work to be done
		if ((gClientSockets == null) || (gClientSockets.isEmpty())) {
			return;
		}

		// Log session terminations
		KLog.debug("Terminating {} client sessions", gClientSockets.size());
		
		for (Socket clientSocket : gClientSockets) {
			
			// Close client socket to force client thread termination
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (Exception e) {
				KLog.error("Unable to close client socket: {}", e.toString());
			}
		}
		
		// Clear vector
		gClientSockets.removeAllElements();
	}
	
	/**
	 * Main entry point for thread.
	 */
	@Override
	public void run() {

		// Declarations
		boolean threadTermination = false;
		
		// Check if initialization was successful
		if (!gInitialized) {
			KLog.error("{} thread terminating because of previous initialization error", gThisClassName);
			return;
		}
		
		// Wait for incoming client connection and start new thread to handle client requests
		while (!threadTermination) {
			
			try {
				// Wait for next client connection
				KLog.debug("{} waiting on port {} ({}TLS)", gThisClassName, gPortNumber, (!gIsSecuredConnection ? "non-" : ""));
				Socket socket = gServerSocket.accept();
				KLog.debug("{} connected on port {} ({}TLS)", gThisClassName, gPortNumber, (!gIsSecuredConnection ? "non-" : ""));
				
				// Save client socket to vector for cleanup
				gClientSockets.add(socket);
				
				// Remove terminated sockets from vector
				cleanupSockets();
				KLog.debug("Active client connections: {}", gClientSockets.size());
			
				// Create a new instance for the user class and call the constructor
				Constructor<?> classConstructor = gUserClass.getConstructor( Socket.class );		
				Object object = classConstructor.newInstance(socket);
				
				// Call the start() method which itself calls the run() method
				Method startMethod = gUserClass.getMethod("start");
				startMethod.invoke(object);

			} catch (SocketException e1) {
				
				// Terminate this thread if server socket error occurred (mostly due to KSocketServer.close() or KSocketServerListener.close()
				threadTermination = true;
	
			} catch (Exception e3) {
			
				// Log all other exceptions
				KLog.error(e3.toString());
			}
		}
		
		// Terminate thread and close all connections
		KLog.debug("{} on port {} ({}TLS) terminated", gThisClassName, gPortNumber, (!gIsSecuredConnection ? "non-" : ""));
		close();
		
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSocketServerListener [gClientSockets=" + gClientSockets + ", gUserClass=" + gUserClass
				+ ", gServerSocket=" + gServerSocket + ", gThisClassName=" + gThisClassName + ", gPortNumber="
				+ gPortNumber + ", gInitialized=" + gInitialized + ", gIsSecuredConnection=" + gIsSecuredConnection
				+ "]";
	}
}
