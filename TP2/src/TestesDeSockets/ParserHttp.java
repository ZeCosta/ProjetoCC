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
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;




class SessionTCP extends Thread{
	Socket socket;
	CoordinatorHttpGw coord;

	SessionTCP(Socket socket){
		this.socket=socket;
	}
	SessionTCP(Socket socket, CoordinatorHttpGw c){
		this.socket=socket;
		this.coord=c;
	}

	public boolean parseTopHeader(String line) throws HttpException{
		boolean flag = false;
		String[] parts = line.split("\s+", 3);
		if (parts.length != 3
				|| !parts[1].startsWith("/")
				|| !parts[2].toUpperCase().startsWith("HTTP/")) {
			throw new BadRequest();
		}
		else{
			flag = true;
		}
		method = HttpMethods.cleanMethod(parts[0]);
		

		return flag;
	}

	private boolean parseCommonHeader(String line) throws HttpException{
		boolean flag = false;
		String[] pair = line.split(":", 2);
		if (pair.length != 2) {
			throw new BadRequest();
		}
		else{
			flag = true;
		}

		return flag;
	}


	private boolean isGet(String line) throws HttpException{
		String[] pair = line.split("\s+", 3);
		boolean flag = false;
		if(pair[0].toUpperCase().startsWith("GET")){
			flag = true;
		}
		return flag;
	}


	//Mine

	private static HttpMethods httpMethod; 	//httpMethod == HttpMethods.GET
	private static String filename;
	private static boolean badrequest=false;
	private boolean parseTopHeaderHttpRequest(String line) throws HttpException{
		String[] parts = line.split("\s+", 3);
		if (parts.length != 3
				|| !parts[1].startsWith("/")
				|| !parts[2].toUpperCase().startsWith("HTTP/")) {
			badrequest = true;
		}else{
			httpMethod = HttpMethods.cleanMethod(parts[0]);
			filename = part[1].substring(1);

		}
		return badrequest;
	}


	public void run(){
		try{
			float soma=0,aux;
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        PrintWriter out = new PrintWriter(socket.getOutputStream());

	        String line;
	        int i=0;

	        line = in.readLine();

	        parseTopHeader(line);

			if(isGet(line)){ // responde com index

			}


			boolean badrequest = parseTopHeaderHttpReq(line);


			boolean flag = false;
	        while (line != null && line.length()>0) {
	        	System.out.println(i+": "+line);
	            //out.println("linha "+i);
	            //out.flush();
	            i+=1;
	        	line = in.readLine();
				//flag = parseCommonHeader(line);
	        }

	        System.out.println("TCPSession end");

	        String str = new String("Packet sent by session sent");
	        PacketUDP p1 = new PacketUDP(1,0,0,str.getBytes());
	        coord.sendPacketRandomFFS(p1);

 			
 			out.println("linha "+i);
	        System.out.println("aaa");
	        out.flush();

	        socket.shutdownOutput();
	        socket.shutdownInput();
	        socket.close();

		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
