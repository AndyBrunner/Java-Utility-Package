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
	private Calendar	gStartDateTime	= null;
	private long		gStartTimeNs	= 0;
	
	/**
	 * Start a timer.
	 */
	public KTimer() {
		reset();
	}

	/**
	 * Return elapsed time in milliseconds.
	 * 
	 * @return	Elapsed time in milliseconds
	 */
	public long getElapsedMilliseconds() {
		return (System.nanoTime() - gStartTimeNs) / 1_000_000;
	}

	/**
	 * Return elapsed time in nanoseconds.
	 * 
	 * @return	Elapsed time in nanoseconds
	 * 
	 * @since 2025.05.19
	 */
	public long getElapsedNanoseconds() {
		return System.nanoTime() - gStartTimeNs;
	}

	
	/**
	 * Return start time.
	 * 
	 * @return	Date/time of timer start
	 * 
	 * @since 2024.05.28
	 */
	public Calendar getStartTime() {
		return gStartDateTime;
	}

	/**
	 * Reset (restart) timer.
	 * 
	 * @return	Date/time of timer restart
	 * 
	 * @since 2025.01.28
	 */
	public synchronized Calendar reset() {
		gStartTimeNs	= System.nanoTime();
		gStartDateTime	= Calendar.getInstance();
		return gStartDateTime;
	}
	
	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KTimer [gStartDateTime=" + gStartDateTime + ", gStartTimeNs=" + gStartTimeNs + "]";
	}
}
