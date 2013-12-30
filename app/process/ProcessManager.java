package process;

import java.util.List;

import models.Process;

/**
 * This class that runs all processes created by users, 
 * and manages all concurrency issues of running multiple processes
 */
public class ProcessManager {
	
	public ProcessManager() { }
	
	
	/**
	 * This process gets all the active processes and then executes them one by one
	 */
	public void runProcesses() {
		List<Process> processes = Process.getAllActivePollableProcesses();
		
		for(Process process : processes) {
			ProcessExecutor.executeProcess(process); 
		}
	}

}
