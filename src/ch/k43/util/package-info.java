/**
 * <h2>Java Utility Package (Open Source)</h2><p>
 * 
 * <b>A high-performance and user-friendly toolkit tailored for developing small to medium-sized back-end applications</b><p>
 * 
 * This package holds all classes used by the Java Utility Package. The full documentation with examples can be found on <a href="https://java-util.k43.ch">java-util.k43.ch</a>
 * 
 * <pre>
 * <b>1. Logging Example</b>
 * 
 * KLog.info("Program started at {}", new Date());
 * KLog.debug("Any debugging message");
 *		
 * 2025-04-25T09:52:08.753 D main[1]:ch.k43.util.KLog:init:197                        ===== Application started 2025-04-25T09:52:08.711 =====
 * 2025-04-25T09:52:08.753 D main[1]:ch.k43.util.KLog:init:198                        Java Utility Package (Open Source/Freeware) Version 2025.04.25
 * 2025-04-25T09:52:08.753 D main[1]:ch.k43.util.KLog:init:199                        Homepage java-util.k43.ch - Please send any feedback to andy.brunner@k43.ch
 * 2025-04-25T09:52:08.753 D main[1]:ch.k43.util.KLog:init:202                        KLog properties read from file KLog.properties
 * 2025-04-25T09:52:08.792 D main[1]:ch.k43.util.KLog:init:218                        Network host ab-macbook-pro (10.0.0.108)
 * 2025-04-25T09:52:08.792 D main[1]:ch.k43.util.KLog:init:222                        OS platform Mac OS X Version 15.4.1/aarch64
 * 2025-04-25T09:52:08.793 D main[1]:ch.k43.util.KLog:init:227                        OS disk space total 3.63 TiB, free 2.30 TiB, usable 2.30 TiB
 * 2025-04-25T09:52:08.793 D main[1]:ch.k43.util.KLog:init:233                        Java version 23 (Java HotSpot(TM) 64-Bit Server VM - Oracle Corporation)
 * 2025-04-25T09:52:08.793 D main[1]:ch.k43.util.KLog:init:238                        Java directory /Library/Java/JavaVirtualMachines/graalvm-jdk-23.0.1+11.1/Contents/Home
 * 2025-04-25T09:52:08.794 D main[1]:ch.k43.util.KLog:init:243                        Java CPUs 10, de/CH, UTF-8, UTC +02:00 (Europe/Zurich)
 * 2025-04-25T09:52:08.794 D main[1]:ch.k43.util.KLog:init:253                        Java heap maximum 16.00 GiB, current 1.01 GiB, used 9.95 MiB, free 1022.05 MiB
 * 2025-04-25T09:52:08.794 D main[1]:ch.k43.util.KLog:init:260                        Java classpath ../bin/:../lib/angus-mail-2.0.3.jar:../lib/jakarta.mail-api-2.1.3.jar
 * 2025-04-25T09:52:08.794 D main[1]:ch.k43.util.KLog:init:264                        Current user andybrunner, language de, directory /Users/andybrunner/
 * 2025-04-25T09:52:08.794 D main[1]:ch.k43.util.KLog:init:270                        Current directory /Users/andybrunner/Documents/Eclipse-Workspace/Java-Utility-Package/src/
 * 2025-04-25T09:52:08.795 D main[1]:ch.k43.util.KLog:init:274                        Temporary directory /var/folders/9s/tbyqn_vn7bs9rf3f1rc2jpxw0000gn/T/
 * 2025-04-25T09:52:08.800 I main[1]:Test:main:18                                     Program started at Fri Apr 25 09:52:08 CEST 2025
 * 2025-04-25T09:52:08.800 D main[1]:Test:main:19                                     Any debugging message
 * 
 * <b>2. JDBC Example</b>
 * 
 * try (KDB db = new KDB(KDB.JDBC_H2, "jdbc:h2:mem:mydb", "", "")) {
 *
 *    KLog.abort(!db.isConnected(), "Error: {}", db.getErrorMessage());
 *		
 *    db.exec("CREATE TABLE addresses (sequence INT AUTO_INCREMENT, lastname VARCHAR(20), firstname VARCHAR(20))");
 *    
 *    db.prepare("INSERT INTO addresses (lastname, firstname) VALUES (?, ?)");
 *    db.execPrepare("Smith", "Joe");
 *    db.execPrepare("Miller", "Bob");
 *    db.execPrepare("Johnson", "Evelyn");
 *    
 *    db.exec("SELECT * FROM addresses");
 *    System.out.println(db.getDataAsJSON());
 * }
 * 
 * <b>3. HTTPS Example</b>
 * 
 * KHTTPClient http = new KHTTPClient();
 * 
 * if (!http.get("https://reqbin.com/echo/get/json")) {
 *    KLog.error("Error: {}", http.getLastError());
 * } else {
 *    System.out.println(http.getResponseDataAsString());
 * }
 * 
 * <b>4. SMTP Example</b>
 * 
 * KSMTPMailer mailer = new KSMTPMailer();
 *
 * mailer.setFrom("john.doe@acme.com");
 * mailer.setTo("bob.smith@hotmail.com");
 * mailer.setSubject("Subject");
 * mailer.addText("Here is your requested file");
 * mailer.addFile("test.txt");
 * mailer.addText("Regards, John");
 * mailer.send();
 * 
 * <b>5. Password Vault</b>
 * 
 * KPasswordVault vault = new KPasswordVault("Pa$$w0rd");
 *
 * System.out.println("Password hash: " + K.toHex(vault.getPasswordHash()));
 * System.out.println("Password salt: " + K.toHex(vault.getSalt()));
 * System.out.println("Iterations:    " + vault.getIterations());
 * System.out.println("Hashing time:  " + vault.getHashTimeMs() + " ms");
 * </pre>
 *  
 * @author andy.brunner@k43.ch
 */
package ch.k43.util;