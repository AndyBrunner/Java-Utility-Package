package ch.k43.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Format logging output as single formatted line with time stamp, logging level, code location and the logging information. 
 * The KLog.debug(), KLog.info() and KLog.error() adds the code location, delimited by Klog.DELIMITER which is then formatted here.
 * 
 * <pre>
 * Example:
 * KLog.info("Program start");
 *
 * Output:
 * 2024-06-21T17:51:48.059 I main[1]:Test:main:21                                         Program start
 * </pre>
 */
public class KLogLineFormatter extends Formatter {
	
	// Class variables
	private static final	String		ERROR_PREFIX			= "===> ";
	private static final	int			MAX_CODE_LOCATION_SIZE	= 60;
	private static final	String		FORMAT_LOCATION_STRING	= "%-" + (MAX_CODE_LOCATION_SIZE + 1) + 's';

	// Instance variables
	private					String		gLastTraceLocation		= "N/A";
	
	/**
	 * Class constructor.
	 */
	public KLogLineFormatter() {
		super();
	}
	
	/**
	 * Format the log message
	 */
	@Override
	public String format(LogRecord argRecord) {
		
		// Declarations
		StringBuilder		logString				= new StringBuilder();
		boolean				logSevere				= false;

		// Format time stamp in ISO 8601 format (e.g. 2024-02-24T14:12:44.234)
		logString.append(K.getTimeISO8601()).append(' ');
		
		// Format abbreviated logging level
		switch (argRecord.getLevel().toString().toUpperCase()) {
			case "FINEST":
				logString.append('D');
				break;
			case "INFO":
				logString.append('I');
				break;
			case "SEVERE":
				logString.append('E');
				logSevere = true;
				break;
			default:
				logString.append('?');
				break;
		}

		logString.append(' ');

		// Get the passed message and split it into code location and text (delimited by KLog.DELIMITER)
		String	traceLocation	= null;
		String	traceMessage	= argRecord.getMessage();
		int		posDelimiter	= traceMessage.indexOf(KLog.LOG_DELIMITER);
		
		if (posDelimiter != -1) {
			traceLocation		= traceMessage.substring(0, posDelimiter);
			traceMessage		= traceMessage.substring(posDelimiter + KLog.LOG_DELIMITER.length());
			// Save last trace location in case the next entry has none
			gLastTraceLocation	= traceLocation; 
		} else {
			// Use last trace location if none was found
			traceLocation	= gLastTraceLocation;
		}

		logString.append(String.format(FORMAT_LOCATION_STRING, K.truncateMiddle(traceLocation, MAX_CODE_LOCATION_SIZE)));
		
		// Format the passed log message
		if (logSevere) {
			traceMessage = ERROR_PREFIX + traceMessage;
		}
		
		logString.append(traceMessage);
		
		// Return formatted message
		return (logString.toString() + K.LINE_SEPARATOR);
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogLineFormatter [gLastTraceLocation=" + gLastTraceLocation
				+ "]";
	}
}
