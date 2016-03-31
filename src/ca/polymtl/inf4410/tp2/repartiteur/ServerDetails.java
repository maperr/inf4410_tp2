package ca.polymtl.inf4410.tp2.repartiteur;

public class ServerDetails {
	public int port;
	public String ip_address;
	
	public ServerDetails(String s, int x) 
	{
		ip_address = s;
		port = x;
	}
}
