package ca.polymtl.inf4410.tp2.shared;

import java.rmi.*;

public interface CalculateurServer extends Remote 
{
	int compute(Task task) throws RemoteException;
}
