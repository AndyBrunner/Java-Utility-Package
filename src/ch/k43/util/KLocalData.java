package ch.k43.util;

import java.util.ArrayList;

/**
 * Class to hold per-thread data to support data objects for static classes and methods.
 * 
 * This data object is saved in a global HashMap where the Thread object is the HashMap key. To be thread-safe, the accessing code
 * needs to implement the required synchronization thru the variable lock. 
 * 
 * @see K.getLocalData(), K.getLastError(), K.getLastErrors(), K.saveError()
 * 
 * @since 2025.02.03
 */
class KLocalData {
	
	// Per-thread variables - Must be synchronized by the accessing thread thru the lock variable
	String				threadName	= null;
	Object				lock		= new Object();
	ArrayList<String>	kLastErrors	= new ArrayList<>(K.MAX_SAVED_ERRORS);

	/**
	 * Constructor
	 */
	KLocalData(String argThreadName) {
		threadName = argThreadName;
	}
}
