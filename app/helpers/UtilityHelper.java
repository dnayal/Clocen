package helpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.Play;

public class UtilityHelper {
	
	private static final String COMPONENT_NAME = "Utility Helper";	
	public static final String REMEMBER_ME = "rememberMe"; // param name to remember user

	public static String getUniqueId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	
	public static Boolean isEmptyString(String string) {
		if (string==null || string.trim().equalsIgnoreCase(""))
			return true;
		else
			return false;
	}
	
	
	public static String getString(String string) {
		if(string==null)
			return "";
		else
			return string.trim();
	}
	
	
	public static JsonNode getCountries() {
		JsonNode result = null;
		JsonFactory factory = new MappingJsonFactory();
		try {
			JsonParser parser = factory.createJsonParser(Play.application().classloader().getResourceAsStream("countries.json"));
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
	
	
	/**
	 * Method used to get the node config (data node) as string
	 * Used for debugging purposes
	 */
	@SuppressWarnings("unchecked")
	public static String convertMapObjectToString(Map<String, Object> data) {
		StringBuffer string = new StringBuffer();
		
		String id = (String) data.get("id");
		if(UtilityHelper.isEmptyString(id))
			string.append("id:"+id+"..");
		
		String name = (String) data.get("name");
		if(UtilityHelper.isEmptyString(name))
			string.append("name:"+name+"..");

		ArrayList<Map<String, String>> inputs = (ArrayList<Map<String, String>>)data.get("input");
		if(inputs!=null) {
			string.append("..INPUTS..");
			for(Map<String, String> input : inputs) {
				Iterator<String> iterator = input.keySet().iterator();
				while(iterator.hasNext()) {
					String key = iterator.next();
					Object value = input.get(key);
					if(value instanceof String)
						string.append(key+":"+input.get(key)+"..");
				}
			}
		}
		
		ArrayList<Map<String, String>> outputs = (ArrayList<Map<String, String>>)data.get("output");
		if(inputs!=null) {
			string.append("..OUTPUTS..");
			for(Map<String, String> output : outputs) {
				Iterator<String> iterator = output.keySet().iterator();
				while(iterator.hasNext()) {
					String key = iterator.next();
					string.append(key+":"+output.get(key)+"..");
				}
			}
		}

		return string.toString();
	}
	
	
	public static void logMessage(String component, String method, String message) {
		Logger.debug("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message);
	}
	

	public static void logError(String component, String method, String message, Throwable error) {
		Logger.error("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message, error);
	}
	

	public static void sendMail(final String email, final String subject, final String body) {
		Runnable mailThread = new Runnable() {
			@Override
			public void run() {
				try {
					final String EMAIL_USER = "info@clocen.com";
					final String EMAIL_PASSWORD = "windows@2000";
					final String EMAIL_HOST = "smtp-mail.outlook.com"; // for outlook.com - http://windows.microsoft.com/en-us/windows/outlook/send-receive-from-app
			
					Properties props = new Properties();
					props.setProperty("mail.smtp.starttls.enable","true");
					props.setProperty("mail.smtp.auth","true");
					props.setProperty("mail.transport.protocol", "smtp");
					props.put("mail.smtp.port", "25");
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
	

	/**
	 * Converts the dateTime, in the given format, to UTC time
	 */
	public static DateTime convertToUTCTime(String dateTime, String format) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(format);
		return dateTimeFormatter.parseDateTime(dateTime).withZone(DateTimeZone.UTC);
	}
	

	/**
	 * Returns the current time in UTC format, minus the poller interval.
	 * Used for polling for triggers where nodes do not support Webhooks
	 */
	public static DateTime getCurrentTimeMinusPollerInterval() {
		Integer pollerInterval = Play.application().configuration().getInt("process.poller.interval");
		return DateTime.now(DateTimeZone.UTC).minusMinutes(pollerInterval);
	}
	
	
	/**
	 * Returns the asset (javascript, image, stylesheet, etc.) for the application
	 */
	public static String getAsset(String path) {
		return Play.application().configuration().getString("application.URL.assets") + path;
		// return controllers.routes.Assets.at("images/nodes/asana.png").absoluteURL(Controller.request());
	}
	
}
