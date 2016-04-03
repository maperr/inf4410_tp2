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
    private static final Boolean SHOW_DEBUG_INFO = true;
    
	private TaskStatus mStatus;
	private ServerInterface mServer; // reference to the remote server on which the task will be done
	private List<Operation> mOperations; // task to be executed
	private int mIdentifier;  // identifier provided by the repartiteur
	private Map mParentUnexecutedTasks;
	private AtomicInteger mResultRef;
	
    public CalculateurThread(ServerInterface serv,
			Map unexecutedTasksToThreads, 
			List<Operation> ops, 
			int id,
			AtomicInteger result) 
    {
    	mOperations = ops;
		mServer = serv;
		mIdentifier = id;
		mParentUnexecutedTasks = unexecutedTasksToThreads;
		mResultRef = result;
	}

	public int getIdentifier()
	{
		return mIdentifier;
	}
	
	@Override
	public void run()
	{
		if (SHOW_DEBUG_INFO) 
		{
			displayDebugInfo("I have been created with task of size " + mOperations.size() + " operations");
	    }
		
		mStatus = TaskStatus.SUBMITTED;
		int res = -1;		
		
		try 
		{
			mStatus = TaskStatus.WORKING;
			res = mServer.executeTask(mOperations);
			
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Result from RMI call is " + res);
			}
			
			mStatus = TaskStatus.DONE;
		} 
		catch (RemoteException e) 
		{
			// do something clever like throwing a message to main thread
			mStatus = TaskStatus.REJECTED_LOAD;
			displayDebugInfo(e.getMessage());
		} 
		catch (NullPointerException npe) 
		{
			mStatus = TaskStatus.REJECTED_LOAD;
			if (mServer == null)
			{
				displayDebugInfo("The server stub you tried to reach is not defined");
			}
			else
			{
				displayDebugInfo(npe.getMessage());
			}
		}
		
		// handle the result
		if (mStatus == TaskStatus.DONE) 
		{
			// remove task from map
			synchronized(mParentUnexecutedTasks)
			{
				mParentUnexecutedTasks.remove(mOperations);
			}
				
			mResultRef.getAndAdd(res % 5000);
			mResultRef.set(mResultRef.get() % 5000);
			
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Adding " + res + " to current res and applying % 5000, new result = " + mResultRef.get());
			}			
		}
		else if (mStatus == TaskStatus.REJECTED_LOAD)  // the calculateur refused the task, do not add the result to the sum
		{
			// add calculateur to task in map
			synchronized(mParentUnexecutedTasks)
			{
				List<Integer> calculateurs = (List<Integer>) mParentUnexecutedTasks.get(mOperations);
				calculateurs.add(mIdentifier);
			}
			
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Task was rejected, adding " + mIdentifier + " to list of calculateurs that failed the task");
			}
		} 
		
		// Shouldn't do something || Notify the result ?
		// TODO Tell main thread of the result
		//setChanged();
		//notifyObservers(res);
		return;
	}
	
	// used to identify thread while printing debug info
	private void displayDebugInfo(String message) 
	{
		System.out.println("Message from calculateur " + mIdentifier + ": " + message);
	}
}
