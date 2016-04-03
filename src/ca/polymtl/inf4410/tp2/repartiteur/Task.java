package ca.polymtl.inf4410.tp2.repartiteur;

import ca.polymtl.inf4410.tp2.shared.*;

import java.util.ArrayList;
import java.util.List;

public class Task 
{
	public List<Operation> mOperations;
	public int mId;
	public List<CalculateurThread> mUnfitThreads;
	
	public Task(List<Operation> operations, int id)
	{
		mOperations = operations;
		mId = id;
		
		// the starting list of unfitting threads is always empty
		mUnfitThreads = new ArrayList<>();
	}
}
