package ch.k43.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Format logging output as YAML formatted string with UUID, time stamp, logging level, code location and the logging information. 
 * The KLog.debug(), KLog.info() and KLog.error() adds the code location, delimited by KLog.DELIMITER which is then formatted here.
 * 
 * <pre>
 * Example:
 * KLog.info("Program start");
 * 
 * Output:
 * ---
 * KLog:
 * - UUID: "ce95a558-ed95-4288-b759-0d2ba69dbd5e"
 *   Logtime: "2024-09-14T10:12:42.428"
 *   Level: "Debug"
 *   Location: "main[1]:ch.k43.util.KLog:open:458"
 *   Text: "===== Application started 2024-09-14T10:12:42.375 ====="
 * - UUID: "fec270e6-7bfb-40ad-8460-5295ff617626"
 *   Logtime: "2024-09-14T10:12:42.429"
 *   Level: "Debug"
 *   Location: "main[1]:ch.k43.util.KLog:open:459"
 *   Text: "Java Utility Package (Freeware) ch.k43.util Version 2024.09.12"
 * ...
 * </pre>
 */
public class KLogYAMLFormatter extends Formatter {

	// Declarations
	private String	gLastTraceLocation	= "N/A";
	
	/**
	 * Class constructor.
	 */
	public KLogYAMLFormatter() {
		super();
	}

	/**
	 * Format the log message
	 */
	@Override
	public String format(LogRecord argRecord) {
		
		// Declarations
		StringBuilder logString = new StringBuilder();
		
		// Append UUID
		logString.append("- " + "UUID: " + K.encodeYAML(K.getUniqueID()) + K.LINE_SEPARATOR);
				
		// Append time stamp in ISO 8601 format (e.g. 2024-02-24T14:12:44.234)
		logString.append("  " + "Logtime: " + K.encodeYAML(K.getTimeISO8601())  + K.LINE_SEPARATOR);
				
		// Append logging level
		logString.append("  " + "Level: ");
		
		switch (argRecord.getLevel().toString()) {
		
			case "FINEST":
				logString.append(K.encodeYAML("Debug"));
				break;
			case "INFO":
				logString.append(K.encodeYAML("Information"));
				break;
			case "SEVERE":
				logString.append(K.encodeYAML("Error"));
				break;
			default:
				logString.append(K.encodeYAML("N/A"));
				break;
		}

		logString.append(K.LINE_SEPARATOR);
	
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
		logString.append("  " + "Location: " + K.encodeYAML(traceLocation) + K.LINE_SEPARATOR);
		
		// Append log message
		logString.append("  " + "Text: " + K.encodeYAML(traceMessage) + K.LINE_SEPARATOR);
		
		// Return formatted message
		return (logString.toString());
	}

	/**
	 * Set header.
	 */
	@Override
	public String getHead(Handler argHandler) {
		return ("---" + K.LINE_SEPARATOR + "KLog:" + K.LINE_SEPARATOR);
	}

	/**
	 * Set YAML footer. Note: Due to a bug in the JVM, the getTail() is not called by the ConsoleHandler.
	 */
	@Override
	public String getTail(Handler argHandler) {
		return ("..." + K.LINE_SEPARATOR);
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogYAMLFormatter [gLastTraceLocation=" + gLastTraceLocation
				+ "]";
	}
}
