package ch.k43.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Class to handle user HTTP connections accepted by KSocketServerListener. The user class needs to override the default implementations
 * for the HTTP methods get(), put(), etc.
 * 
 * The HTTP response headers Date, Server, Content-Type, Content-Length and Connection are automatically provided unless overridden
 * thru setResponseHeaders().
 *
 * @since 2025.04.13
 */
public abstract class KHTTPServerThread extends KSocketServerThread {

	// Class variables
	private	static final	int			BUFFER_SIZE			= 4_096;	
	
	private 				Properties	gResponseHeaders	= null;
	private					Properties	gRequestHeaders		= null;
	private					byte[]		gPayloadData		= null;
	private					int			gMaxPayloadSize		= (int) (50 * K.SIZE_MIB);
	
	/**
	 * Thread constructor.
	 * 
	 * @param argSocket		Socket passed by the KSocketServerListener during thread initialization
	 */
	protected KHTTPServerThread(Socket argSocket) {
		super(argSocket);
	}

	/**
	 * HTTP DELETE method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void delete(String argURL, byte[] argPayload) {
		sendText(400, "HTTP DELETE method not implemented");
	}
	
	/**
	 * HTTP GET method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 */
	public void get(String argURL) {
		sendText(400, "HTTP GET method not implemented");
	}
	
	/**
	 * Get maximum size of payload to be read.
	 * 
	 * @return	Size of maximum payload
	 */
	public int getMaxPayloadSize() {
		return gMaxPayloadSize;
	}
	
	/**
	 * Get HTTP request headers.
	 * 
	 * @return	HTTP request headers
	 */
	public Properties getRequestHeaders() {
		return gRequestHeaders;
	}
	
	/**
	 * HTTP HEAD method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 */
	public void head(String argURL) {
		sendText(400, "HTTP HEAD method not implemented");
	}
	
	/**
	 * HTTP OPTIONS method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void options(String argURL, byte[] argPayload) {
		sendText(400, "HTTP OPTIONS method not implemented");
	}
	
	/**
	 * HTTP CONNECT method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void connect(String argURL, byte[] argPayload) {
		sendText(400, "HTTP CONNECT method not implemented");
	}
		
	/**
	 * HTTP PATCH method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void patch(String argURL, byte[] argPayload) {
		sendText(400, "HTTP PATCH method not implemented");
	}
	
	/**
	 * HTTP POST method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void post(String argURL, byte[] argPayload) {
		sendText(400, "HTTP POST method not implemented");
	}
	
	/**
	 * HTTP PUT method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void put(String argURL, byte[] argPayload) {
		sendText(400, "HTTP PUT method not implemented");
	}
	
	/**
	 * Read HTTP payload into memory.
	 * 
	 * @return	True if success, false otherwise
	 */
	private boolean readPayload() {
		
		try {

			ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

		    char[]	dataBlock	= new char[BUFFER_SIZE];
		    int		dataSize	= 0;
		    int		streamSize	= 0;
		    
		    while (isDataAvailable()) {
				
		    	dataSize = read(dataBlock);
		    	
		    	if (dataSize == -1) {
		    		return false;
		    	}
		    	
		    	if (dataSize == 0) {
		    		break;
		    	}
		    	
		    	streamSize += dataSize;
		    	
		       	if (streamSize > gMaxPayloadSize) {
		    		sendText(413, "HTTP payload exceeds {} bytes", gMaxPayloadSize);
		    		return false;
		    	}
		        dataBuffer.write(new String(dataBlock, 0, dataSize).getBytes(StandardCharsets.UTF_8));
		    }
		    
		    dataBuffer.flush();
		    gPayloadData = dataBuffer.toByteArray();
		    return true;
		    
		} catch (Exception e) {
			KLog.error("Unable to read HTTP payload: {}", e.toString());
			return false;
		}
	}
	
	/**
	 * Thread main entry point (called by KSocketServerListener).
	 */
	@Override
	public void run() {

		// Log program start
		KLog.debug("HTTP server thread started");

		//
		// Wait for client data and process request
		//
		while (true) {
		
			//
			// Get first line which must be a HTTP request (GET, PUT, etc)
			//
			String inputLine = readLine();
			
			KTimer timer = new KTimer();
			
			if (K.isEmpty(inputLine)) {
				break;
			}
			
			String[]	httpRequest		= inputLine.split(" ");
			String		httpMethod		= null;
			String		httpParameter	= null;
			int			contentLength	= -1;
			
			if (httpRequest.length < 2) {
				break;
			}
			
			httpMethod		= httpRequest[0];
			httpParameter	= httpRequest[1];
			
			// Remove starting character '/'
			if (httpParameter.startsWith("/")) {
				httpParameter = httpParameter.length() == 1 ? "" : httpParameter.substring(1);
			}
			
			//
			// Get all HTTP request headers and save them in a Property object
			//
			String[] requestHeader	= null;
			gRequestHeaders			= new Properties();
			
			while (true) {
				
				inputLine = readLine();
				
				if (K.isEmpty(inputLine)) {
					break;
				}
				
				requestHeader = inputLine.split(":", 2);
				
				if (requestHeader.length == 2) {
					
					String headerKey	= requestHeader[0].trim();
					String headerValue	= requestHeader[1].trim();
					
					// Save Content-Length value
					if (headerKey.equalsIgnoreCase("Content-Length")) {
						contentLength = K.isInteger(headerValue) ? Integer.parseInt(headerValue) : -1;
					}
					
					gRequestHeaders.setProperty(headerKey, headerValue);
				}
			}

			//
			// Read payload data into memory
			//
			if (!readPayload()) {
				break;
			}

			// Check if passed content length matches payload size
			if ((contentLength != -1) && (contentLength != gPayloadData.length)) {
				KLog.debug("Received payload size ({}) does not match Content-Length request parameter ({})", K.formatBytes(gPayloadData.length), K.formatBytes(contentLength));
			}
			
			KLog.debug("HTTP request read ({} headers, {} payload, {} ms)", gRequestHeaders.size(), K.formatBytes(gPayloadData.length), timer.getElapsedMilliseconds());
			
			//
			// Call appropriate method to handle HTTP request
			//
			timer.reset();
			
			KLog.debug("HTTP {} /{} started", httpMethod, httpParameter);
			
			switch (httpMethod.toUpperCase()) {
			
				case "GET": {
					if (!K.isEmpty(gPayloadData)) {
						KLog.debug("Unsupported payload in HTTP GET request ignored");
					}
					get(K.decodeURL(httpParameter));
					break;
				}

				case "HEAD": {
					if (!K.isEmpty(gPayloadData)) {
						KLog.debug("Unsupported payload in HTTP HEAD request ignored");
					}
					head(K.decodeURL(httpParameter));
					break;
				}
				
				case "POST": {
					if (K.isEmpty(gPayloadData)) {
						KLog.debug("No payload sent with HTTP POST request");
					}
					post(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				case "PUT": {
					if (K.isEmpty(gPayloadData)) {
						KLog.debug("No payload sent with HTTP PUT request");
					}
					put(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				case "PATCH": {
					if (K.isEmpty(gPayloadData)) {
						KLog.debug("No payload sent with HTTP PATCH request");
					}
					patch(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				case "DELETE": {
					delete(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				case "OPTIONS": {
					options(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				case "TRACE": {
					trace(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
								
				case "CONNECT": {
					connect(K.decodeURL(httpParameter), gPayloadData);
					break;
				}
				
				default: {
					sendText(400, "{} method not supported", httpMethod);
					break;
				}
			}
			
			KLog.debug("HTTP {} method completed ({} ms)", httpMethod, timer.getElapsedMilliseconds());
		}
		
		//
		// Close connection
		//
		close();
		
		// Log program start
		KLog.debug("HTTP server thread ended");
	}
	
	/**
	 * Send file.
	 * 
	 * @param argFileName Name (and path) of file to be sent.
	 * 
	 * @return	True for success, false otherwise
	 */
	public boolean sendFile(String argFileName) {
		return sendFile(argFileName, false);
	}
	
	/**
	 * Send file.
	 * 
	 * @param argFileName Name (and path) of file to be sent
	 * @param argDownload	Tell client (browser) to locally save the file
	 * 
	 * @return	True for success, false otherwise
	 */
	public boolean sendFile(String argFileName, boolean argDownload) {
		
		try {
			
			File file = new File(argFileName);

			if (!file.exists()) {
				sendText(404, "File {} does not exist", argFileName);
				return false;
			}
        
			// Set download option if specified
			Properties properties = new Properties();
			
			if (argDownload) {
				String safeName = file.getName().replaceAll("[\\r\\n\"]", "_");
				properties.setProperty("Content-Disposition", "attachment; filename=\"" + safeName + "\"");
			}

			// Read file content and guess file type
			byte[] fileData	= KFile.readByteFile(file.toPath().toString());
			String fileType	= Files.probeContentType(file.toPath());
			
			// Set default file type if not detectable
			if (fileType == null) {
				fileType = "application/octet-stream";
			}

			KLog.debug("Sending file as type {}", fileType);
			sendResponse(200, fileType, properties, fileData);
			return true;
        
		} catch (Exception e) {
			KLog.error("Unable to read file {}: {}", argFileName, e.toString());
			sendText(500, K.getLastError());
			return false;
		}
	}
	
	/**
	 * Send HTML string.
	 * 
	 * @param argMessage	HTML string to be sent
	 * @param argObjects	Replace parameters, if needed
	 *
	 * @return	True for success, false otherwise
	 */
	public boolean sendHTML(String argMessage, Object... argObjects) {
		return sendResponse(200, "text/html; charset=UTF-8", null, K.replaceParams(argMessage, argObjects).getBytes());
	}
	
	/**
	 * Send complete response to the client.
	 * 
	 * @param argStatus			HTTP status code
	 * @param argType			Payload content type
	 * @param argProperties		Additional HTTP response headers (will override existing headers)
	 * @param argData			Payload to be sent
	 * 
	 * @return	True for success, false otherwise
	 */
	private boolean sendResponse(int argStatus, String argType, Properties argProperties, byte[] argData) {
	
		KTimer timer = new KTimer();
		
		try {
			StringBuilder clientData = new StringBuilder();
			
			// Set HTTP response code
			clientData.append("HTTP/1.1 ")
				.append(Integer.toString(argStatus))
				.append(' ')
				.append(K.getHTTPStatusText(argStatus));
			KLog.debug("HTTP Response: {}", clientData.toString());
			clientData.append(K.LINE_SEPARATOR);
			
			// Set standard HTTP response headers
			Properties headers = new Properties();
			headers.setProperty("Content-Type", argType);
			headers.setProperty("Content-Length", Integer.toString(K.isEmpty(argData) ? 0 : argData.length));
			headers.setProperty("Connection", "keep-alive");
			headers.setProperty("Date", new Date().toString());
			headers.setProperty("Server", this.getClass().getName() + '/' + K.VERSION);
			
			// Add or override response headers
			if (argProperties != null) {
				headers.putAll(argProperties);
			}
			
			// Add or override response headers from user code
			if (gResponseHeaders != null) {
				headers.putAll(gResponseHeaders);
			}

			// Construct HTTP response headers
			Enumeration<?> enumHeaders = headers.propertyNames();
            
			while (enumHeaders.hasMoreElements()) {
				String propertyKey		= (String) enumHeaders.nextElement();
				String propertyValue	= headers.getProperty(propertyKey);

			    clientData.append(propertyKey)
			    	.append(": ")
			    	.append(propertyValue.replaceAll("[\\r\\n]", ""))
			    	.append(K.LINE_SEPARATOR);
			}
						
			clientData.append(K.LINE_SEPARATOR);
			
			// Write out complete HTTP header
			write(clientData.toString().toCharArray());
			
			// Write out HTTP response payload data
			if (!K.isEmpty(argData)) {
				write(argData);
			}
			
			KLog.debug("HTTP response sent ({} headers, {} payload, {} ms)",
					headers.size(),
					K.formatBytes((K.isEmpty(argData) ? 0 : argData.length)),
					timer.getElapsedMilliseconds());
			
			return true;
			
		} catch (Exception e) {
			KLog.error("Unable to send HTTP data to client: {}", e.toString());
			return false;
		}
	}
	
	/**
	 * Send text string.
	 *
	 * @param argStatus		HTTP status code
	 * @param argMessage	Text string to be sent
	 * @param argObjects	Replace parameters, if needed
	 *
	 * @return	True for success, false otherwise
	 */
	public boolean sendText(int argStatus, String argMessage, Object... argObjects) {
		return sendResponse(argStatus, "text/plain; charset=UTF-8", null, K.replaceParams(argMessage, argObjects).getBytes());
	}

	/**
	 * Send text string.
	 * 
	 * @param argMessage	Text string to be sent
	 * @param argObjects	Replace parameters, if needed
	 *
	 * @return	True for success, false otherwise
	 */
	public boolean sendText(String argMessage, Object... argObjects) {
		return sendResponse(200, "text/plain; charset=UTF-8", null, K.replaceParams(argMessage, argObjects).getBytes());
	}
	
	/**
	 * Set maximum payload size to be read.
	 * 
	 * @param argMaxSize	Maximum payload size
	 */
	public void setMaxPayloadSize(int argMaxSize) {
		gMaxPayloadSize = argMaxSize;
	}
	
	/**
	 * Set additional headers to be sent in HTTP response.
	 * 
	 * @param argHeaders HTTP response header(s)
	 */
	public void setResponseHeaders(Properties argHeaders) {
		gResponseHeaders = argHeaders;
	}
	
	/**
	 * String representation of object.
	 */
	@Override
	public String toString() {
		return "KHTTPServerThread [gResponseHeaders=" + gResponseHeaders + ", gRequestHeaders=" + gRequestHeaders
				+ ", gPayloadData=" + Arrays.toString(gPayloadData) + ", gMaxPayloadSize=" + gMaxPayloadSize + "]";
	}
	
	/**
	 * HTTP TRACE method. Override this method to implement it.
	 *  
	 * @param argURL		Passed URL e.g. "/test/s=any"
	 * @param argPayload	HTTP payload passed with HTTP request
	 */
	public void trace(String argURL, byte[] argPayload) {
		sendText(400, "HTTP TRACE method not implemented");
	}
}
