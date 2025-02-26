package ch.k43.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Supported TCP socket connections to non-TLS, TLS and TLS with client authentication.<p>
 * 
 * Notes:<br>
 * - The remote host certificate must be present in the JVM trusted store to be authenticated.<br>
 * - If client authentication is required by the host, the JKS file with the client certificate must be accessible.<br>
 */
public class KSocketClient implements AutoCloseable{

	// Class variables
	private SSLSocket		gTLSSocket				= null;
	private Socket			gNoTLSSocket			= null;
	private BufferedReader	gBufferedReader			= null;
	private BufferedWriter	gBufferedWriter			= null;
	private String			gUsedProtocol			= null;
	private String			gUsedCiphers			= null;
	private String			gAuthenticatedClient	= null;
	private String			gLastErrorMessage		= null;
	
	/**
	 * Class constructor to open TLS-secured socket to remote host.
	 *  
	 * @param argHostName	Remote host name
	 * @param argHostPort	Remote host port
	 */
	public KSocketClient(String argHostName, int argHostPort) {
		this(argHostName, argHostPort, true, null, null);
	}
	
	/**
	 * Class constructor to open TLS-secured or non-secured socket to remote host.
	 *  
	 * @param	argHostName				Remote host name
	 * @param	argHostPort				Remote host port
	 * @param	argTLS					True for TLS-secured socket, false for non-secured socket
	 */
	public KSocketClient(String argHostName, int argHostPort, boolean argTLS) {
		this(argHostName, argHostPort, argTLS, null, null);
	}
	
	/**
	 * Class constructor to open TLS-secured or non-secured socket to remote host with client certificate.<p>
	 * 
	 * Note:<br>
	 * - Use KClientSocket.isConnected() to see if the connection was established<br>
	 * - For TLS client authentication, use the key store file name and password (JKS file)<br>
	 *  
	 * @param	argHostName				Remote host name
	 * @param	argHostPort				Remote host port
	 * @param	argTLS					True for TLS-secured socket, false for non-secured socket
	 * @param	argKeyStoreFileName		Key store file name to be loaded or null
	 * @param	argKeyStorePassword		Key store file password or null
	 */
	public KSocketClient(String argHostName, int argHostPort, boolean argTLS, String argKeyStoreFileName, char[] argKeyStorePassword) {

		// Check arguments
		KLog.argException(K.isEmpty(argHostName), "KSocketClient(): Host name is required");
		KLog.argException(((argHostPort < 1) || (argHostPort > 65535)), "KSocketClient(): Host port must be between 1 and 65535");
		
		KLog.debug("Connecting to {}:{} ({}TLS)", argHostName, argHostPort, (!argTLS ? "non-" : ""));

		try {

			if (argTLS) {
				//
				// TLS socket connection
				//
				
				// Create key store
				KeyStore			keyStore			= null;
				KeyManagerFactory	keyManagerFactory	= null;
				SSLContext			sslContext			= null;

				if (!K.isEmpty(argKeyStoreFileName)) {
					//
					// TLS socket connection with client authentication
					//
					keyStore = KeyStore.getInstance("JKS");

					KLog.debug("Loading key store file {}", argKeyStoreFileName);
					keyStore.load(new FileInputStream(argKeyStoreFileName), argKeyStorePassword);
					
					logKeyStore(keyStore);
					
					keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					keyManagerFactory.init(keyStore, argKeyStorePassword);
					
					sslContext = SSLContext.getInstance("TLS");
					sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
					KLog.debug("Key manager and TSL context initialized");
					
					gTLSSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(argHostName, argHostPort);
					
				} else {
					//
					// TLS socket connection without client authentication
					//
					gTLSSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(argHostName, argHostPort);
				}

				// Force TLS handshake
				gTLSSocket.startHandshake();

				// Save used TLS protocol and ciphers
				SSLSession sslSession = gTLSSocket.getSession();
				gUsedProtocol = sslSession.getProtocol();
	            gUsedCiphers = sslSession.getCipherSuite();
	            KLog.debug("Protocol/cipher used {}/{}", gUsedProtocol, gUsedCiphers);
	            
	    		// Try to get name of authenticated client
	    		try {
	    			gAuthenticatedClient = sslSession.getLocalPrincipal().getName();
	            	KLog.debug("Client authenticated as {}", getAuthenticatedClientCN());
	    		} catch (Exception e1) {
	            	KLog.debug("No client authentication used");
	    			// Exception expected if no client authentication used
	    		}
	            
	            // Establish input/output streams
				gBufferedReader	= new BufferedReader(new InputStreamReader(gTLSSocket.getInputStream()));
				gBufferedWriter	= new BufferedWriter(new OutputStreamWriter(gTLSSocket.getOutputStream()));
				
			} else {
				//
				// Non-TLS socket connection
				//
				gNoTLSSocket = new Socket(argHostName, argHostPort);

	            // Establish input/output streams
				gBufferedReader	= new BufferedReader(new InputStreamReader(gNoTLSSocket.getInputStream()));
				gBufferedWriter	= new BufferedWriter(new OutputStreamWriter(gNoTLSSocket.getOutputStream()));
			}

            KLog.debug("Connected to {}:{} ({}TLS)", argHostName, argHostPort, (!argTLS ? "non-" : ""));

		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
		}
	}
	
	/**
	 * Class constructor to open TLS-secured socket with client authentication to remote host.
	 *  
	 * @param	argHostName				Remote host name
	 * @param	argHostPort				Remote host port
	 * @param	argKeyFileName			Key store file name to be loaded or null
	 * @param	argKeyFilePassword		Key store file password or null
	 */
	public KSocketClient(String argHostName, int argHostPort, String argKeyFileName, char[] argKeyFilePassword) {
		this(argHostName, argHostPort, true, argKeyFileName, argKeyFilePassword);
	}
	
	/**
	 * Close the socket connection.
	 */
	public void close() {
		
		// Close reader and writer
		try {
			if (gBufferedReader != null) {
				gBufferedReader.close();
				gBufferedReader = null;
				KLog.debug("Socket stream reader closed");
			}
		}  catch (Exception e) {
			KLog.error(e);
		}

		try {
			if (gBufferedWriter != null) {
				gBufferedWriter.close();
				gBufferedWriter = null;
				KLog.debug("Socket stream writer closed");
			}
		} catch (Exception e) {
			KLog.error(e);
		}
		
		// Close sockets
		try {
			if (gTLSSocket != null) {
				gTLSSocket.close();
				gTLSSocket = null;
				KLog.debug("TLS socket closed");
			}
		} catch (Exception e) {
			KLog.error(e);
		}

		try {
			if (gNoTLSSocket != null) {
				gNoTLSSocket.close();
				gNoTLSSocket = null;
				KLog.debug("Non-TLS socket closed");
			}
		}  catch (Exception e) {
			KLog.error(e);
		}
		
		// Reset state
		gUsedCiphers			= null;
		gUsedProtocol		= null;
		gLastErrorMessage	= null;
	}

	/**
	 * Flush the output data.<br>
	 * 
	 * @return boolean	True if successful, false otherwise
	 */
	public boolean flush() {
		
		// Clear error message
		gLastErrorMessage = null;

		try {
			gBufferedWriter.flush();
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(e);
			return (false);
		}
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
		
		if (K.isEmpty(gAuthenticatedClient)) {
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
	 * Return last error
	 * 
	 * @return	String		Error message or null
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
	 * Return connection state
	 * 
	 * @return	boolean		True if connected, false otherwise
	 */
	public boolean isConnected() {
		
		// Check if TLS socket is connected
		if ((gTLSSocket != null) && (!gTLSSocket.isClosed())) {
			return (true);
		}

		// Check if non-TLS socket is connected
		if ((gNoTLSSocket != null) && (!gNoTLSSocket.isClosed())) {
			return (true);
		}
		
		// Return not-connected state
		return (false);
	}
	
	/**
	 * Check if data is available
	 * 
	 * @return	boolean		True if data is available, false otherwise
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
			KLog.error(e);
		}
		
		return (false);
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
	 * Read socket into character array
	 * 
	 * @param	argData	Character buffer
	 * @return	int		Number of bytes read or -1
	 */
	public int read(char[] argData) {
	
		// Declarations
		int bytesRead = 0;

		// Clear error message
		gLastErrorMessage = null;
		
		try {
			bytesRead = gBufferedReader.read(argData, 0, argData.length);
			
			if (bytesRead != -1) {
				KLog.debug("Character array data received ({})", K.formatBytes(bytesRead));
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
	 * Read line from socket
	 * 
	 * @return String	String read or null for end-of-data
	 */
	public String readLine() {

		// Declarations
		String lineRead = null;

		// Clear error message
		gLastErrorMessage = null;
		
		try {
			lineRead = gBufferedReader.readLine();
			
			if (lineRead != null) {
				KLog.debug("String data received ({} characters)", lineRead.length());
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
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSocketClient [gTLSSocket=" + gTLSSocket + ", gNoTLSSocket=" + gNoTLSSocket + ", gBufferedReader="
				+ gBufferedReader + ", gBufferedWriter=" + gBufferedWriter + ", gUsedProtocol=" + gUsedProtocol
				+ ", gUsedCiphers=" + gUsedCiphers + ", gAuthenticatedClient=" + gAuthenticatedClient
				+ ", gLastErrorMessage=" + gLastErrorMessage + "]";
	}
	
	/**
	 * Write byte array to socket
	 * 
	 * @param argData	Byte array to be written
	 * @return boolean	True if successful, false otherwise
	 */
	public boolean write(byte[] argData) {
		
		// Clear error message
		gLastErrorMessage = null;

		try {
			gBufferedWriter.write(new String(argData, StandardCharsets.UTF_8).toCharArray());
			gBufferedWriter.flush();
			KLog.debug("Byte array data sent ({})", K.formatBytes(argData.length));
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (false);
		}
	}

	/**
	 * Write character array to socket
	 * 
	 * @param argData	Character array to be written
	 * @return boolean	True if successful, false otherwise
	 */
	public boolean write(char[] argData) {
		
		// Clear error message
		gLastErrorMessage = null;

		try {
			gBufferedWriter.write(argData);
			gBufferedWriter.flush();
			KLog.debug("Character array data sent ({} characters)", argData.length);
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (false);
		}
	}

	/**
	 * Write string to socket
	 * 
	 * @param argLine	String to be written
	 * @return boolean	True if successful, false otherwise
	 */
	public boolean write(String argLine) {
		
		// Clear error message
		gLastErrorMessage = null;

		try {
			gBufferedWriter.write(argLine);
			gBufferedWriter.flush();
			KLog.debug("String data sent ({} characters)", argLine.length());
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(gLastErrorMessage);
			return (false);
		}
	}
}
