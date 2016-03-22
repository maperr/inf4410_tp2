package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {
	int executeTask(List<Operation> x) throws RemoteException;
}
