package ca.polymtl.inf4410.tp2.repartiteur;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

import ca.polymtl.inf4410.tp2.repartiteur.Task.TaskStatus;
import ca.polymtl.inf4410.tp2.shared.*;

class CalculateurThread extends Thread
{
    private static final Boolean SHOW_DEBUG_INFO = true;
    
	public static enum CalculateurStatus
	{
		WAITING, RUNNING, DONE, BREAKDOWN
	}
    
	private ServerInterface mServer; // reference to the remote server on which the task will be done
	private AtomicInteger mResultRef;
	private CalculateurStatus mStatus;
	private int mIdentifier;
	public Task mTask; // task to be executed
	
    public CalculateurThread(int id, ServerInterface serv, Task t) 
    {
    	mIdentifier = id;
		mServer = serv;
		// initialize the CalculateurThread as waiting for a task
		mStatus = CalculateurStatus.WAITING;
		mTask = t;
	}
    
    public CalculateurStatus getStatus()
    {
    	return mStatus;
    }
    
    public void setStatus(CalculateurStatus st)
    {
    	mStatus = st;
    }
    
    public void setResult(AtomicInteger result)
    {
		mResultRef = result;
    }
    
    public Task getTask()
    {
    	return mTask;
    }
    
    public void launchTask(Task t) 
    {
    	if (SHOW_DEBUG_INFO) 
		{
			displayDebugInfo("Associated with task " + t.mId);
	    }
		mTask = t;
		mTask.mStatus = TaskStatus.RUNNING;
		mTask.notify();
    }
    
    /*public boolean isOutOfOrder() // TODO (need to check if the calculateur is dead)
    {
    		if (SHOW_DEBUG_INFO) 
    		{
        		displayDebugInfo("out of order!");
    	    }
    		
    }*/
    
	@Override
	public void run()
	{
		// main thread loop, each loop executes a task on the server.
		while(true)
		{
			// wait to be notified of a new task to execute
			try {
				mTask.wait();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			mStatus = CalculateurStatus.RUNNING;
			
			if (SHOW_DEBUG_INFO) 
			{
				displayDebugInfo("I have been started work on task #" + mTask.mId + " of " + mTask.mOperations.size() + " operations");
		    }
			
			int res = -1;		
			
			try 
			{
				res = mServer.executeTask(mTask.mOperations);
				if (SHOW_DEBUG_INFO)
				{
					displayDebugInfo("Adding " + res + " to result and applying % 5000, new current result is " + mResultRef.get());
				}
				mResultRef.getAndAdd(res % 5000);
				mResultRef.set(mResultRef.get() % 5000);
				mTask.mStatus = TaskStatus.DONE;
			} 
			catch (RemoteException e) 
			{
				displayDebugInfo("Task was rejected, adding " + mIdentifier + " to list of calculateurs that failed the task");
				mTask.mStatus = TaskStatus.REJECTED;
				displayDebugInfo(e.getMessage());
			} 
			catch (NullPointerException npe) 
			{
				displayDebugInfo("Task was rejected, adding " + mIdentifier + " to list of calculateurs that failed the task");
				mTask.mStatus = TaskStatus.REJECTED;
				if (mServer == null)
				{
					displayDebugInfo("The server stub you tried to reach is not defined");
				}
				else
				{
					displayDebugInfo(npe.getMessage());
				}
			}

			mStatus = CalculateurStatus.DONE;
			
			synchronized(mResultRef)
			{
				mResultRef.notify();
			}
		}
	}
	
	// used to identify thread while printing debug info
	private void displayDebugInfo(String message) 
	{
		System.out.println("Message from calculateur " + mIdentifier + ": " + message);
	}
}
