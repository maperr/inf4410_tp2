package ca.polymtl.inf4410.tp2.shared;


public class Operation implements java.io.Serializable
{
    public OperationType type;
    public int value;

	public Operation(OperationType t, int x)
	{
		type = t;
		value = x;
	}

}
