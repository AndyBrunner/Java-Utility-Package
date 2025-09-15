package ch.k43.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Implements a fast, simple, thread-safe and named queue as FIFO (first-in-first-out) or LIFO (last-in-first-out).<p>
 * 
 * The name of the queue may be used by other threads to open and access the queue.
 * 
 * <p>Use try-with-resources or explicit close() to ensure queue resources are cleaned up:
 * <pre>
 *   try (KQueue q = new KQueue("MyQueue", KQueue.FIFO)) {
 *   ...
 *   }
 * </pre>
 * 
 * @since 2025.09.15
 */
public final class KQueue implements AutoCloseable {

	/**
	 *  First-in-first out queue mode
	 */
	public static final int									FIFO				= 0x01;

	/**
	 *  Last-in-first out queue mode
	 */
	public static final int									LIFO				= 0x02;
 	
	// Declarations
	private static final ConcurrentHashMap<String, KQueue>	QUEUE_REGISTRY		= new ConcurrentHashMap<>();
	private static final Object								SENTINEL			= new Object();

	private final LinkedBlockingDeque<Object>				gQueue				= new LinkedBlockingDeque<>();
	private String											gQueueName			= null;
	private volatile int									gQueueMode			= 0x00;
	private volatile boolean								gClosed				= false;

	/**
	 * Create a FIFO or LIFO queue. The generated name may be obtained thru getName(). 
	 * 
	 * @param argQueueMode	The type of the queue (LIFO or FIFO)
	 */
	public KQueue(int argQueueMode) {
		this(K.getUniqueID(), argQueueMode);
	}

	/**
	 * Create a FIFO or LIFO named queue.
	 * 
	 * @param argQueueName	The name of the queue
	 * @param argQueueMode	The type of the queue (LIFO or FIFO)
	 */
	public KQueue(String argQueueName, int argQueueMode) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argQueueName), "Required queue name missing");
		KLog.argException(argQueueMode != FIFO && argQueueMode != LIFO, "Queue mode must be FIFO or LIFO");

		// Save queue name
		gQueueName = argQueueName;
		
		// Save FIFO/LIFO queue type
		gQueueMode = argQueueMode;
		
		// Save KQueue object in HashMap for later retrieval thru static method open()
		if (QUEUE_REGISTRY.putIfAbsent(gQueueName, this) != null) {
			KLog.argException("Queue name {} already exist", gQueueName);
		}
		
		KLog.debug("{} queue {} created", gQueueMode == FIFO ? "FIFO" : "LIFO", gQueueName);
	}
	
	/**
	 * Return a list of all queue names.
	 * 
	 * @return	List of names
	 */
	public static String[] getAllNames() {
		return QUEUE_REGISTRY.keySet().toArray(new String[0]);
	}
	
	/**
	 * Return the queue if present.
	 * 
	 * @param	argQueueName	The name of the queue
	 * @return	Queue object
	 */
	public static KQueue open(String argQueueName) {

		// Check argument
		KLog.argException(K.isEmpty(argQueueName), "Required queue name missing");

		// Return KQueue object for given queue name
		KQueue queue = QUEUE_REGISTRY.get(argQueueName);

		if (queue == null) {
			KLog.error("Queue {} does not exist", argQueueName);
			return null;
		} else {
			KLog.debug("{} queue {} opened", queue.gQueueMode == FIFO ? "FIFO" : "LIFO", queue.getName());
			return queue;
		}
	}

	/**
	 * Clear (empty) the queue.
	 */
	public void clear() {
		
		// Check if queue closed
		if (gClosed) {
			return;
		}
		
		gQueue.clear();
	}
	
    /**
	 * Close the queue.
	 */
	public void close() {

		// Check if queue closed
		if (gClosed) {
			return;
		}
		
		// Mark the queue as closed, empty the queue and send sentinel to wake up any consumer(s)
		gClosed = true;

		gQueue.clear();
		gQueue.offerLast(SENTINEL);
		
		// Remove the queue from the active list
		if (QUEUE_REGISTRY.remove(gQueueName) == null) {
			KLog.error("Queue {} could not be removed from the active list of queues", gQueueName);
		} else {
			KLog.debug("Queue {} closed", gQueueName);
		}
	}
	
	/**
     * Return the next queue element and wait if queue is empty.
     * 
	 * @return	Queue element or null for errors
     */
	public Object get() {
		
		// Check if queue closed
		raiseExceptionIfClosed();
		
		return get(true);
	}
	
	/**
     * Return the next queue element and optionally wait if queue is empty.
     * 
	 * @param	argWait If true, wait for next element, false otherwise
	 * @return	Queue element or null if no element found or error
	 */
	public Object get(boolean argWait) {

		// Check if queue closed
		raiseExceptionIfClosed();
				
		// Get next element in queue
		Object queueElement = null;
		
		if (!argWait) {
			// Get element without blocking
			queueElement = (gQueueMode == FIFO) ? gQueue.pollFirst() : gQueue.pollLast();  
		} else {
			// Get element with blocking if necessary 
			try {
				queueElement = (gQueueMode == FIFO) ? gQueue.takeFirst() : gQueue.takeLast();  
			} catch (InterruptedException e) {
				KLog.error(e);
				Thread.currentThread().interrupt();
			}
		}

		// Check if sentinel placed in queue by close() to wake up consumers
		if (queueElement == SENTINEL) {
			
			// Place sentinel back in queue for other consumers
			gQueue.offerLast(SENTINEL);
			KLog.error("Queue {} has been closed", gQueueName);
			return null;
		}
		
		return queueElement;
	}
	
	/**
	 * Get the mode of the queue (FIFO or LIFO).
	 * 
	 * @return	FIFO or LIFO mode
	 */
	public int getMode() {
		return gQueueMode;
	}
	
	/**
	 * Get the name of the queue.
	 * 
	 * @return	Name of the queue
	 */
	public String getName() {
		return gQueueName;
	}
	
	/**
	 * Get number of elements in queue.
	 * 
	 * @return	Number of elements
	 */
	public int getSize() {

		// Check if queue closed
		raiseExceptionIfClosed();
		
		return gQueue.size();
	}
	
	/**
	 * Check if queue is closed.
	 * 
	 * @return	True if queue is closed, false otherwise
	 */
	public boolean isClosed() {
		return gClosed;
	}
	
	/**
	 * Check if queue is empty.
	 * 
	 * @return	True if queue is empty, false otherwise
	 */
	public boolean isEmpty() {

		// Check if queue closed
		raiseExceptionIfClosed();
		
		return gQueue.isEmpty();
	}
	
	/**
     * Return the next queue element without removing it.
     * 
	 * @return	Queue element or null if no element found or error
	 */
	public Object peek() {

		// Check if queue closed
		raiseExceptionIfClosed();
				
		// Get next element in queue
		Object queueElement = (gQueueMode == FIFO) ? gQueue.peekFirst() : gQueue.peekLast();  

		// Check if sentinel placed in queue by close() to wake up consumers
		if (queueElement == SENTINEL) {
			
			// Place sentinel back in queue for other consumers
			gQueue.offerLast(SENTINEL);
			KLog.error("Queue {} has been closed", gQueueName);
			return null;
		}
		
		return queueElement;
	}
	
	/**
	 * Add an element to the queue.
	 * 
	 * @param argObject	Element to add to the queue
	 * @return	True if success, false otherwise
	 */
	public boolean put(Object argObject) {
	
		// Check argument
		KLog.argException(K.isEmpty(argObject), "Missing required element");
		
		// Check if queue closed
		raiseExceptionIfClosed();
		
		try {
			gQueue.putLast(argObject);
			return true;
		} catch (InterruptedException e) {
			KLog.error(e);
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	/**
	 * Check if queue is closed. If so, this method raises an IllegalArgumentException.
	 */
    private void raiseExceptionIfClosed() {
    	KLog.argException(gClosed, "Queue is in closed state");
    }
	
    /**
     * Set queue mode (LIFO or FIFO).
     * 
     * @param argQueueMode	LIFO or FIFO queue mode
     */
    public void setMode(int argQueueMode) {
    	
    	// Check argument
		KLog.argException(argQueueMode != FIFO && argQueueMode != LIFO, "Queue mode must be FIFO or LIFO");
		
		// Check if queue closed
		raiseExceptionIfClosed();
		
		gQueueMode = argQueueMode;
    }
    
	/**
	 * String representation of object.
	 * 
	 * @return	Object representation
	 */
	@Override
	public String toString() {
		return "KQueue [gQueue=" + gQueue + ", gQueueName=" + gQueueName + ", gQueueMode=" + gQueueMode + ", gClosed="
				+ gClosed + "]";
	}
}