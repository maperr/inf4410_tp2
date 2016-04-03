package ca.polymtl.inf4410.tp2.calculateur;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.ConnectException;
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

public class Calculateur implements ServerInterface 
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
	//		./Calculateur txMalicieux nbOperationsMaximum portDuRegistry
	public static void main(String[] args)
	{
		if (args.length < 3) 
		{
			System.out.println("Invalid number of arguments");
		}
		else if (args.length == 3)
		{
			try 
			{  
		         int txMalicieux = Integer.parseInt(args[0]);  
		         int nbOpMax = Integer.parseInt(args[1]);  
		         int port = Integer.parseInt(args[2]);
		         Calculateur c = new Calculateur(txMalicieux, nbOpMax, port);
		         c.run();
		    } 
			catch (NumberFormatException e) 
			{  
		    	  throw new IllegalArgumentException("Invalid argument");
		    }  
		}
		else
		{
		 	throw new IllegalArgumentException("Too many arguments");
		}
	}
	
	public int executeTask(List<Operation> x) throws RemoteException 
	{
		if(isRefused(x.size())) 
		{
			throw new RemoteException("Task refused!");
		}
		
		if(isMalicious())
		{
			System.out.println("Malicious operation");
			Random rand = new Random();
			return rand.nextInt((4999) + 1);
		}
		
		int sum = 0;
		
		for(Operation op : x)
		{
			int current;
			//if (op.type == OperationType.FIB)
			if (op.type == 0) 
			{
			    System.out.println("Doing fib("+op.value+")");
				current = Operations.fib(op.value); 
			} 
			else 
			{
			    System.out.println("Doing prime("+op.value+")");
				current = Operations.prime(op.value);
			}
			// current = current % 5000;
			sum = (sum + current % 5000) % 5000;
		}

		System.out.println("My task consisting of " + x.size() + " tasks is equal to " + sum);
		return sum;
	}
	
	private void run() 
	{
		if (System.getSecurityManager() == null) 
		{
			System.setSecurityManager(new SecurityManager());
		}

		try 
		{
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry(this.port);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} 
		catch (ConnectException e) 
		{
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} 
		catch (Exception e) 
		{
			System.err.println("Erreur: " + e.getMessage());
		}
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
