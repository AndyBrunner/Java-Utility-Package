package ch.k43.util;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to support all JDBC compliant databases with basic database functions. The result set from SELECT statements is kept
 * in memory with easy to access objects and functions.
 * 
 * Example:<br>
 * <pre>
 * try (KDB db = new KDB(KDB.JDBC_H2, "jdbc:h2:mem:mydb", "", "")) {
 *
 *    KLog.abort(!db.isConnected(), "Error: {}", db.getErrorMessage());
 *		
 *	  db.exec("CREATE TABLE addresses (sequence INT AUTO_INCREMENT, lastname VARCHAR(20), firstname VARCHAR(20))");
 *		   
 *	  db.prepare("INSERT INTO addresses (lastname, firstname) VALUES (?, ?)");
 *	  db.execPrepare("Smith", "Joe");
 *	  db.execPrepare("Miller", "Bob");
 *	  db.execPrepare("Johnson", "Evelyn");
 *	  db.exec("SELECT * FROM addresses");
 *
 *    System.out.println(db.getDataAsJSON());
 * </pre>
 * 
 * @since 2024.06.14
 */
public class KDB implements AutoCloseable{
	
	// Predefined JDBC driver names
	
	/** 
	 * Oracle JDBC driver
	 */
	public static final String	JDBC_ORACLE		= "oracle.jdbc.driver.OracleDriver";

	/** 
	 * MySQL JDBC driver
	 */
	public static final String	JDBC_MYSQL		= "com.mysql.jdbc.Driver";

	/** 
	 * H2 JDBC driver
	 */
	public static final String	JDBC_H2			= "org.h2.Driver";

	/** 
	 * DB2 JDBC driver
	 */
	public static final String	JDBC_DB2		= "com.ibm.db2.jcc.DB2Driver";

	/** 
	 * PostGreSQL JDBC driver
	 */
	public static final String	JDBC_POSTGRESQL	= "org.postgresql.Driver";
	
	/** 
	 * MS SQL JDBC driver
	 */
	public static final String	JDBC_MSSQL		= "com.microsoft.sqlserver.jdbc.SQLServerDriver";

	/** 
	 * Sybase JDBC driver
	 */
	public static final	String	JDBC_SYBASE		= "com.sybase.jdbc4.jdbc.SybDriver";

	/** 
	 * Cloudscape JDBC driver
	 */
	public static final	String	JDBC_CLOUDSCAPE	= "COM.cloudscape.core.JDBCDriver";

	/** 
	 * Informix JDBC driver
	 */
	public static final String	JDBC_INFORMIX	= "com.ibm.db2.jcc.DB2Driver";

	/** 
	 * HSQLDB JDBC driver
	 */
	public static final String	JDBC_HSQLDB		= "org.hsqldb.jdbcDriver";

	/** 
	 * Derby drivers
	 */
	public static final	String	JDBC_DERBY		= "org.apache.derby.iapi.jdbc.AutoloadedDriver";

	/** 
	 * SQLite JDBC driver
	 */
	public static final	String	JDBC_SQLITE		= "org.sqlite.JDBC";

	/** 
	 * MariaDB JDBC driver
	 */
	public static final	String	JDBC_MARIADB	= "org.mariadb.jdbc.Driver";
	
	
	// Class variables
	private static final String		INDENT_LEVEL_1		= "  ";
	private static final String		INDENT_LEVEL_2		= "    ";
	private static final String		INDENT_LEVEL_3		= "      ";

	private Class<?>				gDriverClass		= null;
	private	Connection				gConnection			= null;
	private PreparedStatement		gPreparedStatement	= null;
	private	ArrayList<Object[]>		gRawData			= null;
	private String[]				gColumnNames		= null;
	private String[]				gTableNames			= null;
	private String					gErrorMessage		= null;
	private	int[]					gColumnWidths		= null;
	private	long					gPrepareMaxRows		= 0;
	private int						gPrepareTimeOutSec	= 0;
	private	long					gRowCount			= 0;
	private long					gElapsedTime		= 0;
	private int						gColumnCount		= 0;
	private boolean					gConnected			= false;

	/**
	 * Load JDBC driver and establish connection to database.
	 * 
	 * @param argDriverClass	JDBC driver class name (Example: "org.h2.Driver")
	 * @param argURL			JDBC connection URL (Example: "jdbc:h2:mem:myDb")
	 * @param argUserName		User name
	 * @param argPassword		Password
	 */
	public KDB(String argDriverClass, String argURL, String argUserName, String argPassword) {

		// Declarations
		KTimer timer = new KTimer();

		KLog.argException(K.isEmpty(argDriverClass), "KDB: JDBC driver class name is required");
		KLog.argException(K.isEmpty(argURL), "KDB: JDBC connection URL is required");
		
		// Load the JDBC driver
		KLog.debug("JDBC driver {}", argDriverClass);
		gDriverClass = K.loadClass(argDriverClass);
		
		if (gDriverClass == null) {
			gErrorMessage = "Unable to load JDBC driver " + argDriverClass;
			KLog.error(gErrorMessage);
			return;
		}
		
		// Connect to database
		try {
			KLog.debug("JDBC URL {}", argURL);
			
			if (!K.isEmpty(argUserName)) {
				KLog.debug("JDBC connect with user name {}", argUserName);
			} else {
				KLog.debug("JDBC connect as anonymous user");
			}
			
			gConnection	= DriverManager.getConnection(argURL, argUserName, argPassword);
			gConnected	= true;

			KLog.debug("JDBC connection established ({} ms)", timer.getElapsedMilliseconds());

		} catch (Exception e) {
			
			clearTransactionData();
			
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
		}
	}
	
	/**
	 * Initialize (clear) global variables
	 */
	private void clearTransactionData() {

		gRawData			= null;
		gColumnNames		= null;
		gTableNames			= null;
		gErrorMessage		= null;
		gColumnWidths		= null;
		gRowCount			= 0;
		gElapsedTime		= 0;
		gColumnCount		= 0;
	}
	
	/**
	 * Close the JDBC connection.
	 */
	public void close() {

		// Close prepared statement
		if (gPreparedStatement != null) {
			try {
				gPreparedStatement.close();
			} catch (Exception e) {
				KLog.error(e.toString());
			}
			gPreparedStatement = null;
		}

		// Close JDBC connection
		if (gConnection != null) {
			try {
				gConnection.close();
			} catch (Exception e) {
				KLog.error(e.toString());
			}
			gConnection = null;
		}

		// Reset variables
		clearTransactionData();
		
		gDriverClass	= null;
		gConnected		= false;
		
		KLog.debug("JDBC connection closed");
	}
	
	/**
	 * Commit transaction.
	 * 
	 * @return True if successful, false otherwise
	 * 
	 * @since 2024.06.17
	 */
	public boolean commit() {
		
		// Check of database is connected
		if (!isConnected()) {
			return (false);
		}
		
		try {
			gConnection.commit();
		} catch (Exception e) {
			
			clearTransactionData();
			
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
			return (false);
		}
		
		return (true);
	}

	/**
	 * Execute dynamic SQL statement. For SELECT statements, the result set is fetched and saved as convenient Java Objects to be
	 * retrieved by getDataXXX().
	 * 
	 * @param	argStatement	SQL statement
	 * @return	Success or failure
	 */
	public boolean exec(String argStatement) {
		return (exec(argStatement, 0, 0));
	}

	/**
	 * Execute dynamic SQL statement. For SELECT statements, the result set is fetched and saved as convenient Java Objects to be
	 * retrieved by getDataXXX().
	 * 
	 * @param	argStatement	SQL statement
	 * @param	argMaxRows		Maximum number of rows to fetch or 0 for all
	 * @return	Success or failure
	 * 
	 * @since 2024.08.19
	 */
	public boolean exec(String argStatement, long argMaxRows) {
		return (exec(argStatement, argMaxRows, 0));
	}
	
	/**
	 * Execute dynamic SQL statement. For SELECT statements, the result set is fetched and saved as convenient Java Objects to be
	 * retrieved by getDataXXX().
	 * 
	 * @param	argStatement	SQL statement
	 * @param	argMaxRows		Maximum number of rows to fetch or 0 for all
	 * @param	argTimeOutSec	Execution time out in seconds or 0 for no timeout
	 * @return	Success or failure
	 * 
	 * @since 2024.09.17
	 */
	public boolean exec(String argStatement, long argMaxRows, int argTimeOutSec) {
		
		// Declarations
		KTimer timer = new KTimer();
		
		// Check arguments
		KLog.argException(K.isEmpty(argStatement), "KDB.exec(): SQL statement is required");
		KLog.argException(argMaxRows < 0, "KDB.exec(): argMaxRows value is invalid");
		KLog.argException(argTimeOutSec < 0, "KDB.exec(): argTimeOutSec value is invalid");
		
		// Check if database is connected
		if (!isConnected()) {
			return (false);
		}
		
		// Create and execute SQL statement
		try (Statement statement = gConnection.createStatement()) {
			
			clearTransactionData();
			
			KLog.debug("SQL dynamic statement transaction started");
			
			// Set SQL statement timeout
			statement.setQueryTimeout(argTimeOutSec);
			
			if (argTimeOutSec != 0) {
				KLog.debug("SQL dynamic statement transaction timeout is {} seconds", argTimeOutSec);
			}
			
			// Execute SQL statement and process result
			statement.execute(argStatement);
			
			if (!processSQLResult(statement, argMaxRows)) {
				return (false);
			}
			
			// Save transaction elapsed time
			gElapsedTime = timer.getElapsedMilliseconds();
			
			KLog.debug("SQL dynamic statement transaction completed ({} ms, {} rows)", gElapsedTime, gRowCount);

			return (true);
			
		} catch (Exception e) {
			
			clearTransactionData();
		
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);

			return (false);
		}
	}
	
	/**
	 * Complete and execute precompiled SQL statement. For SELECT statements, the result set is fetched and saved as convenient Java Objects to be
	 * retrieved by getDataXXX().
	 * 
	 * @param	argObjects...		Values to be inserted in the sequence of the placeholder(s) '?' of the precompiled SQL statement
	 * @return	Success or failure
	 * 
	 * @since 2024.10.23
	 */
	public boolean execPrepare(Object... argObjects) {
		
		// Declarations
		KTimer timer = new KTimer();
		
		// Check if database is connected
		if (!isConnected()) {
			return (false);
		}
		
		// Check if SQL statement previously prepared
		KLog.argException(gPreparedStatement == null, "KDB.execPrepare(): No previous SQL statement prepared thru KDB.prepare()");
		
		// Clear variables
		clearTransactionData();
		
		//
		// Execute and process result
		//
		try {
					
			// Clear previous parameters
			gPreparedStatement.clearParameters();
			
			// Set SQL statement timeout
			gPreparedStatement.setQueryTimeout(gPrepareTimeOutSec);
			
			if (gPrepareTimeOutSec != 0) {
				KLog.debug("SQL dynamic statement transaction timeout is {} seconds", gPrepareTimeOutSec);
			}
			
			KLog.debug("SQL prepared statement transaction started");
			
			// Check if correct number of parameters passed
			int parameterCount = gPreparedStatement.getParameterMetaData().getParameterCount();
			KLog.argException(parameterCount != argObjects.length, "KDB.execPrepare(): Expected number of arguments: {}", parameterCount);

			//
			// Replace all '?' SQL placeholder(s) with the passed values
			//
			int placeHolder = 1;
			
			for (Object argObject : argObjects) {
					
				switch (argObject.getClass().getName()) {
					
				// 
				// Java types
				//
				case "java.lang.String":
					gPreparedStatement.setString(placeHolder, argObject.toString());
					break;
						
				case "java.lang.Integer":
					gPreparedStatement.setInt(placeHolder, ((Integer) argObject));
					break;
						
				case "java.lang.Short":
					gPreparedStatement.setShort(placeHolder, ((Short) argObject));
					break;

				case "java.lang.Byte":
					gPreparedStatement.setByte(placeHolder, ((Byte) argObject));
					break;

				case "java.lang.Long":
					gPreparedStatement.setLong(placeHolder, ((Long) argObject));
					break;

				case "java.math.BigDecimal":
					gPreparedStatement.setBigDecimal(placeHolder, ((java.math.BigDecimal) argObject));
					break;
						
				case "java.lang.Float":
					gPreparedStatement.setFloat(placeHolder, ((Float) argObject));
					break;

				case "java.lang.Double":
					gPreparedStatement.setDouble(placeHolder, ((Double) argObject));
					break;
			
				case "java.lang.Boolean":
					gPreparedStatement.setBoolean(placeHolder, ((Boolean) argObject));
					break;

				//
				// SQL types
				//
				case "java.sql.Date":
					gPreparedStatement.setDate(placeHolder, ((java.sql.Date) argObject));
					break;
					
				case "java.sql.Time":
					gPreparedStatement.setTime(placeHolder, ((java.sql.Time) argObject));
					break;
						
				case "java.sql.Timestamp":
					gPreparedStatement.setTimestamp(placeHolder, ((java.sql.Timestamp) argObject));
					break;
												
				case "java.sql.Array":
					gPreparedStatement.setArray(placeHolder, ((java.sql.Array) argObject));
					break;
							
				default:
					KLog.argException(true, "KDB.execPrepare(): Unsupported object type {}", argObject.getClass().getName());
					break;
				}
					
				placeHolder++;
			}
			
			// Execute SQL statement and process result
			gPreparedStatement.execute();
			
			if (!processSQLResult(gPreparedStatement, gPrepareMaxRows)) {
				return (false);
			}
			
			// Save transaction elapsed time
			gElapsedTime = timer.getElapsedMilliseconds();
			
			KLog.debug("SQL prepared statement transaction completed ({} ms, {} rows)", gElapsedTime, gRowCount);
			return (true);
		
		} catch (Exception e) {
			
			clearTransactionData();
			
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);

			return (false);
		}
	}
	
	/**
	 * Get number of columns in result set.
	 * 
	 * @return	Number of columns or 0
	 */
	public int getColumnCount() {
		return (gColumnCount);
	}
	
	/**
	 * Get column names in result set.
	 * 
	 * @return	Column names or null
	 */
	public String[] getColumnNames() {
		return (gColumnNames);
	}
	
	/**
	 * Get column widths in result set.
	 * 
	 * @return	Column width or null
	 */
	public int[] getColumnWidths() {
		return (gColumnWidths);
	}
	
	/**
	 * Get fetched data as an ArrayList (rows) with an array of Objects (columns).
	 * 
	 * @return	Array with data or null
	 */
	public ArrayList<Object[]> getData() {
		return (gRawData);
	}
	
	/**
	 * Get result set formatted as CSV string. The default field delimiter is a comma.
	 * 
	 * @return	CSV string or null
	 */
	public String getDataAsCSV() {
		return (getDataAsCSV(',', true));
	}
	
	/**
	 * Get result set formatted as CSV string.
	 * 
	 * @param	argDelimiter	Delimiter character
	 * @return	CSV string or null
	 */
	public String getDataAsCSV(char argDelimiter) {
		return (getDataAsCSV(argDelimiter, true));
	}

	/**
	 * Get result set formatted as CSV string delimited by the passed character (Example: ',')
	 * 
	 * @param	argDelimiter	Delimiter character
	 * @param	argHeader		Write header line with column names
	 * @return	CSV string or null
	 * 
	 * @since 2024.06.21
	 */
	public String getDataAsCSV(char argDelimiter, boolean argHeader) {
			
		// Declarations
		ArrayList<Object[]>	rows	= getData();
		KTimer				timer	= new KTimer();

		// Check arguments
		KLog.argException(argDelimiter == ' ', "K.getDataAsCSV(): Non-blank delimiter is required");
		KLog.argException(argDelimiter == '\"', "KgetDataAsCSV(): Quote character not allowed as delimiter");
		
		if (rows == null) {
			return (null);
		}
			
		StringBuilder	csvString		= new StringBuilder();
		String			lineSeparator	= "\r\n";							// RFC 4180: Line break must be CRLF
		String[]		columnNames		= getColumnNames();
			
		// Set CSV header line with column names
		if (argHeader) {
			
			for (int index = 0; index < columnNames.length; index++) {
				csvString.append(K.encodeCSV(columnNames[index]) + argDelimiter);
			}
			
			// Remove last comma and terminate line
			csvString.deleteCharAt(csvString.length() - 1);
			csvString.append(lineSeparator);
		}
			
		// Loop thru all rows
		for (Object[] row: rows) {
	
			// Write out all columns data
			for (int index = 0; index < row.length; index++) {
					
				// Encode string for CSV
				csvString.append(K.encodeCSV((row[index] + "").trim(), argDelimiter) + argDelimiter);
			}
			
			// Remove last comma and terminate line
			csvString.deleteCharAt(csvString.length() - 1);
			csvString.append(lineSeparator);
		}
	
		KLog.debug("CSV string generated from result set ({}, {} ms)", K.formatBytes(csvString.length()), timer.getElapsedMilliseconds());
			
		return (csvString.toString());
	}

	/**
	 * Get result set formatted as JSON string
	 * 
	 * @return	JSON string or null
	 */
	public String getDataAsJSON() {

		// Declarations
		ArrayList<Object[]>	rows	= getData();
		KTimer				timer	= new KTimer();
	
		if (rows == null) {
			return (null);
		}
	
		StringBuilder	jsonString		= new StringBuilder();
		String			tableName		= getTableName();
		String[]		columnNames		= getColumnNames();
		int				recordCount		= 0;
	
		// Set JSON header
		jsonString.append('{' + K.LINE_SEPARATOR + INDENT_LEVEL_1 + K.encodeJSON(tableName) + ": [" + K.LINE_SEPARATOR);		
	
		for (Object[] row: rows) {

			// Add the continuation string for the previous JSON array entry if necessary
			if (recordCount++ > 0) {
				jsonString.append(',' + K.LINE_SEPARATOR);	
			}
		
			jsonString.append(INDENT_LEVEL_2 + '{' + K.LINE_SEPARATOR);

			for (int index = 0; index < row.length; index++) {
				jsonString.append(INDENT_LEVEL_3 + K.encodeJSON(columnNames[index]) + ": " + K.encodeJSON(("" + row[index]).trim(), false));

				if (index < (row.length - 1)) {
					jsonString.append(',');
				}
			
				jsonString.append(K.LINE_SEPARATOR);
			}
			jsonString.append(INDENT_LEVEL_2 + '}');
		}
	
		// Set JSON footer
		jsonString.append(K.LINE_SEPARATOR + INDENT_LEVEL_1 + ']' + K.LINE_SEPARATOR + '}' + K.LINE_SEPARATOR);
	
		KLog.debug("JSON string generated from result set ({}, {} ms)", K.formatBytes(jsonString.length()), timer.getElapsedMilliseconds());
	
		return (jsonString.toString());
	}
	
	/**
	 * Get result set formatted as display table with column headers
	 * 
	 * @return	String with formatted table
	 */
	public String getDataAsTable() {
		return (getDataAsTable(true));
	}
	
	/**
	 * Get result set formatted as display table
	 * 
	 * @param	argHeader	True to add column header, false otherwise
	 * @return	String with formatted table
	 * 
	 * @since 2024.09.01
	 */
	public String getDataAsTable(boolean argHeader) {

		// Declarations
		ArrayList<Object[]>	rows	= getData();
		KTimer				timer	= new KTimer();
			
		if (rows == null) {
			return (null);
		}
			
		StringBuilder	tableString		= new StringBuilder();
		String[]		columnNames		= getColumnNames();
		int[]			columnWidths	= getColumnWidths();
			
		// Format header line
		if (argHeader) {
			for (int index = 0; index < columnNames.length; index++) {
				tableString.append(String.format("%-" + (columnWidths[index] + 1) + 's', columnNames[index]));
			}
			tableString.append(K.LINE_SEPARATOR);
		}
		
		// Loop thru all rows
		for (Object[] row: rows) {
	
			// Write out all columns data
			for (int index = 0; index < row.length; index++) {
				tableString.append(String.format("%-" + (columnWidths[index] + 1) + 's', ("" + row[index]).trim()));
			}
			tableString.append(K.LINE_SEPARATOR);
		}
	
		KLog.debug("Table string generated from result set ({}, {} ms)", K.formatBytes(tableString.length()), timer.getElapsedMilliseconds());

		return (tableString.toString());
	}
	
	/**
	 * Get result set formatted as XML UTF-8 string
	 * 
	 * @return	XML string or null
	 */
	public String getDataAsXML() {

		// Declarations
		ArrayList<Object[]>	rows 	= getData();
		KTimer				timer	= new KTimer();
			
		if (rows == null) {
			return (null);
		}
			
		StringBuilder	xmlString		= new StringBuilder();
		String			tableName		= getTableName();
		String[]		columnNames		= getColumnNames();
			
		// Format header lines
		xmlString.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + K.LINE_SEPARATOR);
		xmlString.append("<!-- " + this.getClass().getName() + " Version " + K.VERSION + " -->" + K.LINE_SEPARATOR);
		
		xmlString.append('<'+ tableName + '>' + K.LINE_SEPARATOR);
		
		// Loop thru all rows
		for (Object[] row: rows) {
	
			xmlString.append(INDENT_LEVEL_1 + "<ROW>" + K.LINE_SEPARATOR);

			// Write out all columns data
			for (int index = 0; index < row.length; index++) {
				xmlString.append(INDENT_LEVEL_2 + '<' + columnNames[index] + '>');
				xmlString.append(K.encodeXML(("" + row[index]).trim()));
				xmlString.append("</" + columnNames[index] + '>' + K.LINE_SEPARATOR);
			}
			xmlString.append(INDENT_LEVEL_1 + "</ROW>" + K.LINE_SEPARATOR);
		}
	
		xmlString.append("</" + tableName + '>' + K.LINE_SEPARATOR);
		
		// Convert to UTF-8
		byte[] xmlBytes = xmlString.toString().getBytes(StandardCharsets.UTF_8);
		String utf8String = new String(xmlBytes, StandardCharsets.UTF_8);
		KLog.debug("XML string generated from result set ({}, {} ms)", K.formatBytes(utf8String.length()), timer.getElapsedMilliseconds());

		return (utf8String);
	}
	
	/**
	 * Get result set formatted as YAML string
	 * 
	 * @return	YAML string or null
	 * 
	 * @since 2024.09.14
	 */
	public String getDataAsYAML() {

		// Declarations
		ArrayList<Object[]>	rows	= getData();
		KTimer				timer	= new KTimer();
	
		if (rows == null) {
			return (null);
		}
	
		StringBuilder	yamlString		= new StringBuilder();
		String			tableName		= getTableName();
		String[]		columnNames		= getColumnNames();
	
		// Set YAML header
		yamlString.append("---" + K.LINE_SEPARATOR + tableName + ":" + K.LINE_SEPARATOR);		
	
		for (Object[] row: rows) {

			for (int index = 0; index < row.length; index++) {
				if (index == 0) {
					yamlString.append(" -");
				} else {
					yamlString.append("  ");
				}
				yamlString.append(' ' + columnNames[index] + ": " + K.encodeYAML(("" + row[index]).trim(), false) + K.LINE_SEPARATOR);
			}
		}
	
		// Set YAML footer
		yamlString.append("..." + K.LINE_SEPARATOR);
	
		KLog.debug("YAML string generated from result set ({}, {} ms)", K.formatBytes(yamlString.length()), timer.getElapsedMilliseconds());
	
		return (yamlString.toString());
	}
	
	/**
	 * Get elapsed time of last SQL statement.
	 * 
	 * @return	Elapsed time in milliseconds or 0
	 * 
	 * @since 2024.06.24
	 */
	public long getElapsedTime() {
		return (gElapsedTime);
	}
	
	/**
	 * Get last error message.
	 * 
	 * @return	Error message or null
	 */
	public String getErrorMessage() {
		return (gErrorMessage);
	}
	
	/**
	 * Get number of rows read or updated.
	 * 
	 * @return	Row count
	 */
	public long getRowCount() {
		return (gRowCount);
	}
	
	/**
	 * Get table name of first column of result set.
	 * 
	 * Note: The underlying JDBC call does not always return the table name, as in SELECT COUNT(*).
	 * 
	 * @return	Table name or null
	 */
	public String getTableName() {
		return (getTableName(1));
	}
	
	/**
	 * Get table name of given column number.
	 * 
	 * Note: The underlying JDBC call does not always return the table name, as in SELECT COUNT(*).
	 * 
	 * @param	argColumnNumber	Column number for which the table name is returned
	 * @return	Table name or null
	 * 
	 * @since 2024.06.27
	 */
	public String getTableName(int argColumnNumber) {
		return (gTableNames[argColumnNumber]);
	}
	
	/**
	 * Get state of JDBC connection.
	 * 
	 * @return	True if connected, false otherwise
	 */
	public boolean isConnected() {
		return (gConnected);
	}
    
	/**
	 * Prepare SQL statement. The prepared statement must later be executed with <code>execPrepared()</code>.
	 * 
	 * @param	argStatement	SQL statement
	 * @return	Success or failure
	 * 
	 * @since 2024.09.25
	 */
	public boolean prepare(String argStatement) {
		return (prepare(argStatement, 0, 0));
	}
	
	/**
	 * Prepare SQL statement. The prepared statement must later be executed with <code>execPrepared()</code>.
	 * 
	 * @param	argStatement	SQL statement
	 * @param	argMaxRows		Maximum number of rows to fetch or 0 for all
	 * @return	Success or failure
	 * 
	 * @since 2024.09.25
	 */
	public boolean prepare(String argStatement, long argMaxRows) {
		return (exec(argStatement, argMaxRows, 0));
	}
	
	/**
	 * Prepare SQL statement. The prepared statement must later be executed with <code>execPrepare()</code>.
	 * 
	 * @param	argStatement	SQL statement
	 * @param	argMaxRows		Maximum number of rows to fetch or 0 for all
	 * @param	argTimeOutSec	Execution time out in seconds or 0 for no time out
	 * @return	Success or failure
	 * 
	 * @since 2024.10.24
	 */
	public boolean prepare(String argStatement, long argMaxRows, int argTimeOutSec) {
	
		// Declarations
		KTimer timer = new KTimer();
		
		// Check arguments
		KLog.argException(K.isEmpty(argStatement), "KDB.prepare(): SQL statement is required");
		KLog.argException(argMaxRows < 0, "KDB.prepare(): argMaxRows value is invalid");
		KLog.argException(argTimeOutSec < 0, "KDB.prepare(): argTimeOutSec value is invalid");
		
		// Check if database is connected
		if (!isConnected()) {
			return (false);
		}
		
		// Save maximum number of rows to be fetched and transaction time out
		gPrepareMaxRows		= argMaxRows;
		gPrepareTimeOutSec	= argTimeOutSec;
		
		// Close previous prepared SQL statement
		if (gPreparedStatement != null) {
			
			try {
				gPreparedStatement.close();
			} catch (Exception e) {
				KLog.error(e.toString());
			}
			
			gPreparedStatement = null;
		}
		
		// Clear variables
		clearTransactionData();
		
		KLog.debug("SQL prepare statement started");
		
		// Prepare SQL statement
		try {
			gPreparedStatement = gConnection.prepareStatement(argStatement);
			
			// Save transaction elapsed time
			gElapsedTime = timer.getElapsedMilliseconds();
			
			KLog.debug("SQL prepare statement completed ({} ms)", gElapsedTime);
			
			return (true);
			
		} catch (Exception e) {
			
			clearTransactionData();
			
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
			
			try {
				if (gPreparedStatement != null) {
					gPreparedStatement.close();
				}
			} catch (Exception ee) {
				KLog.error(ee.toString());
			}
			gPreparedStatement = null;
			
			return (false);
		}
	}
	
	/**
	 * Execute SQL statement. For SELECT statements, the result set is fetched and saved as convenient Java Objects to be
	 * retrieved by getDataXXX().
	 * 
	 * @param	argStatement	SQL statement
	 * @param	argMaxRows		Maximum number of rows to fetch or 0 for all
	 * @return	Success or failure
	 * 
	 * @since 2024.10.25
	 */
	private boolean processSQLResult(Statement argStatement, long argMaxRows) {
		
		// Declarations
		ResultSet resultSet = null;

		// Check if database is connected
		if (!isConnected()) {
			return (false);
		}
		
		// Process SQL result
		clearTransactionData();
		
		try {

			//
			// Non-SELECT: Get number of affected rows (deleted, insert or update)
			//
			resultSet = argStatement.getResultSet();

			if (resultSet == null) {
				gRowCount = argStatement.getLargeUpdateCount();
				return (true);
			}
			
			//
			// Build arrays with table names, column names and column widths
			//
			ResultSetMetaData metaData	= resultSet.getMetaData();
			
			gColumnCount 				= metaData.getColumnCount();

			gTableNames					= new String[gColumnCount];
			gColumnNames				= new String[gColumnCount];
			gColumnWidths				= new int[gColumnCount];
			
			for (int index = 1; index <= gColumnCount; index++) {
				gTableNames[index - 1]		= metaData.getTableName(index);
				gColumnNames[index - 1]		= metaData.getColumnLabel(index);
				gColumnWidths[index - 1]	= metaData.getColumnDisplaySize(index);
			}
			
			//
			// Build array list from result set data (rows = Objects[], columns = Object)
			//
			gRawData	= new ArrayList<>();
			gRowCount	= 0L;
			
			if (argMaxRows != 0) {
				KLog.debug("SQL fetching data to a maximum of {} rows", argMaxRows);
			}
			
			while (resultSet.next()) {

				// Stop after maximum number of rows reached
				if ((argMaxRows != 0) && (gRowCount >= argMaxRows)) {
					KLog.debug("SQL maximum number of rows fetched");
					break;
				}

				++gRowCount;

				// Get Java objects of all columns
				Object[] columnValues = new Object[gColumnCount];
					
				for (int index = 1; index <= gColumnCount; index++) {
					columnValues[index - 1] = resultSet.getObject(index);
				}
					
				// Add row objects (columns) to array (rows)
				gRawData.add(columnValues);
			}

			// Close the result set
			resultSet.close();
			return (true);
			
		} catch (Exception e) {
			
			clearTransactionData();
						
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
			
			// Cleanup
			try {
				if (resultSet != null) {
					resultSet.close();
				}
			} catch (Exception ee) {
				KLog.error(ee.toString());
			}
			
			return (false);
		}
	}
	
	/**
	 * Rollback transaction.
	 * 
	 * @return True if successful, false otherwise
	 * 
 	 * @since 2024.06.17
	 */
	public boolean rollback() {
		
		// Check if database is connected
		if (!isConnected()) {
			return (false);
		}
		
		try {
			gConnection.rollback();
			return (true);
		} catch (Exception e) {
			
			clearTransactionData();
			
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
			return (false);
		}
	}
	
	/**
	 * Set auto commit state.
	 * 
	 * @param argState	True for auto commit, false otherwise
	 * 
	 * @since 2024.06.26
	 */
	public void setAutoCommit(boolean argState) {
		
		// Check if database is connected
		if (!isConnected()) {
			return;
		}
		
		try {
			gConnection.setAutoCommit(argState);
		} catch (Exception e) {
			// Log error and ignore
			gErrorMessage = e.toString();
			KLog.error(gErrorMessage);
		}
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KDB [gDriverClass=" + gDriverClass + ", gConnection=" + gConnection + ", gPreparedStatement="
				+ gPreparedStatement + ", gRawData=" + gRawData + ", gColumnNames=" + Arrays.toString(gColumnNames)
				+ ", gTableNames=" + Arrays.toString(gTableNames) + ", gErrorMessage=" + gErrorMessage
				+ ", gColumnWidths=" + Arrays.toString(gColumnWidths) + ", gPrepareMaxRows=" + gPrepareMaxRows
				+ ", gPrepareTimeOutSec=" + gPrepareTimeOutSec + ", gRowCount=" + gRowCount + ", gElapsedTime="
				+ gElapsedTime + ", gColumnCount=" + gColumnCount + ", gConnected=" + gConnected + "]";
	}
}
