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


public class HttpGw {
	public static int port = 8080;
	public static int backlog = 50;
	public static String ip;

    public static void main(String[] args) {
		try {
	        //InetAddress addr =InetAddress.getByName("10.1.1.1");
	        ip = myip();
			InetAddress addr2 = InetAddress.getByName(ip);
			System.out.println("IP in use: " + ip);
			System.out.println("Port in use: " + port);
			System.out.println("Backlog: " + backlog);
	        try {
	        	ServerSocket ss = new ServerSocket(port,backlog,addr2);
	            while (true) {
	                Socket socket = ss.accept();
	                new Session2(socket).start();  
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }catch(Exception e){
            System.out.println("BBBBBB");
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






class Session2 extends Thread{
	Socket socket;

	Session2(Socket socket){
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
