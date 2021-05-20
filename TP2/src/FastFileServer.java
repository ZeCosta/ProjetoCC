//	Create handlers? -> recives packet and creates thread to hadle it
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.NetworkInterface;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.StringBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FastFileServer{
  public static int port = 8888;
  public static String serverIP;
  public static String myIP;

  public static String password;
  //public static String passKey;
  
  public static void main(String[] args) throws IOException{
    try{
		myIP=myip();
		System.out.println("My ip:   "+ myIP);
		System.out.println("My port: "+ port);


		DatagramSocket ffsSocket = new DatagramSocket();

		BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("Server ip: ");
		serverIP = systemIn.readLine();
		System.out.println();

		InetAddress ipAddress = InetAddress.getByName(serverIP);

		byte[] sendingDataBuffer = new byte[1024];
		byte[] receivingDataBuffer = new byte[1024];


		StringBuilder sb = new StringBuilder();
		sb.append("Subscribe");
		sb.append(myIP);
		//sb.append(":").append(port);
		password = new String(sb);
		//System.out.println(password);
		
		//hash!!!
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(password.getBytes());


		PacketUDP p1 = new PacketUDP(1,0,0,hash);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
   		ObjectOutputStream os = new ObjectOutputStream(out);		
   		os.writeObject(p1);
		sendingDataBuffer = out.toByteArray();

		DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
		ffsSocket.send(sendingPacket);

		System.out.println("Package Subscribe Sent");

		/*
			//tests for byte arrray length
			p1 = new PacketUDP(50,30,0,new byte[400]);
			out = new ByteArrayOutputStream();
	   		os = new ObjectOutputStream(out);		
	   		os.writeObject(p1);
			sendingDataBuffer = out.toByteArray();
			System.out.println(sendingDataBuffer + " -> " +sendingDataBuffer.length);
		*/

		Boolean running = true;
		while (running) {
			DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer,receivingDataBuffer.length);
			ffsSocket.receive(receivingPacket);

			try{
				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivingPacket.getData()));
				PacketUDP p2 = (PacketUDP) in.readObject();
				in.close();

				String receivedData = new String(receivingPacket.getData());
				System.out.println("Sent from the server: "+receivedData);

				System.out.println(p2);
			    System.out.println(p2.getPackettype());
			    System.out.println(p2.getPacketid());
			    System.out.println(p2.getChunkid());
			    System.out.println(p2.getChunk());
			    System.out.println();

				String arr = new String(p2.getChunk());
				System.out.println(arr);

				if(receivingPacket.getAddress().equals(ipAddress)){
					if(p2.getPackettype()==3){
						System.out.println("Send filesize");
						int filesize=5;
						p2.setChunkid(filesize);
						
						out = new ByteArrayOutputStream();
				   		os = new ObjectOutputStream(out);		
				   		os.writeObject(p2);
						sendingDataBuffer = out.toByteArray();

						sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
						ffsSocket.send(sendingPacket);
						System.out.println("Package Sent");	
					}else if(p2.getPackettype()==4){
						System.out.println("Send chunk");
					}else System.out.println("Ignoring packet!");

				}else System.out.println("Not the HttpGw");
			}catch(Exception e){
				System.out.println(e);
			}
		}


		// Closing the socket connection with the server
		ffsSocket.close();
    }
    catch(SocketException e) {
      e.printStackTrace();
    }
    catch(NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public static String myip(){
      String ip = new String();
        try{
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    //System.out.println(iface.getDisplayName() + " " + ip);
                }
            }
            //System.out.println(ip);
            return ip;
            

        } catch (Exception e) {
            System.out.println("Erro");
        }
        return null;
    }
}
