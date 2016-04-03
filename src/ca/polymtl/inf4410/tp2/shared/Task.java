package ca.polymtl.inf4410.tp2.shared;

import java.util.List;

import ca.polymtl.inf4410.tp2.shared.Operation.OperationType;

public class Task 
{
	static public enum TaskStatus
	{
		SUBMITTED, WORKING, DONE, REJECTED 
	}
	
	private List<Operation> mOperations;
	private int mId;
	private TaskStatus mStatus;
	
	public Task(List<Operation> operations, int id, TaskStatus status)
	{
		mOperations = operations;
		mId = id;
		mStatus = status;
	}
	
	public List<Operation> getOperations()
	{
		return mOperations;
	}
	
	public int getId()
	{
		return mId;
	}
	
	public TaskStatus getStatus()
	{
		return mStatus;
	}
	
	// prints the operations associated with a task, used for debugging
	public void printTask()
	{
	    System.out.println("Task " + mId + ":");
	    for (int j = 0; j < mOperations.size() ; j++) 
	    {
	    	System.out.println("\t " + (mOperations.get(j).getType() == OperationType.FIB ? "Fib " : "Prime ") + mOperations.get(j).getValue());
	    }
	}
}
