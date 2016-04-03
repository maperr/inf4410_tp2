package ca.polymtl.inf4410.tp2.repartiteur;

import ca.polymtl.inf4410.tp2.repartiteur.Task.TaskStatus;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Repartiteur
{

	private static final Boolean SHOW_DEBUG_INFO = true;
	
	private List<Operation> mOperations;
	private ArrayList<ServerDetails> mServersDetails; 
	private ArrayList<ServerInterface> mCalculateurs;
	private Boolean mIsModeSecurise;
    private List<CalculateurThread> mCalculateurThreads;
	protected List<Task> mTasks;

	private AtomicInteger mResult; // used asynchronously, use mutexes.
	
	/*
	 * utilisation:
	 *		Repartiteur nomFichierOperations nomFichierServerDetails modeSecurise(true/false)
	 */
	public static void main(String[] args) 
	{
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
        mCalculateurs = new ArrayList<>();
        mCalculateurThreads = new ArrayList<>();
		mResult = new AtomicInteger();
        
		File opFile = Repartiteur.getFilePath(operationsFile).toFile();
		mOperations = getOperationsFromFile(opFile);
		
		File sFile = Repartiteur.getFilePath(serverFile).toFile();
		mServersDetails = getServerDetailsFromFile(sFile);
		
		mIsModeSecurise = Boolean.valueOf(modeSecurise);
	}
	
	public void run()
	{
		// thread pour chaque serveur
		// check 50% + 1 si tous d'accord (majorite) -> continue sinon envoie a un autre
		// creer fichier configuration au lieu hardcode
		
		if(System.getSecurityManager() == null) 
		{
			System.setSecurityManager(new SecurityManager());	
		}
		
		// create the server stubs and add them to the list of available servers
		for(ServerDetails si : mServersDetails)
		{
		    try
		    {
		    	mCalculateurs.add(loadServerStub(si.ip_address, si.port));
		    } 
		    catch (NotBoundException e) // server not reachable
		    {
		    	System.out.println("Server stub at " + si.ip_address + ":" + si.port + " is unreachable, not adding it to the server list.");
		    }
		}
		
		if (mCalculateurs.isEmpty())
		{
			System.out.println("Unable to execute request, there are no servers available.");
			return;
		}
		
		if (SHOW_DEBUG_INFO) 
		{
		    System.out.println("We have " + mCalculateurs.size() + " servers.");
		}
		
		mResult.set(0);
		
		if(!mIsModeSecurise) 
		{
			// split the operations in different tasks (group of operations) to be executed on threads
			int nOperationByTask = (int) Math.ceil((double) mOperations.size() / (double) mCalculateurs.size());
			List<List<Operation>> list_operations = chunk(mOperations, nOperationByTask);
			
			if (SHOW_DEBUG_INFO)
			{
			    PrintTasksList(list_operations);
			}
			
			
			
			// for each server, initialize its associated thread
			for(int i = 0; i < mCalculateurs.size(); i++) 
			{
			    if (SHOW_DEBUG_INFO)
			    {
			    	System.out.println("Creating thread " + i);
			    }
			    
			    CalculateurThread ct = new CalculateurThread(mCalculateurs.get(i), i, mResult);
			    mCalculateurThreads.add(ct);
			}
			
			
			// initialize, fill and launch the tasks
			int it = 0;
			mTasks = new ArrayList<>();
			for(List<Operation> operations : list_operations) 
			{
				Task t = new Task(operations, it, TaskStatus.WAITING);
				mTasks.add(t);
				mCalculateurThreads.get(it).launchTask(t);
				mCalculateurThreads.get(it).start(); 
				it++;
			}

			try 
			{
			    while (!AllTasksFinished()) 
			    {
			    	LaunchTasksOnThreads();
			    	
			    	if(ImpossibleTask()) 
			    		return;
			    	
			    	synchronized(mResult)
			    	{
			    		mResult.wait();
			    	}
			    }
			} 
			catch (InterruptedException e) 
			{
			    System.out.println(e.getMessage());
			}
			
			System.out.println("Final result is " + mResult.get());
		}
		else 
		{
			// are we supposed to do something here?
		}
		
	}
	
	private Boolean AllTasksFinished()
	{
		for(Task t : mTasks)
		{
			if (t.mStatus != TaskStatus.DONE) 
				return false;
		}
		return true;
	}
	
	private Boolean ImpossibleTask()
	{
		for(Task t : mTasks)
		{
			if (t.mUnfitThreads.size() == mCalculateurThreads.size()) 
			{
				System.out.println("Task " + t.mId + " has too many operations to be executed on any of the servers.\n Exiting.");
				return true;
			}
		}
		return false;
	}
	
	private void LaunchTasksOnThreads()
	{
		for(Task t : mTasks) 
		{
			for(CalculateurThread ct : mCalculateurThreads)
			{
				if(!ct.isBusy() && !t.mUnfitThreads.contains(ct))
				{
					ct.launchTask(t);
				}
			}
		}
	}
	
	// prints the operations associated with a task, used for debugging
    private void PrintTasksList(List<List<Operation>> listOfOps) 
    {
		for (int i = 0; i < listOfOps.size() ; i++) 
		{
		    System.out.println("Task " + i + ":");
		    for (int j = 0; j < listOfOps.get(i).size() ; j++) 
		    {
		    	System.out.println("\t " + (listOfOps.get(i).get(j).type == 0 ? "Fib " : "Prime ") + listOfOps.get(i).get(j).value);
		    }
		}
    }

	private ServerInterface loadServerStub(String hostname, int port) throws NotBoundException {
		ServerInterface stub = null;

		try 
		{
			Registry registry = LocateRegistry.getRegistry(hostname, port);
			stub = (ServerInterface) registry.lookup("server");
		} 
		catch (NotBoundException e) 
		{
			// System.out.println("Erreur: Le nom '" + e.getMessage()
			//		+ "' n'est pas defini dans le registre.");
			throw new NotBoundException();
		} 
		catch (AccessException e) 
		{
		    //System.out.println("Erreur: " + e.getMessage());
			throw new NotBoundException();
		} 
		catch (RemoteException e) 
		{
		    //System.out.println("Erreur: " + e.getMessage());
			throw new NotBoundException();
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
		    	// OperationType type;
			int type;
			if(splited[0].equals("fib")) 
	    	{
				//type = OperationType.FIB;
		        type = 0;
	    	} 
	    	else if (splited[0].equals("prime")) 
	    	{
	    		// type = OperationType.PRIME;
		        type = 1;
	    	} 
	    	else
	    	{
	    		throw new IllegalArgumentException("File not correctly formatted");
	    	}
	    	
		    	ops.add(new Operation(type,Integer.parseInt(splited[1])));
		    }
		}
		catch (IOException e) 
		{
			throw new IllegalArgumentException("Unable to read file properly");
		} 
		
		return ops;
	}
	
	// source: http://stackoverflow.com/a/29111959
	private static <T> List<List<T>> chunk(List<T> input, int chunkSize) 
	{

        int inputSize = input.size();
        int chunkCount = (int) Math.ceil(inputSize / (double) chunkSize);

        Map<Integer, List<T>> map = new HashMap<>(chunkCount);
        List<List<T>> chunks = new ArrayList<>(chunkCount);

        for (int i = 0; i < inputSize; i++) 
        {

            map.computeIfAbsent(i / chunkSize, (ignore) -> 
            {

                List<T> chunk = new ArrayList<>();
                chunks.add(chunk);
                return chunk;

            }).add(input.get(i));
        }

        return chunks;
    }
    // Display a message, preceded by
    // the name of the current thread
    static void threadMessage(String message) 
    {
        String threadName = Thread.currentThread().getName();
        System.out.format("%s: %s%n", threadName, message);
    }
}
