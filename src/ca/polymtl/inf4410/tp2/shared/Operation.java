package ca.polymtl.inf4410.tp2.shared;

public class Operation
{
	static public enum OperationType
	{
		PRIME, FIB
	}
	
	private int mValue;
	private OperationType mType;
	
	public Operation(int value, OperationType type)
	{
		mValue = value;
		mType = type;
	}
	
	public int getValue() 
	{
		return mValue;
	}
	
	public OperationType getType()
	{
		return mType;
	}
}
