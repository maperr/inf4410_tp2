package ca.polymtl.inf4410.tp2.repartiteur;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

import ca.polymtl.inf4410.tp2.shared.*;
import ca.polymtl.inf4410.tp2.shared.Task.TaskStatus;

class CalculateurThread extends Observable implements Runnable
{
    private static final Boolean SHOW_DEBUG_INFO = true;
    
	private Task mTask;
	private CalculateurServer mServer; // reference to the remote server on which the task will be done
	private Map mParentUnexecutedTasks;
	private AtomicInteger mResultRef;
	private int mIdentifier;
	
    public CalculateurThread(CalculateurServer serv, Map unexecutedTasksToThreads, Task task, AtomicInteger result, int id) 
    {
		mServer = serv;
		mParentUnexecutedTasks = unexecutedTasksToThreads;
		mResultRef = result;
		mTask = task;
		mIdentifier = id;
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
			displayDebugInfo("I have been created with task of size " + mTask.getOperations().size() + " operations");
	    }
		
		mTask.setStatus(TaskStatus.SUBMITTED);
		int res = -1;		
		
		try 
		{
			mTask.setStatus(TaskStatus.WORKING);
			res = mServer.compute(mTask);
			mTask.setStatus(TaskStatus.DONE);
		} 
		catch (RemoteException e) 
		{
			// do something clever like throwing a message to main thread
			mTask.setStatus(TaskStatus.REJECTED);
			displayDebugInfo(e.getMessage());
		} 
		catch (NullPointerException npe) 
		{
			mTask.setStatus(TaskStatus.REJECTED);
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
		if (mTask.getStatus() == TaskStatus.DONE) 
		{
			// remove task from map
			synchronized(mParentUnexecutedTasks)
			{
				mParentUnexecutedTasks.remove(mTask);
			}
			
			mResultRef.getAndAdd(res % 5000);
			mResultRef.set(mResultRef.get() % 5000);
			
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Adding " + res + " to result and applying % 5000, new current result is " + mResultRef.get());
			}			
		}
		else if (mTask.getStatus() == TaskStatus.REJECTED)  // the calculateur refused the task, do not add the result to the sum
		{
			// add calculateur to task in map
			synchronized(mParentUnexecutedTasks)
			{
				List<Integer> calculateurs = (List<Integer>) mParentUnexecutedTasks.get(mTask);
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
