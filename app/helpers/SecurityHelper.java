package helpers;

import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import play.Play;

public class SecurityHelper {

	private static final String COMPONENT_NAME = "Utility Helper";	
	private static final String CIPHER_ALGO = "AES";
	private static final String KEY = "5f8848e7ceb84a8b91c2f3e937d4135f";
	
	/**
	 * Encrypts and encodes string
	 */
	public static String generateHash(String salt, String string) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String data = salt.concat(string);
			for (int i=0 ; i<100 ; i++)
				md.update(data.getBytes(Play.application().configuration().getString("application.encoding")));
			return Base64.encodeBase64URLSafeString(md.digest());
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "generateHash()", "Error while generating hash string", exception);
			return null;
		}
	}
	

	/**
	 * Encrypts a string
	 */
	public static String encrypt(String string) {
		try {
			Key key = new SecretKeySpec(KEY.getBytes(Play.application().configuration().getString("application.encoding")), CIPHER_ALGO);
			Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return Base64.encodeBase64URLSafeString(
					cipher.doFinal(string.getBytes(Play.application().configuration().getString("application.encoding"))));
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "encrypt()", "Error while encrypting string", exception);
			return null;
		}
	}


	/**
	 * Decrypts a string
	 */
	public static String decrypt(String encryptedString) {
		try {
			Key key = new SecretKeySpec(KEY.getBytes(Play.application().configuration().getString("application.encoding")), CIPHER_ALGO);
			Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] bytes = cipher.doFinal(Base64.decodeBase64(encryptedString));
			return new String(bytes);
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "encrypt()", "Error while decrypting string", exception);
			return null;
		}
	}
}
