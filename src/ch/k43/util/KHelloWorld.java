package ch.k43.util;

/**
 * Hello World example. This class is marked as the main class in the jar file to allow the command "java -jar ch.k43.util.jar"
 * 
 * @since 2025.02.19
 */
public class KHelloWorld {

	/**
	 * Main entry point
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {

		KLog.info("Hello World started");
		
		System.out.println("Java Utility Package (Freeware) Version " + K.VERSION);
		
		if (!KLog.isActive()) {
			System.out.println("Note: To enable logging, place a valid KLog.properties file in the current directory (or use the -DKLogPropertyFile startup parameter)");
		} else {
			System.out.println("Note: Logging is configured in property file " + KLog.PROPERTY_FILE);
		}

		System.out.println("JVM version " + K.JVM_MAJOR_VERSION + " running on " + K.JVM_PLATFORM);
			
		KLog.info("Hello World ended");
	}
	
	/**
	 * Default constructor
	 */
	public KHelloWorld() {
		// Default constructor
	}
}
