package ch.k43.util;

import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Java logging handler to send logging data to an SMTP host. To avoid a high number of emails sent to the server, 
 * only log data of type error SEVERE is sent. Additionally the number of emails per minutes is restricted.
 * 
 * The following properties are supported:
 * <pre>
 * ch.k43.util.KLogSMTPHandler.mail.from = Sender email address
 * ch.k43.util.KLogSMTPHandler.mail.to = recipient(s)
 * ch.k43.util.KLogSMTPHandler.mail.subject = subject (default "KLog Error Report")
 * ch.k43.util.KLogSMTPHandler.smtp.hostname = host name (default MX record of first recipient domain)
 * ch.k43.util.KLogSMTPHandler.smtp.hostport = host port (default 25)
 * ch.k43.util.KLogSMTPHandler.smtp.username = user name for authentication (default none)
 * ch.k43.util.KLogSMTPHandler.smtp.password = user password for authentication (default none)
 * ch.k43.util.KLogSMTPHandler.smtp.tls = true/false (default true)
 * ch.k43.util.KLogSMTPHandler.threshold = nn (maximum number of emails per minute, 1 to 10, default 3)
 * ch.k43.util.KLogSMTPHandler.debug = true/false (Output sent to System.out/System.err, default false)	
 * </pre>
 * 
 * @since 2024.08.26
 */

public class KLogSMTPHandler extends Handler {

	// Class variables
	private String		gClassName			= this.getClass().getName();
	private String		gSMTPHostName		= null;
	private String		gSMTPUsername		= null;
	private String		gSMTPPassword		= null;
	private String		gMailFrom			= null;
	private String		gMailTo				= null;
	private String		gMailSubject		= null;
	private String		gTimeCurrentMinute	= K.getTimeISO8601().substring(0, 16);
	private String		gLastTraceLocation	= "N/A";
	private int			gSMTPHostPort		= 25;
	private int			gThreshold			= 3;
	private int			gCurrentThreshold	= 0;
	private boolean		gSMTPTLS			= true;
	private boolean		gDebugLogActive		= false;								// Enable only during development and testing
	private boolean		gIsActive			= false;
	
	/**
	 * Class constructor.
	 */
	public KLogSMTPHandler()  {

		// Read properties
		Properties logProps = KFile.readPropertiesFile(KLog.PROPERTY_FILE);
		
		if (logProps == null) {
			return;
		}

		// ch.k43.util.KLogSMTPHandler.debug
		String debugValue = logProps.getProperty(gClassName + ".debug", "false").trim();
		
		if (debugValue.equals("true")) {
			gDebugLogActive = true;
		}
		logDebug("Logging handler initalizing");
		
		// ch.k43.util.KLogSMTPHandler.mail.from
		gMailFrom = logProps.getProperty(gClassName + ".mail.from", "").trim();

		if (K.isEmpty(gMailFrom)) {
			logError("Property " + gClassName + ".mail.from must be set");
			return;
		}
		
		// ch.k43.util.KLogSMTPHandler.mail.to
		gMailTo = logProps.getProperty(gClassName + ".mail.to", "").trim();

		if (K.isEmpty(gMailTo)) {
			logError("Property " + gClassName + ".mail.to must be set");
			return;
		}

		// ch.k43.util.KLogSMTPHandler.mail.subject
		gMailSubject = logProps.getProperty(gClassName + ".mail.subject", "KLog Error Report").trim();
		
		// ch.k43.util.KLogSMTPHandler.threshold
		String threadhold = logProps.getProperty(gClassName + ".threshold", "").trim();

		if (!K.isInteger(threadhold, 1, 10)) {
			logError("Property " + gClassName + ".threshold must be between 1 and 10");
			return;
		}
		gThreshold = Integer.parseInt(threadhold);

		// ch.k43.util.KLogSMTPHandler.smtp.hostname
		gSMTPHostName = logProps.getProperty(gClassName + ".smtp.hostname", "").trim();
		
		// ch.k43.util.KLogSMTPHandler.smtp.hostport
		String hostport	= logProps.getProperty(gClassName + ".smtp.hostport", "25").trim();
		
		if (!K.isInteger(hostport, 1, 65535)) {
			logDebug("Property " + gClassName + ".smtp.hostport must be between 1 and 65535");
			return;
		}
		gSMTPHostPort = Integer.parseInt(hostport);
		
		// ch.k43.util.KLogSMTPHandler.smtp.username
		gSMTPUsername = logProps.getProperty(gClassName + ".smtp.username", "").trim();
		
		// ch.k43.util.KLogSMTPHandler.smtp.password
		gSMTPPassword = logProps.getProperty(gClassName + ".smtp.password", "").trim();
		
		// ch.k43.util.KLogSMTPHandler.smtp.tls
		String smtpTLS = logProps.getProperty(gClassName + ".smtp.tls", "true").trim();
		
		if (smtpTLS.equalsIgnoreCase("false")) {
			gSMTPTLS = false;
		}

		// Log objects data
		logDebug(toString());
		
		// Enable driver
		gIsActive = true;
		
		logDebug("Logging handler initalized");
	}
	
	/**
	 * Close the handler.
	 */
	public void close() {
		logDebug("Logging handler terminated");
	}

	/**
	 * Flush the data.
	 */
	public void flush() {
		// Not implemented
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
	 * Write error log.
	 * 
	 * @param argMessage	Message to be logged
	 */
	private void logError(String argMessage) {

		if (gDebugLogActive) {
			System.err.println(K.getTimeISO8601() + " E " + gClassName + ": " + argMessage);
		}
	}
			
	/**
	 * Send log record a email to SMTP host.
	 * 
	 * @param	argRecord Log record
	 */
	public void publish(LogRecord argRecord) {
		
		// Check if driver is active
		if (!gIsActive) {
			return;
		}
	
		// Check if logging level is SEVERE
		String logRecordLevel = argRecord.getLevel().toString().toUpperCase();
		
		if (!logRecordLevel.equalsIgnoreCase("SEVERE")) {
			return;
		}

		// Check if threshold reached (number of email per minute)
		String timeCurrentMinute = K.getTimeISO8601().substring(0, 16);

		if (gTimeCurrentMinute.equals(timeCurrentMinute)) {
			if (++gCurrentThreshold > gThreshold) {
				logError("Maximum number of log entries per minute excceded");
				return;
			}
		} else {
			gTimeCurrentMinute	= timeCurrentMinute;
			gCurrentThreshold	= 0;
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
		
		// Create mail message
		KSMTPMailer mailer = new KSMTPMailer();

		mailer.setFrom(gMailFrom);
		mailer.setTo(gMailTo);
		mailer.setSubject(gMailSubject);
		
		if (!K.isEmpty(gSMTPHostName)) {
			mailer.setSMTPHost(gSMTPHostName, gSMTPHostPort);
		}
		
		if (!gSMTPTLS) {
			mailer.setSecureConnection(false);
		}
		
		if (!K.isEmpty(gSMTPUsername)) {
			mailer.setAuthentication(gSMTPUsername, gSMTPPassword);
		}
		
		mailer.addHTML("<h2>Error Report</h2>"
				+ "The following error has occurred:<p>"
				+ "<b>" + traceMessage + "</b><p>"
				+ "Time:<br>"
				+ K.getTimeISO8601() + "<p>"
				+ "Code Location:<br>"
				+ traceLocation + "<p>"
				+ "<i>This is an automated e-mail. Please do not reply to this message.</i><br>"
				);
		
		//
		// Start SMTP mailer thread to send email without blocking
		//
		new KLogSMTPHandlerThread(mailer, gDebugLogActive);
		logDebug("Message sent to SMTP worker thread");
	}

	/**
	 * String representation of object.
	 */
	@Override
	public String toString() {
		return "KLogSMTPHandler [gClassName=" + gClassName + ", gSMTPHostName=" + gSMTPHostName + ", gSMTPUsername="
				+ gSMTPUsername + ", gSMTPPassword=" + gSMTPPassword + ", gMailFrom=" + gMailFrom + ", gMailTo="
				+ gMailTo + ", gMailSubject=" + gMailSubject + ", gTimeCurrentMinute=" + gTimeCurrentMinute
				+ ", gLastTraceLocation=" + gLastTraceLocation + ", gSMTPHostPort=" + gSMTPHostPort + ", gThreshold="
				+ gThreshold + ", gCurrentThreshold=" + gCurrentThreshold + ", gSMTPTLS=" + gSMTPTLS
				+ ", gDebugLogActive=" + gDebugLogActive + ", gIsActive=" + gIsActive + "]";
	}
}
