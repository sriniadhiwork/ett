package gov.nist.healthcare.ttt.smtp.testcases;

import gov.nist.healthcare.ttt.smtp.TestInput;
import gov.nist.healthcare.ttt.smtp.TestResult;
import gov.nist.healthcare.ttt.smtp.TestResult.CriteriaStatus;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.sun.mail.imap.IMAPFolder;

public class TTTReceiverTests {

	public static Logger log = Logger.getLogger("TTTReceiverTests");

	/*
	 * Fetches a unread mail from the inbox. The mail is set as read after it's
	 * fetched.
	 */
	public TestResult fetchMail(TestInput ti) throws IOException {

		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		String result1 = "";
		// int j = 0;
		Properties props = System.getProperties();

		try {

			Properties prop = new Properties();
			String path = "./application.properties";
			FileInputStream file = new FileInputStream(path);
			prop.load(file);
			file.close();

			//	Session session = Session.getDefaultInstance(props, null);
			Session session = Session.getInstance(props, null);
			Store store = session.getStore("imap");
			store.close();
			store.connect(ti.tttSmtpAddress, Integer.parseInt(prop.getProperty("ett.imap.port")),
					prop.getProperty("ett.starttls.address"),
					prop.getProperty("ett.password"));
			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {

				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				String sender = ti.sutEmailAddress;
				if (sender_.equals(sender)) {
					// j++;
					// Store all the headers in a map
					Enumeration headers = message.getAllHeaders();
					while (headers.hasMoreElements()) {
						Header h = (Header) headers.nextElement();
						// result.put(h.getName() + " " + "[" + j +"]",
						// h.getValue());
						result.put("\n" + h.getName(), h.getValue() + "\n");
					}

					result.put("Delivered-To", "********");

					// Storing the Message Body Parts
					Multipart multipart = (Multipart) message.getContent();
					for (int i = 0; i < multipart.getCount(); i++) {
						BodyPart bodyPart = multipart.getBodyPart(i);
						InputStream stream = bodyPart.getInputStream();

						
						
						byte[] targetArray = IOUtils.toByteArray(stream);
						System.out.println(new String(targetArray));
						int m = i + 1;
						if (bodyPart.getFileName() != null) {
							bodyparts.put(bodyPart.getFileName(), new String(
									targetArray));
							
							if ((bodyPart.getFileName().contains(".xml") || bodyPart.getFileName().contains(".XML"))){
							// Query MDHT war endpoint
							CloseableHttpClient client = HttpClients.createDefault();
							FileUtils.writeByteArrayToFile(new File("sample.xml"), targetArray);
							File file1 = new File("sample.xml");
							HttpPost post = new HttpPost("http://hit-dev.nist.gov:11080/referenceccdaservice/");
							FileBody fileBody = new FileBody(file1);
							
							
							//
							MultipartEntityBuilder builder = MultipartEntityBuilder.create();
							builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
							builder.addTextBody("validationObjective", "170.315(b)(1)");
							builder.addTextBody("referenceFileName", "CP_Sample1.pdf");
						//	builder.addTextBody("validationObjective", ti.ccdaValidationObjective);
						//	builder.addTextBody("referenceFileName", ti.ccdaReferenceFilename);
							builder.addPart("ccdaFile", fileBody);
							HttpEntity entity = builder.build();
							//
							post.setEntity(entity);
							
							
								HttpResponse response = client.execute(post);
								// CONVERT RESPONSE TO STRING
								result1 = EntityUtils.toString(response.getEntity());
								result.put("\n" + "Validation Result for " + bodyPart.getFileName() , result1 + "\n");
							}
							
						} else {
							bodyparts.put("Message Content" + " " + m,
									new String(targetArray));
						}
						
						

					}
				}
				// inbox.setFlags(messages, new Flags(Flags.Flag.SEEN), true);

			}

			if (result.size() == 0) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses()
				.put("\nERROR",
						"No messages found! Send a message and try again.\nPlease make sure that the Vendor Email Address is entered and matches the email address from which the email is being sent.\nWait for atleast 30 seconds after sending the email to ensure successful delivery to the ETT.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}
		} catch (Exception e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put(
					"\nERROR",
					"Cannot fetch email from " + ti.tttSmtpAddress + " :"
							+ e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * Fetches message-ids for each unread message in the inbox.
	 */
	public TestResult fetchUniqueId(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashSet<String> hash = new HashSet<String>();
		HashMap<String, String> result = tr.getTestRequestResponses();

		int j = 1;
		Properties props = System.getProperties();

		try {
			Properties prop = new Properties();
			String path = "./application.properties";
			FileInputStream file = new FileInputStream(path);
			prop.load(file);
			file.close();

			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			store.connect(ti.tttSmtpAddress, Integer.parseInt(prop.getProperty("ett.imap.port")),
					prop.getProperty("ett.other.address"),
					prop.getProperty("ett.password"));

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {

				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				String sender = ti.sutEmailAddress;
				if (sender_.equals(sender)) {

					// Store all the headers in a map
					Enumeration headers = message.getAllHeaders();
					while (headers.hasMoreElements()) {
						Header h = (Header) headers.nextElement();
						String mID = h.getName();
						if (mID.contains("Message-ID")) {
							result.put("\nMessage-ID " + j, h.getValue());
							hash.add(h.getValue());
							j++;
						}
					}

					inbox.setFlags(messages, new Flags(Flags.Flag.SEEN), true);

				}
			}

			if (hash.size() == result.size()) {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			} else {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("\nERROR",
						"Message IDs not unique. One or more messages have the same message-ID");
			}

			if (result.size() < 3) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put(
						"\nERROR",
						"ETT received "
								+ result.size()
								+ " messages.\n"
								+ "Please verify that the user has sent atleast 3 messages and also wait for few minutes after sending to ensure delivery.");

			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}

		} catch (MessagingException e) {
			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Unknown Host  - " + e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * Fetches the Disposition-Notification-Header from the mail.
	 */
	public TestResult fetchDispositionNotificaton(TestInput ti)
			throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		HashMap<String, String> result = tr.getTestRequestResponses();

		Properties props = System.getProperties();

		try {
			Properties prop = new Properties();
			String path = "./application.properties";
			FileInputStream file = new FileInputStream(path);
			prop.load(file);
			file.close();

			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			store.connect(ti.tttSmtpAddress, Integer.parseInt(prop.getProperty("ett.imap.port")),
					prop.getProperty("ett.other.address"),
					prop.getProperty("ett.password"));

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);
			for (Message message : messages) {

				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				String sender = ti.sutEmailAddress;
				if (sender_.equals(sender)) {
					Enumeration headers = message.getAllHeaders();
					while (headers.hasMoreElements()) {
						Header h = (Header) headers.nextElement();
						String dispositionOptions = h.getName();
						if (dispositionOptions
								.contains("Disposition-Notification-Options")) {
							// result.put("Disposition-Notification-Options ",
							// h.getValue());
							tr.setCriteriamet(CriteriaStatus.TRUE);
						}
						result.put("\n" + h.getName(), h.getValue() + "\n");
					}
					inbox.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
					break;
				}
			}

			if (result.size() == 0) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put(
						"\nERROR",
						"No messages found! Send a message and try again.\nPlease make sure that the vendor email address is entered correctly and matches the email address from which the email is being sent.");
			}

		} catch (MessagingException e) {
			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Error fetching email  - " + e.getLocalizedMessage());
		} catch (Exception e) {
			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error" + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Error  " + e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * Stub to display log messages for manual test on the too
	 */
	public TestResult fetchManualTest(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();

		tr.getTestRequestResponses()
		.put("\nAwating confirmation from proctor",
				"Proctor needs to verify the failure message from invalid recipient.");
		tr.setCriteriamet(CriteriaStatus.MANUAL);

		return tr;
	}

	public TestResult imapFetch(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		Properties props = System.getProperties();
		props.put("mail.imap.starttls.enable", true);
		props.put("mail.imap.starttls.required", true);
		props.put("mail.imap.sasl.enable", true);
		props.put("mail.imap.sasl.mechanisms", "PLAIN");
		props.put("mail.imap.ssl.trust", "*");

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			// store.connect(ti.sutSmtpAddress,110,ti.sutUserName,ti.sutPassword);
			store.connect(ti.sutSmtpAddress, 143, ti.sutUserName,
					ti.sutPassword);

			IMAPFolder inbox = (IMAPFolder) store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {
				long a = inbox.getUID(message);
				String strLong = Long.toString(a);
				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				// j++;
				// Store all the headers in a map
				Enumeration headers = message.getAllHeaders();
				while (headers.hasMoreElements()) {
					Header h = (Header) headers.nextElement();
					// result.put(h.getName() + " " + "[" + j +"]",
					// h.getValue());
					result.put("\n" + h.getName(), h.getValue() + "\n");

				}

				// result.put("Delivered-To", "********");
				result.put("\nUID", strLong);
				Multipart multipart = (Multipart) message.getContent();
				for (int i = 0; i < multipart.getCount(); i++) {
					BodyPart bodyPart = multipart.getBodyPart(i);
					InputStream stream = bodyPart.getInputStream();

					byte[] targetArray = IOUtils.toByteArray(stream);
					System.out.println(new String(targetArray));
					int m = i + 1;
					bodyparts.put("bodyPart" + " " + "[" + m + "]", new String(
							targetArray));

				}

			}
			if (bodyparts.isEmpty()) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses().put("\nERROR",
						"No messages found. Send a message and try again.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}

		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Cannot fetch email from :" + e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * Fecthes an email from inbox. Connects to the IMAP server using MD5 cipher
	 * suite.
	 */
	public TestResult imapFetch13(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		Properties props = System.getProperties();
		props.put("mail.imap.starttls.enable", true);
		props.put("mail.imap.starttls.required", true);
		props.put("mail.imap.ssl.ciphersuites", "TLS_RSA_WITH_RC4_128_MD5");
		props.put("mail.imap.sasl.enable", true);
		props.put("mail.imap.sasl.mechanisms", "PLAIN");
		props.put("mail.imap.ssl.trust", "*");

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			// store.connect(ti.sutSmtpAddress,110,ti.sutUserName,ti.sutPassword);
			store.connect(ti.sutSmtpAddress, 143, ti.sutUserName,
					ti.sutPassword);

			IMAPFolder inbox = (IMAPFolder) store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {
				long a = inbox.getUID(message);
				String strLong = Long.toString(a);
				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				// j++;
				// Store all the headers in a map
				Enumeration headers = message.getAllHeaders();
				while (headers.hasMoreElements()) {
					Header h = (Header) headers.nextElement();
					// result.put(h.getName() + " " + "[" + j +"]",
					// h.getValue());
					result.put("\n" + h.getName(), h.getValue() + "\n");

				}

				// result.put("Delivered-To", "********");
				result.put("\nUID", strLong);
				Multipart multipart = (Multipart) message.getContent();
				for (int i = 0; i < multipart.getCount(); i++) {
					BodyPart bodyPart = multipart.getBodyPart(i);
					InputStream stream = bodyPart.getInputStream();

					byte[] targetArray = IOUtils.toByteArray(stream);
					System.out.println(new String(targetArray));
					int m = i + 1;
					bodyparts.put("bodyPart" + " " + "[" + m + "]", new String(
							targetArray));

				}
				store.close();

			}
			if (bodyparts.isEmpty()) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses().put("\nERROR",
						"No messages found. Send a message and try again.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}

		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Cannot fetch email from :" + e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * Fetches a mail with 3DES Cipher Suite!
	 */
	public TestResult imapFetch14(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		Properties props = System.getProperties();
		props.put("mail.imap.starttls.enable", true);
		props.put("mail.imap.starttls.required", true);
		props.put("mail.imap.ssl.ciphersuites",
				"TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA");
		props.put("mail.imap.sasl.enable", true);
		props.put("mail.imap.sasl.mechanisms", "PLAIN");
		props.put("mail.imap.ssl.trust", "*");

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			// store.connect(ti.sutSmtpAddress,110,ti.sutUserName,ti.sutPassword);
			store.connect(ti.sutSmtpAddress, 143, ti.sutUserName,
					ti.sutPassword);

			IMAPFolder inbox = (IMAPFolder) store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			
			for (Message message : messages) {
				long a = inbox.getUID(message);
				String strLong = Long.toString(a);
				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				// j++;
				// Store all the headers in a map
				Enumeration headers = message.getAllHeaders();
				while (headers.hasMoreElements()) {
					Header h = (Header) headers.nextElement();
					// result.put(h.getName() + " " + "[" + j +"]",
					// h.getValue());
					result.put("\n" + h.getName(), h.getValue() + "\n");

				}

				// result.put("Delivered-To", "********");
				result.put("\nUID", strLong);
				Multipart multipart = (Multipart) message.getContent();
				for (int i = 0; i < multipart.getCount(); i++) {
					BodyPart bodyPart = multipart.getBodyPart(i);
					InputStream stream = bodyPart.getInputStream();

					byte[] targetArray = IOUtils.toByteArray(stream);
					System.out.println(new String(targetArray));
					int m = i + 1;
					bodyparts.put("bodyPart" + " " + "[" + m + "]", new String(
							targetArray) + "\n");

				}
				store.close();

			}
			if (bodyparts.isEmpty()) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses().put("\nERROR",
						"No messages found. Send a message and try again.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}

		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR",
					"Cannot fetch email from :" + e.getLocalizedMessage());
		}

		return tr;
	}

	/*
	 * 
	 * 
	 */
	public TestResult imapFetchWrongPass(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		Properties props = System.getProperties();
		props.put("mail.imap.sasl.enable", true);
		props.put("mail.imap.starttls.enable", true);
		props.put("mail.imap.starttls.required", true);
		props.put("mail.imap.sasl.mechanisms", "PLAIN");
		props.put("mail.imap.ssl.trust", "*");
		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			store.connect(ti.sutSmtpAddress, 143, ti.sutUserName, "badPassword");

			IMAPFolder inbox = (IMAPFolder) store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

		} catch (AuthenticationFailedException e) {

			tr.setCriteriamet(CriteriaStatus.TRUE);
			e.printStackTrace();
			tr.getTestRequestResponses().put(
					"\nSUCCESS",
					"Vendor rejects bad Username/Password combination :"
							+ e.getLocalizedMessage());

		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR :",
					e.getLocalizedMessage());

		}

		return tr;
	}

	/*
	 * Fetches UID for each email in the IMAP inbox. Each UID is a unique number
	 * given by the inbox.
	 */
	public TestResult imapFetchUid(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		Properties props = System.getProperties();
		int j = 1;

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			store.connect(ti.sutSmtpAddress, 143, ti.sutUserName,
					ti.sutPassword);

			IMAPFolder inbox = (IMAPFolder) store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {
				long a = inbox.getUID(message);
				String strLong = Long.toString(a);
				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();

				// Store all the headers in a map
				Enumeration headers = message.getAllHeaders();
				/*
				 * while (headers.hasMoreElements()) { Header h = (Header)
				 * headers.nextElement(); // result.put(h.getName() + " " + "["
				 * + j +"]", h.getValue()); result.put(h.getName(),
				 * h.getValue());
				 * 
				 * }
				 */

				result.put("\nUID " + j, strLong);
				j++;

				Multipart multipart = (Multipart) message.getContent();
				/*
				 * for (int i = 0; i < multipart.getCount(); i++) { BodyPart
				 * bodyPart = multipart.getBodyPart(i); InputStream stream =
				 * bodyPart.getInputStream();
				 * 
				 * byte[] targetArray = IOUtils.toByteArray(stream);
				 * System.out.println(new String(targetArray)); int m = i+1; //
				 * bodyparts.put("bodyPart" + " " + "[" +m +"]", new
				 * String(targetArray)); // bodyparts.put("bodyPart" + "" + ,
				 * new String (targetArray));
				 * 
				 * }
				 */

			}
			if (result.isEmpty()) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses().put("\nERROR",
						"No messages found. Send a message and try again.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}

		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			e.printStackTrace();
			// log.info("Error fetching email " + e.getLocalizedMessage());
			log.info("Error getting mail from TTT server"
					+ e.getLocalizedMessage());
			tr.getTestRequestResponses().put("\nERROR ",
					e.getLocalizedMessage());

		}

		return tr;
	}

	public TestResult testStatusUpdate(TestInput ti) {

		TestResult tr = new TestResult();
		tr.setProctored(true);
		tr.setCriteriamet(CriteriaStatus.MANUAL); // proctored are true unless
		// exception happens
		HashMap<String, String> result = tr.getTestRequestResponses();

		// Create a mail session
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", ti.useTLS ? "true"
				: "false");
		properties.put("mail.smtp.quitwait", "false");
		properties.put("mail.smtp.userset", "true");
		properties.put("mail.smtp.ssl.trust", "*");
		try {
			Session session = Session.getInstance(properties, null);

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(ti.sutUserName));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("wellformed14@hit-testing2.nist.gov"));

			message.setSubject("Email from TTT (Test Case 9)");
			message.setText("This is a mail from JAMES Server");

			BodyPart messageBodyPart = new MimeBodyPart();

			messageBodyPart.setText("This is message body");

			Multipart multipart = new MimeMultipart();
			String aName = "";
			/*
			 * for (Map.Entry<String, byte[]> e :
			 * ti.getAttachments().entrySet()) {
			 * 
			 * DataSource source = new ByteArrayDataSource(e.getValue(),
			 * "text/html"); messageBodyPart.setDataHandler(new
			 * DataHandler(source)); messageBodyPart.setFileName(e.getKey());
			 * aName += e.getKey(); multipart.addBodyPart(messageBodyPart);
			 * 
			 * // Send the complete message parts message.setContent(multipart);
			 * }
			 */
			Transport transport = session.getTransport("smtp");
			transport.connect("smtp.gmail.com", 25, "sut.example@gmail.com",
					"smtptesting123");
			transport.close();

			log.info("SENDING FIRST EMAIL");

			tr.getTestRequestResponses()
			.put("\nAwating confirmation from proctor",
					"Proctor needs to verify the Edge system for updates from the server.");
			System.out.println("Email sent successfully");

		} catch (MessagingException e) {
			e.printStackTrace();
			log.info("Error in Testcase 25");
			result.put("1", "Error Sending Email " + e.getLocalizedMessage()
					+ new String(e.getMessage()));
			tr.setCriteriamet(CriteriaStatus.FALSE);
		}

		return tr;
	}

	String read(DataInputStream s) throws IOException {
		String s1 = "";
		int b;
		while ((b = s.read()) != -1) {
			s1 += String.valueOf(b);
			System.out.println("READ FUNCTION");

		}
		return s1;
	}

	@SuppressWarnings("deprecation")
	public TestResult SocketImap(TestInput ti) throws NoSuchAlgorithmException,
	KeyManagementException {
		TestResult tr = new TestResult();
		ArrayList<String> response = new ArrayList<String>();
		HashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		//	SSLSocket socket = null;
		Socket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			/*socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 993);*/

			socket = new Socket(InetAddress.getByName(ti.sutSmtpAddress), 143);
			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream(), "Windows-1252")), true);
			// output = new DataOutputStream(socket.getOutputStream());
			// is = new DataInputStream(socket.getInputStream());

			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "Couldn't get I/O for the connection to "
					+ ti.sutSmtpAddress + e.getLocalizedMessage());
		}
		// If everything has been initialized then we want to write some data
		// to the socket we have opened a connection to on port 110
		if (socket != null && output != null && is != null) {
			try {

				output.print("A1 CAPABILITY\r\n");
				output.flush();
				output.print("A2 NOOP\r\n");
				output.flush();
				output.print("A3 LOGOUT\r\n");
				output.flush();
				// keep on reading from/to the socket till we receive the "Ok"
				// from IMAP,
				// once we received that then we want to break.
				// String responseLine;
				int i = 1;

				String responseLine;
				while ((responseLine = is.readLine()) != null) {

					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		for (String s : response) {
			if (s.contains("BAD")) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR", "All commands are not implemented");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);

			}

		}

		if (response.size() > 2) {
			tr.setCriteriamet(CriteriaStatus.TRUE);
			result.put("SUCCESS",
					"The CAPABILITY, NOOP and LOGOUT commands are implemented");
		} else
			tr.setCriteriamet(CriteriaStatus.FALSE);

		return tr;

	}

	public TestResult SocketImapBadSyntax(TestInput ti) {
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		ArrayList<String> response = new ArrayList<String>();
		//		SSLSocket socket = null;
		Socket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			/*socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
						.getDefault()).createSocket(
								InetAddress.getByName(ti.sutSmtpAddress), 993);*/

			socket = new Socket(InetAddress.getByName(ti.sutSmtpAddress), 143);
			output = new PrintWriter(socket.getOutputStream(), true);
			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "No route to host " + e.getLocalizedMessage());
		}
		if (socket != null && output != null && is != null) {
			try {

				output.print("A1 CAPABILITYBAD\r\n");
				output.flush();
				output.print("A2 LOGOUT\r\n");
				output.flush();

				// keep on reading from/to the socket till we receive the "Ok"
				// from SMTP,
				// once we received that then we want to break.
				String responseLine;
				int i = 1;
				while ((responseLine = is.readLine()) != null) {
					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				// clean up:
				// close the output stream
				// close the input stream
				// close the socket
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		if (response.size() > 1) {
			if (response.get(1).contains("BAD") || response.get(1).contains("BYE") || response.get(1).contains("FAIL")) {
				tr.setCriteriamet(CriteriaStatus.TRUE);
				result.put("SUCCESS",
						"The server rejects the command with bad syntax");
			} else {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR",
						"The server accepts commands with bad syntax!\n");
			}

		}

		return tr;
	}

	public TestResult SocketImapBadState(TestInput ti) {
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		ArrayList<String> response = new ArrayList<String>();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		//		SSLSocket socket = null;
		Socket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			/*socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
						.getDefault()).createSocket(
								InetAddress.getByName(ti.sutSmtpAddress), 993);*/

			socket = new Socket(InetAddress.getByName(ti.sutSmtpAddress), 143);
			output = new PrintWriter(socket.getOutputStream(), true);
			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "No route to host " + e.getLocalizedMessage());
		}
		if (socket != null && output != null && is != null) {
			try {
				output.print("a1 FETCH\r\n");
				output.flush();
				output.print("a2 LOGOUT\r\n");
				output.flush();

				String responseLine;
				int i = 1;
				while ((responseLine = is.readLine()) != null) {
					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}

				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		if (response.size() > 1) {
			if	(response.get(1).contains("BAD") || response.get(1).contains("NO") || response.get(1).contains("FAIL")) {
				tr.setCriteriamet(CriteriaStatus.TRUE);
				result.put("SUCCESS",
						"The server rejects the command based on the state of the connection");
			} else {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR",
						"The server accepts commands without regards to state of the connection!\n");
			}
		}

		return tr;
	}

	public TestResult SocketStarttls(TestInput ti) {
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		ArrayList<String> response = new ArrayList<String>();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		SSLSocket socket1 = null;
		Socket socket = null;
		PrintWriter output = null;
		DataInputStream is = null;
		try {
			socket1 = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 143);
			socket = new Socket(InetAddress.getByName(ti.sutSmtpAddress), 993);
			output = new PrintWriter(socket.getOutputStream(), true);
			is = new DataInputStream(socket.getInputStream());
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "No route to host " + e.getLocalizedMessage());
		}
		if (socket != null && output != null && is != null) {
			try {
				output.print("a1 STARTTLS\r\n");
				output.flush();
				output.print("a2 LOGOUT\r\n");
				output.flush();
				output.print("a3 EXIT\r\n");
				output.flush();

				String responseLine;
				int i = 1;
				while ((responseLine = is.readLine()) != null) {
					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}

				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		if (response.size() > 1) {
			if (response.get(1).contains("BAD")
					|| response.get(1).contains("NO")) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR",
						"The STARTTLS command is not implemented!\n");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
				result.put("SUCCESS", "The STARTTLS command is implemented\n");
			}
		}

		return tr;
	}

	@SuppressWarnings("deprecation")
	public TestResult SocketPop(TestInput ti) throws NoSuchAlgorithmException,
	KeyManagementException {
		TestResult tr = new TestResult();
		ArrayList<String> response = new ArrayList<String>();
		HashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		SSLSocket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 995);

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream(), "Windows-1252")), true);
			// output = new DataOutputStream(socket.getOutputStream());
			// is = new DataInputStream(socket.getInputStream());

			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "Couldn't get I/O for the connection to "
					+ ti.sutSmtpAddress + e.getLocalizedMessage());
		}
		// If everything has been initialized then we want to write some data
		// to the socket we have opened a connection to on port 110
		if (socket != null && output != null && is != null) {
			try {

				output.print("CAPA\r\n");
				output.flush();
				output.print("NOOP\r\n");
				output.flush();
				output.print("QUIT\r\n");
				output.flush();
				// keep on reading from/to the socket till we receive the "Ok"
				// from IMAP,
				int i = 1;

				String responseLine;
				while ((responseLine = is.readLine()) != null) {

					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		for (String s : response) {
			if (s.contains("ERR")) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR", "All commands are not implemented");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);

			}

		}

		if (response.size() > 2) {
			tr.setCriteriamet(CriteriaStatus.TRUE);
			result.put("SUCCESS",
					"The CAPABILITY, NOOP and QUIT commands are implemented");
		} else
			tr.setCriteriamet(CriteriaStatus.FALSE);

		return tr;

	}

	@SuppressWarnings("deprecation")
	public TestResult SocketPopBadSyntax(TestInput ti)
			throws NoSuchAlgorithmException, KeyManagementException {
		TestResult tr = new TestResult();
		ArrayList<String> response = new ArrayList<String>();
		HashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		SSLSocket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 995);

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream(), "Windows-1252")), true);
			// output = new DataOutputStream(socket.getOutputStream());
			// is = new DataInputStream(socket.getInputStream());

			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "Couldn't get I/O for the connection to "
					+ ti.sutSmtpAddress + e.getLocalizedMessage());
		}
		// If everything has been initialized then we want to write some data
		// to the socket we have opened a connection to on port 110
		if (socket != null && output != null && is != null) {
			try {

				output.print("BADSYN\r\n");
				output.flush();
				output.print("QUIT\r\n");
				output.flush();
				// keep on reading from/to the socket till we receive the "Ok"
				// from IMAP,
				// once we received that then we want to break.
				// String responseLine;
				int i = 1;

				String responseLine;
				while ((responseLine = is.readLine()) != null) {

					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		for (String s : response) {
			if (s.contains("ERR")) {
				tr.setCriteriamet(CriteriaStatus.TRUE);
				result.put("SUCCESS",
						"POP server rejects the command with bad sybtax.");
			} else {
				tr.setCriteriamet(CriteriaStatus.FALSE);

			}

		}

		/*
		 * if(response.size() > 2) { tr.setCriteriamet(CriteriaStatus.TRUE);
		 * result.put("SUCCESS",
		 * "The CAPABILITY, NOOP and QUIT commands are implemented"); } else
		 * tr.setCriteriamet(CriteriaStatus.FALSE);
		 */

		return tr;

	}

	@SuppressWarnings("deprecation")
	public TestResult SocketPopBadState(TestInput ti)
			throws NoSuchAlgorithmException, KeyManagementException {
		TestResult tr = new TestResult();
		ArrayList<String> response = new ArrayList<String>();
		HashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		SSLSocket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 995);

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream(), "Windows-1252")), true);
			// output = new DataOutputStream(socket.getOutputStream());
			// is = new DataInputStream(socket.getInputStream());

			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "Couldn't get I/O for the connection to "
					+ ti.sutSmtpAddress + e.getLocalizedMessage());
		}
		// If everything has been initialized then we want to write some data
		// to the socket we have opened a connection to on port 110
		if (socket != null && output != null && is != null) {
			try {

				output.print("CAPA\r\n");
				output.flush();
				output.print("STAT\r\n");
				output.flush();
				output.print("QUIT\r\n");
				output.flush();
				// keep on reading from/to the socket till we receive the "Ok"
				// from IMAP,
				// once we received that then we want to break.
				// String responseLine;
				int i = 1;

				String responseLine;
				while ((responseLine = is.readLine()) != null) {

					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		for (String s : response) {
			if (s.contains("ERR")) {
				tr.setCriteriamet(CriteriaStatus.TRUE);
				result.put("SUCCESS",
						"POP server rejects the command with bad sybtax.");
			} else {
				tr.setCriteriamet(CriteriaStatus.FALSE);

			}

		}

		/*
		 * if(response.size() > 2) { tr.setCriteriamet(CriteriaStatus.TRUE);
		 * result.put("SUCCESS",
		 * "The CAPABILITY, NOOP and QUIT commands are implemented"); } else
		 * tr.setCriteriamet(CriteriaStatus.FALSE);
		 */

		return tr;

	}

	public TestResult SocketPopStat(TestInput ti) throws NoSuchAlgorithmException,
	KeyManagementException {
		TestResult tr = new TestResult();
		ArrayList<String> response = new ArrayList<String>();
		LinkedHashMap<String, String> result = tr.getTestRequestResponses();
		tr.setCriteriamet(CriteriaStatus.FALSE);
		SSLSocket socket = null;
		PrintWriter output = null;
		BufferedReader is = null;

		// Initialization section:
		// Try to open a socket on port 110
		// Try to open input and output streams
		try {
			System.setProperty("java.net.preferIPv4Stack", "true");
			socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory
					.getDefault()).createSocket(
							InetAddress.getByName(ti.sutSmtpAddress), 995);

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream(), "Windows-1252")), true);
			// output = new DataOutputStream(socket.getOutputStream());
			// is = new DataInputStream(socket.getInputStream());

			is = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "Windows-1252"));
		} catch (UnknownHostException e1) {
			System.err.println("Don't know about host: hostname");
			result.put("ERROR", "Unknown Host " + e1.getLocalizedMessage());
			tr.setCriteriamet(CriteriaStatus.FALSE);
		} catch (IOException e) {
			System.err
			.println("Couldn't get I/O for the connection to: hostname");
			tr.setCriteriamet(CriteriaStatus.FALSE);
			result.put("ERROR", "Couldn't get I/O for the connection to "
					+ ti.sutSmtpAddress + e.getLocalizedMessage());
		}
		// If everything has been initialized then we want to write some data
		// to the socket we have opened a connection to on port 110
		if (socket != null && output != null && is != null) {
			try {

				output.print("USER " + ti.sutUserName + "\r\n");
				output.flush();
				output.print("PASS " + ti.sutPassword + "\r\n");
				output.flush();
				output.print("STAT\r\n");
				output.flush();
				output.print("LIST\r\n");
				output.flush();
				output.print("QUIT\r\n");
				output.flush();
				// keep on reading from/to the socket till we receive the "Ok"
				// from IMAP,
				// once we received that then we want to break.
				// String responseLine;
				int i = 1;

				String responseLine;
				while ((responseLine = is.readLine()) != null) {

					System.out.println("Server: " + responseLine);
					result.put("SERVER " + i, responseLine + "\n");
					response.add(responseLine);
					i++;
					if (responseLine.indexOf("Ok") != -1) {
						break;
					}
				}
				output.close();
				is.close();
				socket.close();
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				tr.getTestRequestResponses().put("ERROR", "Unknown host " + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				tr.getTestRequestResponses().put("ERROR", "IO Exception" + e);
				tr.setCriteriamet(CriteriaStatus.FALSE);
			}
		}

		for (String s : response) {
			if (s.contains("ERR")) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				result.put("ERROR", "All commands are not implemented");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);

			}

		}

		if (response.size() > 2) {
			tr.setCriteriamet(CriteriaStatus.TRUE);
			result.put("SUCCESS",
					"The CAPABILITY, NOOP and QUIT commands are implemented");
		} else
			tr.setCriteriamet(CriteriaStatus.FALSE);

		return tr;

	}
	public TestResult fetchMailPop(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		HashMap<String, String> result = tr.getTestRequestResponses();
		HashMap<String, String> bodyparts = tr.getAttachments();
		// int j = 0;
		Properties props = System.getProperties();
		props.put("mail.pop3s.starttls.enable", true);
		props.put("mail.pop3s.starttls.required", true);

		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("pop3");
			store.connect(ti.sutSmtpAddress,110,ti.sutUserName,ti.sutPassword);

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {

				Address[] froms = message.getFrom();
				String sender_ = froms == null ? ""
						: ((InternetAddress) froms[0]).getAddress();
				// Sender's Address
				String sender = ti.sutEmailAddress;
				if (sender_.equals(sender)) {
					// j++;
					// Store all the headers in a map
					Enumeration headers = message.getAllHeaders();
					while (headers.hasMoreElements()) {
						Header h = (Header) headers.nextElement();
						// result.put(h.getName() + " " + "[" + j +"]",
						// h.getValue());
						result.put("\n" + h.getName(), h.getValue() + "\n");
					}

					// Storing the Message Body Parts
					Multipart multipart = (Multipart) message.getContent();
					for (int i = 0; i < multipart.getCount(); i++) {
						BodyPart bodyPart = multipart.getBodyPart(i);
						InputStream stream = bodyPart.getInputStream();

						byte[] targetArray = IOUtils.toByteArray(stream);
						System.out.println(new String(targetArray));
						int m = i + 1;
						if (bodyPart.getFileName() != null) { 
							bodyparts.put(bodyPart.getFileName(), new String(
									targetArray));
						} else {
							bodyparts.put("Message Content" + " " + m,
									new String(targetArray));
						}

					}
				}

			}

			if (result.size() == 0) {
				tr.setCriteriamet(CriteriaStatus.FALSE);
				tr.getTestRequestResponses()
				.put("\nERROR",
						"No messages found! Send a message and try again.\nPlease make sure that the Vendor Email Address is entered and matches the email address from which the email is being sent.\nWait for atleast 30 seconds after sending the email to ensure successful delivery to the ETT.");
			} else {
				tr.setCriteriamet(CriteriaStatus.TRUE);
			}
		} catch (MessagingException e) {

			tr.setCriteriamet(CriteriaStatus.FALSE);
			;
			e.printStackTrace();
			log.info("Error fetching email " + e.getLocalizedMessage());
			tr.getTestRequestResponses().put(
					"\nERROR",
					"Cannot fetch email from " + ti.sutSmtpAddress + " :"
							+ e.getLocalizedMessage());
		}

		return tr;
	} 

	// Stub to display the log messages in the tool
	public TestResult fetchManualTestEdge(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();

		tr.getTestRequestResponses()
		.put("\nAwating confirmation from proctor",
				"Proctor needs to verify the messages retrieved from Edge Test Tool.");
		tr.setCriteriamet(CriteriaStatus.MANUAL);

		return tr;
	}

	public TestResult fetchTestMailboxNames(TestInput ti) throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		TestResult tr = new TestResult();
		tr.getTestRequestResponses()
		.put("\nAwating confirmation from proctor",
				"Proctor needs to verify the messages retrieved from Edge Test Tool.");
		tr.setCriteriamet(CriteriaStatus.MANUAL);
		return tr;
	}
}