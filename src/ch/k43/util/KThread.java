package ch.k43.util;

/**
 * This class adds some convenient methods for starting and stopping Java threads.
 * 
 *  <pre>
 *  <b>Example: Start/stop thread</b>
 *  
 *  // Create thread and call kStart() method
 *  KThread thread = new TestThread();
 *  ...
 *  
 *  // Signal termination and send interrupt to thread 
 *  thread.kStop();
 *  </pre>
 *  
 *  <pre>
 *  <b>Example: User thread</b>
 *  public class TestThread extends KThread {
 *  
 *  	public TestThread() {
 *			...
 *  	}
 *  
 *  	public void kStart() {
 *			...
 *			// Run until kStop() is called 
 *			while (!kMustTerminate()) {
 *			...
 *			}
 *  	}
 *  
 *  	public synchronized void kCleanup() {
 *			// Do any resource cleanup (will be called automatically)
 *  	}
 *  }
 *  </pre>
 *  
 * @since 2024.09.06
 */
public abstract class KThread extends Thread {

	// Class variables
	private KThread				gThread				= null;
	private String				gThreadName			= null;
	private volatile boolean 	gMustTerminate		= false;	
	
	/**
	 * Initialize thread
	 */
	protected KThread() {
		
		// Save thread and its name
		gThread		= this;
		gThreadName = gThread.getClass().getName();
		
		// Start the thread: Thread.start() -> Thread.run -> KThread.run() -> KThread.kStart() -> (User Code) -> KThread.kCleanup()
		start();
	}
	
	/**
	 * Cleanup method called by run() just before termination.
	 */
	public synchronized void kCleanup() {
		// May by overridden by user thread to cleanup any resources 
	}
	
	/**
	 * Check if thread should be terminated.
	 * 
	 * @return	True (if kStop() was called previously), false otherwise
	 * 
	 * @see kStop()
	 */
	public final boolean kMustTerminate() {
		return (gMustTerminate);
	}
	
	/**
	 * Main entry point for user thread. This method must be implemented by the subclass.
	 */
	public abstract void kStart();
	
	/**
	 * Set the thread termination flag and interrupt the thread. 
	 */
	public final void kStop() {
		kStop(true);
	}

	/**
	 * Set the thread termination flag and optionally send interrupt to the thread.  
	 * 
	 * @param	argInterrupt	Send interrupt signal to thread
	 */
	public final synchronized void kStop(boolean argInterrupt) {
		
		// Set termination flag
		gMustTerminate = true;
		
		// Send interrupt to thread
		if (argInterrupt) {
			gThread.interrupt();
			KLog.debug("{} marked for termination and interrupt sent", gThreadName);
		} else {
			KLog.debug("{} marked for termination", gThreadName);
		}
	}
	
	/**
	 * Start thread by calling method kStart(). After kStart() returns, the method kCleanup() is automatically called to allow
	 * for any resource cleanup by the user thread.
	 */
	@Override
	public final void run() {

		KLog.debug("{} started", gThreadName);

		// invoke main thread entry point
		try {
			kStart();
		} catch (Exception e) {
			KLog.error(e);
		}
		
		// Call thread cleanup
		try {
			kCleanup();
		} catch (Exception e) {
			KLog.error(e);
		}
		
		KLog.debug("{} terminated", gThreadName);
	}
	
	/**
	 * Prohibit overwriting. 
	 */
	@Override
	public final synchronized void start() {
		super.start();
	}

	/**
	 * Output object data.
	 */
	@Override
	public String toString() {
		return "KThread [gThread=" + gThread + ", gThreadName=" + gThreadName + ", gMustTerminate=" + gMustTerminate
				+ "]";
	}
}
