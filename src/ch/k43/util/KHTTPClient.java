package ch.k43.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

/**
 * Support basic HTTP/HTTPS client transactions.<p>
 * 
 * Note: The request headers "Date", "User-Agent", "Host" and "Content-Length" are automatically added.<br>
 * 
 * <pre>
 * Example:
 * 
 * KHTTPClient http = new KHTTPClient();
 *	      
 * if (!http.get("https://reqbin.com/echo/get/json")) {
 *    KLog.error("Error: {}", http.getLastError());
 * } else {
 *    System.out.println(http.getResponseDataAsString());
 * }
 * </pre>
 */
public class KHTTPClient {

	// Class variables
	Properties	gHTTPResponseHeaders	= null;
	byte[]		gHTTPResponseData		= null;
	String		gHTTPErrorMessage		= null;
	int			gHTTPResponseTimeMs		= 0;
	int			gHTTPResponseCode		= -1;
	int			gHTTPTimeOutSec			= 5;
	
	/**
	 * Class constructor.
	 */
	public KHTTPClient() {
		// No object initialization necessary
	}
	
	/**
	 * Execute HTTP DELETE request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @return	True (if HTTP return code 200), else false
	 * 
	 * @since 2024.05.22
	 */
	public boolean delete(String argURL, Properties argHeaders) {
		return (xmit("DELETE", argURL, argHeaders, null));
	}
	
	/**
	 * Execute HTTP GET request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @return	True if HTTP return code is 200, false otherwise
	 */
	public boolean get(String argURL) {
		return (xmit("GET", argURL, null, null));
	}
	
	/**
	 * Execute HTTP GET request and set HTTP request headers.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @return	True if HTTP return code is 200 else false
	 */
	public boolean get(String argURL, Properties argHeaders) {
		return (xmit("GET", argURL, argHeaders, null));
	}
	
	/**
	 * Return last error message.<br>
	 * 
	 * @return	Last error message or null
	 */
	public String getLastError() {
		return (gHTTPErrorMessage);
	}
	
	/**
	 * Return last HTTP response code.<br>
	 * 
	 * @return	Last HTTP response code
	 */
	public int getResponseCode() {
			return (gHTTPResponseCode);
	}
	
	/**
	 * Return HTTP response data.<br>
	 * 
	 * @return	byte[]	HTTP response data
	 */
	public byte[] getResponseDataAsBytes() {
		return (gHTTPResponseData);
	}
	
	/**
	 * Return HTTP response data.<br>
	 * 
	 * @return	HTTP response data or null
	 */
	public String getResponseDataAsString() {
		
		if (gHTTPResponseData == null) {
			return (null);
		} else {
			return (new String(gHTTPResponseData));
		}
	}
	
	/**
	 * Return last HTTP response headers.<br>
	 * 
	 * @return	HTTP response headers
	 */
	public Properties getResponseHeaders() {
		return (gHTTPResponseHeaders);
	}
	
	/**
	 * Get response time of last HTTP request in milliseconds.<br>
	 *  
	 * @return	HTTP response time in milliseconds
	 */
	public int getResponseTimeMs() {
		return (gHTTPResponseTimeMs);
	}
	
	/**
	 * Execute HTTP HEAD request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @return	True (if HTTP return code 200), else false
	 */
	public boolean head(String argURL, Properties argHeaders) {
		return (xmit("HEAD", argURL, argHeaders, null));
	}
	
	/**
	 * Execute HTTP OPTIONS request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @return	True (if HTTP return code 200), else false
	 * 
	 * @since 2024.05.22
	 */
	public boolean options(String argURL, Properties argHeaders) {
		return (xmit("OPTIONS", argURL, argHeaders, null));
	}
	
	/**
	 * Execute HTTP PATCH request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload	Data to be sent with PATCH request
	 * @return	True (if HTTP return code 200), else false
	 * 
 	 * @since 2024.05.22
	 */
	public boolean patch(String argURL, Properties argHeaders, byte[] argPayload) {
		return (xmit("PATCH", argURL, argHeaders, argPayload));
	}
	
	/**
	 * Execute HTTP PATCH request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload		Data to be sent with PATCH request
	 * @return	True (if HTTP return code 200), else false
	 * 
 	 * @since 2024.05.22
	 */
	public boolean patch(String argURL, Properties argHeaders, String argPayload) {
		return (xmit("PATCH", argURL, argHeaders, argPayload.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * Execute HTTP POST request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload	Data to be sent with POST request
	 * @return	True (if HTTP return code 200), else false
	 */
	public boolean post(String argURL, Properties argHeaders, byte[] argPayload) {
		return (xmit("POST", argURL, argHeaders, argPayload));
	}
	
	/**
	 * Execute HTTP POST request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload		Data to be sent with POST request
	 * @return	True (if HTTP return code 200), else false
	 */
	public boolean post(String argURL, Properties argHeaders, String argPayload) {
		return (xmit("POST", argURL, argHeaders, argPayload.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * Execute HTTP PUT request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload	Data to be sent with PUT request
	 * @return	True (if HTTP return code 200), else false
	 * 
 	 * @since 2024.05.22
	 */
	public boolean put(String argURL, Properties argHeaders, byte[] argPayload) {
		return (xmit("PUT", argURL, argHeaders, argPayload));
	}
	
	/**
	 * Execute HTTP PUT request.<br>
	 * 
	 * @param 	argURL		URL to connect
	 * @param 	argHeaders	Additional HTTP request headers or null
	 * @param	argPayload		Data to be sent with PUT request
	 * @return	True (if HTTP return code 200), else false
	 * 
 	 * @since 2024.05.22
	 */
	public boolean put(String argURL, Properties argHeaders, String argPayload) {
		return (xmit("PUT", argURL, argHeaders, argPayload.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * Set timeout for connect and read requests.
	 * 
	 * @param argTimeOutSec	Number of seconds for timeout (0 = indefinite timeout)
	 * 
	 * @since 2025.01.24
	 */
	public void setTimeOutSec(int argTimeOutSec) {
		
		// Check arguments
		KLog.argException(argTimeOutSec < 0, "KHTTPClient.setTimeOutSec(): argTimeOutSec must not be negative");
		
		gHTTPTimeOutSec = argTimeOutSec;
	}
		
	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KHTTPClient [gHTTPResponseHeaders=" + gHTTPResponseHeaders + ", gHTTPResponseData="
				+ Arrays.toString(gHTTPResponseData) + ", gHTTPErrorMessage=" + gHTTPErrorMessage
				+ ", gHTTPResponseTimeMs=" + gHTTPResponseTimeMs + ", gHTTPResponseCode=" + gHTTPResponseCode
				+ ", gHTTPTimeOutSec=" + gHTTPTimeOutSec + "]";
	}

	/**
	 * Execute HTTP request.<br>
	 * 
	 * @param 	argHTTPMethod	HTTP method (GET, POST, etc)
	 * @param 	argURL			URL to connect
	 * @param 	argHeaders		Additional HTTP request headers or null
	 * @param	argPayload		Data to be sent with POST request
	 * @return	True (if HTTP return code 200), else false
	 */
	private boolean xmit(String argHTTPMethod, String argURL, Properties argHeaders, byte[] argPayload) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argURL),  "KHTTPClient.xmit(): URL is required");
		
		// Declarations
		HttpURLConnection 	connection		= null;
		Properties			httpHeaders		= null;
		KTimer 				timer			= null;
		
		// Initialize variables
		gHTTPResponseHeaders	= null;
		gHTTPResponseData		= null;
		gHTTPErrorMessage		= null;
		gHTTPResponseTimeMs		= 0;
		gHTTPResponseCode		= -1;
		
		try {

			//
			// Open connection to target system
			//
			
			// Start response timer
			timer = new KTimer();

			// Open connection
			KLog.debug("HTTP URL: {}", argURL);

			@SuppressWarnings("deprecation")
			URL url = new URL(argURL);
			connection = (HttpURLConnection) url.openConnection();
			
			//
			// Set connection attributes
			//

			// Set HTTP method (GET, POST, etc.)
			KLog.debug("HTTP method: {}", argHTTPMethod);
			connection.setRequestMethod(argHTTPMethod.toUpperCase());

			// Don't use any caching data
			connection.setDefaultUseCaches(false);
			connection.setUseCaches(false);
			
			// Set connect and read timeouts
			connection.setConnectTimeout(gHTTPTimeOutSec * 1_000);
			connection.setReadTimeout(gHTTPTimeOutSec * 1_000);
			KLog.debug("HTTP timeout: {} sec", gHTTPTimeOutSec);
			
			// Allow output
			if (!K.isEmpty(argPayload)) {
				connection.setDoOutput(true);
			}
			
			//
			// Set HTTP Request Headers
			//
						
			// Use passed properties or assign new property
			if (K.isEmpty(argHeaders)) {
				httpHeaders = new Properties();
			} else {
				httpHeaders = argHeaders;
			}
			
			// Add HTTP request header ("Host: localhost-name")
			httpHeaders.put("Host", K.getLocalHostName());
			
			// Add HTTP request header ("Date: Current-Date")
		    Calendar calendar = Calendar.getInstance();
		    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		    httpHeaders.put("Date", dateFormat.format(calendar.getTime()));
			
		    // Add HTTP request header (User-Agent: Java-Class-Name/Version")
		    httpHeaders.put("User-Agent", this.getClass().getName() + '/' + K.VERSION);
			
		    // Add HTTP request header ("Content-Length: length") 
			if (!K.isEmpty(argPayload)) {
				httpHeaders.put("Content-Length", String.valueOf(argPayload.length));
			} else {
				httpHeaders.put("Content-Length", "0");
			}
		    			
			// Set and log HTTP headers 
			Enumeration<?> properties = httpHeaders.propertyNames();

			String propertyKey		= null;
			String propertyValue	= null;
			
		    while (properties.hasMoreElements()) {
		      propertyKey = (String) properties.nextElement();
		      propertyValue = httpHeaders.getProperty(propertyKey);
		      connection.setRequestProperty(propertyKey, propertyValue);
		      KLog.debug("HTTP request header: {}: {}", propertyKey, propertyValue);
			}
		    
		    //
			// Send HTTP request data
		    //
		    
		    // Send data stream
			if (!K.isEmpty(argPayload)) {

				OutputStream outStream = connection.getOutputStream();
				outStream.write(argPayload);
				outStream.flush();
				outStream.close();
				
				KLog.debug("HTTP data sent ({})", K.formatBytes(argPayload.length));
			}
		    		    
			// Force (explicit) connect (should not be necessary)
			connection.connect();
			KLog.debug("HTTP connection successful");
			
			// Log proxy usage
			if (connection.usingProxy()) {
				KLog.debug("HTTP connection: Indirect (Proxy)");
			} else {
				KLog.debug("HTTP connection: Direct (No proxy)");
			}
			
			// Log connection
			if (url.getProtocol().equalsIgnoreCase("HTTPS")) {
				
				KLog.debug("HTTP connection: Secured (TLS)");
				KLog.debug("HTTP ciphers used: {}", ((HttpsURLConnection) connection).getCipherSuite());
				
				Certificate[] serverCertificates = ((HttpsURLConnection) connection).getServerCertificates();
				for (Certificate certificate : serverCertificates) {
					X509Certificate x509Certificate = (X509Certificate) certificate;
					KLog.debug("HTTP peer certificates: {}", x509Certificate.getSubjectX500Principal());
				}
			} else {
				KLog.debug("HTTP connection: Not secured (non-TLS)");
			}
			
			// Save HTTP response code
			gHTTPResponseCode = connection.getResponseCode();
			KLog.debug("HTTP response code: {}", gHTTPResponseCode);
			
			//
			// Save HTTP response headers
			//
			
			// Create properties with HTTP response headers
			gHTTPResponseHeaders = new Properties();
						
			Map<String, ?> headers = connection.getHeaderFields();
			Set<String> headerKeys = headers.keySet();

			for (String headerKey : headerKeys) {
				// Save single header (e.g. "HTTP/1.1 200 OK") oder paired ("Content-Length: 123") header 
				if (headerKey == null) {
					KLog.debug("HTTP response header: {}", connection.getHeaderField(headerKey) );
					gHTTPResponseHeaders.put(headerKeys, "");
				} else {
					KLog.debug("HTTP response header: {}: {}", headerKey, connection.getHeaderField(headerKey));
					gHTTPResponseHeaders.put(headerKey, connection.getHeaderField(headerKey));
				}
			}

			//
			// Read all HTTP data returned
			//

			// Get error data or normal data
			InputStream inputStream = null;
		
			if (connection.getErrorStream() != null) {
				inputStream = connection.getErrorStream(); 
			} else {
				if (connection.getInputStream() != null) {
					inputStream = connection.getInputStream();
				}
			}

			// Save the data
			if (inputStream != null) {

				ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

			    int		readCount	= 0;
			    byte[]	data		= new byte[4096];

			    while ((readCount = inputStream.read(data, 0, data.length)) != -1) {
			        dataBuffer.write(data, 0, readCount);
			    }

			    dataBuffer.flush();
			    inputStream.close();

			    gHTTPResponseData = dataBuffer.toByteArray();
				KLog.debug("HTTP data read ({})", K.formatBytes(gHTTPResponseData.length));
				
				if (gHTTPResponseData.length == 0) {
					gHTTPResponseData = null;
				}
			}
			
			// Close the connection
			connection.disconnect();
			
			// Save response time
			gHTTPResponseTimeMs = (int) timer.getElapsedMilliseconds();
			KLog.debug("HTTP response time: {} ms", gHTTPResponseTimeMs);
			
			// Write error if HEAD request returned data
			if ((argHTTPMethod.equalsIgnoreCase("HEAD")) && (gHTTPResponseCode == 200) && (gHTTPResponseData != null)) {
				KLog.error("HTTP HEAD request should not return any data");
			}
			
		} catch (Exception e) {
			gHTTPErrorMessage = "HTTP transation failed: " + e.toString();
			KLog.error(gHTTPErrorMessage);
			return (false);
		}

		// Return true if normal HTTP response code or false otherwise
		if ((gHTTPResponseCode >= 200 && gHTTPResponseCode <= 299)) {
			return (true);
		} else {
			gHTTPErrorMessage = "HTTP response code " + gHTTPResponseCode;
			return (false);
		}
	}
}
