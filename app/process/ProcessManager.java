package process;

import helpers.UtilityHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import models.Process;
import play.Play;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.RoundRobinRouter;


/**
 * This class that runs all processes created by users, 
 * and manages all concurrency issues of running multiple processes
 * 
 * Akka Getting Started - http://doc.akka.io/docs/akka/2.0.2/intro/getting-started-first-java.html
 */
@SuppressWarnings("serial")
public class ProcessManager {
	
	private static final String COMPONENT_NAME = "ProcessManager";	
	private final String TICK_MESSAGE = "TICK";
	private static ActorSystem system = null;

	
	// initiate the ActorSystem that will manage all Actors
	public ProcessManager() { 
		system = ActorSystem.create(COMPONENT_NAME);
	}
	
	
	/**
	 * Starts the scheduler that sends a tick message periodically
	 */
	public void startScheduler() {
		
		// the router which will manage the pool of actors
		// this router will be used to send messages to 
		// ProcessWorker actor everytime the tick happens
		final ActorRef router = system.actorOf(
				new Props(ProcessWorker.class)
				.withRouter(
					new RoundRobinRouter(Play.application().configuration().getInt("process.executor.threads"))), 
					"ProcessRouter"
			);

		// the actor that will respond to periodic ticks and 
		// will send the messages to the router
		ActorRef tickActor = system.actorOf(new Props().withCreator(
		  new UntypedActorFactory() {
		    public UntypedActor create() {
		      return new UntypedActor() {
				@Override
				public void onReceive(Object message) {
					// check if we got the expected message
					if (message.equals(TICK_MESSAGE)) {
						
						// get all the active pollable processes
						// hook processes are executed by the 
						// events of the specific nodes so we do 
						// not need to execute those
						List<Process> processes = Process.getAllActivePollableProcesses();

						// loop through all the processes and send the messages
						// the router will ensure that even if we have hundreds 
						// of processes, we are able to execute only as many processes
						//  as the number of actors in the router
						for (int i=0 ; i<processes.size() ; i++) {
							router.tell(new ProcessMessage(i, processes.get(i)), router);
						}
					} else {
						unhandled(message);
					}
				}
		      };
		    }
		  }));
		
		// set the scheduler, it will send periodic ticks
		system.scheduler().schedule(
				Duration.Zero(), 
				Duration.create(Play.application().configuration().getInt("process.poller.interval"), TimeUnit.MINUTES), 
				tickActor, 
				TICK_MESSAGE,
				system.dispatcher());

		UtilityHelper.logMessage(COMPONENT_NAME, "startScheduler()", "Process Manager Scheduler Started");
	}
	
	
	/**
	 * Stops the scheduler
	 */
	public void stopScheduler() {
		system.shutdown();

		UtilityHelper.logMessage(COMPONENT_NAME, "stopScheduler()", "Process Manager Scheduler Stopped");
	}
	

	/**
	 * This method is here just to enable manually 
	 * triggering the processes from the Admin interface
	 */
	public void runProcesses() {
		List<Process> processes = Process.getAllActivePollableProcesses();

		for(Process process : processes) {
			ProcessExecutor.executePollProcess(process); 
		}
	}
}
