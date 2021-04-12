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


public class echoSV_tcp {
    public static void main(String[] args) {
		try {
	        InetAddress addr =InetAddress.getByName("10.1.1.1");
	        try {
	        	ServerSocket ss = new ServerSocket(8080,50,addr);
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
