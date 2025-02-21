package ch.k43.util;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.io.File;
import java.io.FileInputStream;

/**
 * Static class for simple logging.<p>
 * 
 * Notes:<br>
 * - To enable logging, the file KLog.properties must be in the current directory or set thru -DKLogPropertyFile.<br>
 * - The syntax follows the rules set by the java.util.logging framework (Java Logging API).<br>
 * - This class only supports Java Logger levels FINEST, INFO and SEVERE thru KLog.debug(), KLog.info() and KLog.error().<br>
 * 
 * <pre>
 * Example:
 *
 * KLog.info("Program started at {}", new Date());
 * KLog.debug("Any debugging message");
 * 
 * Output:
 * 2025-02-18T08:18:18.477 D main[1]:ch.k43.util.KLog:clinit:121                        ===== Application started 2025-02-18T08:18:18.456 =====
 * 2025-02-18T08:18:18.477 D main[1]:ch.k43.util.KLog:clinit:122                        Java Utility Package (Freeware) Version 2025.02.17
 * 2025-02-18T08:18:18.477 D main[1]:ch.k43.util.KLog:clinit:123                        Homepage java-util.k43.ch - Please send any feedback to andy.brunner@k43.ch
 * 2025-02-18T08:18:18.477 D main[1]:ch.k43.util.KLog:clinit:126                        KLog properties read from file KLog.properties
 * 2025-02-18T08:18:18.518 D main[1]:ch.k43.util.KLog:clinit:134                        Network host ab-macbook-pro (10.0.0.100)
 * 2025-02-18T08:18:18.518 D main[1]:ch.k43.util.KLog:clinit:138                        OS platform Mac OS X Version 15.3.1/aarch64
 * 2025-02-18T08:18:18.519 D main[1]:ch.k43.util.KLog:clinit:143                        OS disk space total 3.63 TiB, free 2.25 TiB, usable 2.25 TiB
 * 2025-02-18T08:18:18.519 D main[1]:ch.k43.util.KLog:clinit:149                        Java version 23 (Java HotSpot(TM) 64-Bit Server VM - Oracle Corporation)
 * 2025-02-18T08:18:18.519 D main[1]:ch.k43.util.KLog:clinit:154                        Java directory /Library/Java/JavaVirtualMachines/graalvm-jdk-23.0.1+11.1/Contents/Home
 * 2025-02-18T08:18:18.519 D main[1]:ch.k43.util.KLog:clinit:159                        Java CPUs 10, de/CH, UTF-8, UTC +01:00 (Europe/Zurich)
 * 2025-02-18T08:18:18.520 D main[1]:ch.k43.util.KLog:clinit:169                        Java heap maximum 16.00 GiB, current 1.01 GiB, used 8.98 MiB, free 1023.02 MiB
 * 2025-02-18T08:18:18.520 D main[1]:ch.k43.util.KLog:clinit:176                        Java classpath ../bin/:../lib/angus-mail-2.0.3.jar:../lib/jakarta.mail-api-2.1.3.jar:../lib/org.json.20230618.jar:../lib/h2-2.2.224.jar:../lib/jakarta.activation-api-2.1.3.jar:../lib/angus-activation-2.0.2.jar
 * 2025-02-18T08:18:18.520 D main[1]:ch.k43.util.KLog:clinit:180                        User andybrunner, language de, directory /Users/andybrunner/
 * 2025-02-18T08:18:18.520 D main[1]:ch.k43.util.KLog:clinit:186                        Current directory /Users/andybrunner/Documents/Eclipse-Workspace/ch.k43.util/src/
 * 2025-02-18T08:18:18.521 D main[1]:ch.k43.util.KLog:clinit:190                        Temporary directory /var/folders/9s/tbyqn_vn7bs9rf3f1rc2jpxw0000gn/T/
 * 2025-02-18T08:18:18.521 D main[1]:ch.k43.util.KLog:clinit:194                        KLog initialization completed (86 ms)
 * 2025-02-18T08:18:18.527 I main[1]:Test:main:10                                       Program started at Tue Feb 18 08:18:18 CET 2025
 * 2025-02-18T08:18:18.527 D main[1]:Test:main:11                                       Any debugging message
 * </pre>
 */
public class KLog {

	protected static final String		PROPERTY_FILE			= System.getProperty("KLogPropertyFile", "KLog.properties").trim();
	protected static final String		LOG_LEVEL_OVERRIDE		= System.getProperty("KLogLevel", "").trim().toUpperCase();
	protected static final String		DELIMITER				= "∞∞∞";				// Used by KLog and all KLogxxxx formatter/handler classes

	private static final String[]		EXCLUDE_CLASSES			= {
																KLogJDBCHandler.class.getName(),
																KLogSMTPHandler.class.getName(),
																KLogSMTPHandlerThread.class.getName(),
																KLogCSVFormatter.class.getName(),
																KLogJSONFormatter.class.getName(),
																KLogLineFormatter.class.getName(),
																KLogXMLFormatter.class.getName(),
																KLogYAMLFormatter.class.getName(),
																};
	
	private static Logger				gLogLogger				= null;
	private	static Level				gLogInitLevel			= Level.OFF;
	
	//
	// Static block to initialize class
	//
	static {
		
		// Start timer for initialization
		KTimer timer = new KTimer();
		
		// Get Java logger instance
		gLogLogger = Logger.getLogger(KLog.class.getName());
		
		// Configure logging with Klog.properties file
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(PROPERTY_FILE));
			
			// Disable logging if FINEST, INFO and SEVERE are not enabled 
			if (!gLogLogger.isLoggable(Level.FINEST) && (!gLogLogger.isLoggable(Level.INFO)) && (!gLogLogger.isLoggable(Level.SEVERE))) {
				close();
			} else {
				// Save configured Logger level
				gLogInitLevel = gLogLogger.getLevel();
			}
		} catch (Exception e) {
			// Mark open failed
			gLogLogger = null;
		}

		// Check if command line parameter -JKLogLevel is overriding the logging level
		if (!K.isEmpty(LOG_LEVEL_OVERRIDE)) {
			
			switch (LOG_LEVEL_OVERRIDE) {
			
				case "INFO": {
					setLevelInfo();
					break;
				}

				case "ERROR": {
					setLevelError();
					break;
				}

				case "DEBUG": {
					setLevelDebug();
					break;
				}
	
				case "OFF": {
					setLevelOff();
					break;
				}
	
				default: {
					// Ignoring invalid logging level
				}
			}
		}
		
		// Log environment
		if (isLevelDebug()) {
		
			debug("===== Application started {} =====", K.getTimeISO8601(K.START_TIME));
			debug("Java Utility Package (Freeware) Version {}", K.VERSION);
			debug("Homepage java-util.k43.ch - Please send any feedback to andy.brunner@k43.ch");

			// Show properties file name and logging level override
			debug("KLog properties read from file {}",
					KLog.PROPERTY_FILE);

			if (!K.isEmpty(LOG_LEVEL_OVERRIDE)) {
				debug("KLog logging level overridden to {}", LOG_LEVEL_OVERRIDE);
			}

			// Show network and OS
			debug("Network host {} ({})",
					K.getLocalHostName(),
					K.getLocalHostAddress());
			
			debug("OS platform {}",
					K.JVM_PLATFORM);
		
			// OS disk usage
			File root = new File(K.CURRENT_DIRECTORY);
			debug("OS disk space total {}, free {}, usable {}",
					K.formatBytes(root.getTotalSpace()),
					K.formatBytes(root.getFreeSpace()),
					K.formatBytes(root.getUsableSpace()));

			// Java version
			debug("Java version {} ({})",
					K.JVM_MAJOR_VERSION,
					K.JVM_VERSION_NAME);

			// Java directory
			debug("Java directory {}",
					System.getProperty("java.home", "n/a"));
						
			// Java CPU count and locale
			Locale locale = Locale.getDefault();
			debug("Java CPUs {}, {}/{}, {}, UTC {} ({})",
					K.getJVMCPUCount(),
					locale.getLanguage(),
					locale.getCountry(),
					K.DEFAULT_ENCODING,
					K.getUTCOffsetAsString(),
					TimeZone.getDefault().getID());
			
			// Java heap memory statistics
			long[] memoryStats = K.getJVMMemStats();
			debug("Java heap maximum {}, current {}, used {}, free {}",
					K.formatBytes(memoryStats[0]),
					K.formatBytes(memoryStats[1]),
					K.formatBytes(memoryStats[2]),
					K.formatBytes(memoryStats[3]));

			// Java class path
			debug("Java classpath {}",
					System.getProperty("java.class.path", "n/a").trim());
			
			// User name
			debug("User {}, language {}, directory {}",
					K.USER_NAME,
					K.USER_LANGUAGE,
					K.HOME_DIRECTORY);
			
			// Current directory
			debug("Current directory {}",
					K.CURRENT_DIRECTORY);
			
			// Temporary directory
			debug("Temporary directory {}",
					K.TEMP_DIRECTORY);
			
			// Log initialization time
			debug("KLog initialization completed ({} ms)", timer.getElapsedMilliseconds());
		}
	}
	
	/**
	 * Log error message if expression is true and throws an unchecked RuntimeException.
	 * 
	 * @param	argExpression	Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters 
	 */
	public static void abort(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if expression is true and message present
		if ((!argExpression) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Replace {} parameters if present
		String workString = K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}

		// Format and write log message with code location
		write(Level.SEVERE, formatLogMessage(workString));

		// Log and throw exception
		RuntimeException runtimeException = new RuntimeException(workString);
		logStackTrace(runtimeException);
		throw runtimeException;
	}
	
	/**
	 * Log error message and throws an unchecked RuntimeException.<br>
	 * 
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters 
	 */
	public static void abort(String argMessage, Object... argObjects) {
		abort(true, K.replaceParams(argMessage, argObjects));
	}
	
	/**
	 * Log error message if expression evaluates true and throw an unchecked exception IllegalArgumentException.<br>
	 * 
	 * @param	argExpression				Any expression
	 * @param	argMessage					Message to be logged and used as exception
	 * @param	argObjects					Optional arguments for {} parameters 
	 * @throws	IllegalArgumentException	Explicit exception
	 * 
	 * @since 2024.05.17
	 */
	public static void argException(boolean argExpression, String argMessage, Object... argObjects) {
		
		// Check if expression is true and message present
		if ((!argExpression) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Replace {} parameters if present
		String workString = K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		// Format and write log message with code location
		write(Level.SEVERE, formatLogMessage(workString));
		
		// Create, format and throw unchecked exception
		IllegalArgumentException exception = new IllegalArgumentException(workString);
		logStackTrace(exception);
		throw exception;
	}

	/**
	 * Close the logging.
	 */
	public static synchronized void close() {
		
		// Check if logger already closed
		if (gLogLogger == null) {
			return;
		}
		
		// Write close message
		debug("KLog terminated");
		
		// Close all handlers
		for (Handler handler : gLogLogger.getParent().getHandlers()) {
			handler.flush();
		    handler.close();
		    gLogLogger.removeHandler(handler);
		}
		
		// Reset log handler
		LogManager.getLogManager().reset();
		
		// Mark logger closes
		gLogLogger = null;
	}
	
	/**
	 * Write log message of level FINEST.
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argObjects		Optional arguments for {} parameters 
	 */
	public static void debug(String argMessage, Object... argObjects) {

		// Check if logging is active and valid arguments given
		if ((!isActive()) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Replace {} parameters if present
		String workString = K.replaceParams(argMessage, argObjects);
		
		// Write log message
		write(Level.FINEST, formatLogMessage(workString));
	}
	
	/**
	 * Log error message if expression is true.
	 * 
	 * @param	argExpression	True/False Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters
	 */
	public static void error(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if expression is true and message present
		if ((!argExpression) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Replace {} parameters if present
		String workString = K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		// Write log message
		write(Level.SEVERE, formatLogMessage(workString));
	}
	
	/**
	 * Log formatted exception with stack trace.<br>
	 * 
	 * @param argException	Exception to be formatted
	 */
	public static void error(Exception argException) {

		// Check if expression is true and message present
		if (K.isEmpty(argException)) {
			return;
		}
		
		// Save error message even if logging is not active
		K.saveError(argException.toString());
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		// Write log message
		write(Level.SEVERE, formatLogMessage(argException.toString()));
		logStackTrace(argException);
	}
	
	/**
	 * Log message and exception with stack trace.
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argException	Exception to be formatted
	 * @param	argObjects		Optional arguments for {} parameters 	 */
	public static void error(String argMessage, Exception argException, Object... argObjects) {

		// Check if expression is true and message present
		if ((K.isEmpty(argMessage)) || (K.isEmpty(argException))) {
			return;
		}
		
		// Replace {} parameters if present
		String workString = K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		// Write log message
		write(Level.SEVERE, formatLogMessage(workString));
	
		// Format exception message and stack trace
		logStackTrace(argException);
	}

	/**
	 * Log error message.
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argObjects		Optional arguments for {} parameters
	 */
	public static void error(String argMessage, Object... argObjects) {
		error(true, K.replaceParams(argMessage, argObjects));
	}
	
	/**
	 * Prepend calling code location to log message, delimited by KLog.DELIMITER.
	 */
	@SuppressWarnings("deprecation")
	private static String formatLogMessage(String argMessage) {

		// Check if argument valid and code location not already prepended
		if ((!K.isEmpty(argMessage)) && (argMessage.indexOf(KLog.DELIMITER) != -1)) {
			return (argMessage);
		}

		StackTraceElement[] stackTraceElements	= Thread.currentThread().getStackTrace();
		StringBuilder		strBuilder			= new StringBuilder();

		// Format thread name
		strBuilder.append(Thread.currentThread().getName())
			.append('[')
			.append((K.JVM_MAJOR_VERSION < 19) ? Thread.currentThread().getId() : Thread.currentThread().threadId())
			.append("]:");

		// Format calling class, method and line number
		if (stackTraceElements.length > 3) {
			strBuilder.append(stackTraceElements[3].getClassName())
				.append(':')
				.append(stackTraceElements[3].getMethodName())
				.append(':')
				.append(stackTraceElements[3].getLineNumber());
		} else {
			strBuilder.append("N/A");
		}
		
		// Return formatted string to caller
	    strBuilder.append(KLog.DELIMITER)
	    	.append(argMessage != null ? argMessage : "N/A");
	    
	    return strBuilder.toString();
	}

	/**
	 * Get logger level.
	 * 
	 * @return Logging level
	 * 
	 * @since 2024.06.16
	 */
	public static Level getLevel() {
		
		// Check if logging is active
		if (!isActive()) {
			return (Level.OFF);
		}
		
		return (gLogLogger.getLevel());
	}
	
	/**
	 * Write log message of level INFO.<br>
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argObjects		Optional arguments for {} parameters	 
	 */
	public static void info(String argMessage, Object... argObjects) {

		// Check if logging is active and valid arguments given
		if ((!isActive()) || (K.isEmpty(argMessage))) {
			return;
		}

		// Write log message
		write(Level.INFO, formatLogMessage(K.replaceParams(argMessage, argObjects)));
	}
	
	/**
	 * Check if logging is active.<br>
	 * 
	 * @return	True if logging is active, false otherwise
	 */
	public static boolean isActive() {
		return gLogLogger != null;
	}
	
	/**
	 * Test logger level.
	 * 
	 * @param argLevel Logger level to test
	 * @return True if level matches, false otherwise
	 */
	private static boolean isLevel(Level argLevel) {
		
		// Check if logging is active
		if (!isActive()) {
			return false;
		}
		
		return gLogLogger.getLevel() == argLevel;
	}
	
	/**
	 * Check if logger is at level FINEST.<br>
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelDebug() {
		return isLevel(Level.FINEST);
	}

	/**
	 * Check if logger is at level SEVERE
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelError() {
		return isLevel(Level.SEVERE);
	}
	
	/**
	 * Check if logger is at level INFO.<br>
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelInfo() {
		return isLevel(Level.INFO);
	}
	
	/**
	 * Check if logger is at level OFF.<br>
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelOff() {
		return isLevel(Level.OFF);
	}

	/**
	 * Format exception stack trace
	 */
	private static void logStackTrace(Exception argException) {
	
		// Check argument
		if (K.isEmpty(argException)) {
			return;
		}

		// Format exception message and stack trace
		StackTraceElement[] stackTraceElements = argException.getStackTrace();
			
		int stackPosition = 1;
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			write(Level.SEVERE, "Stack[" + stackPosition + "]: " + stackTraceElement.toString());
			stackPosition++;
		}
	}

	/**
	 * Reset logger level to the configured level in KLog.properties
	 * 
	 * @since 2024.05.25
	 */
	public static void resetLevel() {

		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		setLoggerLevel(gLogInitLevel);
	}
	
	/**
	 * Set logger level
	 * 
	 * @param	argLevel	Logging level to set
	 * @since 2024.06.16
	 */
	public static void setLevel(Level argLevel) {
		setLoggerLevel(argLevel);
	}
	
	/**
	 * Set logger to debug level (FINEST)
	 * 
	 * @since 2024.05.25
	 */
	public static void setLevelDebug() {
		setLoggerLevel(Level.FINEST);
	}
	
	/**
	 * Set logger to error level (SEVERE)
	 * 
	 * @since 2024.05.25
	 */
	public static void setLevelError() {
		setLoggerLevel(Level.SEVERE);
	}
	
	/**
	 * Set logger to info level (INFO)
	 * 
	 * @since 2024.05.25
	 */
	public static void setLevelInfo() {
		setLoggerLevel(Level.INFO);
	}
	
	/**
	 * Set logger off (OFF)
	 * 
	 * @since 2024.05.25
	 */
	public static void setLevelOff() {
		setLoggerLevel(Level.OFF);
	}
	
	/**
	 * Set logger level.<br>
	 * 
	 * @param argLevel	Logging level to be set
	 * 
	 * @since 2024.05.25
	 */
	private static void setLoggerLevel(Level argLevel) {
		
		// Check if logging is active
		if (!isActive()) {
			return;
		}
		
		gLogLogger.setLevel(argLevel);
	}
	
	/**
	 * Write log message.
	 * 
	 * @param argLevel	Message level
	 * @param argData	Message to be written 
	 */
	private static void write(Level argLevel, String argData) {
		
		// Check if any data to be logged
		if (K.isEmpty(argData)) {
			return;
		}
		
		// Get stack trace
		String stackTrace = Arrays.toString(Thread.currentThread().getStackTrace());

		// Prohibit log write recursion by excluding some Java classes
		for (String classExclude : EXCLUDE_CLASSES) {
			if (stackTrace.contains(classExclude)) {
				return;
			}
		}

		// Write logger message
		gLogLogger.log(argLevel, argData);
	}
	
	/**
	 * Private constructor to prevent class instantiation.
	 */
	private KLog() {
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KLog []";
	}
}	
