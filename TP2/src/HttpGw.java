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

import java.util.Date;
import java.text.SimpleDateFormat;

import java.io.OutputStream;



//	ToDo:
//parse http request


public class HttpGw {
	public static int udpport=8888;

	public static int tcpport = 8080;
	public static int backlog = 50;

	//Thread principal
	public static void main(String[] args) {
		try{
			//ip da máquina (automatico ou manual)
			String ip;
			if(args.length>0) ip = args[0];
			else ip = myip();

			System.out.println("HttpGw's IP: " + ip);
			System.out.println("HttpGw's TCPPort: " + tcpport);
			System.out.println("HttpGw's UDPPort: " + udpport);

			//tradução do ip para objeto InetAddress e criação do socket udp
			InetAddress addr =InetAddress.getByName(ip);
			DatagramSocket udpsocket = new DatagramSocket(udpport,addr);

			//criação do objeto que coordena as operações entre threads
			CoordinatorHttpGw coord = new CoordinatorHttpGw(udpsocket);


			//Criação do parser TCP
			new ParserTCP(addr,coord).start();

			//Criação do parser UDP
			new ReceiverUDP(coord).start();
		}catch (Exception e) {
			System.out.println("Erro na main");
			e.printStackTrace();
		}
		
	}


	//Retorna o ip da máquina
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
				}
			}
			return ip;
			

		} catch (Exception e) {
			System.out.println("Erro");
		}
		return null;
	}
}


//Thread que trata de receber os pedidos UDP, avalia-los e guarda-los nos sítios corretos
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
				//Receção do pacote
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				packet = new DatagramPacket(buf, buf.length, address, port);
				

				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
				PacketUDP p1 = (PacketUDP) in.readObject();
				in.close();

			   

				//	tratamento do pacote UDP
				if(p1.getPackettype()==1){
					if(!coord.FFSExists(address,port)){
						try{
							StringBuilder sb = new StringBuilder();
							sb.append("Subscribe");
							sb.append(address.getHostAddress());
							pass = new String(sb);

							//hash da password
							MessageDigest digest = MessageDigest.getInstance("SHA-256");
							byte[] hash = digest.digest(pass.getBytes());

							//confirmação se a password está correta
							if(Arrays.equals(p1.getChunk(),hash)){
								//adição do FFS
								int ns = this.coord.addServer(address,port);
								//System.out.println("\tPassword accepted\nNumber of servers: " + this.coord.getNumberFFS());
								System.out.println("\tFFS subscribed\nNumber of servers: " + ns);
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
						//System.out.println("->FFS recognised");
						if(p1.getPackettype()==3){
							if(coord.getChunkManagerSize(p1.getPacketid())==0){
								String filemetadata = new String(p1.getChunk());
								//System.out.println("Metadata:\n"+filemetadata);
								coord.setChunkManagerSizeAndMetadata(p1.getPacketid(),p1.getChunkid(),filemetadata);
							}
						}
						else if(p1.getPackettype()==4){
							//System.out.println("Chunk Recieved");
							this.coord.setChunk(p1.getPacketid(),p1.getChunkid(),p1.getChunk());
						}
						else if(p1.getPackettype()==2){
							System.out.println("Unsibscribe Request");

							try{
								StringBuilder sb = new StringBuilder();
								sb.append("Unubscribe");
								sb.append(address.getHostAddress());
								pass = new String(sb);
								//sb.append(":").append(port);
								//hash!!!
								MessageDigest digest = MessageDigest.getInstance("SHA-256");
								byte[] hash = digest.digest(pass.getBytes());

								if(Arrays.equals(p1.getChunk(),hash)){
									int ns1 = this.coord.removeServer(address,port);
									System.out.println("\tPassword accepted\nNumber of servers: " + ns1);
								}
								else{
									System.out.println("\tPassword wrong");
								}
							}
							catch(NoSuchAlgorithmException e) {
								e.printStackTrace();
							}
						}
						else System.out.println("->PacketUDP type not recognised");
					}
					else System.out.println("->FFS not recognised");
				}
			   
			}
			socket.close();
		
		}catch(Exception e){
			System.out.println("ReceiverUDP error");
			e.printStackTrace();
		}
	}
}


//Server Socket com controlo de fluxo
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

				//threadpool
				try{
					this.coord.threadLock.lock();
					try{
						while(this.coord.MAXTHREADS==0){
							this.coord.threadCondition.await();
						}
						this.coord.MAXTHREADS-=1;
					}finally{
						this.coord.threadLock.unlock();
					}				
				}catch(InterruptedException e){
					e.printStackTrace();
				}

				//System.out.println("ThreadCreated");
				new SessionTCP(socket,coord).start();  
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


//Sessão que trata da comunicação com o cliente
class SessionTCP extends Thread{
	Socket socket;
	CoordinatorHttpGw coord;

	//Valores que se devem alterar para testes. Estes funcionaram na apresentação
	int ROUNDTRIPTIME = 40;		//tempo que espera por cada pedido (em milisegundos)
	int REQUESTINCREMENT = 30;	//tempo que se incrementa a espera por cada pedido (em milisegundos)
	int TRIES = 4;				//numero de vezes que reenvia o pacote
	
	//codigo html de 404
	String filenotfound = "<html><head><title>404 Not Found</title></head><body><h1>404 Not Found</h1></body></html>";


	SessionTCP(Socket socket){
		this.socket=socket;
	}
	SessionTCP(Socket socket, CoordinatorHttpGw c){
		this.socket=socket;
		this.coord=c;
	}


	String httpMethod;			//metodo do http request
	String filename;			//ficheiro pedido no http request
	boolean badrequest=false;	//se o http request foi mal feitos
	//parse da primeira linha do http request
	private void parseTopHeaderHttpRequest(String line){
		String[] parts = line.split("\\s+", 3);
		if (parts.length != 3
				|| !parts[1].startsWith("/")
				|| !parts[2].toUpperCase().startsWith("HTTP/")) {
			badrequest = true;
		}else{
			httpMethod = parts[0];
			System.out.println("Http Method: "+httpMethod);
			filename = parts[1].substring(1);
			System.out.println("File: "+filename);

		}
	}

	//parse das seguintes linhas do http request
	private void parseCommonHeaderHttpRequest(String line){
		String[] pair = line.split(":", 2);
		if (pair.length != 2) {
			badrequest = true;
		}
	}

	public void run(){
		try{
			int requestID = coord.getRequestID(); //id da sessão -> chave para mapa de chunkmanagers
			//purge chunks
			coord.removeChunkManager(requestID);

			System.out.println("\nMy Session ID: "+ requestID);

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			OutputStream os = socket.getOutputStream();
			PrintWriter out = new PrintWriter(os);



			//Parse Http Request
			String line;

	        line = in.readLine();
	        parseTopHeaderHttpRequest(line);

			line = in.readLine();
			while (line != null && line.length()>0) {
				parseCommonHeaderHttpRequest(line);
				line = in.readLine();
				
			}

			

			//	Bad request
			if(badrequest){
				out.write("HTTP/1.1 400 Bad Request\r\n\r\n");
				out.write("Connection: Closed");
			}
			else if(!httpMethod.equals("GET")){		//Não é um GE?
				out.write("HTTP/1.1 501 Not Implemented\r\n\r\n");
				out.write("Connection: Closed");
			}
			else{
				//formato da data
				SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:Ss z", Locale.ENGLISH);
				
				//nome do ficheiro requirido
				String str = new String(filename);

				PacketUDP p1 = new PacketUDP(3,requestID,0,str.getBytes());
				try{
					//Thread.sleep(20*1000);		//Para testar o limite de treads
					int size = 0;
					int aux=0;
					
					//Criação do chunkmanager e adição ao mapa
					ChunkManager chunk1 = new ChunkManager(str, 0);
					coord.addChunkManager(requestID,chunk1);
									
					while(size==0){
						coord.sendPacketRandomFFS(p1);
						Thread.sleep(this.ROUNDTRIPTIME+(aux*this.REQUESTINCREMENT));
						//getsizeof file
						size = this.coord.getChunkManagerSize(p1.getPacketid());
						aux+=1;
						if(aux==this.TRIES*5)size=-1;
					}
					// Request chunks if size>0
					System.out.println("Saiu do ciclo. Size="+size);
					if(size>0){
						
						//create space for chunks
						this.coord.createChunksSpace(requestID);
						
						aux=0;
						boolean downloadcomplete=false;
						p1.setPackettype(4);
						while(!downloadcomplete && aux<(TRIES*15)){		//TRIES*15 because we know the file is there, but the FFServers can go down

							//request file chunks
							System.out.println("Requested Chunks");
							if(this.coord.requestChunks(requestID, p1)){
								downloadcomplete=true;
							}else{
								//System.out.println(this.ROUNDTRIPTIME+(aux*this.REQUESTINCREMENT));
								Thread.sleep(this.ROUNDTRIPTIME+(aux*this.REQUESTINCREMENT));
							}
							aux+=1;
						}
						if(downloadcomplete){
							System.out.println("Download Complete!");
							os.write("HTTP/1.1 200 OK\r\n".getBytes());
							os.write("Server: HTTP server/0.1\n".getBytes());
							os.write(("Date: "+format.format(new java.util.Date())+"\n").getBytes());
							
							String metadata = this.coord.getChunkManagerMetadata(requestID);
							
							//add metadata
							os.write(metadata.getBytes());
							System.out.println(metadata);
							os.write("Connection: Closed\n\n".getBytes());
							
							//add file to outputstream
							this.coord.getChunksToSocketAsBytes(requestID,socket.getOutputStream());

							os.write("\r\n\r\n".getBytes());
						}else{
							System.out.println("Download Not Complete!");
							out.write("HTTP/1.1 500 Internal Server Error\n");
						}
					}else{
						System.out.println("404");
						out.write("HTTP/1.1 404 Not Found\r\n\r\n");
						out.write(filenotfound);

					} 

				}
				catch(IllegalArgumentException e){
					System.out.println("Error: No Fast File Server Connected");
					out.write("HTTP/1.1 503 Service Unavailable\r\n\r\n");
					out.write("Connection: Closed");
				}
				catch(InterruptedException e){
					System.out.println("Error: Thread Could Not Sleep");
					out.write("HTTP/1.1 500 Internal Server Error\r\n\r\n");
					out.write("Connection: Closed");
				}
			}


			System.out.println("Http Response sent");
			out.flush();


			socket.shutdownOutput();
			socket.shutdownInput();
			socket.close();

		}catch(IOException e){
			e.printStackTrace();
		}


		//Incremento do máximo de threads
		this.coord.threadLock.lock();
		try{
			//System.out.println("Thread dismantled");
			this.coord.MAXTHREADS+=1;
			this.coord.threadCondition.signal();
		}finally{
			this.coord.threadLock.unlock();
		}
	}

}
