package ch.k43.util;

import java.util.Calendar;

/**
 * Simple timer class.<br>
 * 
 * <pre>
 * Example:
 * 
 * KTimer timer = new KTimer();
 * K.waitSeconds(1);
 * System.out.println("Elapsed time: " + timer.getElapsedMilliseconds() + " ms");
 * </pre>
 */
public class KTimer {

	// Class variables
	private Calendar gStartTime	= null;
	
	/**
	 * Start a timer.
	 */
	public KTimer() {
		// Save current date and time
		gStartTime = Calendar.getInstance();
	}

	/**
	 * Return elapsed time in milliseconds.
	 * 
	 * @return	Elapsed time in milliseconds
	 */
	public synchronized long getElapsedMilliseconds() {
		
		// Return elapsed time in milliseconds
		return Calendar.getInstance().getTimeInMillis() - gStartTime.getTimeInMillis();
	}
	
	/**
	 * Return start time.
	 * 
	 * @return	Date/time of timer start
	 * 
	 * @since 2024.05.28
	 */
	public Calendar getStartTime() {
		return gStartTime;
	}

	/**
	 * Reset (restart) timer.
	 * 
	 * @return	Date/time of timer restart
	 * 
	 * @since 2025.01.28
	 */
	public synchronized Calendar reset() {
		
		// Set new start time
		gStartTime = Calendar.getInstance();
		
		return gStartTime;
	}
	
	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KTimer [gStartTime=" + gStartTime + "]";
	}
}
