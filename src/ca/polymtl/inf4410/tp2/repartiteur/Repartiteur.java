package ca.polymtl.inf4410.tp2.repartiteur;

import ca.polymtl.inf4410.tp2.shared.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repartiteur implements Observer{
	
	private List<Operation> operations;
	private ArrayList<ServerDetails> serversDetails; 
	private ArrayList<ServerInterface> calculateurs;
	private Boolean isModeSecurise;
	private List<CalculateurThread> calcThreads;
	
	protected Map unexecutedTasksToThreads;
	
	private AtomicInteger result; // used asynchronously, use mutexes.
	
	/*
	 * utilisation:
	 *		Repartiteur nomFichierOperations nomFichierServerDetails modeSecurise(true/false)
	 */
	public static void main(String[] args) {
		if (args.length <= 2) 
		{
			System.out.println("Invalid number of arguments");
		}
		else if (args.length == 3)
		{
			if (Files.exists(getFilePath(args[0])) && Files.exists(getFilePath(args[1])))
			{
				if(args[2].equals("true") || args[2].equals("false")) {
					Repartiteur r = new Repartiteur(args[0], args[1], args[2]);
					r.run();
				}
				else
				{
					throw new IllegalArgumentException("Invalid operation mode");
				}
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
	
	public Repartiteur(String operationsFile, String serverFile, String modeSecurise) 
	{
	        this.calculateurs = new ArrayList<>();
	        this.calcThreads = new ArrayList<>();
	    
		File opFile = Repartiteur.getFilePath(operationsFile).toFile();
		operations = getOperationsFromFile(opFile);
		
		File sFile = Repartiteur.getFilePath(serverFile).toFile();
		serversDetails = getServerDetailsFromFile(sFile);
		
		isModeSecurise = Boolean.valueOf(modeSecurise);
		result = new AtomicInteger();
	}
	
	public void run() // throws RemoteException 
	{
		// thread pour chaque serveur
		// check 50% + 1 si tous d'accord (majorite) -> continue sinon envoie a un autre
		// creer fichier configuration au lieu hardcode
		
		// instancier les servers
		if(System.getSecurityManager() == null) 
		{
			System.setSecurityManager(new SecurityManager());	
		}
		
		for(ServerDetails si : serversDetails)
		{
			calculateurs.add(loadServerStub(si.ip_address));
		}

		try {
		    String res = calculateurs.get(0).echo("YOYOYO");
		    System.out.println(res);
		} catch (RemoteException e) {
		    System.out.println(e.getMessage());
		} catch (NullPointerException ne) {
		    System.out.println(ne.getMessage());
		}
		result.set(0);
		
		if(isModeSecurise) 
		{
			// split the operations in different tasks (group of operations) to be executed on threads
			List<List<Operation>> list_operations = splitList(operations, calculateurs.size());
			
			// Initialize and fill the atomic hashmap <task, threadId> where threadId is the index of the threads 
			// that tried running said task.
			unexecutedTasksToThreads = Collections.synchronizedMap(new HashMap<List<Operation>, List<Integer>>());
			for(List<Operation> tache : list_operations) 
			{
				unexecutedTasksToThreads.put(tache, new ArrayList<Integer>());
			}
			
			// When a thread executes a task, remove it from the hashmap. If it successfully finished said task,
			// it doesn't add it back to it.
			for(int i = 0; i < calculateurs.size(); i++) 
			{
			    calcThreads.add(new CalculateurThread(calculateurs.get(i), unexecutedTasksToThreads, list_operations.get(i), i, result));
			}
			
			// Wait for child to finish ?
			System.out.println("Result is :" + result.get());
		}
		else 
		{
			
		}
		
	}
	
	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
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
			System.out.println(splited[0] + " : " + splited[1]);
		    	servers.add(new ServerDetails(splited[0], Integer.parseInt(splited[1])));
		    }
		}
		catch (IOException e) 
		{
			throw new IllegalArgumentException("Unable to read file properly");
		} 
		return servers;
	}
	
	private List<Operation> getOperationsFromFile(File f) 
	{
		ArrayList<Operation> ops = new ArrayList<Operation>();
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
	
	private List<List<Operation>> splitList(List<Operation> list, int nbCalculateurs) {
		int nbLists = list.size() / nbCalculateurs;
	    List<List<Operation>> parts = new ArrayList<List<Operation>>();
	    for (int i = 0; i < list.size(); i += nbLists) 
	    {
	        parts.add(new ArrayList<Operation>(list.subList(i, Math.min(list.size(), i + nbLists))));
	    }
	    return parts;
	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	
}
