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
    
	private ServerInterface mServer; // reference to the remote server on which the task will be done
	private Task mTask; // task to be executed
	private int mIdentifier;  // identifier provided by the repartiteur
	private AtomicInteger mResultRef;
	
    public CalculateurThread(ServerInterface serv,
			int id,
			AtomicInteger result) 
    {
		mServer = serv;
		mIdentifier = id;
		mResultRef = result;
		mTask = null;
	}
    
    public void launchTask(Task t)
    {
    	mTask = t;
    }
    
    public boolean isBusy()
    {
    	return mTask == null ? true : false;
    }
    
	@Override
	public void run()
	{
		// infinite loop for the thread
		while(true)
		{
			// thread waits until a task becomes available
			try {
				mTask.wait();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			if (SHOW_DEBUG_INFO) 
			{
				displayDebugInfo("I have been started work on task #" + mTask.mId + " of " + mTask.mOperations.size() + " operations");
		    }
			
			int res = -1;		
			
			try 
			{
				mTask.mStatus = Task.TaskStatus.RUNNING;
				res = mServer.executeTask(mTask.mOperations);
				mTask.mStatus = Task.TaskStatus.DONE;
			} 
			catch (RemoteException e) 
			{
				// do something clever like throwing a message to main thread
				mTask.mStatus = Task.TaskStatus.REJECTED;
				displayDebugInfo(e.getMessage());
			} 
			catch (NullPointerException npe) 
			{
				mTask.mStatus = Task.TaskStatus.REJECTED;
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
			if (mTask.mStatus == Task.TaskStatus.DONE) 
			{	
				mResultRef.getAndAdd(res % 5000);
				mResultRef.set(mResultRef.get() % 5000);
				
				if (SHOW_DEBUG_INFO)
				{
					displayDebugInfo("Adding " + res + " to result and applying % 5000, new current result is " + mResultRef.get());
				}			
			}
			else if (mTask.mStatus == Task.TaskStatus.REJECTED)  // the calculateur refused the task, do not add the result to the sum
			{
				if (SHOW_DEBUG_INFO)
				{
					displayDebugInfo("Task was rejected, adding " + mIdentifier + " to list of calculateurs that failed the task");
				}
			} 

			mTask = null;
			mResultRef.notify();
		}
	}
	
	// used to identify thread while printing debug info
	private void displayDebugInfo(String message) 
	{
		System.out.println("Message from calculateur " + mIdentifier + ": " + message);
	}
}
