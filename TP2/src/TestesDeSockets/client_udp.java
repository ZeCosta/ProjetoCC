
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class client_udp{
  /* The server port to which 
  the client socket is going to connect */
  public final static int SERVICE_PORT = 8888;
  
  public static void main(String[] args) throws IOException{
    try{
      DatagramSocket clientSocket = new DatagramSocket();
      

      InetAddress IPAddress = InetAddress.getByName("localhost");
      
      byte[] sendingDataBuffer = new byte[1024];
      byte[] receivingDataBuffer = new byte[1024];
      
      String userInput;
      BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));
      userInput = systemIn.readLine();

      sendingDataBuffer = userInput.getBytes();
      
      DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,IPAddress, SERVICE_PORT);
      
      clientSocket.send(sendingPacket);
      
      DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer,receivingDataBuffer.length);
      clientSocket.receive(receivingPacket);
      
      String receivedData = new String(receivingPacket.getData());
      System.out.println("Sent from the server: "+receivedData);
      
      String arr[] = receivedData.split(" ", 2);
      System.out.println(arr[0] + " " +arr[0].equals("end"));

      
      // Closing the socket connection with the server
      clientSocket.close();
    }
    catch(SocketException e) {
      e.printStackTrace();
    }
  }
}
