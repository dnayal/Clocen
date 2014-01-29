package process;

import models.Process;
import akka.actor.UntypedActor;

/**
 * The Worker class/actor that executes the 
 * process when it receives the message
 */
public class ProcessWorker extends UntypedActor {

	@Override
	public void onReceive(Object object) throws Exception{
		ProcessMessage message = (ProcessMessage) object;
		Process process = message.getProcess();
		ProcessExecutor.executePollProcess(process);
	}

}
