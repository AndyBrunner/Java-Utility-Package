package ch.k43.util;

import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Format logging output as single CSV line with UUID, time stamp, logging level, code location and the logging information. 
 * The KLog.debug(), KLog.info() and KLog.error() adds the code location, delimited by KLog.DELIMITER which is then formatted here.
 * 
 * <pre>
 * Example:
 * KLog.info("Program start");
 * 
 * Output:
 * dd5ff181-34bb-4f34-982d-65c8b4a0f264,2024-06-21T18:16:49.996,Information,main[1]:Test:main:21,Program start
 * </pre>
 * 
 * The following properties are supported:
 * <pre>
 * ch.k43.util.KLogCSVFormatter.writeheader = true/false	
 * </pre>
 */
public class KLogCSVFormatter extends Formatter {

	// Class variables
	private String	gLastTraceLocation	= "N/A";
	private	char	gDelimiter			= ',';
	private	boolean	gWriteHeader		= true;
	
	/**
	 * Class constructor.
	 */
	public KLogCSVFormatter() {
		super();
	}
	
	/**
	 * Format the log message
	 */
	@Override
	public String format(LogRecord argRecord) {
		
		// Declarations
		StringBuilder	logString		= new StringBuilder();

		// Append UUID
		logString.append(K.encodeCSV(K.getUniqueID()) + gDelimiter);

		// Append time stamp
		logString.append(K.encodeCSV(K.getTimeISO8601()) + gDelimiter);
		
		// Append logging level
		switch (argRecord.getLevel().toString()) {
		
			case "FINEST":
				logString.append(K.encodeCSV("Debug"));
				break;
			case "INFO":
				logString.append(K.encodeCSV("Information"));
				break;
			case "SEVERE":
				logString.append(K.encodeCSV("Error"));
				break;
			default:
				logString.append(K.encodeCSV("N/A"));
				break;
		}
		logString.append(gDelimiter);
	
		// Get the passed message and split it into code location and text (delimited by KLog.DELIMITER)
		String	traceLocation	= null;
		String	traceMessage	= argRecord.getMessage();
		int		posDelimiter	= traceMessage.indexOf(KLog.DELIMITER);
		
		if (posDelimiter != -1) {
			traceLocation	= traceMessage.substring(0, posDelimiter);
			traceMessage	= traceMessage.substring(posDelimiter + 3);
			// Save last trace location in case the next entry has none
			gLastTraceLocation	= traceLocation; 
		} else {
			// Use last trace location if none was found
			traceLocation	= gLastTraceLocation;
		}

		// Append code location
		logString.append(K.encodeCSV(traceLocation) + gDelimiter);

		// Append log message
		logString.append(K.encodeCSV(traceMessage));
		
		// End the CSV line
		logString.append(K.LINE_SEPARATOR);
		
		// Return formatted message
		return (logString.toString());
	}

	/**
	 * Set CSV header.
	 */
	@Override
	public String getHead(Handler argHandler) {

		// Read properties
		Properties logProps = KFile.readPropertiesFile(KLog.PROPERTY_FILE);
		
		if (logProps == null) {
			return ("");
		}
		
		String writeHeader = logProps.getProperty(this.getClass().getName() + ".writeheader", "true").trim();
		
		if (writeHeader.equalsIgnoreCase("false")) {
			gWriteHeader = false;
		}
		
		// Check if CSV header line should be written
		if (!gWriteHeader) {
			return ("");
		}
		
		// Set CSV header
		return (K.encodeCSV("UUID") + ',' +
				K.encodeCSV("LogTime") + ',' +
				K.encodeCSV("Level") + ',' +
				K.encodeCSV("Location") + ',' +
				K.encodeCSV("Text") + K.LINE_SEPARATOR);				
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogCSVFormatter [gLastTraceLocation=" + gLastTraceLocation + 
				", gDelimiter=" + gDelimiter + ", gWriteHeader=" + gWriteHeader + "]";
	}
}
