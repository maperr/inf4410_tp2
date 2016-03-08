package repartiteur;

import repartiteur.OperationType;

public class Operation
{
	public OperationType type;
	public int value;

	public Operation(OperationType t, int x)
	{
		type = t;
		value = x;
	}
}