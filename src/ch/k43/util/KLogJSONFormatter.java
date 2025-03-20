package ch.k43.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Format logging output as JSON formatted string with UUID, time stamp, logging level, code location and the logging information. 
 * The KLog.debug(), KLog.info() and KLog.error() adds the code location, delimited by KLog.DELIMITER which is then formatted here.
 * 
 * <pre>
 * Example:
 * KLog.info("Program start");
 * 
 * Output:
 * {
 *   "KLog": [
 *     {
 *       "UUID": "dd5ff181-34bb-4f34-982d-65c8b4a0f264",
 *       "Logtime": "2024-06-21T18:32:54.958",
 *       "Level": "Information",
 *       "Location": "main[1]:Test:main:21",
 *       "Text": "Program start"
 *     }
 *   ]
 * }
 * </pre>
 */
public class KLogJSONFormatter extends Formatter {

	// Class variables
	private static final String	INDENT_LEVEL_1		= "  ";
	private static final String	INDENT_LEVEL_2		= "    ";
	private static final String	INDENT_LEVEL_3		= "      ";
	
	private String				gLastTraceLocation	= "N/A";
	private	boolean				gFirstRecord		= true;
	
	/**
	 * Class constructor.
	 */
	public KLogJSONFormatter() {
		super();
	}

	/**
	 * Format the log message
	 */
	@Override
	public String format(LogRecord argRecord) {
		
		// Declarations
		StringBuilder	logString		= new StringBuilder();

		// Add the continuation string for the previous JSON array entry if necessary
		if (gFirstRecord) {
			gFirstRecord = false;
		} else {
			logString.append(',' + K.LINE_SEPARATOR);
		}
		
		// Start of the next JSON array entry
		logString.append(INDENT_LEVEL_2 + '{' + K.LINE_SEPARATOR);
		
		// Append UUID
		logString.append(INDENT_LEVEL_3 + K.encodeJSON("UUID") + ": " + K.encodeJSON(K.getUniqueID()) + ',' + K.LINE_SEPARATOR);
		
		// Append time stamp in ISO 8601 format (e.g. 2024-02-24T14:12:44.234)
		logString.append(INDENT_LEVEL_3 + K.encodeJSON("Logtime") + ": ");
		logString.append(K.encodeJSON(K.getTimeISO8601()));
		logString.append("," + K.LINE_SEPARATOR);
				
		// Append logging level
		logString.append(INDENT_LEVEL_3 + K.encodeJSON("Level") + ": ");
		
		switch (argRecord.getLevel().toString()) {
		
			case "FINEST":
				logString.append(K.encodeJSON("Debug"));
				break;
			case "INFO":
				logString.append(K.encodeJSON("Information"));
				break;
			case "SEVERE":
				logString.append(K.encodeJSON("Error"));
				break;
			default:
				logString.append(K.encodeJSON("N/A"));
				break;
		}

		logString.append(',' + K.LINE_SEPARATOR);
	
		// Get the passed message and split it into code location and text (delimited by KLog.DELIMITER)
		String	traceLocation	= null;
		String	traceMessage	= argRecord.getMessage();
		int		posDelimiter	= traceMessage.indexOf(KLog.LOG_DELIMITER);
		
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
		logString.append(INDENT_LEVEL_3 + K.encodeJSON("Location") + ": " + K.encodeJSON(traceLocation) + ',' + K.LINE_SEPARATOR);
		
		// Append log message
		logString.append(INDENT_LEVEL_3 + K.encodeJSON("Text") + ": " + K.encodeJSON(traceMessage) + K.LINE_SEPARATOR);
		
		// End the JSON array entry
		logString.append(INDENT_LEVEL_2 + '}');
		
		// Return formatted message
		return (logString.toString());
	}

	/**
	 * Set header.
	 */
	@Override
	public String getHead(Handler argHandler) {
		return ('{' + K.LINE_SEPARATOR + INDENT_LEVEL_1 + "\"KLog\": [" + K.LINE_SEPARATOR);
	}

	/**
	 * Set JSON footer. Note: Due to a bug in the JVM, the getTail() is not called by the ConsoleHandler.
	 */
	@Override
	public String getTail(Handler argHandler) {
		return (K.LINE_SEPARATOR + INDENT_LEVEL_1 + ']' + K.LINE_SEPARATOR + '}' + K.LINE_SEPARATOR);
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogJSONFormatter [gLastTraceLocation=" + gLastTraceLocation
				+ ", gFirstRecord=" + gFirstRecord + "]";
	}
}
