package ca.polymtl.inf4410.tp2.repartiteur;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import ca.polymtl.inf4410.tp2.shared.*;

class CalculateurThread extends Thread
{
    private static final Boolean SHOW_DEBUG_INFO = true;
    
	public static enum CalculateurStatus
	{
		WAITING, RUNNING, DONE, REJECTED, BREAKDOWN
	}
    
	private ServerInterface mServer; // reference to the remote server on which the task will be done
	private Task mTask; // task to be executed
	private int mIdentifier;  // identifier provided by the repartiteur
	private AtomicInteger mResultRef;
	private CalculateurStatus mStatus;
	
    public CalculateurThread(ServerInterface serv, int id) 
    {
		mServer = serv;
		mIdentifier = id;
		
		// initialize the CalculateurThread as waiting for a task
		mStatus = CalculateurStatus.WAITING;
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
    }
    
    public boolean isOutOfOrder()
    {
    	if(!this.isAlive() && mStatus == CalculateurStatus.RUNNING) {
    		mStatus = CalculateurStatus.BREAKDOWN;
    		mResultRef.notify();
    		return true;
    	}
    	return false;
    }
    
	@Override
	public void run()
	{
		mStatus = CalculateurStatus.RUNNING;
		
		if (SHOW_DEBUG_INFO) 
		{
			displayDebugInfo("I have been started work on task #" + mTask.mId + " of " + mTask.mOperations.size() + " operations");
	    }
		
		int res = -1;		
		
		try 
		{
			res = mServer.executeTask(mTask.mOperations);
			mStatus = CalculateurStatus.DONE;
		} 
		catch (RemoteException e) 
		{
			// do something clever like throwing a message to main thread
			mStatus = CalculateurStatus.REJECTED;
			displayDebugInfo(e.getMessage());
		} 
		catch (NullPointerException npe) 
		{
			mStatus = CalculateurStatus.REJECTED;
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
		if (mStatus == CalculateurStatus.DONE) 
		{	
			mResultRef.getAndAdd(res % 5000);
			mResultRef.set(mResultRef.get() % 5000);
			
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Adding " + res + " to result and applying % 5000, new current result is " + mResultRef.get());
			}			
		}
		else if (mStatus == CalculateurStatus.REJECTED)  // the calculateur refused the task, do not add the result to the sum
		{
			synchronized(mTask) {
				mTask.mUnfitThreads.add(this);
			}
			if (SHOW_DEBUG_INFO)
			{
				displayDebugInfo("Task was rejected, adding " + mIdentifier + " to list of calculateurs that failed the task");
			}
		} 

		synchronized(mResultRef)
		{
			mResultRef.notify();
		}
	}
	
	// used to identify thread while printing debug info
	private void displayDebugInfo(String message) 
	{
		System.out.println("Message from calculateur " + mIdentifier + ": " + message);
	}
}
