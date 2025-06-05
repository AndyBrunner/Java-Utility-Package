package ch.k43.util;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

	protected static final	String		PROPERTY_FILE;			// Initialized in static block
	
	protected static final	String		LOG_LEVEL_OVERRIDE		= System.getProperty("KLogLevel", "").trim().toUpperCase();
	protected static final	String		LOG_EXCLUDE				= System.getProperty("KLogExclude", "").trim();
	protected static final	String		LOG_INCLUDE				= System.getProperty("KLogInclude", "").trim();
	
	protected static final	String		LOG_DELIMITER			= "∞∞∞";				// Used by KLog and all KLogxxxx formatter/handler

	protected static final	int			MAX_LOG_DATA			= 1_000;
	
	private static final	String[]	EXCLUDE_CLASSES			= {
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
	private static Pattern				gExcludeRegEx			= null;
	private static Pattern				gIncludeRegEx			= null;
	
	//
	// Static block to initialize class
	//
	static {

		//
		// Initialize Java logger instance
		//
		String propertyOverride = System.getProperty("KLogPropertyFile", "").trim();
		
		if (!K.isEmpty(propertyOverride)) {
			PROPERTY_FILE = propertyOverride;
		} else {
			PROPERTY_FILE = "KLog.properties";
		}
				
		// Configure logging with Klog.properties file
		gLogLogger = Logger.getLogger(KLog.class.getName());
		
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
			
			// Check if overridden property file could not be opened
			if ((e instanceof FileNotFoundException) && (!K.isEmpty(propertyOverride))) {
				throw new RuntimeException("The specified logging property file " + propertyOverride + " could not be found");
			}

			// Check if any errors except file-not-found
			if (!(e instanceof FileNotFoundException)) {
				throw new RuntimeException("Logging property file " + propertyOverride + " could not be read: " + e.toString());
			}
			
			// Ignore errors and disable logging
			gLogLogger = null;
		}

		//
		// Check if command line parameter -JKLogLevel is overriding the logging level
		//
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
					throw new RuntimeException("Command line parameter -DKLogLevel must be 'Info', 'Error', 'Debug' or 'Off'");
				}
			}
		}

		//
		// Read and process KLog include and exclude RegEx patterns
		//
		if (isActive()) {

			// Use overriding parameter -DKLogInclude or KLog.property "ch.k43.util.KLog.include"
			try {
				if (!K.isEmpty(LOG_INCLUDE)) {
					gIncludeRegEx = Pattern.compile(LOG_INCLUDE, Pattern.CASE_INSENSITIVE);
				} else {
					String patternString = LogManager.getLogManager().getProperty(KLog.class.getName() + ".include");
					if (!K.isEmpty(patternString)) {
						gIncludeRegEx = Pattern.compile(patternString.trim(), Pattern.CASE_INSENSITIVE);						
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Invalid RegEx logging include syntax: " + e.toString());
			}
			
			// Use overriding parameter -DKLogExclude or KLog.property "ch.k43.util.KLog.exclude"
			try {
				if (!K.isEmpty(LOG_EXCLUDE)) {
					gExcludeRegEx = Pattern.compile(LOG_EXCLUDE, Pattern.CASE_INSENSITIVE);
				} else {
					String patternString = LogManager.getLogManager().getProperty(KLog.class.getName() + ".exclude");
					if (!K.isEmpty(patternString)) {
						gExcludeRegEx = Pattern.compile(patternString.trim(), Pattern.CASE_INSENSITIVE);						
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Invalid RegEx logging exclude syntax: " + e.toString());
			}
			
			// Check if include and exclude were given
			if ((gIncludeRegEx != null) && (gExcludeRegEx != null)) {
				throw new RuntimeException("Both logging include and exclude parameters specified");
			}
		}
		
		//
		// Log environment if in debug mode
		//
		if (isLevelDebug()) {
		
			debug("===== Application started {} =====", K.getTimeISO8601(K.START_TIME));
			debug("Java Utility Package (Open Source/Freeware) Version {}", K.VERSION);
			debug("Homepage java-util.k43.ch - Please send any feedback to andy.brunner@k43.ch");

			// Show properties file name and logging level override
			debug("KLog properties read from file {}",
					KLog.PROPERTY_FILE);

			if (!K.isEmpty(LOG_LEVEL_OVERRIDE)) {
				debug("KLog logging level overridden with -DKLogLevel parameter");
			}
			
			if (!K.isEmpty(LOG_EXCLUDE)) {
				debug("KLog logging filter set with -DKLogExclude");
			}
			
			if (!K.isEmpty(LOG_INCLUDE)) {
				debug("KLog logging filter set with -DKLogInclude={}", LOG_INCLUDE);
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
			debug("Current user {}, language {}, home directory {}",
					K.USER_NAME,
					K.USER_LANGUAGE,
					K.HOME_DIRECTORY);
			
			// Current directory
			debug("Current directory {}",
					K.CURRENT_DIRECTORY);
			
			// Temporary directory
			debug("Temporary directory {}",
					K.TEMP_DIRECTORY);
		}
	}
	
	/**
	 * Log error message if expression is true and throws an unchecked RuntimeException.
	 * 
	 * @param	argExpression	Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters 
	 * @throws	RuntimeException Explicit exception
	 */
	public static void abort(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if anything to log
		if (!argExpression) {
			return;
		}

		// Replace {} parameters if present
		String workString = K.isEmpty(argMessage) ? "KLog.abort(): No error message" : K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Create unchecked exception
		RuntimeException runtimeException = new RuntimeException(workString);
		
		// Log error and exception
		if (isActive()) {
			write(Level.SEVERE, formatLogMessage(workString));
			logStackTrace(runtimeException);
		}
		
		// Throw exception
		throw runtimeException;
	}
	
	/**
	 * Log error message and throws an unchecked RuntimeException.
	 * 
	 * @param	argException	Exception to be logged
	 * @throws	RuntimeException Explicit exception
	 * 
	 * @since 2025.05.19
	 */
	public static void abort(Exception argException) {
		
		// Check arguments
		Exception exception = (argException == null) ? new Exception("KLog.abort(): argException must not be empty") : argException;
		
		// Save error message even if logging is not active
		K.saveError(exception.toString());
		
		// Log error and exception
		if (isActive()) {
			write(Level.SEVERE, formatLogMessage(exception.toString()));
			logStackTrace(exception);
		}
		
		// Throw exception
		throw new RuntimeException(exception.toString());
	}
	
	/**
	 * Log error message and throws an unchecked RuntimeException.
	 * 
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters 
	 * @throws	RuntimeException Explicit exception
	 */
	public static void abort(String argMessage, Object... argObjects) {
		abort(true, argMessage, argObjects);
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
		
		// Check if anything to log
		if (!argExpression) {
			return;
		}

		// Replace {} parameters if present
		String workString = K.isEmpty(argMessage) ? "KLog.argException(): No error message" : K.replaceParams(argMessage, argObjects);
		
		// Save error message even if logging is not active
		K.saveError(workString);
		
		// Create unchecked exception
		IllegalArgumentException illegalArgumentException = new IllegalArgumentException(workString);
		
		// Log error and exception
		if (isActive()) {
			write(Level.SEVERE, formatLogMessage(workString));
			logStackTrace(illegalArgumentException);
		}
		
		// Throw exception
		throw illegalArgumentException;
	}
	
	/**
	 * Log error message and throw an unchecked exception IllegalArgumentException.<br>
	 * 
	 * @param	argMessage					Message to be logged and used as exception
	 * @param	argObjects					Optional arguments for {} parameters 
	 * @throws	IllegalArgumentException	Explicit exception
	 * 
	 * @since 2025.03.16
	 */
	public static void argException(String argMessage, Object... argObjects) {
		argException(true, argMessage, argObjects);
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
	 * Log debug message if expression is true.
	 * 
	 * @param	argExpression	True/False Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters
	 * 
	 * @since 2025.04.22
	 */
	public static void debug(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if anything to log
		if ((!isActive()) || (!argExpression) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Write log message
		write(Level.FINEST, formatLogMessage(K.replaceParams(argMessage, argObjects)));
	}
	
	/**
	 * Write log message of level FINEST.
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argObjects		Optional arguments for {} parameters 
	 */
	public static void debug(String argMessage, Object... argObjects) {

		// Check if anything to log
		if ((!isActive()) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Write log message
		write(Level.FINEST, formatLogMessage(K.replaceParams(argMessage, argObjects)));
	}
	
	/**
	 * Log error message if expression is true.
	 * 
	 * @param	argExpression	True/False Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters
	 */
	public static void error(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if anything to log
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

		// Check if anything to log
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

		// Check if anything to log
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

		// Check if anything to log
		if (K.isEmpty(argMessage)) {
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
	 * Prepend calling code location to log message, delimited by KLog.DELIMITER.
	 */
	@SuppressWarnings("deprecation")
	private static String formatLogMessage(String argMessage) {

		// Check if argument valid and code location not already prepended
		if ((!K.isEmpty(argMessage)) && (argMessage.indexOf(KLog.LOG_DELIMITER) != -1)) {
			return (argMessage);
		}

		StackTraceElement[] stackTraceElements	= Thread.currentThread().getStackTrace();
		StringBuilder		strBuilder			= new StringBuilder();

		// Format thread name
		strBuilder.append(Thread.currentThread().getName())
			.append('[')
			.append((K.JVM_MAJOR_VERSION < 19) ? Thread.currentThread().getId() : Thread.currentThread().threadId())
			.append("]:");

		// Format calling class, method and line number (4th element is the user code location issuing the KLog method call)
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
	    strBuilder.append(KLog.LOG_DELIMITER)
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
	 * Log message if expression is true.
	 * 
	 * @param	argExpression	True/False Any expression
	 * @param	argMessage		Message to be logged
	 * @param	argObjects		Optional arguments for {} parameters
	 * 
	 * @since 2025.04.22
	 */
	public static void info(boolean argExpression, String argMessage, Object... argObjects) {

		// Check if anything to log
		if ((!isActive()) || (!argExpression) || (K.isEmpty(argMessage))) {
			return;
		}
		
		// Write log message
		write(Level.INFO, formatLogMessage(K.replaceParams(argMessage, argObjects)));
	}
	
	/**
	 * Write log message of level INFO.
	 * 
	 * @param	argMessage		Message to be written
	 * @param	argObjects		Optional arguments for {} parameters	 
	 */
	public static void info(String argMessage, Object... argObjects) {

		// Check if anything to log
		if ((!isActive()) || (K.isEmpty(argMessage))) {
			return;
		}

		// Write log message
		write(Level.INFO, formatLogMessage(K.replaceParams(argMessage, argObjects)));
	}
	
	/**
	 * Check if logging is active.
	 * 
	 * @return	True if logging is active, false otherwise
	 */
	public static boolean isActive() {
		return ((gLogLogger != null) && (gLogLogger.getLevel() != Level.OFF));
	}
	
	/**
	 * Check if logger is in debug mode (level FINEST).
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelDebug() {
		return ((gLogLogger != null) && (gLogLogger.getLevel() == Level.FINEST));
	}

	/**
	 * Check if logger is in debug mode (level SEVERE).
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelError() {
		return ((gLogLogger != null) && (gLogLogger.getLevel() == Level.SEVERE));
	}
	
	/**
	 * Check if logger is in debug mode (level INFO).
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelInfo() {
		return ((gLogLogger != null) && (gLogLogger.getLevel() == Level.INFO));
	}
	
	/**
	 * Check if logger is in debug mode (level OFF).
	 * 
	 * @return True if level matches, false otherwise
	 */
	public static boolean isLevelOff() {
		return ((gLogLogger != null) && (gLogLogger.getLevel() == Level.OFF));
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
			String stackElement = stackTraceElement.toString();
			if (!stackElement.contains("ch.k43.util.KLog.")) {
				write(Level.SEVERE, "Stack[" + stackPosition + "]: " + stackElement);
				stackPosition++;
			}
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
		if ((argLevel == null) || K.isEmpty(argData)) {
			return;
		}
	
		// Check if log string matches specified RegEx to be excluded from log 
		if ((gExcludeRegEx != null) && (gExcludeRegEx.matcher(argData).find())) {
			return;
		}
		
		// Check if log string matches specified RegEx to be included in log 
		if ((gIncludeRegEx != null) && (!gIncludeRegEx.matcher(argData).find())) {
			return;
		}
		
		// Prohibit log write recursion by excluding some Java classes
	    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
	    for (StackTraceElement element : stackTrace) {
	        for (String classExclude : EXCLUDE_CLASSES) {
	            if (element.getClassName().equals(classExclude)) {
	                return;
	            }
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
