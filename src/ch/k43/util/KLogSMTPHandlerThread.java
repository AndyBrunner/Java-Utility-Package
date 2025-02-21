package ch.k43.util;

/**
 * This thread is started by the SMTP logging handler to send the SMTP mail.
 * 
 * @since 2024.08.26
 */
public class KLogSMTPHandlerThread extends KThread {
	
	// Class variables
	private KSMTPMailer gSMTPMail			= null;
	private boolean		gDebugLog			= false;
	private String		gClassName			= null;
	
	/**
	 * Constructor: Initialize the SMTP mail worker thread.
	 * 
	 * @param argMail	Completed and ready to send SMTP mail object
	 * @param argDebug	True to send debug messages to console, false otherwise
	 */
	public KLogSMTPHandlerThread(KSMTPMailer argMail, boolean argDebug) {
		
		gClassName		= this.getName();
		gSMTPMail		= argMail;
		gDebugLog		= argDebug;
	}
	
	@Override
	public void kStart() {

		logDebug("SMTP worker: Thread started");
		
		try {
			if (!gSMTPMail.send()) {
				logError("SMTP worker: Mail send failed: " + gSMTPMail.getErrorMessage());
			} else {
				logDebug("SMTP worker: Mail successfully sent");
			}
		} catch (Exception e) {
			logError("SMTP worker: " + e.toString());
		}
		
		logDebug("SMTP worker: Terminated");
	}
		
	/**
	 * Write debug log.
	 * 
	 * @param argMessage	Message to be logged
	 */
	private void logDebug(String argMessage) {

		if (gDebugLog) {
			System.out.println(K.getTimeISO8601() + " D " + gClassName + ": " + argMessage);
		}
	}

	/**
	 * Write error log.
	 * 
	 * @param argMessage	Message to be logged
	 */
	private void logError(String argMessage) {

		if (gDebugLog) {
			System.err.println(K.getTimeISO8601() + " E " + gClassName + ": " + argMessage);
		}
	}

	/**
	 * Output objects data.
	 */
	@Override
	public String toString() {
		return "KLogSMTPHandlerThread [gSMTPMail=" + gSMTPMail + ", gDebugLog=" + gDebugLog + ", gClassName="
				+ gClassName + "]";
	}
}
