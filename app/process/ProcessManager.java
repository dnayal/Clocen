package process;

import java.util.List;

import play.Logger;
import models.Process;

/**
 * This class that runs all processes created by users, 
 * and manages all concurrency issues of running multiple processes
 */
public class ProcessManager {
	
	private static final String COMPONENT_NAME = "ProcessManager";

	
	public ProcessManager() { }
	
	
	/**
	 * This process gets all the active processes and then executes them one by one
	 */
	public void runProcesses() {
		List<Process> processes = Process.getAllActiveProcesses();
		
		for(Process process : processes) {
			Logger.info("############");
			ProcessExecutor.executeProcess(process); 
			Logger.info("############");
		}
	}

}
