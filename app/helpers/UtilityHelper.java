package helpers;

import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import play.Logger;
import play.api.Play;

public class UtilityHelper {
	
	private static final String COMPONENT_NAME = "Utility Helper";	

	public static String getUniqueId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	
	public static Boolean isEmptyString(String string) {
		if (string==null || string.trim().equalsIgnoreCase(""))
			return true;
		else
			return false;
	}
	
	
	public static JsonNode getCountries() {
		JsonNode result = null;
		JsonFactory factory = new MappingJsonFactory();
		try {
			JsonParser parser = factory.createJsonParser(Play.current().classloader().getResourceAsStream("countries.json"));
			result = parser.readValueAsTree();
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getCountries()", exception.getMessage(), exception);
		}
		return result;
	}
	
	
	/**
	 * Used to convert Map to a HTTP POST request string 
	 */
	public static String convertMapToRequestString(Map<String, String[]> map) {
		StringBuffer requestString = new StringBuffer();
		for(String key: map.keySet())
			for(String value: map.get(key)) 
				requestString.append(key+"="+value+"&");
		return requestString.toString().substring(0, requestString.length()-1);
	}
	
	
	public static void logMessage(String component, String method, String message) {
		Logger.info("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message);
	}
	

	public static void logError(String component, String method, String message, Throwable error) {
		Logger.error("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message, error);
	}
	

	public static void sendMail(final String email, final String subject, final String body) {
		Runnable mailThread = new Runnable() {
			@Override
			public void run() {
				try {
					final String EMAIL_USER = "info@shadence.com";
					final String EMAIL_PASSWORD = "myn$3007";
					final String EMAIL_HOST = "smtpout.secureserver.net";
			
					Properties props = new Properties();
					props.setProperty("mail.smtp.starttls.enable","true");
					props.setProperty("mail.smtp.auth","true");
					props.setProperty("mail.transport.protocol", "smtp");
					props.setProperty("mail.host", EMAIL_HOST);
			
					Session mailSession = Session.getInstance(props, new Authenticator() {
					    @Override
					    protected PasswordAuthentication getPasswordAuthentication() {
					        return new PasswordAuthentication(EMAIL_USER, EMAIL_PASSWORD);
					    }
					});
					Transport transport = mailSession.getTransport();
					MimeMessage message = new MimeMessage(mailSession);
					message.setSubject(subject);
					message.setFrom(new InternetAddress(EMAIL_USER, "Clocen"));
					message.setContent(body, "text/html");
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
					transport.connect();
					transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
					transport.close();
				} catch (Exception exception) {
					logError(COMPONENT_NAME, "sendMail()", exception.getMessage(), exception);
				}
			}
		};
		
		new Thread(mailThread).start();
	}

}
