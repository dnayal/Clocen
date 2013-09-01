package helpers;

import java.util.Calendar;
import java.util.UUID;

import play.Logger;

public class UtilityHelper {
	
	public static String getUniqueId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	
	public static Boolean isEmptyString(String string) {
		if (string==null || string.trim().equalsIgnoreCase(""))
			return true;
		else
			return false;
	}
	
	
	public static void logMessage(String component, String method, String message) {
		Logger.info("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message);
	}
	

	public static void logError(String component, String method, String message, Throwable error) {
		Logger.error("[" + Calendar.getInstance().getTime() + "] " + "["+ component +"] " + "[" + method + "] " + message, error);
	}
	
}
