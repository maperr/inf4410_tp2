package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
	int fib(int x) throws RemoteException;
	int prime(int x) throws RemoteException;
}
