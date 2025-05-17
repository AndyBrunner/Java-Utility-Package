package ch.k43.util;

import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Java logging handler to output logging data to any compliant JDBC database. The log data rows are deleted according to the
 * parameter {@code retensiondays} during KLogJDBCHandler class instantiation. If the table does not exist during class instantiation,
 * the table will be created with the following SQL statement:
 * 
 * <pre>
 * CREATE TABLE {TableName} (UUID CHAR(36), LOGTIME TIMESTAMP, LEVEL VARCHAR(20), LOCATION VARCHAR(80), TEXT VARCHAR(500))
 * </pre>
 * 
 * The following properties are supported:
 * <pre>
 * ch.k43.util.KLogJDBCHandler.jdbc.driver = Java class name
 * ch.k43.util.KLogJDBCHandler.jdbc.url = JDBC connection URL, e.g. jdbc:h2:/Users/jsmith/KLogDB
 * ch.k43.util.KLogJDBCHandler.jdbc.username = User name
 * ch.k43.util.KLogJDBCHandler.jdbc.password = Password
 * ch.k43.util.KLogJDBCHandler.tablename = SQL table name (default KLOGDATA)
 * ch.k43.util.KLogJDBCHandler.retensiondays = Number of days before log records are deleted (default 30)
 * ch.k43.util.KLogJDBCHandler.debug = true/false (Output sent to System.out/System.err, default false)	
 * </pre>
 * 
 * @since 2024.06.16
 */
public class KLogJDBCHandler extends Handler {

	// Class declarations
	private KDB			gJDBC				= null;
	private String		gClassName			= this.getClass().getName();
	private String		gTableName			= null;
	private String		gLastTraceLocation	= "N/A";
	private boolean		gDebugLogActive		= false;								// Enable only during development and testing
	private boolean		gIsActive			= false;
	
	/**
	 * Class constructor.
	 */
	public KLogJDBCHandler() {

		// Read properties
		Properties logProps = KFile.readPropertiesFile(KLog.PROPERTY_FILE);
		
		if (logProps == null) {
			return;
		}

		gTableName				= logProps.getProperty(gClassName + ".tablename", "KLOGDATA").toUpperCase().trim();

		String	jdbcDriver		= logProps.getProperty(gClassName + ".jdbc.driver", "").trim();
		String	jdbcURL			= logProps.getProperty(gClassName + ".jdbc.url", "").trim();
		String	jdbcUsername	= logProps.getProperty(gClassName + ".jdbc.username", "").trim();
		String	jdbcPassword	= logProps.getProperty(gClassName + ".jdbc.password", "").trim();
		String	logRetension	= logProps.getProperty(gClassName + ".retensiondays", "30").trim();
		String	logDebug		= logProps.getProperty(gClassName + ".debug", "false").trim();
		
		if (logDebug.equalsIgnoreCase("true")) {
			gDebugLogActive = true;
		}
		
		logDebug("Logging handler initializing");
		
		// Connect to JDBC database
		logDebug("JDBC Driver: " + jdbcDriver);
		logDebug("JDBC URL: " + jdbcURL);
		logDebug("JDBC Username: " + jdbcUsername);
		logDebug("JDBC Password: " + jdbcPassword);
		logDebug("Retension: " + logRetension);
		logDebug("Table: " + gTableName);
		logDebug("Debug: " + gDebugLogActive);
		
		gJDBC = new KDB(jdbcDriver, jdbcURL, jdbcUsername, jdbcPassword);

		if (!gJDBC.isConnected()) {
			logError(gJDBC.getErrorMessage());
			return;
		}

		logDebug("JDBC connection established");
		
		// Create table (if necessary)
		if (!gJDBC.exec("SELECT COUNT(*) FROM " + gTableName)) {
			
			if (!gJDBC.exec("CREATE TABLE " + gTableName + '(' +
					"UUID CHAR(36), " +
					"LOGTIME TIMESTAMP, " +
					"LEVEL VARCHAR(20), " +
					"LOCATION VARCHAR(80), " +
					"TEXT VARCHAR(" + KLog.MAX_LOG_DATA + "))")) {
				logError(gJDBC.getErrorMessage());
				return;
			} else {
				logDebug("Table " + gTableName + " created");
			}
		} else {
			logDebug("Number of rows in table " + gTableName + ": " + gJDBC.getData().get(0)[0]);
		}
		
		// Delete old log records
		if (!gJDBC.exec("DELETE FROM " + gTableName + " WHERE "
				+ "DATEADD(D, " + logRetension + ", LOGTIME) <= CURRENT_DATE")) {
			logError(gJDBC.getErrorMessage());
			return;
		} else {
			logDebug("Number of expired rows deleted: " + gJDBC.getRowCount());
		}
		
		// Enable driver
		gIsActive = true;
		
		logDebug("Logging handler initialized");
	}
	
	/**
	 * Close the handler.
	 */
	public void close() {
		
		// Check if driver is active
		if (!gIsActive) {
			return;
		}
		
		// Close JDBC connection
		if (gJDBC != null) {
			gJDBC.close();
			gJDBC = null;
			logDebug("JDBC connection closed");
		}
		
		// Mark driver inactive
		gIsActive = false;
		
		logDebug("Logging handler terminated");
	}
	
	/**
	 * Commit JDBC transaction.
	 */
	public void flush() {

		// Check if driver is active
		if (!gIsActive) {
			return;
		}

		// Commit transaction
		gJDBC.commit();
		logDebug("Transaction committed");
	}

	/**
	 * Write debug log.
	 * 
	 * @param argMessage	Message to be logged
	 */
	private void logDebug(String argMessage) {

		if (gDebugLogActive) {
			System.out.println(K.getTimeISO8601() + " D " + gClassName + ": " + argMessage);
		}
	}

	/**
	 * Write debug log.
	 * 
	 * @param argMessage	Message to be logged
	 */
	private void logError(String argMessage) {

		if (gDebugLogActive) {
			System.err.println(K.getTimeISO8601() + " E " + gClassName + ": " + argMessage);
		}
	}
	
	/**
	 * Write log record to JDBC database.
	 * 
	 * @param	argRecord Log record
	 */
	public void publish(LogRecord argRecord) {
		
		// Check if driver is active
		if (!gIsActive) {
			return;
		}
	
		// Logging level
		String logLevel = null;
		
		switch (argRecord.getLevel().toString()) {
		
		case "FINEST":
			logLevel = "Debug";
			break;
		case "INFO":
			logLevel = "Information";
			break;
		case "SEVERE":
			logLevel = "Error";
			break;
		default:
			logLevel = "N/A";
			break;
		}
		
		// Get the passed message and split it into code location and text (delimited by KLOG.DELIMITER)
		String	traceLocation	= null;
		String	traceMessage	= argRecord.getMessage();
		int		posDelimiter	= traceMessage.indexOf(KLog.LOG_DELIMITER);
		
		if (posDelimiter != -1) {
			traceLocation		= traceMessage.substring(0, posDelimiter);
			traceMessage		= traceMessage.substring(posDelimiter + 3);
			// Save last trace location in case the next entry has none
			gLastTraceLocation	= traceLocation; 
		} else {
			// Use last trace location if none was found
			traceLocation		= gLastTraceLocation;
		}
		
		if (!gJDBC.exec("INSERT INTO KLOGDATA"+
					" (UUID, LOGTIME, LEVEL, LOCATION, TEXT) VALUES(" +
					"'" + K.getUniqueID() + "'," +
					"'" + K.getTimeISO8601() + "'," +
					"'" + logLevel.trim() + "'," +
					"'" + String.format("%-80s", traceLocation).trim() + "'," +
					"'" + String.format("%-s", K.truncateMiddle(traceMessage, KLog.MAX_LOG_DATA)).trim() + "')")) {
			logError(gJDBC.getErrorMessage());
		} else {
			logDebug("Log data written to database");
		}
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLogJDBCHandler [gJDBC=" + gJDBC + ", gClassName=" + gClassName + ", gTableName=" + gTableName
				+ ", gLastTraceLocation=" + gLastTraceLocation + ", gDebugLogActive=" + gDebugLogActive + ", gIsActive="
				+ gIsActive + "]";
	}
}
