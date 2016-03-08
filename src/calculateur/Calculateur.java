package calculateur;

import java.rmi.RemoteException;
import java.util.Random;
import shared.*;

public class Calculateur implements ServerInterface {
	private int tauxMalicieux;
	
	public Calculateur(int txMalicieux) {
		tauxMalicieux = txMalicieux;
	}
	
	private boolean isMalicious() {
		Random r = new Random();
		if(r.nextInt(100) > tauxMalicieux)
			return true;
		return false;
	}
	
	public int fib(int x) throws RemoteException {
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

	public int prime(int x) throws RemoteException {
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
