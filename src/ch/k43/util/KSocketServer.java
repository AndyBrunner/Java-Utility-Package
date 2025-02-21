package ch.k43.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;


/**
 * This class provides support for a TLS-secured or non-secured socket server.<p>
 * 
 * Notes:<br>
 * - The constructor will load and start the KSocketServerListener thread which process all incoming client connects.<br>
 * - The KSocketServerListener will start a separate KSocketServerThread for each accepted client connection.<br>
 * 
 * @see KSocketServerListener
 * @see KSocketServerThread
 */
public class KSocketServer implements AutoCloseable{

	// Class variables
	private ServerSocket	gServerSocket		= null;
	private Thread			gListenerThread		= null;
	private String			gLastErrorMessage	= null;
		
	/**
	 * Start a non-TLS socket server.<p>
	 * 
	 * This constructor will start the KSocketServerListener thread to wait for all incoming client connections. For each successful client connect,
	 * the passed user class will be instantiated as a separate thread to handle the client connection.<br> 
	 * 
	 * <p>Example:<br>
	 * <pre>
	 * KSocketServer server = new KSocketServer(9999, KSocketServerThreadSample.class);
	 * 
	 * KLog.abort(!server.isActive(), "Server could not be started - {}", server.getLastError());
	 * 
	 * ...
	 * 
	 * server.close();
	 * </pre>
	 * 
	 * @see isActive
	 * @see getLastError
	 * @see close
	 * 
	 * @param	argLocalPort	Local host port
	 * @param	argClass		User class handling the client connections (subclass of KSocketServerThread)
	 */
	public KSocketServer(int argLocalPort, Class<?> argClass) {
		this(argLocalPort, argClass.getName(), false, null, null, null, null);
	}
	
	/**
	 * Start a TLS-secured socket server.<p>
	 * 
	 * This constructor will start the KSocketServerListener thread to wait for all incoming client connections. For each successful client connect,
	 * the passed user class will be instantiated as a separate thread to handle the client connection.<br> 
	 * 
	 * <p>Example:<br>
	 * <pre>
	 * KSocketServer server = new KSocketServer(9999, KSocketServerThreadSample.class, "keyfile.jks", "Pa$$w0rd".toBytes());
	 * KLog.abort(!server.isActive(), "Server could not be started - {}", server.getLastError());
	 * 
	 * ...
	 * 
	 * server.close();
	 * </pre>
	 * 
	 * @see isActive
	 * @see getLastError
	 * @see close
	 * 
	 * @param	argLocalPort			Local host port
	 * @param	argClass				User class handling the client socket request (must be subclass of KSocketServerThread)
	 * @param	argKeyStoreFileName		Key store file name to be loaded
	 * @param	argKeyStorePassword		Key store password
	 */
	public KSocketServer(int argLocalPort, Class<?> argClass, String argKeyStoreFileName, char[] argKeyStorePassword) {
		this(argLocalPort, argClass.getName(), true, argKeyStoreFileName, argKeyStorePassword, null, null);
	}
	
	/**
	 * Start a TLS-secured socket server.<p>
	 * 
	 * This constructor will start the KSocketServerListener thread to wait for all incoming client connections. For each successful client connect,
	 * the passed user class will be instantiated as a separate thread to handle the client connection.<br> 
	 * 
	 * <p>Example:<br>
	 * <pre>
	 * KSocketServer server = new KSocketServer(9999, KSocketServerThreadSample.class, "keystore.jks", "Pa$$w0rd".toBytes(), "truststore.jks", "Pa$$w0rd".toBytes());
	 * 
	 * if (!server.isActive()) {
	 *    KLog.error("Server could not be started - {}", server.getLastError());
	 *    ...
	 * }
	 * ...
	 * server.close();
	 * 
	 * </pre>
	 * 
	 * @see isActive
	 * @see getLastError
	 * @see close
	 * 
	 * @param	argLocalPort			Local host port
	 * @param	argClass				User class handling the client socket request (must be subclass of KSocketServerThread)
	 * @param	argKeyStoreFileName		Key store file name to be loaded
	 * @param	argKeyStorePassword		Key store password
	 * @param	argTrustStoreFileName	Trust store file name to be loaded or null for non-TLS
	 * @param	argTrustStorePassword	Trust store password or null
	 */
	public KSocketServer(int argLocalPort, Class<?> argClass, String argKeyStoreFileName, char[] argKeyStorePassword, String argTrustStoreFileName, char[] argTrustStorePassword) {
		this(argLocalPort, argClass.getName(), true, argKeyStoreFileName, argKeyStorePassword, argTrustStoreFileName, argTrustStorePassword);
	}
	
	/**
	 * Start a TLS-secured or non-secured socket server.<p>
	 * 
	 * This constructor will start the KSocketServerListener thread to wait for all incoming client connections. For each successful client connect,
	 * the passed user class will be instantiated as a separate thread to handle the client connection.<br> 
	 * 
	 * <p>Example:<br>
	 * <pre>
	 * KSocketServer server = new KSocketServer(9999, "ch.k43.util.KSocketServerThreadSample", true, "keystore.jks", "Pa$$w0rd".toBytes(), "truststore.jks", "Pa$$w0rd".toBytes());
	 * 
	 * if (!server.isActive()) {
	 *    KLog.error("Server could not be started - {}", server.getLastError());
	 *    ...
	 * }
	 * ...
	 * server.close();
	 * 
	 * </pre>
	 * 
	 * @see isActive
	 * @see getLastError
	 * @see close
	 * 
	 * @param	argLocalPort			Local host port
	 * @param	argClassName			User class name handling the client socket requests (must be subclass of KSocketServerThread)
	 * @param	argTLS					True for TLS-secured socket, false for non-secured socket
	 * @param	argKeyStoreFileName		Key store file name to be loaded (TLS requirement) or null for non-TLS
	 * @param	argKeyStorePassword		Key store password or null
	 * @param	argTrustStoreFileName	Trust store file name to be loaded or null for non-TLS
	 * @param	argTrustStorePassword	Trust store password or null
	 */
	public KSocketServer(int argLocalPort, String argClassName, boolean argTLS, String argKeyStoreFileName, char[] argKeyStorePassword, String argTrustStoreFileName, char[] argTrustStorePassword) {

		// Check arguments
		KLog.argException(argLocalPort < 1 || argLocalPort > 65535, "KSocketServer(): Port number must be between 1 and 65535");
		KLog.argException(argTLS && (K.isEmpty(argKeyStoreFileName)), "KSocketServer(): Key store file name is required for TLS connections");		
		
		// Setup and start the socket server
		try {

			KLog.debug("Starting socket server on port {} ({}TLS)", argLocalPort, (!argTLS ? "Non-" : ""));
			
			if (argTLS) {
				//
				// TLS socket server
				//
				KLog.debug("Loading key store file {}", argKeyStoreFileName);
				KeyStore keyStore = KeyStore.getInstance("JKS");
				keyStore.load(new FileInputStream(argKeyStoreFileName), argKeyStorePassword);

				logKeyStore(keyStore);
	
				// Set key store from JKS file
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, argKeyStorePassword);
		            
				// Set trust store from JKS file
				TrustManagerFactory trustManagerFactory = null;

				if (!K.isEmpty(argTrustStoreFileName)) {

					KLog.debug("Loading trust store file {}", argTrustStoreFileName);
					KeyStore trustStore = KeyStore.getInstance("JKS");
					trustStore.load(new FileInputStream(argTrustStoreFileName), argTrustStorePassword);
					
					// Set key store from JKS file
					keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					keyManagerFactory.init(keyStore, argKeyStorePassword);

					trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					trustManagerFactory.init(trustStore);
					
				} else {
					trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					trustManagerFactory.init(keyStore);
				}
				
				// Setup TLS context
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
				KLog.debug("Key manager and TLS context initialized");

				SSLServerSocket sslServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(argLocalPort);

				// Ask for optional client certificate during TLS handshake 
				sslServerSocket.setWantClientAuth(true);
				
				// Save socket
				gServerSocket = sslServerSocket;
				
			} else {
				//
				// Non-TLS socket server
				//
				gServerSocket = ServerSocketFactory.getDefault().createServerSocket(argLocalPort);
			}
			
		} catch (Exception e) {
			
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			
			if (gServerSocket != null) {
				try {
					gServerSocket.close();
				} catch (Exception ee) {
					KLog.error(ee.toString());
				}
				gServerSocket = null;
			}
			
			// Terminate constructor
			return;
		}
		
		//
		// Create and start connection listener thread
		//
		gListenerThread = new KSocketServerListener(gServerSocket, argClassName);
		gListenerThread.start();
	}
	
	/**
	 * Stop the socket server by terminating the KSocketServerListener thread. During shutdown, KSocketServerListener will itself terminate
	 * all client connections.<br>
	 * 
	 * @see KSocketServerListener    
	 * @see KSocketServerThread
	 */
	public synchronized void close() {

		// Close socket
		KLog.debug("Terminating socket server");
		
		try {
			if (gServerSocket != null) {
				gServerSocket.close();
				gServerSocket = null;
				
				// Give the KSocketServerListener some time to cleanup the KSocketServerThread client threads 
				K.waitMilliseconds(250);
			}
		} catch (IOException e) {
			KLog.error(e.toString());
		}
		
		// Terminate the listener thread
		K.stopThread(gListenerThread);
		gListenerThread = null;
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
	 * Return current status of the socket server.<br>
	 * 
	 * @return	True if server is active, false otherwise
	 */
	public boolean isActive() {
	
		// Check if listener thread is active and running
		if ((gListenerThread == null) || (!gListenerThread.isAlive())) {
			return (false);
		}

		// Check if server socket is active
		if ((gServerSocket == null) || (gServerSocket.isClosed())) {
			return (false);
		}

		return (true);
	}
	
	/**
	 * Log certificates from key store
	 * 
	 * @param argKeyStore	KeyStore to be logged
	 */
	private void logKeyStore(KeyStore argKeyStore) {
		
		// Check if logging is active
		if (!KLog.isActive()) {
			return;
		}
        
        // Log the key store content
        try {

        	Enumeration<String> keyAliases = argKeyStore.aliases();
        	
            while (keyAliases.hasMoreElements()) {

            	X509Certificate x509Certificate = (X509Certificate) argKeyStore.getCertificate(keyAliases.nextElement());
            	KLog.debug("Key store certificate {}", x509Certificate.getSubjectX500Principal().getName().split(",")[0].substring(3));          	

            	// Log key entry aliases
            	Collection<List<?>> certAliases = x509Certificate.getSubjectAlternativeNames();
                if (certAliases != null) {
                    for (List<?> certAlias : certAliases) {
                    	KLog.debug("Key store alias {}", certAlias.get(1));
                    }
                }
            }

        } catch (Exception e) {
        	KLog.error(e.toString());
        }
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSocketServer [gServerSocket=" + gServerSocket + ", gListenerThread=" + gListenerThread
				+ ", gLastErrorMessage=" + gLastErrorMessage + "]";
	}
}
