package ch.k43.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import org.json.JSONObject;

/**
 * Static class with a collection of file utility methods.
 */
public class KFile {
	
	// Class variables
	private	static final int	BUFFER_SIZE			= 16384;	
	
	/**
	 * Delete a file or directory.<br>
	 * 
	 * @param	argFileName	File or directory name
	 * @return	true if file or directory could be deleted, false otherwise
	 */
	public static boolean delete(String argFileName) {
		
		// Check if file exists
		if (K.isEmpty(argFileName)) {
			return (false);
		}
		
		// Access file or directory
		File file = new File(argFileName);

		try {
			// Delete the file or directory
			if (!file.exists()) {
				return (false);
			}
			return (file.delete());
			
		} catch (Exception e) {
	    	KLog.error("Unable to delete file {}: {}", argFileName, e.toString());
			return (false);
		}
	}
	
	/**
	 * Check if file exists.<br>
	 * 
	 * @param	argFileName	File or directory name
	 * @return	true if file or directory exist, false otherwise
	 */
	public static boolean exists(String argFileName) {
		
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (false);
		}
		
		// Access file or directory
		File file = new File(argFileName);

		// Test if file exists
		try {
			return (file.exists());
		} catch (Exception e) {
	    	KLog.error("Unable to test existence of file {}: {}", argFileName, e.toString());
			return (false);
		}
	}
	
	/**
	 * Return size of file.<br>
	 * 
	 * @param	argFileName	File name
	 * @return	Size of file or -1 if not found
	 */
	public static long getSize(String argFileName) {
		
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (-1L);
		}
		
		// Access file
		File file = new File(argFileName);
		
		try {
			// Return file size
			if (!file.exists()) {
				return (-1L);
			}
			return (file.length());
			
		} catch (Exception e) {
	    	KLog.error("Unable to get size of file {}: {}", argFileName, e.toString());
			return (-1L);
		}
	}
	
	/**
	 * Read file into byte array.<br>
	 * 
	 * @param	argFileName	File name
	 * @return	Byte array with file content or null for errors
	 */
	public static byte[] readByteFile(String argFileName) {
				
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (null);
		}
		
		// Read file into buffer
		try (FileInputStream fileInputStream = new FileInputStream(argFileName)) {

			ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

		    int		readCount	= 0;
		    byte[]	data		= new byte[BUFFER_SIZE];

		    while ((readCount = fileInputStream.read(data, 0, data.length)) != -1) {
		        dataBuffer.write(data, 0, readCount);
		    }
		    dataBuffer.flush();

		    KLog.debug("File {} read ({})", argFileName, K.formatBytes(dataBuffer.size()));
			    
		    // Return byte array
		    return (dataBuffer.toByteArray());
			
		} catch (Exception e) {
			KLog.error("Unable to read file {}: {}", argFileName, e.toString());
			return (null);
		}
	}
	
	
	/**
	 * Read JSON file and parse content into org.json.JSONObject.<p>
	 * 
	 * Note: The JSON-Java (org.json) library must be included at runtime.
	 *  
	 * @param	argFileName			File or directory name
	 * @return	JSON object with parsed JSON or null for errors
	 */
	public static org.json.JSONObject readJSONFile(String argFileName) {
		
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (null);
		}
		
		// Read the file into String
	    String fileContent = KFile.readStringFile(argFileName);

	    if (K.isEmpty(fileContent)) {
	    	return (null);
	    }
	    
	    // Parse string into JSON object
	    JSONObject jsonObject = null;
	    
	    try {
		
	    	jsonObject = new JSONObject(fileContent);
		    KLog.debug("JSON object created from file {} ({} keys)", argFileName, jsonObject.length());

	    } catch (Exception e) {
	    	KLog.error("Unable to create JSON object from file {}: {}", argFileName, e.toString());
	    	return (null);
	    }

	    // Return the JSON object
	    return (jsonObject);
	}
	
	/**
	 * Read properties file.<br>
	 * 
	 * @param	argFileName			File or directory name
	 * @return	Properties object or null for errors
	 * 
	 * @since 2024.05.17
	 */
	public static Properties readPropertiesFile(String argFileName) {
		
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (null);
		}
			    
	    // Read properties file
    	Properties properties = new Properties();
	    
	    try (FileInputStream inputFile = new FileInputStream(argFileName)) {

	    	properties.load(inputFile);
	        KLog.debug("Properties file {} read ({} keys)", argFileName, properties.size());
		    return (properties);
		    
	    } catch (Exception e) {
	    	KLog.error("Unable to read properties file {}: {}", argFileName, e.toString());
	    	return (null);
	    }
	}
	
	/**
	 * Read file into string.<br>
	 * 
	 * @param	argFileName	File name
	 * @return	String with file content or null for errors
	 */
	public static String readStringFile(String argFileName) {
		
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (null);
		}
		
		// Read the file into String
	    StringBuilder stringBuilder	= new StringBuilder();
	    
		try (BufferedReader reader = new BufferedReader(new FileReader (new File(argFileName)))) {

			String line				= null;
		    
	        while((line = reader.readLine()) != null) {
	            stringBuilder.append(line);
	            stringBuilder.append(K.LINE_SEPARATOR);
	        }
	        
	        KLog.debug("File {} read ({})", argFileName, K.formatBytes(stringBuilder.length()));
	        
		} catch (Exception e) {
			KLog.error("Unable to read file {}: {}", argFileName, e.toString());
			return (null);
		}
		
		// Return string
		return (stringBuilder.toString());
	}

	/**
	 * Rename a file or directory.<br>
	 * 
	 * @param	argOldFileName	File or directory name
	 * @param	argNewFileName	File or directory name
	 * @return	True if file or directory could be renamed, false otherwise
	 */
	public static boolean rename(String argOldFileName, String argNewFileName) {
		
		// Check arguments
		if (K.isEmpty(argOldFileName) || K.isEmpty(argNewFileName)) {
			return (false);
		}
		
		// Access files or directories
		File oldFile = new File(argOldFileName);
		File newFile = new File(argNewFileName);
				
		try {
			// Rename the file or directory
			if (!oldFile.exists()) {
				return (false);
			}
			if (newFile.exists()) {
				return (false);
			}
			
			return (oldFile.renameTo(newFile));
			
		} catch (Exception e) {
	    	KLog.error("Unable to rename file {} to {}: {}", argOldFileName, argNewFileName, e.toString());
			return (false);
		}
	}
	
	/**
	 * Write bytes to file.<br>
	 * 
	 * @param	argFileName	File name
	 * @param	argBuffer	Byte array to be written
	 * @return	True if successful, false otherwise
	 */
	public static boolean writeFile(byte[] argBuffer, String argFileName) {
				
		// Check argument
		if (K.isEmpty(argFileName)) {
			return (false);
		}
		
		// Read file into buffer
		try (FileOutputStream fileOutputStream = new FileOutputStream(argFileName)) {

			fileOutputStream.write(argBuffer);
			KLog.debug("File {} written ({})", argFileName, K.formatBytes(argBuffer.length));
			    
		    // Return
		    return (true);
			
		} catch (Exception e) {
			KLog.error("Unable to write file {}: {}", argFileName, e.toString());
			return (false);
		}
	}
	
	/**
	 * Write JSON object to file.<p>
	 * 
	 * Note: The JSON-Java (org.json) library must be included at runtime.
	 *  
	 * @param	argJSONObject	org.json.JSONObject to be written out
	 * @param	argFileName		File name
	 * @return	True if successful, false otherwise
	 */
	public static boolean writeFile(JSONObject argJSONObject, String argFileName) {
		
		// Check argument
		if (K.isEmpty(argJSONObject) || K.isEmpty(argFileName)) {
			return (false);
		}
		
		// Write out JSON object to string
		String jsonString = null;
		
		try {
			jsonString = argJSONObject.toString(2);
			KLog.debug("JSON object serialized ({})", K.formatBytes(jsonString.length()));
		} catch (Exception e) {
	    	KLog.error("Unable to serialize JSON object: {}", e.toString());
	    	return (false);
		}
		
	    // Write the string to file
		if (!KFile.writeFile(jsonString, argFileName)) {
			return (false);
		}
	  
	    // Return
	    return (true);
	}
	
	/**
	 * Write string to file.<br>
	 * 
	 * @param	argString	String to be written to file
	 * @param	argFileName	File name
	 * @return	True if successful, false otherwise
	 */
	public static boolean writeFile(String argString, String argFileName) {
		
		// Check arguments
		if (K.isEmpty(argString) || K.isEmpty(argFileName)) {
			return (false);
		}
		
		// Write the file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(argFileName))) {
			writer.write(argString);
		} catch (Exception e) {
			KLog.error("Unable to write file {}: {}", argFileName, e.toString());
			return (false);
		}
		
        KLog.debug("File {} written ({})", argFileName, K.formatBytes(KFile.getSize(argFileName)));

		// Return string
		return (true);
	}
	
	/**
	 * Write properties file.<br>
	 *
 	 * @param	argProperties		Properties to be written
	 * @param	argFileName			File or directory name
	 * @return	true if successful, false otherwise
	 * 
	 * @since 2024.05.17
	 */
	public static boolean writePropertiesFile(Properties argProperties, String argFileName) {
		
		// Check argument
		if (K.isEmpty(argProperties) || K.isEmpty(argFileName)) {
			return (false);
		}
			    
	    try (FileOutputStream outputFile = new FileOutputStream(argFileName)) {

	    	argProperties.store(outputFile, null);
	        KLog.debug("Properties file {} written ({} keys)", argFileName, argProperties.size());
		    return (true);
	    	
	    } catch (Exception e) {
	    	KLog.error("Unable to read properties file {}: {}", argFileName, e.toString());
	    	return (false);
	    }
	}
	
	/**
	 *  Private constructor to prevent class instantiation.
	 */
	private KFile() {
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KFile []";
	}
}
