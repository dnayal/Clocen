
import play.Application;
import play.GlobalSettings;
import process.ProcessManager;


public class Global extends GlobalSettings {

	private static ProcessManager manager = new ProcessManager();

	
	@Override
	public void onStart(Application application) {
		manager.startScheduler();
	}
	

	@Override
	public void onStop(Application application) {
		manager.stopScheduler();
	}
	
}
