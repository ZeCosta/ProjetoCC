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
import java.lang.StringBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//	ToDo:
//create controller class -> with udp socket, id of task (with lock) map of objects to save the bytes (each with locks)
//parse http request
//...


//Pool of threads -> int nthreads=6, decrements when a thread starts, increses when a thread ends, if it's 0 doesn't create a thread
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
	String pass;		//For now its a fixed password -> maybe use an encryption related to a key/ip of the origin
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

			   

				//	Handle the PacketUDP
				if(p1.getPackettype()==1){
					System.out.println("->Subscribe");
					if(!coord.FFSExists(address,port)){
						try{
							StringBuilder sb = new StringBuilder();
							sb.append("Subscribe");
							sb.append(address.getHostAddress());
							pass = new String(sb);
							//sb.append(":").append(port);
							//hash!!!
							MessageDigest digest = MessageDigest.getInstance("SHA-256");
							byte[] hash = digest.digest(pass.getBytes());

							if(Arrays.equals(p1.getChunk(),hash)){
								System.out.println("\tPassword accepted");
								this.coord.addServer(address,port);
								System.out.println(this.coord.getTableSize());
							}
							else{
								System.out.println("\tPassword wrong");
							}
						}
						catch(NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
					}
					else System.out.println("->FFS already subscribed");
				}else{
					if(coord.FFSExists(address,port)){
						System.out.println("->FFS recognised");
						if(p1.getPackettype()==3){
							if(coord.getChunks(p1.getPacketid())==null){
								String fileName = new String(p1.getChunk());
								Chunks chunk1 = new Chunks(fileName, p1.getChunkid());
								coord.addChunks(p1.getPacketid(),chunk1);
							}
						}
						else System.out.println("->PacketUDP type not recognised");
					}
					else System.out.println("->FFS not recognised");
				}
			   

				// Remove this send
				//coord.sendPacketRandomFFS(p1);

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

	int roundTripTime = 10;
	int tries = 3;

	SessionTCP(Socket socket){
		this.socket=socket;
	}
	SessionTCP(Socket socket, CoordinatorHttpGw c){
		this.socket=socket;
		this.coord=c;
	}

	public void run(){
		try{
			int requestID = coord.getRequestID(); //id of the session -> key for the map of chunks
			//purge chunks
			coord.removeChunks(requestID);

			System.out.println("My Session ID: "+ requestID);

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream());


			String line;
			int i=0;

			System.out.println("---------------------");
			line = in.readLine();
			while (line != null && line.length()>0) {
				System.out.println(i+": "+line);
					
				//out.println("linha "+i);
				//out.flush();
				i+=1;
				line = in.readLine();
				
			}
			System.out.println("---------------------");

			//System.out.println("TCPSession end");

			String str = new String("/file.extension");
			PacketUDP p1 = new PacketUDP(3,requestID,0,str.getBytes());
			try{
				int size = 0;
				int aux=0;
				while(size==0){
					coord.sendPacketRandomFFS(p1);
					Thread.sleep(this.roundTripTime);
					//getsizeof file
					size = this.coord.getChunksSize(p1.getPacketid());
					aux+=1;
					if(aux==this.tries)size=-1;
				}
				// Request chunks if size>0
				System.out.println("Saiu do ciclo. Size="+size);
			}
			catch(IllegalArgumentException e){
				System.out.println("Error: No Fast File Server Connected");
			}
			catch(InterruptedException e){
				System.out.println("Error: Thread Could Not Sleep");
			}

 			
 			out.println("Numero de linhas: "+i);
			System.out.println("");
			out.flush();

			socket.shutdownOutput();
			socket.shutdownInput();
			socket.close();

		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
