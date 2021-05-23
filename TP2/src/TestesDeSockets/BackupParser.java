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

			CoordinatorHttpGw coord = new CoordinatorHttpGw(udpsocket);




    		//new ParserTCP(tcpport,backlog,addr).start();
    		new ParserTCP(addr,coord).start();

    		new ReceiverUDP(coord).start();
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
	String pass = new String("ola");		//For now its a fixed password -> maybe use an encryption related to a key/ip of the origin
	CoordinatorHttpGw coord;
	ReceiverUDP(DatagramSocket s){
		this.socket=s;
	}
	ReceiverUDP(CoordinatorHttpGw c){
		this.coord=c;
	}


	public void run(){
		boolean running;
		this.socket=this.coord.getSocket();

	    try{
			running = true;
		    while (running) {
		    	System.out.println("Waiting for packet");
		    	byte[] buf = new byte[256];
		        DatagramPacket packet = new DatagramPacket(buf, buf.length);
		        socket.receive(packet);
		        
		        InetAddress address = packet.getAddress();
		        int port = packet.getPort();
		        packet = new DatagramPacket(buf, buf.length, address, port);
		        
		    	System.out.println("Recieved packet");

		    	ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
				PacketUDP p1 = (PacketUDP) in.readObject();
				in.close();

		        /*
		        	System.out.println(p1);
		        	System.out.println(p1.getPackettype());
		        	System.out.println(p1.getPacketid());
		        	System.out.println(p1.getChunkid());
		        	System.out.println(p1.getChunk() + "->" + arr);
		        	System.out.println(arr + "==" + pass + "? " + arr.equals(pass));
		        	System.out.println();
		       	*/

		        //	Handle the PacketUDP
		        if(p1.getPackettype()==1){
		        	System.out.println("->Subscribe");
		        	String arr = new String(p1.getChunk());
		        	if(arr.equals(pass)){
		        		System.out.println("\tPassword "+arr+" accepted");
		        		this.coord.addServer(address,port);
		        		System.out.println(this.coord.getTableSize());
		        	}
		        	else{
		        		System.out.println("\tPassword "+arr+" wrong");

		        	}
		        }else System.out.println("->PacketUDP type not recognised");
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

				// Remove this send
		        coord.sendPacketRandomFFS(p1);
		        //socket.send(packet);
	            System.out.println();
		    }
		    socket.close();
	    
	    }catch(Exception e){
	        System.out.println("ReceiverUDP error");
            e.printStackTrace();
	    }
	}
}



class ParserTCP extends Thread{
	int tcpport = 8080;
	int backlog = 50;
	String ip;
	InetAddress addr;
	CoordinatorHttpGw coord;

	ParserTCP(int port, int b_log, InetAddress addr_ip){
		this.tcpport=port;
		this.backlog=b_log;
		this.addr=addr_ip;
	}
	ParserTCP(InetAddress addr_ip, CoordinatorHttpGw c){
		this.addr=addr_ip;
		this.coord=c;
	}


	public void run(){
        try {
        	ServerSocket ss = new ServerSocket(tcpport,backlog,addr);
            while (true) {
                Socket socket = ss.accept();
                new SessionTCP(socket,coord).start();  
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}



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
		/*
		try {
			this.url = new URL("http://" + this.getServerNetloc() + parts[1]);
		} catch (MalformedURLException e) {
			throw new BadRequest();
		}

		this.method = HttpMethods.cleanMethod(parts[0]);
		 */

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
		//String name = pair[0].replaceAll(" ", "");
		//String value = pair[1].trim();
		//this.collectedHeaders.put(name, value);
		return flag;
	}

	/*
	private byte[] readBody() throws HttpException, IOException {
		ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
		int contentLength = 0; // for now content without Content-Length header are ignored
		if (this.collectedHeaders.containsKey("Content-Length")) {
			String valueStr = this.collectedHeaders.get("Content-Length");
			try {
				contentLength = Integer.valueOf(valueStr);
			} catch (NumberFormatException e) {
				throw new BadRequest();
			}
		}

		int nextByte;
		while (bodyStream.size() < contentLength) {
			this.waitUntilClientInputReady();
			nextByte = this.in.read();
			bodyStream.write(nextByte);
		}
		return bodyStream.toByteArray();
	}
	 */

	private boolean isGet(String line) throws HttpException{
		String[] pair = line.split("\s+", 3);
		boolean flag = false;
		if(pair[0].toUpperCase().startsWith("GET")){
			flag = true;
		}
		return flag;
	}

	/*
	private String getServerNetloc() {
		String hostname = this.serverAddr.getHostName();
		return hostname + ((this.serverPort != 80) ? (":" + this.serverPort) : "");
	}

	 */


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
