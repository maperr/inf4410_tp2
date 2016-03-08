package repartiteur;

import repartiteur.Operation;
import repartiteur.OperationType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class Repartiteur {
	
	private Queue<Operation> operations;
	
	private ArrayList<ServerDetails> calculateurs; 
	
	/*
	 * utilisation:
	 *		Repatiteur nomFichierOperations nomFichierServerDetails
	 */
	public static void main(String[] args) {
		if (args.length <= 1) 
		{
			System.out.println("Please use an argument");
		}
		else if (args.length == 2)
		{
			if (Files.exists(getFilePath(args[0])) && Files.exists(getFilePath(args[1])))
			{
				Repartiteur r = new Repartiteur(args[0], args[1]);
				r.run();
			}
			else 
			{
				throw new IllegalArgumentException("File not found");
			}
		}
		else
		{
		 	throw new IllegalArgumentException("Too many arguments");
		}
	}
	
	private static Path getFilePath(String fileName) 
	{
		String rc = "./" + fileName;
		return Paths.get(rc);
	}
	
	public Repartiteur(String operationsFile, String serverFile) 
	{
		File opFile = Repartiteur.getFilePath(operationsFile).toFile();
		operations = getOperationsFromFile(opFile);
		
		File sFile = Repartiteur.getFilePath(serverFile).toFile();
		calculateurs = getServerDetailsFromFile(sFile);
	}
	
	public void run() 
	{
		// thread pour chaque serveur
		// check 50% + 1 si tous d'accord (majorite) -> continue sinon envoie a un autre
		// creer fichier configuration au lieu hardcode
	}
	
	private ArrayList<ServerDetails> getServerDetailsFromFile(File f)
	{
		ArrayList<ServerDetails> servers = new ArrayList<ServerDetails>();
		try (BufferedReader br = new BufferedReader(new FileReader(f))) 
		{
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		    	String[] splited = line.split(" ");
		    	servers.add(new ServerDetails(splited[0], Integer.parseInt(splited[1])));
		    }
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unable to read file properly");
		} 
		return servers;
	}
	
	private Queue<Operation> getOperationsFromFile(File f) 
	{
		Queue<Operation> ops = new ArrayDeque<Operation>();
		try (BufferedReader br = new BufferedReader(new FileReader(f))) 
		{
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		    	String[] splited = line.split(" ");
		    	OperationType type;
		    	if(splited[0].equals("fib")) 
		    	{
		    		type = OperationType.FIB;
		    	} 
		    	else if (splited[0].equals("prime")) 
		    	{
		    		type = OperationType.PRIME;
		    	} 
		    	else
		    	{
		    		throw new IllegalArgumentException("File not correctly formatted");
		    	}
		    	
		    	ops.add(new Operation(type,Integer.parseInt(splited[1])));
		    }
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unable to read file properly");
		} 
		return ops;
	}
}
