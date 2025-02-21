package ch.k43.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Format logging output as XML formatted string with UUID, time stamp, logging level, code location and the logging information. 
 * The KLog.debug(), KLog.info() and KLog.error() adds the code location, delimited by KLog.DELIMITER which is then formatted here.
 */
public class KLogXMLFormatter extends Formatter {

	// Class variables
	private static final String	INDENT_LEVEL_1		= "  ";
	private static final String	INDENT_LEVEL_2		= "    ";
	
	private	String				gLastTraceLocation	= "N/A";
		
	/**
	 * Class constructor.
	 */
	public KLogXMLFormatter() {
		super();
	}
	
	/**
	 * Format the log message.
	 */
	@Override
	public String format(LogRecord argRecord) {
		
		// Declarations
		StringBuilder logString = new StringBuilder();

		logString.append(INDENT_LEVEL_1 + "<LogEntry>" + K.LINE_SEPARATOR);
		
		// Append sequence
		logString.append(INDENT_LEVEL_2 + "<UUID>" + K.encodeXML(K.getUniqueID()) + "</UUID>" + K.LINE_SEPARATOR);
		
		// Append log time
		logString.append(INDENT_LEVEL_2 + "<LogTime>" + K.encodeXML(K.getTimeISO8601()) + "</LogTime>" + K.LINE_SEPARATOR);

		// Append log level
		logString.append(INDENT_LEVEL_2 + "<Level>");

		switch (argRecord.getLevel().toString()) {
		
		case "FINEST":
			logString.append(K.encodeXML("Debug"));
			break;
		case "INFO":
			logString.append(K.encodeXML("Information"));
			break;
		case "SEVERE":
			logString.append(K.encodeXML("Error"));
			break;
		default:
			logString.append(K.encodeXML("N/A"));
			break;
		}

		logString.append("</Level>" + K.LINE_SEPARATOR);

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
		logString.append(INDENT_LEVEL_2 + "<Location>" + K.encodeXML(traceLocation) + "</Location>" + K.LINE_SEPARATOR);

		// Append log message
		logString.append(INDENT_LEVEL_2 + "<Text>" + K.encodeXML(traceMessage) + "</Text>" + K.LINE_SEPARATOR);

		logString.append(INDENT_LEVEL_1 + "</LogEntry>" + K.LINE_SEPARATOR);

		// Return formatted message
		return (logString.toString());
	}

	/**
	 * Set header.
	 */
	@Override
	public String getHead(Handler argHandler) {
		
		StringBuilder headerData = new StringBuilder();
		
		// Write XML header
		headerData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + K.LINE_SEPARATOR);
		headerData.append("<!-- " + this.getClass().getName() + " Version " + K.VERSION + " -->" + K.LINE_SEPARATOR);
		headerData.append("<KLog>" + K.LINE_SEPARATOR);
		
		return (headerData.toString());
		
	}

	/**
	 * Set footer. Note: Due to a bug in the JVM, the getTail() is not called by the ConsoleHandler.
	 */
	@Override
	public String getTail(Handler argHandler) {
		return ("</KLog>" + K.LINE_SEPARATOR);
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogXMLFormatter [gLastTraceLocation=" + gLastTraceLocation
				+ "]";
	}
}
