package ca.polymtl.inf4410.tp2.calculateur;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.polymtl.inf4410.tp2.repartiteur.Repartiteur;
import ca.polymtl.inf4410.tp2.repartiteur.ServerDetails;
import ca.polymtl.inf4410.tp2.shared.*;

public class Calculateur implements CalculateurServer 
{
	private int tauxMalicieux;
	private int nbOperationsMax;
	private int port;
	
	public Calculateur(int txMalicieux, int nbOpMax, int p) 
	{
		tauxMalicieux = txMalicieux;
		nbOperationsMax = nbOpMax;
		port = p;
		
	}
	
	// utilisation:
	//		./CalculateurServerImpl tauxMalicieux(0-100) nbOperationsMaximum portDuRegistry
	public static void main(String[] args) throws RemoteException, MalformedURLException
	{
		if (args.length != 3)
			throw new IllegalArgumentException("Invalid number of argument");
		
		Calculateur server;
		int port;
		int nbOpMax;
		int txMalicieux;
		// parse arguments
		try 
		{  
	         txMalicieux = Integer.parseInt(args[0]);  
	         nbOpMax = Integer.parseInt(args[1]);  
	         port = Integer.parseInt(args[2]);
	         server = new Calculateur(txMalicieux, nbOpMax, port);
	    } 
		catch (NumberFormatException e) 
		{  
	    	  throw new IllegalArgumentException("Invalid argument format");
	    }  
		
		// use the default, restrictive security manager
		System.setSecurityManager(new SecurityManager());
		Registry registry = LocateRegistry.getRegistry(port);
		CalculateurServer stub = (CalculateurServer) UnicastRemoteObject.exportObject(server, 0);
		registry.rebind("CalculateurServer", stub);
		System.out.println("Server ready to receive tasks.");
		return;
	}
	
	@Override
	public int compute(Task task) throws RemoteException 
	{
		if(isRefused(task.getOperations().size())) 
		{
			throw new RemoteException("Task's number of operations exceeds server capacity");
		}
		
		if(isMalicious()) 
		{
			Random rand = new Random();
			return rand.nextInt((4999) + 1);
		}
		
		int sum = 0;
		
		for(Operation op : task.getOperations())
		{
			int current;
			
			if (op.getType() == Operation.OperationType.FIB) 
			{
			    System.out.println("Doing fib("+op.getValue()+")");
				current = Operations.fib(op.getValue());
			} 
			else 
			{
				System.out.println("Doing prime("+op.getValue()+")");
				current = Operations.prime(op.getValue());
			}
			
			sum = (sum + current % 5000) % 5000;
		}
		
		System.out.println("My task consisting of " + task.getOperations().size() + " tasks returns " + sum);
		
		return sum;
	}
	
	private boolean isMalicious() 
	{
		Random r = new Random();
		
		if(r.nextInt(100) + 1 < tauxMalicieux)
		{
			return true;
		}
		return false;
	}
	
	private boolean isRefused(int nbOps)
	{
		if (nbOps > nbOperationsMax)
		{
			Random rand = new Random();
			int r = rand.nextInt(100);
			
			double t = (( nbOps - nbOperationsMax ) / ( 9 * nbOperationsMax ) * 100 );
			if (t > r) 
			{
				return false;
			}
			else 
			{
				return true;
			}
		} 
		else 
		{
			return false;
		}
	}
}
