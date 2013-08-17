package helpers;

import java.util.UUID;

public class Helper {
	
	public static String getUniqueId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	
	public static Boolean isEmptyString(String string) {
		if (string==null || string.trim().equalsIgnoreCase(""))
			return true;
		else
			return false;
	}

}
