package ca.polymtl.inf4410.tp2.repartiteur;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

import ca.polymtl.inf4410.tp2.shared.*;

class CalculateurThread extends Observable implements Runnable
{
	private TaskStatus status;
	
        private ServerInterface server; // The reference to the remote server on which the task will be done
	private List<Operation> operations; // The said task
	private int identifiant;  // Identifier provided by the main thread
	private Map parentUnexecutedTasks;
	private AtomicInteger resultRef;
        private static final Boolean SHOW_DEBUG_INFO = true;
        private String myHeader;
	
        public CalculateurThread(ServerInterface serv,
			Map unexecutedTasksToThreads, List<Operation> ops, int i,
			AtomicInteger result) {
		operations = ops;
		server = serv;
		identifiant = i;
		parentUnexecutedTasks = unexecutedTasksToThreads;
		resultRef = result;
		myHeader = "Thread["+identifiant+"] :";
	}

	public int getIdentifiant()
	{
		return identifiant;
	}
	
	@Override
	public void run()
	{

	    if (SHOW_DEBUG_INFO) {
		System.out.println(myHeader + "I have been created !");
	    }
		status = TaskStatus.SUBMITTED;
		int res = -1;		
	
		// TODO: Should check the concurrency on the list
		// Save the list
		List<Integer> currentMapStatus = (List<Integer>) parentUnexecutedTasks.get(operations);
		// Remove task from map
		synchronized(parentUnexecutedTasks){
			parentUnexecutedTasks.remove(operations);
		}
		
	    try {
	    	status = TaskStatus.WORKING;
	    	res = server.executeTask(operations);
		if (SHOW_DEBUG_INFO)
		    System.out.println(myHeader + " result from RMI call is " + res);
		status = TaskStatus.DONE;
	    } catch (RemoteException e) {
	    	// Do something clever like throwing a message to main thread
	    	System.out.println(e.getMessage());
	    	status = TaskStatus.REJECTED_LOAD;
	    } catch (NullPointerException npe) {
		if (server == null)
		    System.out.println("The server stub you tried to reach is not defined");
		else
		    System.out.println(npe.getMessage());
	    }
	    
	    if (status == TaskStatus.REJECTED_LOAD) {
		if (SHOW_DEBUG_INFO)
		    System.out.println(myHeader + " Task was rejected");
	    	// Put back the task into the map
	    	currentMapStatus.add(identifiant);
	    	synchronized (parentUnexecutedTasks) {
	    		parentUnexecutedTasks.put(operations, currentMapStatus);
			}
	    	
	    } else if (status == TaskStatus.DONE) {
		if (SHOW_DEBUG_INFO)
		    System.out.println(myHeader + " Adding " + res + "to current res(" + resultRef.get() + ") and applying % 5000");
	    	// Shouldn't do something || Notify the result ?
	    	resultRef.getAndAdd(res % 5000);
	    }
	    
	    // TODO Tell main thread of the result
	    //setChanged();
	    //notifyObservers(res);
	}
	
	
}
