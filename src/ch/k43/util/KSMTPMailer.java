package ch.k43.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.Authenticator;
import jakarta.mail.Header;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Compose and send MIME multipart email to an SMTP host.<p>
 * 
 * Notes:<br>
 * - The JAR files for jakarta.mail and angus.mail (with the activation classes) must be present in the class path during runtime.<br>
 * - The default transmission is secured (STARTTLS/TLS) which can be disabled with setSecuredConnection(false)<br>
 * - If no host name is specified, the highest priority MX DNS record of the first recipient will be used
 * 	 to connect to the SMTP server on port 25<br>
 * 
 * <pre>
 * Example:
 * 
 * KSMTPMailer mailer = new KSMTPMailer();
 * mailer.setFrom("john.doe@acme.com");
 * mailer.setTo("bob.smith@hotmail.com");
 * mailer.setSubject("Two files");
 * mailer.addText("Here are the two files:");
 * mailer.addFile("file1.txt");
 * mailer.addFile("file2.txt");
 * mailer.addText("Regards, John");
 * mailer.send();
 * </pre>
 *  
 * @since 2024.05.17
 */
public class KSMTPMailer {
	
	// Class variables
	private MimeMultipart			gMimeMultipart			= null;
	private ByteArrayOutputStream	gJakartaMailLog			= null;
	private String					gLastErrorMessage		= null;
	private String					gSubject				= "";
	private String					gSubjectCharSet			= null;
	private String					gFromAddress			= null;
	private String					gToAddresses			= null;
	private String					gCCAddresses			= null;
	private String					gBCCAddresses			= null;
	private String					gReplyToAddress			= null;
	private String					gSMTPHostName			= null;
	private String					gUserName				= null;
	private String					gUserPassword			= null;
	private String					gUnsubscribe			= null;
	private int						gSMTPHostPort			= 25;
	private int						gMimeMessageSize		= 0;
	private boolean					gSecureConnection		= true;
	private boolean					gMultiPartAdded			= false;
	private boolean					gOAuth2Authentication	= false;
		
	/**
	 * Class constructor
	 */
	public KSMTPMailer() {
	
		// Create new empty MIME multipart
		gMimeMultipart = new MimeMultipart();
	}
	
	/**
	 * Add a multipart file item.<br>
	 * 
	 * @param argFileName	File name to be attached
	 * @return	True if success, false otherwise
	 */
	public boolean addFile(String argFileName) {

		// Check arguments
		KLog.argException(K.isEmpty(argFileName), "KSMTPMailer.addFile(): File name is required");
		
		try {
			
			File attFile = new File(argFileName);
			
			MimeBodyPart messageBodyPart = new MimeBodyPart(); 
			messageBodyPart.attachFile(attFile);
			gMimeMultipart.addBodyPart(messageBodyPart);
			gMultiPartAdded = true;
			
			KLog.debug("SMTP file {} multipart added ({})", argFileName, K.formatBytes(attFile.length()));
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(e);
			return (false);
		}
	}

	/**
	 * Add a multipart HTML item. The passed HTML is saved with UTF-8 charset.
	 *  
	 * @param argHTMLBody	HTML to be added
	 * @return	True if success, false otherwise
	 */
	public boolean addHTML(String argHTMLBody) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argHTMLBody), "KSMTPMailer.addHTML(): HTML string is required");
		
		try {
			MimeBodyPart messageBodyPart = new MimeBodyPart(); 
			messageBodyPart.setText(argHTMLBody, "UTF-8", "html");
			gMimeMultipart.addBodyPart(messageBodyPart);
			gMultiPartAdded = true;

			KLog.debug("SMTP text/html multipart added ({})", K.formatBytes(argHTMLBody.length()));
			return (true);
			
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(e);
			return (false);
		}
	}
	
	/**
	 * Add a multipart text item. The passed text is saved with UTF-8 charset.
	 * 
	 * @param argTextBody	Text to be added
	 * @return	True if success, false otherwise
	 */
	public boolean addText(String argTextBody) {
		return (addText(argTextBody, "UTF-8"));
	}
	
	/**
	 * Add a multipart text item.
	 * 
	 * @param argTextBody	Text to be added
	 * @param argCharSet	Character set (e.g. UTF-8)
	 * @return	True if success, false otherwise
	 * 
	 * @since 2024.06.10
	 */
	public boolean addText(String argTextBody, String argCharSet) {

		// Check arguments
		KLog.argException(K.isEmpty(argTextBody), "KSMTPMailer.addText(): Text is required");
		KLog.argException(K.isEmpty(argCharSet), "KSMTPMailer.addText(): Character set is required");
		
		try {
			MimeBodyPart messageBodyPart = new MimeBodyPart(); 
			messageBodyPart.setText(argTextBody, argCharSet, "plain");
			gMimeMultipart.addBodyPart(messageBodyPart);
			gMultiPartAdded = true;

			KLog.debug("SMTP text/plain multipart added ({}, {})", K.formatBytes(argTextBody.length()), argCharSet);
			return (true);
		} catch (Exception e) {
			gLastErrorMessage = e.toString();
			KLog.error(e);
			return (false);
		}
	}

	/**
	 * Get last error message.<br>
	 * 
	 * @return	Last error message
	 */
	public String getErrorMessage() {
		return (gLastErrorMessage);
	}

	/**
	 * Get size of last message sent.<br>
	 * 
	 * @return	Size of last message sent
	 * 
	 * @since 2024.05.27
	 */
	public int getMessageSize() {
		return (gMimeMessageSize);
	}
	
	/**
	 * Compose and send the email multi part message.<P>
	 * 
	 * If no SMTP server was previously set with setSMTPHost(), the message will be sent to the highest priority MX domain from
	 * the first recipient found.
	 * 
	 * @return	True if success, false otherwise
	 */
	public boolean send() {

		// Check if required data is present
		KLog.argException(K.isEmpty(gFromAddress), "KSMTPMailer.send(): Sender is required");
		KLog.argException(K.isEmpty(gToAddresses) && K.isEmpty(gCCAddresses) && K.isEmpty(gBCCAddresses), "KSMTPMailer.send(): Recipient is required");
		KLog.argException(!gMultiPartAdded, "KSMTPMailer.send(): Mail body is required");
		
		//
		// Setup SMTP authentication class
		//
		Authenticator sessionAuth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(gUserName, gUserPassword);
			}
		};
		
		//
		// Setup session properties
		//
		Properties sessionProps = new Properties();
		
		// SMTP host name if none was set previously
		if (gSMTPHostName == null) {
			
			// 2024.06.01 Get first recipient from TO:, CC: or BCC
			String senderAddress = null;
			
			if (gToAddresses != null) {
				senderAddress = gToAddresses.split(",")[0];
			} else if (gCCAddresses != null) {
				senderAddress = gCCAddresses.split(",")[0];
			} else if (gBCCAddresses != null) {
				senderAddress = gBCCAddresses.split(",")[0];
			}
			
			// Get MX record from the first recipient address
			String[] mxHostnames = K.queryDNS("MX", senderAddress);

			if (mxHostnames == null) {
				gLastErrorMessage = "Unable to get domain MX record of the first recipient";
				KLog.error(gLastErrorMessage);
				return (false);
			}
			
			gSMTPHostName = mxHostnames[0];
		}
		
		sessionProps.put("mail.smtp.host", gSMTPHostName);
		sessionProps.put("mail.smtp.port", String.valueOf(gSMTPHostPort));
	
		// 2024.06.11 Set SMTP authentication (Basic or OAuth 2.0)
		if (gUserName != null) {
			if (gOAuth2Authentication) {
				// OAuth 2.0 authentication (SASL/XOAUTH2)
				sessionProps.put("mail.smtp.ssl.enable", "true");
				sessionProps.put("mail.smtp.sasl.enable", "true");
				sessionProps.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
				sessionProps.put("mail.smtp.auth.login.disabe", "true");
				sessionProps.put("mail.smtp.auth.plain.disable", "true");
			} else {
				// Basic authentication
				sessionProps.put("mail.smtp.auth", "true");
			}
		}
		
		// STARTTLS / TLS
		if (gSecureConnection) {
			sessionProps.put("mail.smtp.starttls.enable", "true");
			sessionProps.put("mail.smtp.ssl.checkserveridentity", "true");						// 2024.05.23
			sessionProps.put("mail.smtp.socketFactory.port", String.valueOf(gSMTPHostPort));
			sessionProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		}
		
		// Log mail properties
		Enumeration<?> properties = sessionProps.propertyNames();

	    while (properties.hasMoreElements()) {
	      String key = (String) properties.nextElement();
	      KLog.debug("SMTP property {}: {}", key, sessionProps.getProperty(key));
	    }
		
		//
		// Setup SMTP session
		//
		Session session = Session.getInstance(sessionProps, sessionAuth);

		// 2024.06.03 Initialize and enable Jakarta Mail log
		if (KLog.isLevelDebug()) {

			gJakartaMailLog	= new ByteArrayOutputStream();

			session.setDebugOut(new PrintStream(gJakartaMailLog));
			session.setDebug(true);
		}
		
		//
		// Setup MIME message
		//
		MimeMessage mimeMessage = new MimeMessage(session);
		
		try {

			// Set some defaults
			mimeMessage.setSentDate(new Date());
			mimeMessage.setHeader("X-Mailer", this.getClass().getName() + " Version " + K.VERSION + " (" + K.JVM_PLATFORM + ')');	// 2024.05.22

			// Set From address
			mimeMessage.setFrom(new InternetAddress(gFromAddress));
		
			// Set ReplyTo address
			if (!K.isEmpty(gReplyToAddress)) {
				mimeMessage.setReplyTo(InternetAddress.parse(gReplyToAddress, false));
			}
			
			// Set To address(es)
			if (!K.isEmpty(gToAddresses)) {
			    mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(gToAddresses, false));
			}
		      
			// Set Cc address(es)
			if (!K.isEmpty(gCCAddresses)) {
			    mimeMessage.setRecipients(Message.RecipientType.CC, InternetAddress.parse(gCCAddresses, false));
			}
			
			// Set Bcc address(es)
			if (!K.isEmpty(gBCCAddresses)) {
			    mimeMessage.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(gBCCAddresses, false));
			}		
			
			// 2024.06.19 Set List_Unsubscribe header
			if (!K.isEmpty(gUnsubscribe)) {
			    mimeMessage.setHeader("List-Unsubscribe", gUnsubscribe);
			    mimeMessage.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");		// RFC-8058
			}		
			
			// Set subject
			if (!K.isEmpty(gSubject)) {
				if (K.isEmpty(gSubjectCharSet)) {
					mimeMessage.setSubject(gSubject);
				} else {
					mimeMessage.setSubject(gSubject, gSubjectCharSet);
				}
			}
			
			// Log SMTP headers
			Enumeration<?> headers = mimeMessage.getAllHeaders();
		    while (headers.hasMoreElements()) {
		      Header header = (Header) headers.nextElement();
		      KLog.debug("SMTP header {}: {}", header.getName(), header.getValue());
		    }
		    
			//
			// Send email
			//
		    mimeMessage.setContent(gMimeMultipart);
		    KLog.debug("SMTP message created");
			Transport.send(mimeMessage);
		    
			// Write Jakarta log to KLog
			writeJakartaLog();

			// Get size of MIME message
			ByteArrayOutputStream dummyStream = new ByteArrayOutputStream();
			mimeMessage.writeTo(dummyStream);
			dummyStream.close();
			gMimeMessageSize = dummyStream.size();
			KLog.debug("SMTP message successfully sent ({})", K.formatBytes(gMimeMessageSize));

			// Return success
			return (true);
			
		} catch (Exception e) {

			// Save error message
			gLastErrorMessage = e.toString();
			
			// Write Jakarta log to KLog
			writeJakartaLog();
			
			KLog.error(gLastErrorMessage);
			return (false);
		}
	}
	
	/**
	 * Set user name and password for basic client authentication.
	 * 
	 * @param argUserName		User name
	 * @param argUserPassword	Password
	 */
	public void setAuthentication(String argUserName, String argUserPassword) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argUserName), "KSMTPMailer.setAuthentication(): User name is required");
		KLog.argException(K.isEmpty(argUserPassword), "KSMTPMailer.setAuthentication(): User password is required");
		
		gUserName				= argUserName;
		gUserPassword			= argUserPassword;
		gOAuth2Authentication	= false;
	}
	
	/**
	 * Set the blind carbon copy address(es).<br>
	 * 
	 * @param argBCCAddresses	Email address(es) separated by commas
	 */
	public void setBCC(String argBCCAddresses) {
		gBCCAddresses = argBCCAddresses;
	}
	
	/**
	 * Set the carbon copy address(es).<br>
	 * 
	 * @param argCCAddresses	Email address(es) separated by commas
	 */
	public void setCC(String argCCAddresses) {
		gCCAddresses = argCCAddresses;
	}
	
	/**
	 * Set sender address.<br>
	 * 
	 * @param argFromAddress	Sender email address
	 */
	public void setFrom(String argFromAddress) {
		gFromAddress = argFromAddress;
	}
	
	/**
	 * Set user name and access token for OAuth 2.0 authentication. The access token must previously been obtained from the
	 * authorization server of the hosting provider.
	 * 
	 * @param argUserName		User name
	 * @param argAccessToken	Access token
	 * 
	 * @since 2024.06.11
	 */
	public void setOAuth2Authentication(String argUserName, String argAccessToken) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argUserName), "KSMTPMailer.setOAuth2Authentication(): User name is required");
		KLog.argException(K.isEmpty(argAccessToken), "KSMTPMailer.setOAuth2Authentication(): Access token is required");
		
		gUserName				= argUserName;
		gUserPassword			= argAccessToken;
		gOAuth2Authentication	= true;
	}
	
	/**
	 * Set the reply-to address.<br>
	 * 
	 * @param argReplyToAddress	Email address
	 */
	public void setReplyTo(String argReplyToAddress) {
		gReplyToAddress = argReplyToAddress;
	}
	
	/**
	 * Set secure (STARTTLS or TLS) or non-secure connection.<br>
	 * 
	 * Note: The default is a secured connection.
	 * 
	 * @param argSecureConnection	True for secured connection, false otherwise.
	 */
	public void setSecureConnection(boolean argSecureConnection) {
		gSecureConnection = argSecureConnection;
	}
	
	/**
	 * Set SMTP host name.<br>
	 * 
	 * Note: If SMTP host name is not set, the host name from the highest priority MX record for the first recipient address
	 * (and the port number 25) is taken.<br>
	 * 
	 * @param argSMTPHostName	SMTP host name
	 * @param argSMTPHostPort	Host port
	 */
	public void setSMTPHost(String argSMTPHostName, int argSMTPHostPort) {

		// Check arguments
		KLog.argException(K.isEmpty(argSMTPHostName), "KSMTPMailer.setSMTPHost(): SMTP host name is required");
		KLog.argException((argSMTPHostPort < 1) || (argSMTPHostPort > 65535), "KSMTPMailer.setSMTPHost(): SMTP host port must be between 1 and 65535");
		
		gSMTPHostName	= argSMTPHostName;
		gSMTPHostPort	= argSMTPHostPort;
	}
	
	/**
	 * Set email subject.
	 * 
	 * @param argSubject	Subject to be set (default is no subject)
	 */
	public void setSubject(String argSubject) {
		gSubject		= argSubject;
		gSubjectCharSet = null;
	}
	
	/**
	 * Set email subject with given character set.
	 * 
	 * @param	argSubject	Subject to be set (default is no subject)
	 * @param	argCharSet	Character set (e.g."UTF-8")
	 * 
	 * @since 2024.06.11
	 */
	public void setSubject(String argSubject, String argCharSet) {
		gSubject		= argSubject;
		gSubjectCharSet = argCharSet;
	}
	
	/**
	 * Set the recipient address(es).<br>
	 * 
	 * @param argToAddresses	Email address(es) separated by commas
	 */
	public void setTo(String argToAddresses) {
		gToAddresses = argToAddresses;
	}
	
	/**
	 * Set unsubscribe mail header.
	 * 
	 * @param argLinks		Unsubscribe HTML link (Example {@code <mailto:unsubscribe@acme.com>,<https://unsubscribe.acme.com>}
	 * 
	 * @since 2024.06.19
	 */
	public void setUnsubscribe(String argLinks) {
		gUnsubscribe = argLinks;
	}
	
	/**
	 * Write Jakarta log to KLog
	 * 
	 * @since 2024.06.03
	 */
	private void writeJakartaLog() {
		
		// Check if Jakarta Mail log written
		if (gJakartaMailLog == null) {
			return;
		}
		
		KLog.debug("--- Start Jakarta Mail Debug Log ---");

		Scanner scanner = new Scanner(gJakartaMailLog.toString());
		while (scanner.hasNextLine()) {
            KLog.debug(scanner.nextLine());
        }
        scanner.close();
		
		KLog.debug("--- End Jakarta Mail Debug Log ---");
		
		// Free resource
		try {
			gJakartaMailLog.close();
		} catch (Exception e) {
			// Ignore errors
		}
		
		gJakartaMailLog = null;
	}

	/**
	 * String representation of object.
	 * 
	 * @since 2024.08.23
	 */
	@Override
	public String toString() {
		return "KSMTPMailer [gMimeMultipart=" + gMimeMultipart + ", gJakartaMailLog=" + gJakartaMailLog
				+ ", gLastErrorMessage=" + gLastErrorMessage + ", gSubject=" + gSubject + ", gSubjectCharSet="
				+ gSubjectCharSet + ", gFromAddress=" + gFromAddress + ", gToAddresses=" + gToAddresses
				+ ", gCCAddresses=" + gCCAddresses + ", gBCCAddresses=" + gBCCAddresses + ", gReplyToAddress="
				+ gReplyToAddress + ", gSMTPHostName=" + gSMTPHostName + ", gUserName=" + gUserName + ", gUserPassword="
				+ gUserPassword + ", gUnsubscribe=" + gUnsubscribe + ", gSMTPHostPort=" + gSMTPHostPort
				+ ", gMimeMessageSize=" + gMimeMessageSize + ", gSecureConnection=" + gSecureConnection
				+ ", gMultiPartAdded=" + gMultiPartAdded + ", gOAuth2Authentication=" + gOAuth2Authentication + "]";
	}
}
