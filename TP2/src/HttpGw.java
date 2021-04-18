import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

//	ToDo:
//create controller class -> with udp socket, id of task (with lock) map of objects to save the bytes (each with locks)
//parse http request
//...

public class HttpGw {
	public static int udpport=8888;

	public static int tcpport = 8080;
	public static int backlog = 50;

    public static void main(String[] args) {
    	try{
    		//get machine's ip
    		String ip = myip();

    		//translate ip into InetAddress object and create the udp socket
    		InetAddress addr =InetAddress.getByName(ip);
			DatagramSocket udpsocket = new DatagramSocket(udpport,addr);






    		new ParserTCP(tcpport,backlog,addr).start();
    	}catch (Exception e) {
            System.out.println("Erro");
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
            System.out.println(ip);
            return ip;
            

        } catch (Exception e) {
            System.out.println("Erro");
        }
        return null;
    }
}

class ReceiverUDP extends Thread{
	DatagramSocket socket;

	ReceiverUDP(DatagramSocket s){
		this.socket=s;
	}


	public void run(){
		boolean running;

	    try{
			running = true;
		    while (running) {
		    	byte[] buf = new byte[256];
		        DatagramPacket packet = new DatagramPacket(buf, buf.length);
		        socket.receive(packet);
		        
		        InetAddress address = packet.getAddress();
		        int port = packet.getPort();
		        packet = new DatagramPacket(buf, buf.length, address, port);
		        
		        System.out.println(packet.getData());
		        System.out.println(packet.getLength());
		        System.out.println();
		        String received = new String(packet.getData(), 0, packet.getLength());
		        
		        /*
			        //String teste = "end";
			        System.out.println(received + "!");
			        System.out.println(received.equals("end"));
			        //System.out.println(teste.equals("end"));
			        if (received.equals("end")) {
			            running = false;
			            System.out.println("AAAAAAA");
			            continue;
			        }
		        */

		        socket.send(packet);
	            System.out.println();
	            System.out.println();
	            System.out.println();
		    }
		    socket.close();
	    
	    }catch(Exception e){
	        System.out.println("BBBBBB");
	    }
	}
}



class ParserTCP extends Thread{
	int tcpport = 8080;
	int backlog = 50;
	String ip;
	InetAddress addr;

	ParserTCP(int port, int b_log, InetAddress addr_ip){
		this.tcpport=port;
		this.backlog=b_log;
		this.addr=addr_ip;
	}


	public void run(){
        try {
        	ServerSocket ss = new ServerSocket(tcpport,backlog,addr);
            while (true) {
                Socket socket = ss.accept();
                new SessionTCP(socket).start();  
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}



class SessionTCP extends Thread{
	Socket socket;

	SessionTCP(Socket socket){
		this.socket=socket;
	}

	public void run(){
		try{
			float soma=0,aux;
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        PrintWriter out = new PrintWriter(socket.getOutputStream());

	        String line;
	        int i=0;
	        while ((line = in.readLine()) != null) {
	        		System.out.println(i+": "+line);
	            	
	                out.println("linha "+i);
	                out.flush();
	                i+=1;
	        	
	        }


	        out.flush();

	        socket.shutdownOutput();
	        socket.shutdownInput();
	        socket.close();

		}catch(IOException e){
            e.printStackTrace();			
		}
	}
}
