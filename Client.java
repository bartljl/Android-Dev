package edu.buffalo.cse.cse486_586.simpledht;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable
{
  
	
	int targetport;
	String serverIpAddress;
	Data send_data = new Data();
	
	public Client(String server,int target,Data data)
	{
		this.serverIpAddress = server;
		this.targetport = target;
		this.send_data = data;
	}

	public void run() {
		// TODO Auto-generated method stub
		
		 Socket Clientsocket = null;
			
			try {
				
				InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
				Clientsocket = new Socket(serverAddr,targetport);
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				ObjectOutputStream out = new ObjectOutputStream(Clientsocket.getOutputStream());
				out.writeObject(send_data);
			
				out.close();
				Clientsocket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}
  
}
