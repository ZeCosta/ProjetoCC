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
import java.lang.StringBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.HashSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.RandomAccessFile;

import java.lang.Runtime;

import java.util.Date;
import java.text.SimpleDateFormat;

//maybe
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//MIME-Type
import java.io.File;



class FFSCoordinator {
	boolean resubscribe;
	ReentrantLock resubscribeLock = new ReentrantLock();

	public FFSCoordinator(){
		this.resubscribe=false;
	}

	public boolean setBool(boolean b){		//returns the boolean before change
		this.resubscribeLock.lock();
		try{
			boolean ret = this.resubscribe;
			this.resubscribe=b;
			return ret;
		}finally{
			this.resubscribeLock.unlock();
		}
	}
}


public class FastFileServer{
  public static int MAXCHUNKSIZE = 400;
  public static int port;
  public static String serverIP;
  public static String myIP;


  public static Set<String> fileregister = new HashSet<String>();

  public static String password;
  //public static String passKey;
  
  public static class Resubscribe extends Thread{
	int RESUBSCRIBETIME = 60;		//in seconds
  	DatagramSocket ffsSocket;
	PacketUDP p1;
	InetAddress ipAddress;
	int port;
	FFSCoordinator coord;

	public Resubscribe(DatagramSocket f,PacketUDP pa,InetAddress ip, int po, FFSCoordinator c){
		this.ffsSocket=f; 
		this.p1=pa; 
		this.ipAddress=ip; 
		this.port=po; 
		this.coord=c;
	}

	public void run(){
		boolean running = true;
		while(running){
			//sleep
			try{
				Thread.sleep(RESUBSCRIBETIME*1000);
				if(this.coord.setBool(true)){
					//re-send
					try{
						byte[] sendingDataBuffer = new byte[1024];


						ByteArrayOutputStream out = new ByteArrayOutputStream();
						ObjectOutputStream os = new ObjectOutputStream(out);		
						os.writeObject(this.p1);
						sendingDataBuffer = out.toByteArray();

						DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,this.ipAddress, this.port);
						this.ffsSocket.send(sendingPacket);
						System.out.println("Resubscribe sent at " +new Date()+"\n");
					}catch(IOException e){
						e.printStackTrace();			
					}
				}
			}	
			catch(Exception e){
				System.out.println("Problem in the ReLogin");
				e.printStackTrace();
			}
		}
	}
  }

  public static class Unsubscribe extends Thread {
	DatagramSocket ffsSocket;
	PacketUDP p1;
	InetAddress ipAddress;
	int port;

	public Unsubscribe(DatagramSocket f,PacketUDP pa,InetAddress ip, int po){
		this.ffsSocket=f; 
		this.p1=pa; 
		this.ipAddress=ip; 
		this.port=po; 
	}

	public void run() {
		System.out.println("");
		System.out.println("Ctrl+C was pressed");

		try{
			byte[] sendingDataBuffer = new byte[1024];


			ByteArrayOutputStream out = new ByteArrayOutputStream();
	   		ObjectOutputStream os = new ObjectOutputStream(out);		
	   		os.writeObject(this.p1);
			sendingDataBuffer = out.toByteArray();

			DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,this.ipAddress, this.port);
			this.ffsSocket.send(sendingPacket);
			System.out.println("Unsubscribe sent");
			
			this.ffsSocket.close();

		}catch(IOException e){
			System.out.println("Unable to close socket");
			e.printStackTrace();			
		}
	}
  }

  public static void main(String[] args) throws IOException{
	try{
		fileregister.add("index.html");
		fileregister.add("frontpage.html");
		fileregister.add("twochunkfile.html");
		fileregister.add("enunciado.pdf");
		fileregister.add("images.png");
		fileregister.add("audio.mp4");

		myIP=myip();
		System.out.println("My ip: "+ myIP);
		//System.out.println("My port: "+ port);
		System.out.println();


		DatagramSocket ffsSocket = new DatagramSocket();

		BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

		if(args.length>0){
			serverIP = args[0];
			System.out.print("Server ip: " + serverIP);
			System.out.println();
		}else{
			System.out.print("Input Server ip: ");
			serverIP = systemIn.readLine();
		}

	   	if(args.length>1){
			port = Integer.parseInt(args[1]);
			System.out.print("Server port: " +port);
			System.out.println();
		}else{
			port=8888;
			System.out.print("Default Server port: " + port);
			System.out.println();
		}
		System.out.println();


		InetAddress ipAddress = InetAddress.getByName(serverIP);

		byte[] sendingDataBuffer = new byte[1024];
		byte[] receivingDataBuffer = new byte[1024];


		StringBuilder sb = new StringBuilder();
		sb.append("Subscribe");
		sb.append(myIP);
		password = new String(sb);
		
		//hash!!!
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(password.getBytes());


		PacketUDP p1 = new PacketUDP(1,0,0,hash);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
   		ObjectOutputStream os = new ObjectOutputStream(out);		
   		os.writeObject(p1);
		sendingDataBuffer = out.toByteArray();

		DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
		ffsSocket.send(sendingPacket);

		System.out.println("Package Subscribe Sent");



		//Sistema de re-login
		FFSCoordinator coord = new FFSCoordinator();
		new Resubscribe(ffsSocket,p1.clone(),ipAddress,port,coord).start();



		//		create unsubscribe package
		sb = new StringBuilder();
		sb.append("Unubscribe");
		sb.append(myIP);
		password = new String(sb);
		
		//hash!!!
		digest = MessageDigest.getInstance("SHA-256");
		hash = digest.digest(password.getBytes());


		p1 = new PacketUDP(2,0,0,hash);
		
		
			Runtime.getRuntime().addShutdownHook(new Unsubscribe(ffsSocket,p1,ipAddress, port));

			Boolean running = true;
			while (running) {
				DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer,receivingDataBuffer.length);
				ffsSocket.receive(receivingPacket);
				coord.setBool(false);

				try{
					ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivingPacket.getData()));
					PacketUDP p2 = (PacketUDP) in.readObject();
					in.close();

					String receivedData = new String(receivingPacket.getData());
					//
					

					String arr = new String(p2.getChunk());
					
					//

					if(receivingPacket.getAddress().equals(ipAddress)){
						if(p2.getPackettype()==3){	// size request
							System.out.println("->Send filesize+metadata");
							int filesize=-1;
							
							String file = new String(p2.getChunk());
							String metadata = new String();

							if(fileregister.contains(file)){
								filesize=getFileSizeNIO(file);
								metadata=GetFileMetadata(file);
							}

							p2.setChunkid(filesize);
							p2.setChunk(metadata.getBytes());
							out = new ByteArrayOutputStream();
					   		os = new ObjectOutputStream(out);		
					   		os.writeObject(p2);
							sendingDataBuffer = out.toByteArray();

							sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
							ffsSocket.send(sendingPacket);

						}else if(p2.getPackettype()==4){	// chunk request
							String file = new String(p2.getChunk());
							//System.out.println("->Send chunk from "+file);
							
							//System.out.println(file);
							if(fileregister.contains(file)){
								
								p2.setChunk(getFileChunk(file,p2.getChunkid()));
								out = new ByteArrayOutputStream();
						   		os = new ObjectOutputStream(out);		
						   		os.writeObject(p2);
								sendingDataBuffer = out.toByteArray();

								sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
								ffsSocket.send(sendingPacket);							

							}

						}else System.out.println("Ignoring packet, Operation not recognised");

					}else System.out.println("Not the HttpGw");
				}catch(ClassNotFoundException e){
		  			e.printStackTrace();
				}
			}
		
	}
	catch(SocketException e) {
		System.out.println("Socket closed");

		//e.printStackTrace();
	}
	catch(NoSuchAlgorithmException e) {
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
				}
			}
			//System.out.println(ip);
			return ip;
			

		} catch (Exception e) {
			System.out.println("Erro");
		}
		return null;
	}

	public static int getFileSizeNIO(String fileName) {

		Path path = Paths.get(fileName);

		int bytes = -1;
		try {
			// size of a file (in bytes)
			bytes = (int) Files.size(path);
		}catch (IOException e) {
			e.printStackTrace();
		}

		return bytes;
	}


	public static byte[] getFileChunk(String fileName, int offset) {

		Path path = Paths.get(fileName);

		int size = getFileSizeNIO(fileName);
		byte[] chunk = new byte[((offset*MAXCHUNKSIZE+MAXCHUNKSIZE<size)?MAXCHUNKSIZE:size-offset*MAXCHUNKSIZE)];
		if(size>0){
			try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
				raf.seek(offset*MAXCHUNKSIZE);
				raf.readFully(chunk);
			}catch (IOException e) {
				e.printStackTrace();
			}
		}

		return chunk;
	}


	public static SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	//String of metadata
	public static String GetFileMetadata(String filename){
		StringBuilder sb = new StringBuilder();

		try{
			Path filep = Paths.get(filename);
			BasicFileAttributes attr = Files.readAttributes(filep, BasicFileAttributes.class);

			//System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
			sb.append("Last-Modified: ");
			FileTime fileTime = attr.lastModifiedTime();
			sb.append(format.format(fileTime.toMillis()));
			//System.out.println("lastModifiedTime: " + formatDateTime(fileTime));
			//System.out.println("lastModifiedTime: " + format.format(fileTime.toMillis()));
			sb.append("\n");
			
			//System.out.println("size: " + attr.size());
			sb.append("Content-Length: ").append(attr.size()).append("\n");

			File file = new File(filename);
			FileNameMap fileNameMap = URLConnection.getFileNameMap();
			String mimeType = fileNameMap.getContentTypeFor(file.getName());
			//System.out.println("MIME-Type: " + mimeType);
			sb.append("Content-Type: ").append(mimeType).append("\n");

		}catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}

	public static String formatDateTime(FileTime fileTime) {
		DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:Ss z");

        LocalDateTime localDateTime = fileTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return localDateTime.format(dateformatter);
    }

}
