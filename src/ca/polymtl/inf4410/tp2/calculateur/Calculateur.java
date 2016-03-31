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

public class Calculateur implements ServerInterface {
	private int tauxMalicieux;
	private int nbOperationsMax;
	private int port;
	
	public Calculateur(int txMalicieux, int nbOpMax, int p) {
		tauxMalicieux = txMalicieux;
		nbOperationsMax = nbOpMax;
		port = p;
		
	}
	
	// utilisation:
	//		./Calculateur txMalicieux nbOperationsMaximum
	public static void main(String[] args)
	{
		if (args.length < 3) 
		{
			System.out.println("Invalid number of arguments");
		}
		else if (args.length == 3)
		{
			try {  
		         int txMalicieux = Integer.parseInt(args[0]);  
		         int nbOpMax = Integer.parseInt(args[1]);  
		         int port = Integer.parseInt(args[2]);
		         Calculateur c = new Calculateur(txMalicieux, nbOpMax, port);
		         c.run();
		      } catch (NumberFormatException e) {  
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

		int sum = 0;
		
		for(Operation op : x)
		{
			int current;
			if (op.type == OperationType.FIB) 
			{
				current = fib(op.value); 
			} 
			else 
			{
				current = prime(op.value);
			}
			current = current % 5000;
			sum += current;
		}
		
		return sum;
	}
	
	private void run() 
	{
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry(this.port);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}
	
	private boolean isMalicious() {
		Random r = new Random();
		if(r.nextInt(100) > tauxMalicieux)
			return true;
		return false;
	}
	
	private boolean isRefused(int nbOps) {
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
	
	private int fib(int x) throws RemoteException {
		if (isMalicious()) {
			Random rand = new Random();
			return rand.nextInt((4999) + 1);
		}
		if (x == 0)
			return 0;
		if (x == 1)
			return 1;
		return fib(x - 1) + fib(x - 2);
	}

	private int prime(int x) throws RemoteException {
		if (isMalicious()) {
			Random rand = new Random();
			return rand.nextInt((4999) + 1);
		}
		
		int highestPrime = 0;
		
		for (int i = 1; i <= x; ++i)
		{
			if (isPrime(i) && x % i == 0 && i > highestPrime)
				highestPrime = i;
		}
		
		return highestPrime;
	}

	private boolean isPrime(int x) throws RemoteException {	
		boolean result;
		
		if (x <= 1)
			result = false;

		for (int i = 2; i < x; ++i)
		{
			if (x % i == 0)
				result = false;
		}
		
		result = true;
		
		if (isMalicious()) {
			result = !result;
		}
		
		return result;
	}

}
