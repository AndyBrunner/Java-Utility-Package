package ch.k43.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.directory.*;

/**
 * Static class with many utility methods. 
 */
public class K {
	
	//
	// Public constants
	//
	/**
	 * Package version number. Example is "2025.01.24".
	 */
	public static final		String				VERSION				= "2025.04.26";			// Also change docs/version-check/version.txt
	
	/**
	 * Application start time.
	 */
	public static final		Calendar			START_TIME			= Calendar.getInstance();
	
	/**
	 * Application instance UUID. Example is "D94CF874-5159-4B1F-8381-AA22812FFEDA".
	 */
	public static final		String				INSTANCE_UUID		= K.getUniqueID();
	
	/**
	 * Platform dependent line separator. Examples are "\r", "\n", "\r\n".
	 */
	public static final 	String				LINE_SEPARATOR		= System.lineSeparator();
	
	/**
	 * Platform dependent file separator. Examples are "/", "\".
	 */
	public static final 	String				FILE_SEPARATOR		= File.separator;
	
	/**
	 * Platform dependent path separator. Examples are ":", ";".
	 */
	public static final 	String				PATH_SEPARATOR		= File.pathSeparator;
	
	/**
	 * User name. Examples are "joesmith", "bob".
	 */
	public static final 	String				USER_NAME			= System.getProperty("user.name", "n/a").trim();
	
	/**
	 * User directory. The returned string is terminated with the platform dependent file separator.
	 */
	public static final 	String				CURRENT_DIRECTORY;	// Initialized in static block
	
	/**
	 * Home directory. The returned string is terminated with the platform dependent file separator.
	 */
	public static final 	String				HOME_DIRECTORY;		// Initialized in static block
	
	/**
	 * Temporary file directory. The returned string is terminated with the platform dependent file separator.
	 */
	public static final 	String				TEMP_DIRECTORY;		// Initialized in static block
	
	/**
	 * User language. Examples are "en", "fr", "de".
	 */
	public static final 	String				USER_LANGUAGE		= System.getProperty("user.language", "en").trim();
	
	/**
	 * Default encoding. Example is "UTF-8".
	 */
	public static final 	String				DEFAULT_ENCODING	= System.getProperty("native.encoding", "UTF-8").trim();
	
	/**
	 * JVM major version number
	 */
	public static final		int					JVM_MAJOR_VERSION;	// Initialized in static block

	/**
	 * JVM version name. Example is "Java HotSpot(TM) 64-Bit Server VM - Oracle Corporation"
	 */
	public static final 	String				JVM_VERSION_NAME	= System.getProperty("java.vm.name", "n/a").trim() + " - " + System.getProperty("java.vendor", "n/a").trim();

	/**
	 * JVM platform. Example is "Mac OS X Version 15.3.1/aarch64"
	 */
	public static final 	String				JVM_PLATFORM		= System.getProperty("os.name", "n/a").trim() + " Version " + System.getProperty("os.version", "n/a").trim() + '/' +  System.getProperty("os.arch", "n/a").trim();
	
	/**
	 * Minimum JVM version supported
	 */
	public static final 	int					JVM_MINIMAL_VERSION	= 8;
	
	/**
	 * Maximum number of saved errors
	 */
	public static final		int					MAX_SAVED_ERRORS	= 10;
	
	/**
	 * Value of KiB
	 */
	public static final 	double				SIZE_KIB			= 1_024d;

	/**
	 * Value of MiB
	 */
	public static final 	double				SIZE_MIB			= SIZE_KIB * 1_024d;
	
	/**
	 * Value of GiB
	 */
	public static final 	double				SIZE_GIB			= SIZE_MIB * 1_024d;
	
	/**
	 * Value of TiB
	 */
	public static final 	double				SIZE_TIB 			= SIZE_GIB * 1_024d;
	
	/**
	 * Value of PiB
	 */
	public static final		double				SIZE_PIB 			= SIZE_TIB * 1_024d;
	
	/**
	 * Value of EiB
	 */
	public static final 	double				SIZE_EIB 			= SIZE_PIB * 1_024d;
	
	/**
	 * Value of ZiB
	 */
	public static final 	double				SIZE_ZIB 			= SIZE_EIB * 1_024d;
	
	/**
	 * Value of YiB
	 */
	public static final 	double				SIZE_YIB 			= SIZE_ZIB * 1_024d;
	
	/**
	 * Default buffer size for file operations
	 */
	public static final		int					FILE_IO_BUFFER_SIZE	= 4_096;
	
	//
	// Private class constants
	//
	private static final	Random				SIMPLE_RANDOMIZER	= new Random();
	private static final	SecureRandom		SECURE_RANDOM		= new SecureRandom();
	private static final 	String				VERSION_URL			= "https://andybrunner.github.io/Java-Utility-Package/version-check/version.txt";	
	private static final 	String				AES_256_CIPHER		= "AES/CBC/PKCS5Padding";
	private static final 	String				SHA_256				= "SHA-256";

	//
	// Private class variables
	//
	private static ConcurrentHashMap<Thread, KLocalData>	gLocalData			= new ConcurrentHashMap<>(1);		

	//
	// Static block to initialize class
	//
	static {

		//
		// JVM_MAJOR_VERSION
		//
		int majorVersion = 0;
		
		try {

			// Get java version and splits it into major and minor numbers
			String[] versionElements = System.getProperty("java.version", "0.0").trim().split("\\.");
			
			majorVersion = Integer.parseInt(versionElements[0]);
		    
			// Treat version 1.x as x (e.g. 1.8 as 8) 
			if (majorVersion == 1) {
				majorVersion = Integer.parseInt(versionElements[1]);
		    }

		} catch (Exception e) {
			// Ignore errors
		}

		// Check minimum Java version
		if (majorVersion < K.JVM_MINIMAL_VERSION) {
			throw new RuntimeException("Version of Java must be " + K.JVM_MINIMAL_VERSION + " or higher");
		}
		
		JVM_MAJOR_VERSION = majorVersion;
		
		//
		// Set user, current and home directory and make sure they end with the platform specific file separator 
		//
		String directoryName = System.getProperty("user.dir", ".").trim();
		
		if (!directoryName.endsWith(K.FILE_SEPARATOR)) {
			directoryName += K.FILE_SEPARATOR;
		}
		
		CURRENT_DIRECTORY = directoryName;
		
		directoryName = System.getProperty("user.home", ".").trim();
		
		if (!directoryName.endsWith(K.FILE_SEPARATOR)) {
			directoryName += K.FILE_SEPARATOR;
		}
		
		HOME_DIRECTORY = directoryName;
		
		directoryName = System.getProperty("java.io.tmpdir", ".").trim();
		
		if (!directoryName.endsWith(K.FILE_SEPARATOR)) {
			directoryName += K.FILE_SEPARATOR;
		}
		
		TEMP_DIRECTORY = directoryName;
	}

	/**
	 * Compress the passed data using the GZIP algorithm.
	 * 
	 * @param argBuffer Data to be compressed
	 * @return Decompressed data or empty array for for errors
	 * 
	 * @since 2025.04.18
	 */
	public static byte[] compressGZIP(byte[] argBuffer) {

		// Check arguments
		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}

		// Declarations
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
	    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
	    	gzipOutputStream.write(argBuffer);
	    } catch (Exception e) { 
    		KLog.error("GZIP compression failed: ", e.toString());
    		return new byte[0];
    	}
	    
		byte[] compressedData = byteArrayOutputStream.toByteArray();
	    KLog.debug("GZIP compression successful (input {}, output {})", K.formatBytes(argBuffer.length), K.formatBytes(byteArrayOutputStream.size()));
        return compressedData;
	}
	
	/**
	 * Compress the passed data using the ZLIB algorithm.
	 * 
	 * @param argBuffer Data to be compressed
	 * @return Compressed data
	 * 
	 * @since 2024.08.11
	 */
	public static byte[] compressZLIB(byte[] argBuffer) {
		
		// Check arguments
		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
		// Create and initialize deflater
		Deflater deflater = new Deflater();
	    deflater.setInput(argBuffer);
	    deflater.finish();
	    
	    byte[]	buffer 			= new byte[FILE_IO_BUFFER_SIZE];
	    int		bufferLength	= 0;

	    try (ByteArrayOutputStream outputStream	= new ByteArrayOutputStream()) {
	    	
		    while (!deflater.finished()) {
		        bufferLength = deflater.deflate(buffer);
		        outputStream.write(buffer, 0, bufferLength);
		    }
		    
		    byte[] compressedData = outputStream.toByteArray();
		    KLog.debug("ZLIB compression successful (input {}, output {})", K.formatBytes(argBuffer.length), K.formatBytes(compressedData.length));

		    return compressedData;
		    
	    } catch (Exception e) {
    		KLog.error("ZLIB compression failed: {}", e.toString());
    		return new byte[0];
	    } finally {
	    	deflater.end();
	    }
	}
	
	/**
	 * Return decoded Base64 string.
	 * 
	 * @param	argBuffer	Base64 string to decode
	 * @return	byte[]		Decoded string
	 */
	public static byte[] decodeBase64(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
			return new byte[0];
    	}

    	// Return decoded string
    	return Base64.getDecoder().decode(argBuffer);
 	}
	
	/**
	 * Return decoded CSV string with the delimiter ','
	 * 
	 * @param	argBuffer	String to be decoded
	 * @return	String		Decoded string or null
	 * 
	 * @since 2024.06.15
	 */
	public static String decodeCSV(String argBuffer) {
		return (decodeCSV(argBuffer, ','));
	}
	
	/**
	 * Return decoded CSV string
	 * 
	 * @param	argBuffer		String to be decoded
	 * @param	argDelimiter	Delimiter character (Example: ',')
	 * @return	String			Decoded string
	 * 
 	 * @since 2024.06.15
	 */
	public static String decodeCSV(String argBuffer, char argDelimiter) {
		
    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

    	KLog.argException(argDelimiter == '"', "K.decodeCSV(): Double quote character not allowed as field delimiter");
		
    	String csvString = argBuffer;
    	
		// Change double quote characters to single quote characters
		if (csvString.contains("\"\"")) {
			csvString = csvString.replace("\"\"", "\"");
		}

		// Remove enclosing quotes
		if (csvString.startsWith("\"") && csvString.endsWith("\"")) {
			if (csvString.length() < 3) {
				csvString = "";
			} else {
				csvString = csvString.substring(1, csvString.length() - 1);
			}
		}
		
		return csvString;
	}
	
	/**
	 * Return decoded HTML String
	 * 
	 * @param	argBuffer	String to be decoded
	 * @return	String		Decoded string
	 * 
 	 * @since 2025.03.13
	 */
	public static String decodeHTML(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

        // Replace standard named entities
        return argBuffer
        		.replace("&lt;", "<")
        		.replace("&gt;", ">")
        		.replace("&quot;", "\"")
        		.replace("&#39;", "'")
        		.replace("&amp;", "&");	// Must be done last
    }
	
	/**
	 * Return decoded JSON string
	 * 
	 * @param	argBuffer	String to be decoded
	 * @return	String		Decoded string or null
	 *
	 * @since 2024.06.15
	 */
	public static String decodeJSON(String argBuffer) {
		
    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return ("");
    	}

		// Remove enclosing quotes
    	String jsonString = argBuffer;
    	
		if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
			if (jsonString.length() < 3) {
				jsonString = "";
			} else {
				jsonString = jsonString.substring(1, jsonString.length() - 1);
			}
		}
    	
		jsonString = jsonString.replace("\\b", "\b");
		jsonString = jsonString.replace("\\f", "\f");
		jsonString = jsonString.replace("\\n", "\n");
		jsonString = jsonString.replace("\\r", "\r");
		jsonString = jsonString.replace("\\t", "\t");
		jsonString = jsonString.replace("\\\"", "\"");
    	jsonString = jsonString.replace("\\\\", "\\");		// Must be done last

		return (jsonString);
	}
	
	/**
	 * Return decoded UTF-8 string.
	 *  
	 * @param	argBuffer	is the String to be decoded
	 * @return	String 		Decoded string
	 */
	public static String decodeURL(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}
    	
    	// Encode the string with UTF-8
    	try {
            return (URLDecoder.decode(argBuffer, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {
        	KLog.error("Unable to URL decode: {}", e.toString());
        	return "";
        }
    }
	
	/**
	 * Return decoded XML String
	 * 
	 * @param	argBuffer	String to be decoded
	 * @return	String		Decoded string
	 * 
 	 * @since 2024.06.16
	 */
	public static String decodeXML(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

		// Escape special characters
    	String	xmlString = argBuffer;

 		xmlString = xmlString.replace("&apos;", "'");
		xmlString = xmlString.replace("&quot;", "\"");
		xmlString = xmlString.replace("&gt;", ">");
		xmlString = xmlString.replace("&lt;", "<");
	   	xmlString = xmlString.replace("&amp;", "&");		// Must be done last

		return (xmlString);
    }
	
	/**
	 * Return decoded YAML string
	 * 
	 * @param	argBuffer	String to be decoded
	 * @return	String		Decoded string or null
	 *
	 * @since 2024.09.14
	 */
	public static String decodeYAML(String argBuffer) {
		
    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

		// Remove enclosing quotes
    	String yamlString = argBuffer;
    	
		if (yamlString.startsWith("\"") && yamlString.endsWith("\"")) {
			if (yamlString.length() < 3) {
				yamlString = "";
			} else {
				yamlString = yamlString.substring(1, yamlString.length() - 1);
			}
		}
    	
		yamlString = yamlString.replace("\\b", "\b");
		yamlString = yamlString.replace("\\f", "\f");
		yamlString = yamlString.replace("\\n", "\n");
		yamlString = yamlString.replace("\\r", "\r");
		yamlString = yamlString.replace("\\t", "\t");
		yamlString = yamlString.replace("\\\"", "\"");
    	yamlString = yamlString.replace("\\\\", "\\");		// Must be done last

		return (yamlString);
	}
	
	/**
	 * Decompress the passed data using the GZIP algorithm.
	 * 
	 * @param argBuffer Data to be decompressed
	 * @return Decompressed data or empty array for for errors
	 * 
 	 * @since 2025.04.18
	 */
	public static byte[] decompressGZIP(byte[] argBuffer) {
		
		// Check arguments
		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
	    try (ByteArrayInputStream	byteArrayInputStream	= new ByteArrayInputStream(argBuffer);
	    	 GZIPInputStream		gzipInputStream			= new GZIPInputStream(byteArrayInputStream);
	         ByteArrayOutputStream	outputStream			= new ByteArrayOutputStream()) {
	    	    	
	    	byte[] buffer = new byte[FILE_IO_BUFFER_SIZE];
	        int len;
	    
	        while ((len = gzipInputStream.read(buffer)) != -1) {
	            outputStream.write(buffer, 0, len);
	        }

	        byte[] result = outputStream.toByteArray();

	        KLog.debug("GZIP decompression successful (input {}, output {})", 
	                      K.formatBytes(argBuffer.length), K.formatBytes(result.length));
	        return result;

	    } catch (Exception e) {
	        KLog.error("GZIP decompression failed: {}", e.toString());
	        return new byte[0];
	    }
	}
	
	/**
	 * Decompress the passed data using the ZLIB algorithm.
	 * 
	 * @param argBuffer Data to be decompressed
	 * @return Decompressed data or empty array for for errors
	 * 
 	 * @since 2024.08.11
	 */
	public static byte[] decompressZLIB(byte[] argBuffer) {
		
		// Check arguments
		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
		// Create and initialize inflater
	    Inflater inflater = new Inflater();
	    inflater.setInput(argBuffer);

	    byte[]	buffer 			= new byte[FILE_IO_BUFFER_SIZE];
	    int		bufferLength	= 0;

	    try (ByteArrayOutputStream outputStream	= new ByteArrayOutputStream()) {
	    	
		    while (!inflater.finished()) {
		        bufferLength = inflater.inflate(buffer);
		        
		        // Occasionally, inflater returns 0
		        if (bufferLength == 0 && inflater.needsInput()) {
		            break;
		        }
		        
		        outputStream.write(buffer, 0, bufferLength);
		    }
		    
		    byte[] decompressedData = outputStream.toByteArray();
		    KLog.debug("ZLIB decompression successful (input {}, output {})", K.formatBytes(argBuffer.length), K.formatBytes(decompressedData.length));

		    return decompressedData;
		    
	    } catch (Exception e) {
			KLog.error("ZLIB decompression failed: {}", e.toString());
			return new byte[0];
	    } finally {
	        inflater.end();
	    }
	}
	
	/**
	 * Return decrypted AES-256 buffer (AES/CBC/PKCS5Padding).<p>
	 * 
	 * Note: Before being used as the decryption key, the passed secret key is hashed with SHA-256 to always create a 256 bit key.<br>
	 * 
	 * <pre>
	 * Example:
	 * byte[] clearText		= "Some datq to be encrypted".getBytes(StandardCharsets.UTF_8);
	 * byte[] secureKey		= "SomeSecureKey".getBytes(StandardCharsets.UTF_8);
	 * byte[] iv			= K.getRandomBytes(16);
	 * byte[] encrBuffer	= K.encryptAES256(clearText, secureKey, iv);
	 * byte[] decrBuffer	= K.decryptAES256(encrBuffer, secureKey, iv);
	 * </pre>
	 * 
	 * @param	argBuffer		Encrypted buffer
	 * @param	argSecretKey	Secret key for decryption
	 * @param	argInitVector	Initialization vector (16 bytes)
	 * @return	byte[]			Decrypted buffer
	 */
	public static byte[] decryptAES256(byte[] argBuffer, byte[] argSecretKey, byte[] argInitVector) {

		// Check arguments
		KLog.argException(K.isEmpty(argSecretKey), "K.decryptAES256(): Secret key is required");
		KLog.argException(K.isEmpty(argInitVector) || (argInitVector.length != 16), "K.decryptAES256(): AES-256 cipher initialization vector must be 16 bytes (128 bits)");
		
		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
		try {
			
			// SHA-256 hash the secret key to get the AES-256 key
			MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
			messageDigest.update(argSecretKey);
			byte[] secretKeyHash256Bit = Arrays.copyOf(messageDigest.digest(), 32);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyHash256Bit, "AES");
			
			// Create the cipher
			Cipher cipher = Cipher.getInstance(AES_256_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(argInitVector));

			// Encrypt the buffer
			return (cipher.doFinal(argBuffer));
			
		} catch (Exception e) {
			KLog.error("Unable to AES-256 decrypt: {}", e.toString());
			return new byte[0];
		}
	}
	
	/**
	 * Return base64 decoded and deserialized object.
	 * 
	 * @param	argString	Object to decode and deserialize
	 * @return	Object or null for errors
	 * 
	 * @since 2024.08.22
	 */
	public static Object deserialize(String argString) {
		
        // Base64 decode serialized object
        byte[] decodedData	= Base64.getDecoder().decode(argString);
        Object newObject	= null;
        
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decodedData))) {
            newObject = ois.readObject();
        } catch (Exception e) {
			KLog.error("Unable to deserialize object: {}", e.toString());
			return null;
        }
        
        KLog.debug("Decode and deserialize object {} successful", newObject.getClass().getName());
        
        return newObject;
	}
	
	/**
	 * Return encoded buffer to Base64 string
	 * 
	 * @param	argBuffer	Buffer to convert
	 * @return	String		Base64 encoded string
	 */
	public static String encodeBase64(byte[] argBuffer) {
		
    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}
    	
    	// Return encoded buffer
    	return Base64.getEncoder().encodeToString(argBuffer);
	}
	
	/**
	 * Return encoded string for CSV with the delimiter ','
	 * 
	 * @param	argBuffer	String to be encoded
	 * @return	String		Encoded string or null
	 * 
 	 * @since 2024.06.15
	 */
	public static String encodeCSV(String argBuffer) {
		return (encodeCSV(argBuffer, ','));
	}
		
	/**
	 * Return encoded string for CSV
	 * 
	 * @param	argBuffer		String to be encoded
	 * @param	argDelimiter	Delimiter character (Example: ',')
	 * @return	String			Encoded string
	 * 
	 * @since 2024.06.15
	 */
	public static String encodeCSV(String argBuffer, char argDelimiter) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return ("\"\"");
    	}

    	KLog.argException(argDelimiter == '"', "K.encodeCSV(): Double quote character not allowed as field delimiter");
    	
    	String	csvString	= argBuffer;
    	boolean quoteFlag	= false;
    	
		// CR or LF characters: Enclose field with quotes
		if (csvString.contains("\r") || csvString.contains("\n")) {
			quoteFlag = true;
		}

		// Delimiter characters: Enclose field with quotes
		if (csvString.contains("" + argDelimiter)) {
			quoteFlag = true;
		}
		
		// Quote characters: Change to double quotes and enclose field with quotes
		if (csvString.contains("\"")) {
			csvString = csvString.replace("\"", "\"\"");
			quoteFlag = true;
		}
		
		// Enclose field with quotes if it contains special characters
		if (quoteFlag) {
			csvString = '"' + csvString + '"';
		}
    	
		return csvString;
    }
	
	/**
	 * Encode HTML string
	 * 
	 * @param	argBuffer	String to be encoded
	 * @return	String		Encoded string
	 * 
 	 * @since 2025.03.13
	 */
	public static String encodeHTML(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

    	StringBuilder encodedString = new StringBuilder();
        
        for (char ch : argBuffer.toCharArray()) {
            switch (ch) {
                case '&':
                    encodedString.append("&amp;");
                    break;
                case '<':
                    encodedString.append("&lt;");
                    break;
                case '>':
                    encodedString.append("&gt;");
                    break;
                case '"':
                    encodedString.append("&quot;");
                    break;
                case '\'':
                    encodedString.append("&#39;");
                    break;
                default:
                    encodedString.append(ch);
            }
        }
        
        return encodedString.toString();
    }
	
	/**
	 * Return encoded string for JSON. The returned string is always enclosed in double quotes.
	 * 
	 * @param	argBuffer		String to be encoded
	 * @return	String			Encoded string
	 * 
	 * @see 	encodeJSON(String, boolean)
	 * 
 	 * @since 2024.06.15
	 */
	public static String encodeJSON(String argBuffer) {
		return (encodeJSON(argBuffer, true));
	}
	
	/**
	 * Return encoded string for JSON.
	 * 
	 * @param	argBuffer			String to be encoded
	 * @param	argEnforceQuotes	Flag to enforce surrounding quotes (required for JSON keys, optional for JSON boolean/numeric values)
	 * @return	String				Encoded string
	 * 
 	 * @since 2024.06.28
	 */
	public static String encodeJSON(String argBuffer, boolean argEnforceQuotes) {

		// Handle null value
    	if (argBuffer == null) {
    		return (argEnforceQuotes) ? "\"null\"" : "null";
    	}
  
    	// Handle empty string
    	if (K.isEmpty(argBuffer)) {
    		return "\"\"";
    	}
		
		// Escape special characters
    	String	jsonString	= argBuffer;

    	jsonString = jsonString.replace("\\", "\\\\");		// Must be done first
		jsonString = jsonString.replace("\b", "\\b");
		jsonString = jsonString.replace("\f", "\\f");
		jsonString = jsonString.replace("\n", "\\n");
		jsonString = jsonString.replace("\r", "\\r");
		jsonString = jsonString.replace("\t", "\\t");
		jsonString = jsonString.replace("\"", "\\\"");

		// Set enclosing quote
		if (argEnforceQuotes) {
			return '"' + jsonString + '"';
		}

  		// Check if boolean value
		if (jsonString.equalsIgnoreCase("true") || jsonString.equalsIgnoreCase("false")) {
			return jsonString.toLowerCase();
		}

		// Check if numeric value
		if (K.isNumber(jsonString)) {
			return jsonString;
		}

		// Return enclosed string
		return '"' + jsonString + '"';
    }

	/**
	 * Return encoded string in UTF-8
	 * 
	 * @param	argBuffer	is the String to be encoded
	 * @return	String		Encoded stringl
	 */
	public static String encodeURL(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}
    	
    	// Encode the string with UTF-8
    	try {
            return (URLEncoder.encode(argBuffer, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {
        	KLog.error("Unable to URL encode: {}", e.toString());
        	return "";
        }
    }
	
	/**
	 * Encode string for XML
	 * 
	 * @param	argBuffer	String to be encoded
	 * @return	String		Encoded string
	 * 
 	 * @since 2024.06.16
	 */
	public static String encodeXML(String argBuffer) {

    	// Check passed parameter
    	if (K.isEmpty(argBuffer)) {
    		return "";
    	}

		// Escape special characters
    	String	xmlString = argBuffer;

	   	xmlString = xmlString.replace("&", "&amp;");			// Must be done first
 		xmlString = xmlString.replace("'", "&apos;");
		xmlString = xmlString.replace("\"", "&quot;");
		xmlString = xmlString.replace(">", "&gt;");
		xmlString = xmlString.replace("<", "&lt;");

		return xmlString;
    }

	/**
	 * Return encoded string for YAML. The returned string is always enclosed in double quotes.
	 * 
	 * @param	argBuffer		String to be encoded
	 * @return	String			Encoded string
	 * 
	 * @see 	encodeYAML(String, boolean)
	 * 
 	 * @since 2024.09.14
	 */
	public static String encodeYAML(String argBuffer) {
		return (encodeYAML(argBuffer, true));
	}
	
	/**
	 * Return encoded string for YAML.
	 * 
	 * Note: Strings are always enclosed with double quotes.
	 * 
	 * @param	argBuffer			String to be encoded
	 * @param	argEnforceQuotes	Flag to enforce surrounding quotes (optional for YAML values)
	 * @return	String				Encoded string
	 * 
 	 * @since 2024.09.14
	 */
	public static String encodeYAML(String argBuffer, boolean argEnforceQuotes) {

		// Handle null value
    	if (argBuffer == null) {
    		return ((argEnforceQuotes) ? "\"null\"" : "null");
    	}
    	
    	// Handle empty string
    	if (K.isEmpty(argBuffer)) {
    		return ("\"\"");
    	}

		// Escape special characters
    	String yamlString = argBuffer;

    	yamlString = yamlString.replace("\\", "\\\\");		// Must be done first
    	yamlString = yamlString.replace("\'", "\\'");
    	yamlString = yamlString.replace("\"", "\\\"");
    	yamlString = yamlString.replace("\n", "\\n");
    	yamlString = yamlString.replace("\r", "\\r");
    	yamlString = yamlString.replace("\t", "\\t");
    	yamlString = yamlString.replace("\b", "\\b");
    	yamlString = yamlString.replace("\f", "\\f");

		// Set enclosing quote
		if (argEnforceQuotes) {
			return '"' + yamlString + '"';
		}
    	
		// Check if boolean value
		if (yamlString.equalsIgnoreCase("true") || yamlString.equalsIgnoreCase("false")) {
			return yamlString.toLowerCase();
		}

		// Check if numeric value
		if (K.isNumber(yamlString)) {
			return yamlString;
		}

		// Always enclose string with double quotes
		return '"' + yamlString + '"';
    }
	
	/**
	 * Return encrypted AES-256 buffer (AES/CBC/PKCS5Padding).<p>
	 * 
	 * Note: Before being used as the encryption key, the passed secret key is hashed with SHA-256 to always create a 256 bit key.<br>
	 * 
	 * <pre>
	 * Example:
	 * byte[] clearText		= "Some data to be encrypted".getBytes(StandardCharsets.UTF_8);
	 * byte[] secureKey		= "SomeSecureKey".getBytes(StandardCharsets.UTF_8);
	 * byte[] iv			= K.getRandomBytes(16);
	 * byte[] encrBuffer	= K.encryptAES256(clearText, secureKey, iv);
	 * byte[] decrBuffer	= K.decryptAES256(encrBuffer, secureKey, iv);
	 * </pre>
	 *  
	 * @param	argBuffer		Clear text buffer
	 * @param	argSecretKey	Secret key for encryption
	 * @param	argInitVector	Initialization vector (16 bytes)
	 * @return	byte[]			Encrypted buffer or empty array
	 */
	public static byte[] encryptAES256(byte[] argBuffer, byte[] argSecretKey, byte[] argInitVector) {
				
		// Check arguments
		KLog.argException(K.isEmpty(argSecretKey), "K.encryptAES256(): Secret key is required");
		KLog.argException(K.isEmpty(argInitVector) || (argInitVector.length != 16), "K.encryptAES256(): AES-256 cipher initialization vector must be 16 bytes (128 bits)");

		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
		try {
			// SHA-256 hash the secret key to get the AES-256 key
			MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
			messageDigest.update(argSecretKey);
			byte[] secretKeyHash256Bit = Arrays.copyOf(messageDigest.digest(), 32);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyHash256Bit, "AES");
			
			// Create the cipher
			Cipher cipher = Cipher.getInstance(AES_256_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(argInitVector));

			// Encrypt the buffer
			return cipher.doFinal(argBuffer);
			
		} catch (Exception e) {
			KLog.error("Unable to AES-256 encrypt: {}", e.toString());
			return new byte[0];
		}
	}
	
	/**
	 * Convert number of bytes to formatted string (Example: 212 B, 21.23 KiB, 3.00 MiB).
	 * 
	 * @param argSizeInBytes Number of bytes to be formatted
	 * @return String with formatted size (Example 12.03 GiB)
	 * 
	 * @since 2024.06.23
	 */
	public static String formatBytes(double argSizeInBytes) {
		
		// Format returned string
		if (argSizeInBytes >= SIZE_YIB) {
			return String.format("%.2f YiB", argSizeInBytes / SIZE_YIB);
		}
		
		if (argSizeInBytes >= SIZE_ZIB) {
			return String.format("%.2f ZiB", argSizeInBytes / SIZE_ZIB);
		}
		
		if (argSizeInBytes >= SIZE_EIB) {
			return String.format("%.2f EiB", argSizeInBytes / SIZE_EIB);
		}
		
		if (argSizeInBytes >= SIZE_PIB) {
			return String.format("%.2f PiB", argSizeInBytes / SIZE_PIB);
		}
		
		if (argSizeInBytes >= SIZE_TIB) {
			return String.format("%.2f TiB", argSizeInBytes / SIZE_TIB);
		}
		
		if (argSizeInBytes >= SIZE_GIB) {
			return String.format("%.2f GiB", argSizeInBytes / SIZE_GIB);
		}
		
		if (argSizeInBytes >= SIZE_MIB) {
			return String.format("%.2f MiB", argSizeInBytes / SIZE_MIB);
		}
		
		if (argSizeInBytes >= SIZE_KIB) {
			return String.format("%.2f KiB", argSizeInBytes / SIZE_KIB);
		}

		return String.format("%d B", (long) argSizeInBytes);
	}
	
	/**
	 * Return compute Hash (MD5, SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-384 or SHA3-512)
	 * @param	argHashType	Hash algorithm
	 * @param	argBuffer	argBuffer to hash
	 * @return	byte[]		Hashed data
	 */
	public static byte[] generateHash(String argHashType, byte[] argBuffer) {

		// Check argument
		KLog.argException(K.isEmpty(argHashType), "K.generateHash(): Hash algorithm is required");

		// Declarations
		String hashType = argHashType.toUpperCase();

		// Validate argument
		KLog.argException(
				!hashType.equals("MD5") &&
				!hashType.equals(SHA_256) &&
				!hashType.equals("SHA-384") &&
				!hashType.equals("SHA-512") &&
				!hashType.equals("SHA3-256") &&
				!hashType.equals("SHA3-384") &&
				!hashType.equals("SHA3-512"),
				"K.generateHash(): Hash algorithm must be MD5, SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-384 or SHA3-512");	// 2024.05.23


		if (K.isEmpty(argBuffer)) {
			return new byte[0];
		}
		
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(hashType);
			messageDigest.update(argBuffer);
			return ( messageDigest.digest());
		} catch (Exception e) {
			KLog.error("Unable to {} hash: {}", hashType, e.toString());
			return new byte[0];
		}
	}

	/**
	 * Generate RSA-4096 public/private key pair.
	 * 
	 * @return	KeyPair	Generated key pair
	 */
	public static KeyPair generateRSAKeyPair() {
	   		
		// Generate key pair
		try {
			KTimer timer = new KTimer();
			
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(4096);
			
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			KLog.debug("RSA-4096 key pair generated ({} ms)", timer.getElapsedMilliseconds());
			
			return keyPair;
			
		} catch (Exception e) {
			KLog.error("Unable to generate RSA-4096 key pair: {}", e.toString());
			return null;
		}
	}
	
	/**
	 * Get certificate from JKS keystore file.
	 * 
	 * @param argFileName		JKS file name
	 * @param argFilePassword	JKS file password
	 * @param argKey			Key (alias) name
	 * 
	 * @return	Certificate or null for errors
	 * 
	 * @since 2024.12.06
	 */
	public static Certificate getCertificate(String argFileName, char[] argFilePassword, String argKey) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argFileName), "K.getCertificate(): argFileName is required");
		KLog.argException(K.isEmpty(argKey), "K.getCertificate(): argKey is required");
		
        try {
        	// Load Java KeyStore File (JKS)
        	KeyStore keyStore = K.loadKeyStore(argFileName, argFilePassword);

        	if (keyStore == null) {
        		return null;
        	}
            
            // Get certificate
            Certificate certificate = keyStore.getCertificate(argKey);
            if (certificate == null) {
            	KLog.error("Certificate {} not found in keyfile {}", argKey, argFileName);
            	return null;
            }
            
            KLog.debug("Certificate {} from keyfile {} retrieved", argKey, argFileName);
            
            return certificate;

        } catch (Exception e) {
        	KLog.error("Unable to get certificate {} from keyfile {}: {}", argKey, argFileName, e.toString());
        	return null;
        }
	}
	
	/**
	 * Return current directory.<br>
	 * 
	 * @deprecated Use K.CURRENT_DIRECTORY instead
	 * 
	 * @return Current directory, e.g. "/Users/johnsmith"
	 * 
	 * @since 2024.05.24
	 */
	@Deprecated
	public static String getCurrentDir() {
		return (CURRENT_DIRECTORY);
	}
	
	/**
	 * Return the currently available version of this package. The check is done by fetching
	 * https://andybrunner.github.io/Java-Utility-Package/version-check/version.txt.
	 * 
	 * @return Current version number (YYYY-MM-DD) or null if web site is not reachable
	 * 
	 * @since 2025.02.23
	 */
	public static String getCurrentVersionNumber() {
		
		KLog.debug("Checking for current version of package ch.k43.util");
		
		KHTTPClient http = new KHTTPClient();
		
		if (!http.get(K.VERSION_URL)) {
			KLog.error("Unable to read from {}: {}", K.VERSION_URL, http.getLastError());
			return null;
		}
		
		// Get current version
		String currentVersion = http.getResponseDataAsString().trim();
		
		if (K.isEmpty(currentVersion) || (currentVersion.length() != 10)) {
			KLog.error("Unable to get current version from {} - Data received {}", K.VERSION_URL, currentVersion);
			return null;
		}
		
		KLog.debug("Currently available version is {}", currentVersion);
		return currentVersion;
	}
	
	/**
	 * Return status text for given HTTP status code.
	 * 
	 * @param	argStatusCode	HTTP status code
	 * @return	Status message
	 * 
	 * @since 2025.04.07
	 */
	public static String getHTTPStatusText(int argStatusCode) {
		
		switch (argStatusCode) {

			// Information
    		case 100: return "Continue";
    		case 101: return "Switching Protocols";
    		case 102: return "Processing";
    		case 103: return "Early Hints";
    		
    		// Success
    		case 200: return "OK";
    		case 201: return "Created";
    		case 202: return "Accepted";
    		case 203: return "Non-Authoritative Information";
    		case 204: return "No Content";
    		case 205: return "Reset Content";
    		case 206: return "Partial Content";
    		case 207: return "Multi-Status";
    		case 208: return "Already Reported";
    		case 209: return "IM Used";
    		
    		// Redirect
    		case 300: return "Multiple Choices";
    		case 301: return "Moved Permanently";
    		case 302: return "Found (Moved Temporarily)";
    		case 303: return "See Other";
    		case 304: return "Not Modified";
    		case 305: return "Use Proxy";
    		case 306: return "(reserved)";
    		case 307: return "Temporary Redirect";
    		case 308: return "Permanent Redirect";
    		
    		// Client Error
    		case 400: return "Bad Request";
    		case 401: return "Unauthorized";
    		case 402: return "Payment Required";
    		case 403: return "Forbidden";
    		case 404: return "Not Found";
    		case 405: return "Method Not Allowed";
    		case 407: return "Proxy Authentication Required";
    		case 408: return "Request Timeout";
    		case 409: return "Conflict";
    		case 410: return "Gone";
    		case 411: return "Length Required";
    		case 412: return "Precondition Failed";
    		case 413: return "Payload Too Large";
    		case 414: return "URI Too Long";
    		case 415: return "Unsupported Media Type";
    		case 416: return "Range Not Satisfiable";
    		case 417: return "Expectation Failed";
    		case 421: return "Misdirected Request";
    		case 422: return "Unprocessable Entity";
    		case 423: return "Locked";
    		case 424: return "Failed Dependency";
    		case 425: return "Too Early";
    		case 426: return "Upgrade Required";
    		case 428: return "Precondition Required";
    		case 429: return "Too Many Requests";
    		case 431: return "Request Header Fields Too Large";
    		case 451: return "Unavailable For Legal Reasons";
     		
    		// Server Error
    		case 500: return "Internal Server Error";
    		case 501: return "Not Implemented";
    		case 502: return "Bad Gateway";
    		case 503: return "Service Unavailable";
    		case 504: return "Gateway Timeout";
    		case 505: return "HTTP Version not supported";
    		case 506: return "Variant Also Negotiates";
    		case 507: return "Insufficient Storage";
    		case 508: return "Loop Detected";
    		case 509: return "Bandwidth Limit Exceeded";
    		case 510: return "Not Extended";
    		case 511: return "Network Authentication Required";
    		
    		// Unknown
    		default:  return "Unknown Status Code";
		}
	}
	
	/**
	 * Return IP address of hostname
	 * 
	 * @param	argHostname	Hostname
	 * @return	String	IP address or null
	 */
	public static String getIPAddress(String argHostname) {

		// Check argument
		KLog.argException(K.isEmpty(argHostname), "K.getIPAddress(): Host name is required");
		
		try {
			return InetAddress.getByName(argHostname).getHostAddress();
		} catch (Exception e) {
			KLog.error("Unable to resolve IP address of host {}: {}", argHostname, e.toString());
			return "";
		}
	}
	
	/**
	 * Return number of processors for the JVM. The number of processors may change during the applications lifetime.
	 * 
	 * @return Number of processors
	 */
	public static int getJVMCPUCount() {
		
		// Get access to Runtime
		return Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * Return JVM memory statistics.<p>
	 * 
	 * Note: The returned array has the following values:<br>
	 * [0] = Maximum heap size in bytes<br>
	 * [1] = Current heap size in bytes<br>
	 * [2] = Used heap size in bytes<br>
	 * [3] = Free heap size in bytes<br>
	 * 
	 * @return JVM memory statistics
	 */
	public static long[] getJVMMemStats() {
		
		// Get access to Runtime
		Runtime runtime = Runtime.getRuntime();
		
		long[] memoryStats = new long[4];
		
		memoryStats[0] = runtime.maxMemory();
		memoryStats[1] = runtime.totalMemory();
		memoryStats[2] = (runtime.totalMemory() - runtime.freeMemory());
		memoryStats[3] = runtime.freeMemory();
		
		return memoryStats;
	}
	
	/**
	 * Return JVM name (Example "OpenJDK 64-Bit Server VM - Eclipse Adoptium").
	 * 
	 * @deprecated Use K.JVM_VERSION_NAME instead
	 * @return	String		JVM version number and vendor 
	 */
	@Deprecated
	public static String getJVMName() {
		return JVM_VERSION_NAME;
	}
		
	/**
	 * Return JVM platform (Example: "Mac OS X (Version 14.5/aarch64)").
	 * 
	 * @deprecated Use K.JVM_PLATFORM instead
	 * 
	 * @return	String		JVM platform 
	 */
	@Deprecated
	public static String getJVMPlatform() {
		return JVM_PLATFORM;
	}
	
	/**
	 * Return JVM major version (Example: 1.9.x as 9, 12.4 as 12).
	 * 
	 * @deprecated Use K.JVM_MAJOR_VERSION instead
	 * 
	 * @return	int		Major version number of JVM runtime or 0 for errors
	 */
	@Deprecated
	public static int getJVMVersion() {
		return JVM_MAJOR_VERSION;
	}
	
	/**
	 * Return the last error message. 
	 * 
	 * @return Last error message or null
	 * 
	 * @since 2024.12.08
	 */
	public static String getLastError() {
		
		synchronized (gLocalData) {
			ArrayList<String> lastErrorArray = K.getLocalData().kLastErrors;
			String[] lastErrors = lastErrorArray.toArray(new String[lastErrorArray.size()]);
			return lastErrors.length == 0 ? null : lastErrors[0];
		}
	}
	
	/**
	 * Return the last error messages. 
	 * 
	 * @return Array of error messages
	 * 
	 * @since 2024.12.08
	 */
	public static String[] getLastErrors() {
		
		synchronized (gLocalData) {
			ArrayList<String> lastErrorArray = K.getLocalData().kLastErrors;
			return lastErrorArray.toArray(new String[lastErrorArray.size()]);
		}
	}
	
	/**
	 * Get line separator
	 *  
	 * @deprecated Use K.LINE_SEPARATOR instead
	 * 
	 * @return	String	Platform dependent line separator (\r, \n or \r\n)
	 */
	@Deprecated
	public static String getLineSeparator() {
		return LINE_SEPARATOR;
	}
	
	/**
	 * Get local data of thread.
	 * 
	 * @return Local data object
	 * 
	 * @since 2025.02.03
	 */
	static synchronized KLocalData getLocalData() {
		
		// Remove local data objects if owning thread is no longer active
		for (Map.Entry<Thread, KLocalData> entry : gLocalData.entrySet()) {
				
		    Thread thread = entry.getKey();

		    if (!thread.isAlive()) {
				KLog.debug("Local data of thread {} removed", entry.getValue().threadName);
				gLocalData.remove(thread);
			}
		} 
			
		KLocalData localData = gLocalData.get(Thread.currentThread());
			
		// Create local data object if it does not exist
		if (localData == null) {
			localData = new KLocalData(Thread.currentThread().getName());
			gLocalData.put(Thread.currentThread(), localData);
			KLog.debug("Local data of thread {} created", localData.threadName);
		}

		return localData;
	}
	
	/**
	 * Return local TCP/IP address.
	 * 
	 * @return	IP address
	 */
	public static String getLocalHostAddress() {

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			KLog.error("Unable to get local IP address: {}", e.toString());
			return "";
		}
	}
	
	/**
	 * Return local TCP/IP host name
	 * 
	 * @return	Host name
	 */
	public static String getLocalHostName() {

		try {
			return InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception e) {
			KLog.error("Unable to get local hostname: {}", e.toString());
			return "";
		}
	}

	/**
	 * Generate hash from password with salt. The hashing is repeated for the number of iterations with SHA3-512(password + salt).
	 *
	 * @param argPassword	Password to be hashed
	 * @param argSalt		Salt to be added to password (should be at least 32 bytes and must be unique for each password)
	 * @param argIteration	Number of hash cycles (recommended between 100'000 and 1'000'000)
	 *
	 * @return Hashed password (64 bytes)
	 * 
	 * @since 2025.02.15
	 */
	public static byte[] getPasswordHash(byte[] argPassword, byte[] argSalt, int argIteration) {

		// Declarations
		KTimer timer = new KTimer();
		
		// Check arguments
		KLog.argException(K.isEmpty(argPassword), "K.getPasswordHash(): argPassword is required");
		KLog.argException(K.isEmpty(argSalt), "K.getPasswordHash(): argSalt is required");
		KLog.argException(argIteration < 1, "K.getPasswordHash(): argIteration must be a positive integer");
		
		// Append salt to password
		byte[] passwordHash = new byte[argPassword.length + argSalt.length];
		System.arraycopy(argPassword, 0, passwordHash, 0, argPassword.length);
		System.arraycopy(argSalt, 0, passwordHash, argPassword.length, argSalt.length);
	
		// Hash (password + salt) with SHA3-512 for the specified number of times
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA3-512");
			for (int index = 0; index < argIteration; index++) {
				messageDigest.reset();
				messageDigest.update(passwordHash);
				passwordHash = messageDigest.digest();
			}
		} catch (Exception e) {
			KLog.error("Unable to SHA3-512 hash: {}", e.toString());
			return new byte[0];
		}
		
		KLog.debug("Password hashing with SHA3-512 completed ({} iterations, {} ms)", argIteration, timer.getElapsedMilliseconds());
		return passwordHash;
	}
	
	/**
	 * Generate hash from password with salt. The hashing is repeated 500'000 times with SHA3-512(password + salt).
	 *
	 * @param argPassword	Password to be hashed
	 * @param argSalt		Salt to be added to password (should be at least 32 bytes and must be unique for each password)
	 *
	 * @return Hashed password (64 bytes)
	 * 
	 * @since 2025.02.15
	 */
	public static byte[] getPasswordHash(String argPassword, String argSalt) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argPassword), "K.getPasswordHash(): argPassword is required");
		KLog.argException(K.isEmpty(argSalt), "K.getPasswordHash(): argSalt is required");
		
		return getPasswordHash(argPassword.getBytes(StandardCharsets.UTF_8), argSalt.getBytes(StandardCharsets.UTF_8), 500_000);
	}
	
	/**
	 * Get private key from JKS key store file.
	 * 
	 * @param argFileName		JKS file name
	 * @param argFilePassword	JKS file password
	 * @param argKey			Key (alias) name
	 * @param argKeyPassword	Key password
	 * 
	 * @return	Private key or null for errors
	 * 
	 * @since 2024.12.06
	 */
	public static PrivateKey getPrivateKey(String argFileName, char[] argFilePassword, String argKey, char[] argKeyPassword) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argFileName), "K.getPrivateKey(): argFileName is required");
		KLog.argException(K.isEmpty(argKey), "K.getPrivateKey(): argKey is required");
		
        try (FileInputStream fis = new FileInputStream(argFileName)) {
        	
        	// Load Java KeyStore File (JKS) 
        	KeyStore keyStore = K.loadKeyStore(argFileName, argFilePassword);
 
        	if (keyStore == null) {
        		return null;
        	}
            
            // Return private key
            Key key = keyStore.getKey(argKey, argKeyPassword);
            
            if (key == null) {
            	KLog.error("Key {} not found in keyfile {}", argKey, argFileName);
            	return null;
            }
            
            if (!(key instanceof PrivateKey)) {
            	KLog.error("Key {} in keyfile {} is not a private key", argKey, argFileName);
            	return null;
            }
            
            KLog.debug("Private key {} from keyfile {} retrieved", argKey, argFileName);
            
            return (PrivateKey) key;

        } catch (Exception e) {
        	KLog.error("Unable to get private key {} from keyfile {}: {}", argKey, argFileName, e.toString());
        	return null;
        }
	}
	
	/**
	 * Get public key from JKS key store file.
	 * 
	 * @param argFileName		JKS file name
	 * @param argFilePassword	JKS file password
	 * @param argKey			Key (alias) name
	 * 
	 * @return	Public key or null for errors
	 * 
	 * @since 2024.12.07
	 */
	public static PublicKey getPublicKey(String argFileName, char[] argFilePassword, String argKey) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argFileName), "K.getPublicKey(): argFileName is required");
		KLog.argException(K.isEmpty(argKey), "K.getPublicKey(): argKey is required");
		
		// Get certificate
		Certificate certificate = K.getCertificate(argFileName, argFilePassword, argKey);

		if (certificate == null) {
			return null;
		}
		
        KLog.debug("Public key from certificate {} retrieved", argKey);
        
		return certificate.getPublicKey();
	}
	
	/**
	 * Return random bytes generated by the SecureRandom class.<br>
	 * 
	 * @param	argLength	Number of bytes to be generated
	 * @return	byte[]		Generated bytes
	 */
	public static byte[] getRandomBytes(int argLength) {
	   
		// Check initialization vector
		KLog.argException(argLength < 1, "K.getRandomBytes(): Number of bytes to randomized must be > 0");
		
		// Generate random bytes
		byte[] buffer = new byte[argLength];
		SECURE_RANDOM.nextBytes(buffer);
		
		// Return generated bytes
	    return buffer;
	}
	
	/**
	 * Return random integer between the given range.
	 * 
	 * @param argMinInt	Lowest possible integer (inclusive)
	 * @param argMaxInt	Highest possible integer (exclusive)
	 * @return	Random integer
	 * 
	 * @since 2024.05.29
	 */
	public static int getRandomInt(int argMinInt, int argMaxInt) {
		
		// Check argument
		KLog.argException(argMinInt >= argMaxInt, "K.getRandomInt(): Minimum value must be smaller than maximum value");
		
		// Return random integer
	    return SIMPLE_RANDOMIZER.nextInt(argMaxInt - argMinInt + 1) + argMinInt;
	}
	
	/**
	 * Get application start time and date.
	 * 
	 * @deprecated Use K.START_TIME instead
	 * 
	 * @return Start date and time
	 * 
	 * @since 2024.05.24
	 */
	@Deprecated
	public static Calendar getStartTime() {
		return START_TIME;
	}
	
	/**
	 * Return current date and time in ISO 8601 format (Example: "2024-02-24T14:12:44.234").
	 * 
	 * @return	String	ISO 8601 date/time
	 */
	public static String getTimeISO8601() {
		
		// Return current date/time in ISO 8601 format (e.g. 2024-02-24T14:12:44.234)
		return getTimeISO8601(Calendar.getInstance());
	}

	/**
	 * Return date and time in ISO 8601 format (Example: "2024-02-24T14:12:44.234").<br>
	 * 
	 * @param	argDateTime	Date/time
	 * @return	String		ISO 8601 date/time
	 */
	public static String getTimeISO8601(Calendar argDateTime) {
		
		// Check argument
		KLog.argException(K.isEmpty(argDateTime), "K.getTimeISO8601(): argDateTime is required");
		
		// Return date/time in ISO 8601 format (e.g. 2024-02-24T14:12:44.234)
		return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d",
				argDateTime.get(Calendar.YEAR),
				argDateTime.get(Calendar.MONTH) + 1,
				argDateTime.get(Calendar.DAY_OF_MONTH),
				argDateTime.get(Calendar.HOUR_OF_DAY),
				argDateTime.get(Calendar.MINUTE),
				argDateTime.get(Calendar.SECOND),
				argDateTime.get(Calendar.MILLISECOND));
	}
	
	/**
	 * Return unique id (Example: 27F1E0F5-186F-48FF-BA46-10E6E4A0FAA0).
	 * 
	 * @return Unique id
	 * 
	 * @since 2024.05.24
	 */
	public static String getUniqueID() {
		return UUID.randomUUID().toString().toUpperCase();
	}
	
	/**
	 * Return offset from local time zone to UTC as string (Example +02:00).
	 * 
	 * @return String with zone offset
	 */
	public static String getUTCOffsetAsString() {
		
		// Get difference from UTC to local time zone
		int totalDifference = K.getUTCOffsetMin();
		
		StringBuilder utcString = new StringBuilder();
		
		// Set +/- UTC
		utcString.append((totalDifference >= 0) ? '+' : '-');
		
		// Set UTC offset hh:mm
		utcString.append(String.format("%02d:%02d", Math.abs(totalDifference) / 60, Math.abs(totalDifference) % 60));
		
		return utcString.toString();
	}
	
	/**
	 * Return difference in number of minutes between UTC and the local time zone.
	 *  
	 * @return Difference in minutes
	 */
	public static int getUTCOffsetMin() {
		return (Calendar.getInstance().getTimeZone().getOffset(new Date().getTime()) / 1_000 / 60);
	}
	
	/**
	 * Check if object is empty.
	 * 
	 * @param	argObject	Object to test for emptiness
	 * @return	True if object is empty (null or no elements), false otherwise
	 * 
	 * @since 2024.05.20
	 */
	public static boolean isEmpty(Object argObject) {
		
		// Check if object is null
		if (argObject == null) {
			return true;
		}
		
		// Check if String is empty
		if ((argObject instanceof String) && ((String) argObject).isEmpty()) {
			return true;
		}
		
	    // Check if Collection (List, Set, etc.) is empty
	    if (argObject instanceof Collection && ((Collection<?>) argObject).isEmpty()) {
	        return true;
	    }
	    
        // Check if Map (HashMap, Properties, etc.) is empty
        if (argObject instanceof Map && ((Map<?, ?>) argObject).isEmpty()) {
            return true;
        }
        
        // Check if array (including primitive arrays) is empty
        if (argObject.getClass().isArray() && Array.getLength(argObject) == 0) {
            return true;
        }
        
		// May add other object type checks in the future
		return false;
	}
	
	/**
	 * Check whether the passed string contains an integer.
	 * 
	 * @param	argString	String to be tested
	 * @return	True if number, false otherwise
	 * 
	 * @since 2024.08.26
	 */
	public static boolean isInteger(String argString) {
		return isInteger(argString, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}	
	
	/**
	 * Check whether the passed string contains an integer and is within the allowed range.
	 * 
	 * @param	argString	String to be tested
	 * @param	argMinimum	Minimum allowed value
	 * @param	argMaximum	Maximum allowed value
	 * @return	True if number, false otherwise
	 * 
	 * @since 2024.08.26
	 */
	public static boolean isInteger(String argString, int argMinimum, int argMaximum) {

		// Check arguments
		KLog.argException(argMaximum < argMinimum, "K.isInteger(): argMinimum must be smaller than argMaximum");
		
		if (K.isEmpty(argString)) {
			return false;
		}
		
		// Check if string is a valid integer
		// Note: Checking a number by rising Exception may seem inefficient, but Java is able to raise more than 1000 Exceptions within 1ms (MacBook Pro). 
		try {
			
			int intValue = Integer.parseInt(argString.trim());
			
			// Check if integer value is within allowed range
			if ((argMinimum != Integer.MIN_VALUE) && (intValue < argMinimum)) {
				return false;
			}
			
			if ((argMaximum != Integer.MAX_VALUE) && (intValue > argMaximum)) {
				return false;
			}
			
			return true;
			
		} catch(Exception e){  
		    return false;  
		} 
	}
	
	/**
	 * Check if a new version of the package ch.k43.util is available. The check is done by fetching
	 * https://andybrunner.github.io/Java-Utility-Package/version-check/version.txt.
	 * 
	 * @return true (new version available) or false (current version up-to-date or web site is not reachable).
	 * 
	 * @since 2024.12.23
	 */
	public static boolean isNewVersionAvailable() {
		
		String currentVersion = getCurrentVersionNumber();
		
		if (currentVersion == null) {
			return false;
		}
		
		KLog.debug("Current version: {}, active version: {}", currentVersion, K.VERSION);
		return !currentVersion.equals(K.VERSION);
	}
	
	/**
	 * Check whether the passed string contains a number.
	 * 
	 * @param	argString String to be tested
	 * @return	True if number, false otherwise
	 * 
	 * @since 2024.06.28
	 */
	public static boolean isNumber(String argString) {
		return (isNumber(argString, Double.MIN_VALUE, Double.MAX_VALUE));
	}
	
	/**
	 * Check whether the passed string contains a number and is within the allowed range.
	 * 
	 * @param	argString	String to be tested
	 * @param	argMinimum	Minimum allowed value
	 * @param	argMaximum	Maximum allowed value
	 * @return	True if number, false otherwise
	 * 
	 * @since 2024.09.12
	 */
	public static boolean isNumber(String argString, double argMinimum, double argMaximum) {
		
		// Check arguments
		KLog.argException(argMaximum < argMinimum, "K.isNumber(): argMinimum must be smaller than argMaximum");

		if (K.isEmpty(argString)) {
			return false;
		}
		
		// Check if string is a valid (double) number
		// Note: Checking a number by risking Exception may seem inefficient, but Java is able to raise more than 1000 Exceptions within 1ms (MacBook Pro). 
		try {
			
			double doubleValue = Double.parseDouble(argString.trim());
			
			// Check if double value is within allowed range
			if ((argMinimum != Double.MIN_VALUE) && (doubleValue < argMinimum)) {
				return false;
			}
			
			if ((argMaximum != Double.MAX_VALUE) && (doubleValue > argMaximum)) {
				return false;
			}
			
			return true;
			
		} catch(Exception e){  
		    return false;  
		} 
	}
	
	/**
	 * Dynamic load a Java class
	 * 
	 * @param	argClassName	Java class name, e.g ch.k43.util.KSocketServerSample
	 * @return	Class			Loaded Java class or null for errors
	 */
	public static Class<?> loadClass(String argClassName) {
		
		// Check argument
		KLog.argException(K.isEmpty(argClassName), "K.loadClass(): Class name is required");
		
		// Get class loader
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	    // Load user class
	    try {
	    	KLog.debug("Loading Java class {}", argClassName);
	        return classLoader.loadClass(argClassName);
	    } catch (Exception e) {
	        KLog.error("Unable to load class {}: {}", argClassName, e.toString());
	        return null;
	    }
	}
	
	/**
	 * Load Java KeyStore file.
	 * 
	 * @param argFileName	Java KeyStore file name (JKS)
	 * @param argPassword	KeyStore file password
	 * 
	 * @return KeyStore or null for errors
	 * 
	 * @since 2025.03.17
	 */
	public static KeyStore loadKeyStore(String argFileName, char[] argPassword) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argPassword), "K.loadKeyStore(): argFileName must not be empty");
		
	    KLog.debug("Loading Java KeyStore File {}", argFileName);
		
	    try (FileInputStream fis = new FileInputStream(argFileName)) {

	    	KeyStore keyStore = KeyStore.getInstance("JKS");
	        keyStore.load(fis, argPassword);
	        return keyStore;
	        
		} catch (Exception e) {
			KLog.error("Unable to load Java KeyStore File {}: {}", argFileName, e.toString());
			return null;
		}
	}
	
	/**
	 * Return DNS records for the specified record type.<br>
	 * 
	 * @param argDNSRecordType	DNS record type (MX, A, etc.)
	 * @param argMailDomain		Domain to query
	 * @return Array with matching DNS records
	 * 
	 * @since 2024.05.17
	 */
	public static String[] queryDNS(String argDNSRecordType, String argMailDomain) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argDNSRecordType), "K.queryDNS(): DNS RR type is mandatory");
		KLog.argException(K.isEmpty(argMailDomain), "K.queryDNS(): DNS domain name is mandatory");
		
		// Declarations
        String[]	dnsRecords	= null;
        String		dnsRRType	= argDNSRecordType.toUpperCase();
	
		// Get domain name if complete email address specified
		String[] 	mailParts	= argMailDomain.split("@");
		String 		domainName	= mailParts[mailParts.length - 1];
		
		// 2024.05.22
		try {
			
			// Set up directory context
			InitialDirContext iDirC = new InitialDirContext();

			// Get DNS records for the passed type
			Attributes	dnsAttributes	= iDirC.getAttributes("dns:/" + domainName, new String[] {dnsRRType});
			Attribute	dnsRRs			= dnsAttributes.get(dnsRRType);
			
	        // MX query: Return the domain name if no MX records is found (RFC 974)
	        if ((dnsRRType.equals("MX")) && (dnsRRs == null)) {
	            return new String[] {domainName};
	        }
			
	        // Return null if no RRs found
	        if (dnsRRs == null) {
	        	return new String[] {};
	        }
	        
	        // Save DNS RRs in string array 
	        dnsRecords = new String[dnsRRs.size()];
	        
	        for (int index = 0; index < dnsRRs.size(); index++) {

	        	dnsRecords[index] = (String) dnsRRs.get(index);

	        	// Strip trailing dots
	        	if (dnsRecords[index].endsWith(".")) {
					dnsRecords[index] = dnsRecords[index].substring(0, dnsRecords[index].length() - 1);
	        	}
	        }
			
	        KLog.debug("{} query for DNS {}: {}", dnsRRType, domainName, Arrays.toString(dnsRecords));

	        // MX query: Sort records by priority
			if (dnsRRType.equals("MX")) {

				// Step 1: Format MX records for sorting, e.g. "10 smtp.acme.com" to "0000000010 smtp.acme.com" and strip trailing dot
		        for (int index = 0; index < dnsRecords.length; index++) {
					String[] mxSplitter = dnsRecords[index].split(" ");
					dnsRecords[index] = String.format("%010d %s", Integer.parseInt(mxSplitter[0]), mxSplitter[1]);
			    }
		        
		        // Step 2: Sort the array
				Arrays.sort(dnsRecords);
				
				// Step 3: Remove the priority fields
				for (int index = 0; index < dnsRecords.length; index++) {
					dnsRecords[index] = dnsRecords[index].split(" ")[1];
				}
			}
							
		} catch (Exception e) {
			KLog.error("Unable to query DNS for type {}: {}", dnsRRType, e.toString());
        	return new String[] {};
		}

		// Return result as array
		return dnsRecords;
	}
	
	/**
	 * Repeat the given character.
	 * 
	 * @param argCharacter	Character to repeat
	 * @param argCount		Number to repeat
	 * @return	Resulting string
	 * 
	 * @since 2025.04.19
	 */
	public static String repeat(char argCharacter, int argCount) {
		return repeat("" + argCharacter, argCount);
	}

	/**
	 * Repeat the given string.
	 * 
	 * @param argString	String to repeat
	 * @param argCount	Number to repeat
	 * @return	Resulting string
	 * 
	 * @since 2025.04.19
	 */
	public static String repeat(String argString, int argCount) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argString), "K.repeat(): argString must not be empty");
		KLog.argException(argCount < 1 || argCount > 1000000, "K.repeat(): argCount must be between 1 and 1000000");
		
		StringBuilder newString = new StringBuilder(argString.length() * argCount);

	    for (int i = 0; i < argCount; i++) {
	    	newString.append(argString);
	    }
	    return newString.toString();
	}
	
	/**
	 * Replace parameter holders "{}" with the passed arguments.
	 * 
	 * @param	argData		Message text
	 * @param	argObjects	Data to be inserted
	 * 
	 * @return	Formatted string
	 * 
	 * @since 2025.01.20
	 */
	public static String replaceParams(String argData, Object... argObjects) {
		
		// Check if nothing to do
		if (K.isEmpty(argData)) {
			return "";
		}
		
		if (argObjects.length == 0) {
			return argData;
		}
		
		// Replace parameter holders, e.g. KLog.info("Program {} started at {}", "Test", new Date())
		StringBuilder	result				= new StringBuilder(argData);
		int				placeholderIndex	= 0;
				
		for (Object argObject : argObjects) {

			// Find the next placeholder after the current position
			placeholderIndex = result.indexOf("{}", placeholderIndex);

			if (placeholderIndex == -1) {
				// No more place holders found
				break;
			} else {
				// Replace {} with the passed argument
				String replacement = (argObject == null) ? "null" : argObject.toString();
				result.replace(placeholderIndex, placeholderIndex + 2, replacement);
				placeholderIndex += replacement.length();
			}
		}

		return result.toString();
	}
	
	/**
     * Return rounded value.
     * 
	 * @param	argValue		Value to be rounded
	 * @param	argPrecision	Number of decimal digits
	 * @return	double			Rounded value 
	 * 
	 * @since 2025.08.10
     */
	public static double round(double argValue, int argPrecision) {

		double scale = Math.pow(10, argPrecision);
		
    	return Math.round(argValue * scale) / scale;
    }
	
	/**
     * Return rounded value according to the "Swiss Rounding Rule 5+".
     * 
     * Note: The calculation is equivalent to Math.round(value * 20.0) / 20.0 and is mainly used by Swiss financial applications.<br> 
     * 
	 * Examples:<br>
	 * - Values between 0.0000 and 0.0249 are rounded down to 0.00<br>
	 * - Values between 0.0250 and 0.0749 are rounded to 0.05<br>
	 * - Values between 0.0750 and 0.0999 are rounded up to 0.10<br>
	 * 
	 * @param	argValue	Value to be rounded
	 * @return	double		Rounded value 
     */
	public static double roundSwiss(double argValue) {
    	return Math.round(argValue * 20.0) / 20.0;
    }
	
	/**
	 * Manually run garbage collector of Java virtual machine.
	 * 
	 * Note: The manual execution of the garbage collector is not needed under normal operation.
	 *  
	 * @return Number of bytes reclaimed
	 */
	public static long runGC() {

		// Run JVM garbage collector
		long heapFreeKBStart = K.getJVMMemStats()[3];
		System.gc();
		long heapFreeKBStop = K.getJVMMemStats()[3];
		
		KLog.debug("Java free heap size {} (reclaimed {})", K.formatBytes(heapFreeKBStop), K.formatBytes((heapFreeKBStop - heapFreeKBStart)));
		
		return heapFreeKBStop - heapFreeKBStart;
	}
	
	/**
	 * Save error message.
	 * 
	 * @param argMessage Error Message
	 * 
	 * @see KLocalData
	 * @see getLastError()
	 * @see getLastErrors()
	 * 
	 * @since 2025.02.02
	 */
	public static synchronized void saveError(String argMessage) {
		
		// Ignore empty message
		if (K.isEmpty(argMessage)) {
			return;
		}
		
		KLocalData localData = K.getLocalData();
			
		// Delete oldest error message if maximum number of errors reached
		if (localData.kLastErrors.size() == MAX_SAVED_ERRORS) {
			localData.kLastErrors.removeLast();
		}
			
		localData.kLastErrors.add(0, argMessage);
	}
	
	/**
	 * Return base64 encoded and serialized object.
	 * 
	 * @param	argObject	Object to serialize and encode
	 * @return	Base64 string
	 * 
	 * @since 2024.08.22
	 */
	public static String serialize(Serializable argObject) {
		
        ByteArrayOutputStream	byteOutputStream	= new ByteArrayOutputStream();
        
        try (ObjectOutputStream objectOutputStream	= new ObjectOutputStream(byteOutputStream)) {
        	objectOutputStream.writeObject(argObject);
        } catch (Exception e) {
			KLog.error("Unable to serialize object: {}", e.toString());
			return "";
        }

        // Base64 encode serialized object
        String encodedString = Base64.getEncoder().encodeToString(byteOutputStream.toByteArray());
        
        KLog.debug("Serialize and encode object {} successful ({})", argObject.getClass().getName(), K.formatBytes(encodedString.length()));
        
        return encodedString;
	}
	
	/**
	 * Signal interrupt to thread.
	 * 
	 * @param argThread		Thread to interrupt for termination
	 * @return 				True for success, false otherwise
	 * 
	 * @since 2024.08.29
	 */
	public static boolean stopThread(Thread argThread) {
		return stopThread(argThread, 0);
	}
	
	/**
	 * Signal interrupt to thread and wait for its termination.
	 * 
	 * @param argThread		Thread to terminate
	 * @param argTimeOutSec	Number of seconds to wait for thread termination (0 to 60)
	 * @return 				True for success, false otherwise
	 * 
	 * @since 2024.08.29
	 */
	public static boolean stopThread(Thread argThread, int argTimeOutSec) {
		
		// Static declarations
		final int WAIT_TIME_MS = 100;
		
		// Check arguments
		KLog.argException(argTimeOutSec < 0 || argTimeOutSec > 60, "K.stopThread(): argTimeOutSec must be between 0 and 60");
		
		// Check if thread is running
		if ((argThread == null) || (!argThread.isAlive())) {
			return false;
		}

		// Get name of thread
		String threadName = argThread.getName();
		
		// Try to terminate thread
		if (argThread instanceof KThread) {
			KLog.debug("Calling {}.kStop()", threadName);
			((KThread) argThread).kStop();
		} else {
			KLog.debug("Sending interrupt to thread {}", threadName);
			argThread.interrupt();
		}

		// Wait for the thread to terminate
		int timeOutMs = argTimeOutSec * 1_000;
		
		while (argThread.isAlive()) {
			
			// Check for timeout
			if (timeOutMs <= 0) {
				break;
			}
		
			// Wait some time
			K.waitMilliseconds(WAIT_TIME_MS);
			timeOutMs -= WAIT_TIME_MS;
		}

		// Return to caller
		if (argThread.isAlive()) {
			KLog.debug("Thread {} did not terminate", threadName);
			return false;
		} else {
			KLog.debug("Thread {} stopped", threadName);
			return true;
		}
	}
	
	/**
	 * Format byte array as hexadecimal string.
	 * 
	 * @param	argBytes	Byte array
	 * @return	Hexadecimal string
	 * 
	 * @since 2024.05.24
	 */
	public static String toHex(byte[] argBytes) {

		// Check argument
		if (K.isEmpty(argBytes)) {
			return "";
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		
		for (byte singleByte : argBytes) {
			stringBuilder.append(String.format("%02X", singleByte));
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * Format char array as hexadecimal string representation.
	 * 
	 * @param	argChars	Character array to be formatted
	 * @return	Hexadecimal string
	 * 
	 * @since 2025.04.21
	 */
	public static String toHex(char[] argChars) {

		// Check argument
		if (K.isEmpty(argChars)) {
			return "";
		}
		
		return toHex(new String(argChars).getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Format string as hexadecimal string representation.
	 * 
	 * @param	argString	String to be formatted
	 * @return	Hexadecimal string
	 * 
	 * @since 2024.05.24
	 */
	public static String toHex(String argString) {

		// Check argument
		if (K.isEmpty(argString)) {
			return "";
		}
		
		return toHex(argString.getBytes(StandardCharsets.UTF_8));
	}
	
	
	/**
	 * Return PEM formatted string from the passed object. Supported objects are PrivateKey, PublicKey, Certificate, KeyPair and KeyStore. 
	 * 
	 * @param 	argObject Object to be formatted
	 * @return	Formatted PEM string or empty string for errors
	 * 
	 * @since 2025.03.16
	 */
	public static String toPEM(Object argObject) {
		return toPEM(argObject, null, false);
	}
	
	/**
	 * Return PEM formatted string from the passed object. Supported objects are PrivateKey, PublicKey, Certificate, KeyPair and KeyStore. 
	 * 
	 * @param 	argObject Object to be formatted
	 * @param	argComments	Add description to PEM
	 * @return	Formatted PEM string or empty string for errors
	 * 
	 * @since 2025.03.22
	 */
	public static String toPEM(Object argObject, boolean argComments) {
		return toPEM(argObject, null, argComments);
	}
	
	/**
	 * Return PEM formatted string from the passed object. Supported objects are PrivateKey, PublicKey, Certificate, KeyPair and KeyStore.  
	 * 
	 * @param 	argObject Object to be formatted
	 * @param	argPassword	Password for protected objects or null
	 * @return	Formatted PEM string or empty string for errors
	 * 
	 * @since 2025.03.16
	 */
	public static String toPEM(Object argObject, char[] argPassword) {
		return toPEM(argObject, argPassword, false);
	}
	
	/**
	 * Return PEM formatted string from the passed object. Supported objects are PrivateKey, PublicKey, Certificate, KeyPair and KeyStore.  
	 * 
	 * @param 	argObject Object to be formatted
	 * @param	argPassword	Password for protected objects or null
	 * @param	argComments	Add description to PEM
	 * @return	Formatted PEM string or empty string for errors
	 * 
	 * @since 2025.03.22
	 */
	@SuppressWarnings({ "deprecation"})
	public static String toPEM(Object argObject, char[] argPassword, boolean argComments) {
		
		// Check argument
		KLog.argException(argObject == null, "K.toPEM(): argObject is required");
		
    	StringBuilder	pemString		= new StringBuilder();
		String			generatorString	= "# PEM-Created: " + K.getTimeISO8601() + " (" + K.class.getName() + '/' + K.VERSION + ")\n";
		String			pemLabel		= null;
		String			pemDescription	= "n/a";
		byte[]			pemEncoded		= null;
		
		// Format PEM according to passed object type
		try {
			//
			// Private Key
			//
	        if (argObject instanceof PrivateKey) {
	        	
	        	PrivateKey privateKey = (PrivateKey) argObject;
	        	
	            pemLabel		= "Private key";
	            pemDescription	= privateKey.getAlgorithm() + "-Algorithm";
	            pemEncoded		= privateKey.getEncoded();
	            
	            if (argComments) {
		        	pemString.append("# Private-Key: ").append(pemDescription).append('\n');
	        	   	pemString.append(generatorString);
	            }
	        //
	        // Public Key
	        //
			} else if (argObject instanceof PublicKey) {
	        	
	        	PublicKey publicKey = (PublicKey) argObject;

	            pemLabel		= "Public key";
	            pemDescription	= publicKey.getAlgorithm() + "-Algorithm";
	            pemEncoded		= publicKey.getEncoded();

	            if (argComments) {
		        	pemString.append("# Public-Key: ").append(pemDescription).append('\n');
		        	pemString.append(generatorString);
	            }
	        //
	        // Certificate
	        //
			} else if (argObject instanceof Certificate) {
	        	
	        	Certificate certificate = (Certificate) argObject;
	        	pemLabel		= "Certificate";
	            pemEncoded		= certificate.getEncoded();

	            if (argObject instanceof X509Certificate) {
	            	
	            	// Format certificate details
	            	X509Certificate x509Certificate = (X509Certificate) argObject;
	            	pemDescription = x509Certificate.getSubjectDN().toString();
	            	
	            	if (argComments) {

			        	pemString.append("# Certificate: ").append(pemDescription).append('\n');
			        	pemString.append("# Issued-By: ").append(x509Certificate.getIssuerDN()).append('\n');		        	

			        	// Is Root CA?
			        	boolean rootCA = (x509Certificate.getBasicConstraints() != -1);
			        	
			        	// Is Self-signed?
			        	boolean selfSigned = x509Certificate.getSubjectDN().toString().equals(x509Certificate.getIssuerDN().toString());
			        	
			        	pemString.append("# Type: ");
			        	if (rootCA && selfSigned) {
			        		pemString.append("Self-Signed Root");
			        	}
			        	if (rootCA && !selfSigned) {
			        		pemString.append("Intermediate");
			        	}
			        	if (!rootCA) {
			        		pemString.append("Leaf");
			        	}
			        	pemString.append(" Certificate\n");
			        	
			        	pemString.append("# Algorithm: ").append(x509Certificate.getSigAlgName()).append('\n');
			        	pemString.append("# Serial-Number: ").append(x509Certificate.getSerialNumber()).append('\n');
			        	
			        	// Format dates as ISO 8601
			        	Calendar fromDate = Calendar.getInstance();
			        	fromDate.setTime(x509Certificate.getNotBefore());
			        	Calendar toDate = Calendar.getInstance();
			        	toDate.setTime(x509Certificate.getNotAfter());
			        	pemString.append("# Valid: ").append(K.getTimeISO8601(fromDate)).append(" - ").append(K.getTimeISO8601(toDate)).append('\n');
			        	pemString.append(generatorString);
	            	}
	            }
	        //
	        // Private and Public Key pair
	        //
			} else if (argObject instanceof KeyPair) {

	        	// Format private and public keys
	        	PrivateKey privateKey = ((KeyPair) argObject).getPrivate();
	        	pemString.append(toPEM(privateKey, argPassword, argComments));

	        	PublicKey publicKey = ((KeyPair) argObject).getPublic();
	        	pemString.append(toPEM(publicKey, argPassword, argComments));
	        	
	        	return pemString.toString();
	        //
	        // KeyFile
	        //
			} else if (argObject instanceof KeyStore) {
	        	
	        	// Loop thru all aliases
	        	KeyStore			keyStore	= (KeyStore) argObject;
	        	Enumeration<String>	aliases		= keyStore.aliases();
	        	
	            while (aliases.hasMoreElements()) {
	                
	            	String alias = aliases.nextElement();

	            	// Private key entry
	                if (keyStore.isKeyEntry(alias)) {
	                	Key key = keyStore.getKey(alias, argPassword);

	                	// Private key
	                	if (key instanceof PrivateKey) {
	                	    pemString.append(K.toPEM(key, argPassword, argComments));
	                	    
	                	    // Certificates
	                        Certificate[] certChain = keyStore.getCertificateChain(alias);
	                        if (certChain != null) {
	                            for (Certificate cert : certChain) {
	    	                	    pemString.append(K.toPEM(cert, argPassword, argComments));
	                            }
	                        }
	                	}
	                }

	                // Certificate key entry
	                if (keyStore.isCertificateEntry(alias)) {
	                	pemString.append(K.toPEM(keyStore.getCertificate(alias), argPassword, argComments));
	                }
	            }
	        	
	        	return pemString.toString();
	        } else {
	            KLog.argException("K.toPEM(): Unsupported object type {}", argObject.getClass().getName());
	            return "";
	        }
		
		} catch (Exception e) {
			KLog.error("Unable to encode PEM from object type {}: {}", argObject.getClass().getName(), e.toString());
			return "";
		}
				
	    // Base64 encode and format lines with maximum of 64 characters
	    String encodedString = K.encodeBase64(pemEncoded);
	    String formattedBase64 = String.join("\n", encodedString.split("(?<=\\G.{64})"));

	    // Construct PEM string
	    pemString.append("-----BEGIN ").append(pemLabel.toUpperCase()).append("-----\n")
	        .append(formattedBase64).append("\n")
	        .append("-----END ").append(pemLabel.toUpperCase()).append("-----\n");
	    
	    KLog.debug("{} formatted: {}", pemLabel, pemDescription);
	    
	    return pemString.toString();
	}
	
	/**
	 * Truncate string by replacing characters from the middle with "...".
	 * 
	 * @param	argString		String to be shortened
	 * @param	argMaxLength	Maximum string length
	 * @return	Resulting string or original string if invalid parameters were given
	 * 
	 * @since 2025.04.17
	 */
	public static String truncateMiddle(String argString, int argMaxLength) {
		return truncateMiddle(argString, argMaxLength, "...");
	}
	
	/**
	 * Truncate string by replacing characters from the middle.
	 * 
	 * @param	argString		String to be shortened
	 * @param	argMaxLength	Maximum string length
	 * @param	argEllipsis		Replacement string
	 * @return	Resulting string
	 * 
	 * @since 2025.04.17
	 */
	public static String truncateMiddle(String argString, int argMaxLength, String argEllipsis) {
		
		// Check parameters
		KLog.argException(K.isEmpty(argString), "K.truncateMiddle(): argString must not be empty");
		KLog.argException(K.isEmpty(argEllipsis), "K.truncateMiddle(): argEllipsis must not be empty");
		KLog.argException(argMaxLength < 1 || argMaxLength < (argEllipsis.length() + 2), "K.truncateMiddle(): argMaxLength too small for argEllipsis");
		
		// Check if string does not needed to be shortened
		if (argString.length() <= argMaxLength) {
			return argString;
		}
		
		int stringLength	= argString.length();
        int ellipsisLength	= argEllipsis.length();
        int partLength		= (argMaxLength - ellipsisLength) / 2;
        
        // Add 1 extra character to the start part if maximum string length is uneven
        int adjustLength = (argMaxLength - ellipsisLength) % 2;
                        
        String start	= argString.substring(0, partLength + adjustLength);
        String end		= argString.substring(stringLength - partLength);

        return start + argEllipsis + end;
	}
	
	/**
	 *	Waits the specified time.
	 *
	 *	@param	argHours				Number of hours to wait
	 */
	public static void waitHours(int argHours) {
		waitThread(argHours * 3_600_000L);
	}
	
	/**
	 *	Waits the specified time.
	 *
	 *	@param	argMilliseconds		Number of milliseconds to wait
	 */
	public static void waitMilliseconds(int argMilliseconds) {
		waitThread(argMilliseconds);
	}
		
	/**
	 *	Waits the specified time.
	 *
	 *	@param	argMinutes				Number of minutes to wait
	 */
	public static void waitMinutes(int argMinutes) {
		waitThread(argMinutes * 60_000L);
	}
	
	/**
	 *	Waits the specified time.
	 *
	 *	@param	argSeconds				Number of seconds to wait
	 */
	public static void waitSeconds(int argSeconds) {
		waitThread(argSeconds * 1_000L);
	}
    
	/**
	 *	Waits the specified time.
	 *
	 *	@param	argMilliseconds		Number of milliseconds to wait
	 */
	private static void waitThread(long argMilliseconds) {
	
		// Check arguments
		if (argMilliseconds <= 0) {
			return;
		}
		
		// Delay the current thread
		try	{
			Thread.sleep(argMilliseconds);
		} catch (InterruptedException e) {
			// Log error and continue
			KLog.error(e.toString());
			Thread.currentThread().interrupt();
		}
	}
    
    /**
	 * Private constructor to prevent class instantiation
	 */
	private K() {
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "K []";
	}
}
