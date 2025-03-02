/**
 * <b>Java Utility Package (Freeware)</b><p>
 * 
 * A high-performance, user-friendly programming toolkit designed for Java back-end developers.<p>
 * 
 * This package holds all classes used by the Java Utility Package. The full documentation with examples can be found on <a href="https://java-util.k43.ch">java-util.k43.ch</a>
 * 
 * <pre>
 * // JDBC Example
 * try (KDB db = new KDB(KDB.JDBC_H2, "jdbc:h2:mem:mydb", "", "")) {
 *
 *    KLog.abort(!db.isConnected(), "Error: {}", db.getErrorMessage());
 *		
 *    db.exec("CREATE TABLE addresses (sequence INT AUTO_INCREMENT, lastname VARCHAR(20), firstname VARCHAR(20))");
 *    db.prepare("INSERT INTO addresses (lastname, firstname) VALUES (?, ?)");
 *    db.execPrepare("Smith", "Joe");
 *    db.execPrepare("Miller", "Bob");
 *    db.execPrepare("Johnson", "Evelyn");
 *    db.exec("SELECT * FROM addresses");
 *    System.out.println(db.getDataAsJSON());
 * }
 * 
 * // HTTPS Example
 * KHTTPClient http = new KHTTPClient();
 * 
 * if (!http.get("https://reqbin.com/echo/get/json")) {
 *    KLog.error("Error: {}", http.getLastError());
 * } else {
 *    System.out.println(http.getResponseDataAsString());
 * }
 * 
 * // SMTP Example
 * KSMTPMailer mailer = new KSMTPMailer();
 *
 * mailer.setFrom("john.doe@acme.com");
 * mailer.setTo("bob.smith@hotmail.com");
 * mailer.setSubject("Subject");
 * mailer.addText("Here is your requested file");
 * mailer.addFile("test.txt");
 * mailer.addText("Regards, John");
 * mailer.send();
 * </pre>
 *  
 * @author andy.brunner@k43.ch
 */
package ch.k43.util;