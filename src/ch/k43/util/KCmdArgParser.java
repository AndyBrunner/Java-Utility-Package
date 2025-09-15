package ch.k43.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Yet another simple command line argument parser.
 * 
 * <P>The pattern to initialize the parser must be in the following format: "xyyyyz:xyyyyz":
 *
 * <pre>
 * <b>Command line pattern syntax:</b>
 * 
 * - Multiple patterns are separated by colons  (e.g. ?-l:?-a)  
 * - 1st character must be be ? for an optional or ! for a required argument
 * - The following string is either a short or long option (e.g. -x or --xxx) or a specifies a single or multiple argument (. or ..)
 * - The last character can be set for an optional ? or required option value ?
 * 
 * <b>Example:</b>
 * 
 * - ?-l:?-a:!.		Allows optional arguments -l and -a, but requires one single argument
 * - ?-n?:!..		Allows the optional argument -n (with an optional value, e.g. -n or -n=10) and at least one argument
 * - ?--header=!	Allows an optional long option. If specified, it requires an option value, e.g. --header=yes
 * - (empty)		Empty pattern prohibits any arguments
 * 
 * <b>Command line parsing rules:</b>
 * 
 * - All arguments are case sensitive
 * - Short options may be combined, e.g. -la instead of -l -a
 * - Short option characters are a-z, A-Z and 0-9
 * - Long option names may include - (e.g. --top-header)
 * - Short or long option may have a value (e.g. -n=10 or --header=none)
 * - All tokens after '--' are treated as arguments to allow arguments with a starting dash, e.g. cmd -l -- -filename.txt
 * </pre>
 * 
 * <pre>
 * <b>Code example:</b>
 * 
 * KCmdArgParser argParser = new KCmdArgParser("?-l:?-a:!.");
 * 
 * if (!argParser(args)) {
 *    System.err.println("Syntax error: " + argParser.getLastError());
 *    System.err.println("Usage: cmd -l -a filename");
 *    System.exit(1);
 * }
 * 
 * if (argParser.hasOption("-l")) {
 *    ...
 * }
 * </pre>
 * 
 * @since 2025.09.02
 */
public class KCmdArgParser {

	// Pattern definitions
	HashMap<String, Boolean>	gPatternNames				= new HashMap<>();
	HashMap<String, Boolean>	gPatternValueRequired		= new HashMap<>();
	HashMap<String, Boolean>	gPatternValueOptional		= new HashMap<>();
	
	// Parsed arguments
	HashMap<String, String>		gOptionNames				= null;
	ArrayList<String>			gArguments					= null;

	// Variables
	String						gCommandLine				= null;
	String						gLastError					= null;
	
	/**
	 * Initialize the parser with the syntax pattern for each valid command line argument.
	 *  
	 * @param argPattern	Command line pattern rule or null to prevent any argument
	 */
	public KCmdArgParser(String argPattern) {

		// Valid RegEx patterns
		final String	REGEX_PATTERN_SYNTAX		= "^[!?].+?[!?]?$";							// e.g. ?-a, !-b?, ?--header?, ?., !..
		final String	REGEX_PATTERN_SHORT_OPTION	= "^-[a-zA-Z0-9]$";							// e.g. -a, -0
		final String	REGEX_PATTERN_LONG_OPTION	= "^--[A-Za-z0-9][A-Za-z0-9-]*$";			// e.g. --a, --help, --no-header
		final String	REGEX_PATTERN_CMD_ARGUMENT	= "^\\.{1,2}$";								// . or ..
		
		// Check if no arguments are supported 
		if (K.isEmpty(argPattern)) {
			return;
		}
		
		KLog.debug("Processing argument syntax pattern: {}", argPattern);
		
		// Process all pattern definitions separated by colons
		String[] argArray = argPattern.split(":");

		for (String arg : argArray) {

			// Ignore empty token
			arg = arg.trim();

			if (arg.isEmpty()) {
				continue;
			}
			
			// Check syntax
			KLog.argException(!arg.matches(REGEX_PATTERN_SYNTAX), "Invalid parser syntax definition: {}", arg);

			// Process required or optional flag
			boolean argRequired			= arg.startsWith("!");
			arg = arg.substring(1);

			// Process required or optional option value
			boolean argValueRequired	= arg.endsWith("!");
			boolean argValueOptional	= arg.endsWith("?");
			arg = arg.replaceFirst("[!?]$", "");
			
			// Categorize type
			boolean shortOption			= arg.matches(REGEX_PATTERN_SHORT_OPTION);
			boolean longOption			= arg.matches(REGEX_PATTERN_LONG_OPTION);
			boolean cmdArgument			= arg.matches(REGEX_PATTERN_CMD_ARGUMENT);

			KLog.argException(!shortOption && !longOption && !cmdArgument, "Invalid pattern syntax: {}", arg);
			
			// Process argument
			if (cmdArgument) {

				// Check if option value flag given
				KLog.argException(argValueRequired || argValueOptional, "Argument syntax does not allow option value flag");
				
				KLog.argException(gPatternNames.containsKey(".") || gPatternNames.containsKey(".."), "Only single argument pattern allowed: {}", arg);

				gPatternNames.put(arg, argRequired);
				gPatternValueRequired.put(arg, false);
				gPatternValueOptional.put(arg, false);

			} else {
				
				KLog.argException(gPatternNames.containsKey(arg), "Duplicate option pattern not allowed: {}", arg);
				
				gPatternNames.put(arg, argRequired);
				gPatternValueRequired.put(arg, argValueRequired);
				gPatternValueOptional.put(arg, argValueOptional);
			}
		}
		
		// Log definitions
		for (Map.Entry<String, Boolean> entry : gPatternNames.entrySet()) {

			String	keyName		= entry.getKey();
			boolean	keyRequired	= entry.getValue();
			
			KLog.debug("Argument: {} (required {}) - Option value: required {}, optional {}", keyName, keyRequired, gPatternValueRequired.get(keyName), gPatternValueOptional.get(keyName));
		}
		
		KLog.debug("Parsed {} argument definitions", gPatternNames.size());
	}
	
	/**
	 * Clear the global object variables.
	 */
	private void clearData() {
		
		// Initialize
		gOptionNames	= new HashMap<>();
		gArguments		= new ArrayList<>();
		gCommandLine	= null; 
		gLastError		= null;
	}
	
	/**
	 * Return number of command line arguments.
	 * 
	 * @return	Number of command line arguments
	 */
	public int getArgumentCount() {
		return gArguments.size();
	}
	
	/**
	 * Return all command line arguments.
	 * 
	 * @return	Array with all command line arguments.
	 */
	public String[] getArguments() {
		return gArguments.toArray(new String[0]);
	}

	/**
	 * Return all arguments as given in the command line (e.g. "-l -a arg1 arg2 arg3").
	 * 
	 * @return	Command line arguments separated by a blank character or null if no argument were given
	 */
	public String getCommandLine() {
		return gCommandLine;
	}
	
	/**
	 * Return the last error message or null for no errors.
	 * 
	 * @return	Last error message
	 */
	public String getLastError() {
		return gLastError;
	}
	
	/**
	 * Get the passed option value.
	 * 
	 * @param	argOption	Option name (e.g. -a or --headers)
	 * @return	Option value or null if no value
	 */
	public String getOptionValue(String argOption) {
		return gOptionNames.get(argOption);
	}
	
	/**
	 * Return the first command line argument.
	 * 
	 * @return	First command line argument or null.
	 */
	public String getArgument() {

		if (!gArguments.isEmpty()) {
			return gArguments.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * Check if any command line argument given.
	 * 
	 * @return	True if any argument given or false
	 */
	public boolean hasArgument() {
		return !gArguments.isEmpty();
	}
	
	/**
	 * Check if the passed option is present.
	 * 
	 * @param	argOption	Option name (e.g. -l or --headers)
	 * @return	True if found, else otherwise
	 */
	public boolean hasOption(String argOption) {
		return gOptionNames.containsKey(argOption);
	}

	/**
	 * Check if the passed option value is present.
	 * 
	 * @param	argOption	Option name (e.g. -l or --headers)
	 * @return	True if found, else otherwise
	 */
	public boolean hasOptionValue(String argOption) {
		return (gOptionNames.get(argOption) != null);
	}
	
	/**
	 * Parse the passed command line arguments.
	 * 
	 * @param	argArgs	Array of Strings as passed to any main() method.
	 * @return	True if parsing successful, false otherwise
	 * 
	 * @see		getLastError() 
	 */
	public boolean parse(String[] argArgs) {
		
		// Initialize
		clearData();

		// Save passed arguments as given in the command line
		gCommandLine = String.join(" ", argArgs);
		
		KLog.debug("Parsing command line arguments: {}", gCommandLine);
		
		//
		// Process all arguments
		//
		boolean endOfOptions = false;
		
		for (String arg : argArgs) {
		
			//
			// Ignore empty argument
			//
			arg = arg.trim();
			
			if (arg.isEmpty()) {
				continue;
			}
			
			//
			// Categorize token and perform some basic syntax checks
			//
			int		argLength		= arg.length();
			boolean	longOption 		= false;
			boolean shortOption 	= false;
			boolean cmdArgument		= false;

			//
			// Long option
			//
			if (!endOfOptions && argLength > 1 && arg.startsWith("--")) {

				longOption = true;
				
				// Treat everything after a double dash as an argument to allow e.g. cmd -a -- -filename.txt
				if (argLength == 2) {
					endOfOptions = true;
					// Get next argument
					continue;
				}
			}

			//
			// Short option
			//
			if (!endOfOptions && !longOption && arg.charAt(0) == '-') {

				shortOption = true;
				
				if (argLength == 1) {
					return saveError("Invalid short-option syntax on the command line: {}", arg);
				}
			}

			// Set as command line argument if not short/long option and no previous '--' found
			cmdArgument = (endOfOptions || (!shortOption && !longOption));

			//
			// Command argument
			//
			if (cmdArgument) {
				
				// Check if any command line argument is allowed
				if (!gPatternNames.containsKey(".") && !gPatternNames.containsKey("..")) {
					return saveError("No command-line arguments are supported: {}", arg);
				}

				// Check if a single command line argument is already specified
				if (gPatternNames.containsKey(".") && !gArguments.isEmpty()) {
					return saveError("Multiple command-line arguments are not supported: {}", arg);
				}
				
				// Save argument and get next argument
				gArguments.add(arg);
				continue;
			}

			//
			// Short option: Split multiple options (e.g. -la into -l -a)
			//
			if (shortOption) {
				
				// Check if short option has a value (e.g. -n=50)
				if (arg.length() > 2 && arg.charAt(2) == '=') {

					if (!storeOption(arg)) {
						return false;
					}
					
				} else {

					// Create array entry for each option
					for (int index = 1; index < arg.length(); index++) {

						if (!storeOption("-" + arg.charAt(index))) {
							return false;
						}
					}
				}
			}
			
			//
			// Long option
			//
			if (longOption && !storeOption(arg)) {
				return false;
			}
		}

		// Log all command line options
		for (Map.Entry<String, String> entry : gOptionNames.entrySet()) {
			String	keyName		= entry.getKey();
			String	keyValue	= entry.getValue();
			KLog.debug("Command line option: {}{}", keyName, (keyValue != null) ? "=" + keyValue : "");
		}
		
		// Log all command line arguments
		for (String param : gArguments) {
			KLog.debug("Command line argument: {}", param);
		}
	
		//
		// Check if all required tokens were given
		//
		for (Map.Entry<String, Boolean> entry : gPatternNames.entrySet()) {
			
			String	keyName		= entry.getKey();
			boolean	keyRequired	= entry.getValue();
			
			if (keyRequired) {
				
				// Single argument
				if (keyName.equals(".")) {
					
					if (gArguments.isEmpty()) {
						return saveError("Required command-line argument is missing");
					}

					if (gArguments.size() > 1) {
						return saveError("More than one command-line argument was given");
					}
				}
				
				// Multiple arguments
				if (keyName.equals("..") && gArguments.isEmpty()) {
					return saveError("A required command-line argument is missing");
				}
				
				// Check option and value
				if (!keyName.startsWith(".")) {

					if (!gOptionNames.containsKey(keyName)) {
						return saveError("Required command-line option not specified: {}", keyName);
					}
					
					if ((gPatternValueRequired.get(keyName) == Boolean.TRUE) && (gOptionNames.get(keyName) == null)) {
						return saveError("Required value not set for command-line option: {}", keyName);
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Check syntax and add option to stored array.
	 * 
	 * @param argOption		Option with optional value
	 * @return True if successful, false otherwise
	 */
	private boolean storeOption(String argParam) {
		
		// Valid RegEx patterns
		final String	REGEX_SHORT_OPTION	= "^-[a-zA-Z0-9]*$";							// e.g. -l, -1
		final String	REGEX_LONG_OPTION	= "--[A-Za-z0-9][A-Za-z0-9-]*";					// e.g. --x, --20, --noheader, --top-header

		// Initialize
		String	optionName	 	= null;
		String	optionValue		= null;
		boolean valueGiven		= argParam.contains("=");
	
		// Check if option has a value (e.g. -n=50 or --header=none)
		if (valueGiven) {

			if (argParam.endsWith("=")) {
				return saveError("Option value not found for command-line parameter: {}", argParam);
			}
			
			String[] splitter	= argParam.split("=", 2);
			optionName			= splitter[0];
			optionValue			= splitter[1];
			
		} else {
			
			optionName			= argParam;
			optionValue 		= null;
		}

		// Check if short option followed by alphanumeric character or long option followed by alphanumeric characters with optional dash (e.g. --header-name)
		if (!optionName.matches(REGEX_SHORT_OPTION) && (!optionName.matches(REGEX_LONG_OPTION))) {
			return saveError("Invalid option syntax: {}", optionName);
		}
		
		// Check if duplicate option
		if (gOptionNames.containsKey(optionName)) {
			return saveError("Duplicate command-line option: {}", optionName);
		}
		
		// Check if option name is allowed
		if (!gPatternNames.containsKey(optionName)) {
			return saveError("Command-line option not supported: {}", optionName);
		}

		// Check option value
		boolean	valueRequired	= gPatternValueRequired.get(optionName);
		boolean	valueOptional	= gPatternValueOptional.get(optionName);	
		
		if (optionValue != null && !valueRequired && !valueOptional) {
			return saveError("Value not supported for command-line option: {}", argParam);
		}
		
		// Check if required option value is given
		if (optionValue == null && valueRequired) {
			return saveError("Missing required value for command-line option: {}", optionName);
		}
		
		// Add to options array
		gOptionNames.put(optionName, optionValue);
		
		return true;
	}
	
	/**
	 * Save error message locally and on the Thread KLocalData
	 * 
	 * @param argMessage	Error message
	 * @param argArguments	Optional parameters
	 * @return Always returns false
	 */
	private boolean saveError(String argMessage, Object... argArguments) {
		
		// Clear objects data
		clearData();
		
		// Save error locally and on KLocalData
		gLastError = K.replaceParams(argMessage, argArguments);
		K.saveError(gLastError);
		
		return false;
	}

	/**
	 * String representation of object.
	 */
	@Override
	public String toString() {
		return "KCmdArgParser [gPatternNames=" + gPatternNames + ", gPatternValueRequired=" + gPatternValueRequired
				+ ", gPatternValueOptional=" + gPatternValueOptional + ", gOptionNames=" + gOptionNames
				+ ", gArguments=" + gArguments + ", gCommandLine=" + gCommandLine + ", gLastError=" + gLastError + "]";
	}
}
