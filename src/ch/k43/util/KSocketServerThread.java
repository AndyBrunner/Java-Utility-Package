package ch.k43.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Class to handle user connections accepted by KSocketServerListener. The input is read as characters while output is sent as bytes.
 * 
 * @see getLastError
 * @see isSecuredConnection
 * @see isConnected
 * @see isDataAvailable
 * @see getAuthenticatedClient() 
 */
public abstract class KSocketServerThread extends Thread implements AutoCloseable {

	// Class variables
	private	Socket				gSocket					= null;
	private BufferedReader		gBufferedReader			= null;
	private OutputStream		gOutputStream			= null;
	private String				gThisClassName			= null;
	private String				gUsedProtocol			= null;
	private String				gUsedCiphers			= null;
	private String				gLastErrorMessage		= null;
	private String				gAuthenticatedClient	= null;
	private int					gPortNumber				= 0;
	private boolean				gIsConnected			= false;
	private boolean				gIsSecuredConnection	= false;
	
	/**
	 * Prohibit default class constructor without arguments.
	 */
	@SuppressWarnings("unused")
	private KSocketServerThread() {
		// Dummy constructor to prevent class instantiation without arguments
	}
	
	/**
	 * Constructor to initialize user thread.<br>
	 * 
	 * @param argSocket	Socket created and passed by KSocketServerListener during thread creation
	 */
	protected KSocketServerThread(Socket argSocket) {
	
		// Check arguments
		KLog.argException(K.isEmpty(argSocket), "KSocketServerThread(): argSocket is required");
		
		// Save TLS socket
		gSocket = argSocket;
	
		// Get own class name
		gThisClassName = this.getClass().getName();
				
		// Set up connection
		try {
		
			// Save local port
			gPortNumber = gSocket.getLocalPort();
			
			// Set and save TLS attributes
			if (gSocket instanceof SSLSocket) {
				
				// Mask connection as secured
				gIsSecuredConnection = true;
				
				// Force TLS handshake
				((SSLSocket) gSocket).startHandshake();
				
				// Save TLS protocol and used ciphers
				SSLSession sslSession = ((SSLSocket) gSocket).getSession();
				gUsedProtocol = sslSession.getProtocol();
	            gUsedCiphers = sslSession.getCipherSuite();
	            KLog.debug("Protocol/cipher used {}/{}", gUsedProtocol, gUsedCiphers);
	            	            
	    		// Try to get name of authenticated client
	    		try {
	    			gAuthenticatedClient = sslSession.getPeerPrincipal().getName();
	            	KLog.debug("Client authenticated as {}", getAuthenticatedClientCN());
	    		} catch (Exception e1) {
	    			// Exception expected if no client authentication used
	    			KLog.debug("No client authentication used");
	    		}
			}
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
		}

		KLog.debug("Thread {} started", gThisClassName);
		
		// Get remote endpoint without starting slash
		String remoteAddress = gSocket.getRemoteSocketAddress().toString();
		if (remoteAddress.startsWith("/")) {
			remoteAddress = remoteAddress.substring(1);
		}
		
		KLog.debug("Client {} connected ({}TLS)", remoteAddress, (gIsSecuredConnection ? "" : "Non-"));
		
		// Establish input/output streams
		try {
			gBufferedReader	= new BufferedReader(new InputStreamReader(gSocket.getInputStream()));
			gOutputStream	= gSocket.getOutputStream();
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
		}
	}
	
	/**
	 * Close the socket connection.<br>
	 */
	public void close() {
		
		// Close reader and writer
		try {
			if (gBufferedReader != null) {
				gBufferedReader.close();
				gBufferedReader = null;
			}
		}  catch (Exception e) {
			KLog.error(e.toString());
		}

		try {
			if (gOutputStream != null) {
				gOutputStream.close();
				gOutputStream = null;
			}
		} catch (Exception e) {
			KLog.error(e.toString());
		}
		
		// Close sockets
		try {
			if (gSocket != null) {
				gSocket.close();
				gSocket = null;
			}
		} catch (Exception e) {
			KLog.error(e.toString());
		}

		// Reset state
		gUsedCiphers			= null;
		gUsedProtocol			= null;
		gLastErrorMessage		= null;
		gIsConnected			= false;
		gIsSecuredConnection	= false;
		
		KLog.debug("Thread {} terminated", this.getClass().getName());
	}
	
	/**
	 * Return name of authenticated client based on the used client certificate.<br>
	 * 
	 * @return DN name of peer principal or null if not TLS authenticated
	 * 
	 * @since 2024.05.17
	 */
	public String getAuthenticatedClient() {
		return (gAuthenticatedClient);
	}

	/**
	 * Return common name (without CN=) of the DN (distinguished name) from the used client certificate.<br>
	 * 
	 * @return Common name of peer principal or null if not TLS authenticated
	 * 
	 * @since 2024.05.17
	 */
	public String getAuthenticatedClientCN() {
		
		if (gAuthenticatedClient == null) {
			return (null);
		}
		
		if (gAuthenticatedClient.toUpperCase().startsWith("CN=")) {
			return (gAuthenticatedClient.split(",")[0].substring(3));
		} else {
			return (gAuthenticatedClient);
		}
	}
	
	/**
	 * Return used TCP cipher suite (Example: "TLS_AES_256_GCM_SHA384").<br>
	 * 
	 * @return	TLS cipher suite or null
	 */
	public String getCiphers() {
		return (gUsedCiphers);
	}
	
	/**
	 * Return last error.<br>
	 * 
	 * @return	Error message or null
	 */
	public String getLastError() {
		return (gLastErrorMessage);
	}
	
	/**
	 * Return used TCP connection protocol (Example: "TLSv1.3").<br>
	 * 
	 * @return	TLS protocol used or null
	 */
	public String getProtocol() {
		return (gUsedProtocol);
	}

	/**
	 * Return connection state.<br>
	 * 
	 * @return	True if connected, false otherwise
	 */
	public boolean isConnected() {

		// Check if TLS socket is connected and active
		if ((gSocket == null) || (gSocket.isClosed()) || (!gSocket.isBound())) {
			return (false);
		}
	
		// Return
		return (true);
	}
	
	/**
	 * Check if data is available without blocking.<br>
	 * 
	 * @return	True if data is available, false otherwise
	 */
	public boolean isDataAvailable() {

		// Clear error message
		gLastErrorMessage = null;
		
		// Return status
		try {
			if (gBufferedReader != null) {
				return (gBufferedReader.ready());
			}
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
		}
		
		return (false);
	}

	/**
	 * Check if connection is TLS secured.<br>
	 * 
	 * @return True if connection is TLS secured, false otherwise
	 */
	public boolean isSecuredConnection() {
		return (gIsSecuredConnection);
	}
	
	/**
	 * Read socket into character array.<br>
	 * 
	 * @param	argData	Character buffer
	 * @return	Number of bytes read or -1
	 */
	public int read(char[] argData) {
	
		// Check arguments
		KLog.argException(K.isEmpty(argData), "KSocketServerThread.read(): argData must not be empty");
		
		// Declarations
		int bytesRead = 0;

		// Clear error message
		gLastErrorMessage = null;
		
		try {
			bytesRead = gBufferedReader.read(argData, 0, argData.length);
			
			if (bytesRead != -1) {
				KLog.debug("Data read ({} characters)", K.formatBytes(bytesRead));
			} else {
				KLog.debug("End-of-data received");
			}

			return (bytesRead);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (-1);
		}
	}
	
	/**
	 * Read line from socket.<br>
	 * 
	 * @return String read or null for end-of-data
	 */
	public String readLine() {

		// Declarations
		String lineRead = null;

		// Clear error message
		gLastErrorMessage = null;
		
		try {
			lineRead = gBufferedReader.readLine();
			
			if (lineRead != null) {
				KLog.debug("Data read ({} characters)", lineRead.length());
			} else {
				KLog.debug("End-of-data received");
			}
			
			return (lineRead);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (null);
		}
	}
	
	/**
	 * Main thread entry point - Will be overwritten by user class.<br>
	 */
	@Override
	public void run() {
		KLog.argException(true, "{}.run(): Method not implemented", gThisClassName);
	}
	
	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSocketServerThread [gSocket=" + gSocket + ", gBufferedReader=" + gBufferedReader + ", gOutputStream="
				+ gOutputStream + ", gThisClassName=" + gThisClassName + ", gUsedProtocol=" + gUsedProtocol
				+ ", gUsedCiphers=" + gUsedCiphers + ", gLastErrorMessage=" + gLastErrorMessage
				+ ", gAuthenticatedClient=" + gAuthenticatedClient + ", gPortNumber=" + gPortNumber + ", gIsConnected="
				+ gIsConnected + ", gIsSecuredConnection=" + gIsSecuredConnection + "]";
	}
	
	/**
	 * Write byte array to socket.
	 * 
	 * @param	argData	Character array to be written
	 * @return	True if successful, false otherwise
	 */
	public boolean write(char[] argData) {
		return write(new String(argData).getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Write character array to socket.
	 *
	 * @param	argData	Byte to be written
	 * @return 	True if successful, false otherwise
	 */
	public boolean write(byte[] argData) {
		
		// Clear error message
		gLastErrorMessage = null;

		try {
			gOutputStream.write(argData);
			gOutputStream.flush();
			KLog.debug("Data sent ({})", K.formatBytes(argData.length));
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (false);
		}
	}

	/**
	 * Write string to socket.
	 * 
	 * @param	argLine	String to be written
	 * @return	True if successful, false otherwise
	 */
	public boolean write(String argLine) {
		return write(argLine.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Write line terminated with a platform dependent end-of-line to socket.
	 * 
	 * @param	argLine	String to be written
	 * @return	True if successful, false otherwise
	 * 
	 * @since 2024.05.25
	 */
	public boolean writeLine(String argLine) {
		return write((argLine + K.LINE_SEPARATOR).getBytes(StandardCharsets.UTF_8));
	}
}
