
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
      serverIP = systemIn.readLine();
      InetAddress IPAddress = InetAddress.getByName(serverIP);
      
      byte[] sendingDataBuffer = new byte[1024];
      byte[] receivingDataBuffer = new byte[1024];
      
      String userInput;
      userInput = systemIn.readLine();

      sendingDataBuffer = userInput.getBytes();
      
      DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,IPAddress, port);
      
      ffsSocket.send(sendingPacket);
      
      DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer,receivingDataBuffer.length);
      ffsSocket.receive(receivingPacket);
      
      String receivedData = new String(receivingPacket.getData());
      System.out.println("Sent from the server: "+receivedData);
      
      String arr[] = receivedData.split(" ", 2);
      System.out.println(arr[0] + " " +arr[0].equals("end"));

      
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
