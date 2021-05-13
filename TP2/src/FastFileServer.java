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

public class FastFileServer{
  public final static int port = 8888;
  public static String serverIP;
  public static String myIP;
  
  public static void main(String[] args) throws IOException{
    try{
      

		myIP=myip();
		System.out.println("My ip: "+ myIP);


		DatagramSocket ffsSocket = new DatagramSocket();

		BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("Server ip: ");
		serverIP = systemIn.readLine();
		System.out.println();

		InetAddress ipAddress = InetAddress.getByName(serverIP);

		byte[] sendingDataBuffer = new byte[1024];
		byte[] receivingDataBuffer = new byte[1024];

		String userInput;
		userInput = systemIn.readLine();

		PacketUDP p1 = new PacketUDP(1,0,0,userInput.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
   		ObjectOutputStream os = new ObjectOutputStream(out);		
   		os.writeObject(p1);
		sendingDataBuffer = out.toByteArray();

		DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
		ffsSocket.send(sendingPacket);

		System.out.println("Package sent");



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
