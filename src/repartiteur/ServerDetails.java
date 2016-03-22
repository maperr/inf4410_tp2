package repartiteur;

public class ServerDetails {
	public int port;
	public String ip_address;
	public int tauxMalicieux;
	public int nbOperationsMax;
	
	
	public ServerDetails(String s, int x, int y, int z) 
	{
		ip_address = s;
		port = x;
		tauxMalicieux = y;
		nbOperationsMax = z;
	}
}
