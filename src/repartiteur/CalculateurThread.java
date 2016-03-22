package repartiteur;

import java.util.List;

import shared.Operation;
import shared.OperationType;

class CalculateurThread implements Runnable 
{
	private List<Operation> operations;
	private int identifiant;
	
	public CalculateurThread(List<Operation> ops, int id)
	{
		operations = ops;
		identifiant = id;
	}
	
	public int getIdentifiant()
	{
		return identifiant;
	}
	
	@Override
    public void run() 
	{
		List<Operation> ops = list_operations.get(0);
		
		if(op == null)
		{
			break;
		}
		else
		{
			if(op.type == OperationType.FIB) 
			{
				calculateurs.get(i).fib(op.value);
			}
			else 
			{
				calculateurs.get(i).prime(op.value);
			}
		}
		
		if (i + 1 == serversDetails.size())
		{
			i = -1;
		}
	}
}