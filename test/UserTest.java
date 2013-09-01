

import helpers.UtilityHelper;

import java.util.Calendar;
import java.util.Iterator;

import models.ServiceAccessToken;
import models.User;
import models.UserServiceNode;
import nodes.asana.Asana;
import play.test.FakeApplication;
import play.test.Helpers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserTest {
	
	static FakeApplication fakeApplication = null;
	String testUserId = "22382e5eca9b4471a55e7daa51a386ba";
	
	@BeforeClass
	public static void startApplication() {
		fakeApplication = Helpers.fakeApplication();
		Helpers.start(fakeApplication);
		
		resetDatabase();
	}
	

	private static void resetDatabase() {
		Iterator<Object> iterator = User.find.findIds().iterator();
		while(iterator.hasNext())
			User.find.ref((String)iterator.next()).delete();
		
		iterator = ServiceAccessToken.find.findIds().iterator();
		while(iterator.hasNext())
			ServiceAccessToken.find.ref((UserServiceNode) iterator.next()).delete();
	}
	
	
	@Test
	public void createUser() {
		String uniqueId = UtilityHelper.getUniqueId();
		String userName = "Deepak Nayal";
		User user = new User(uniqueId, userName, "dnayal@gmail.com", Calendar.getInstance().getTime());
		user.save();
		Assert.assertEquals("User Created", userName, User.find.byId(uniqueId).getName());
	}

	
	@Test
	public void createServiceAccessToken() {
		UserServiceNode userNode = new UserServiceNode(testUserId, new Asana().getNodeId());
		ServiceAccessToken serviceToken = new ServiceAccessToken(userNode, "accessToken", "refreshToken",
				Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
		serviceToken.save();
	}
	
	
	@AfterClass
	public static void stopApplication() {
		Helpers.stop(fakeApplication);
	}
	
}
