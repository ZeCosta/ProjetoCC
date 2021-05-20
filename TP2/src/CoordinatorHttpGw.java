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
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class CoordinatorHttpGw {
	ReentrantLock socketWriteLock = new ReentrantLock();
	DatagramSocket socket;

	//reentrant lock for table manage
	HashMap<Integer, FFSInfo> ffsTable = new HashMap<Integer, FFSInfo>();

	int tablekey=0; //number of FFServers
	
	ReentrantLock requestIDLock = new ReentrantLock();
	int requestID=0;	//number of the request -> key for the chunks map/list

	HashMap<Integer, Chunks> chunks = new HashMap<Integer, Chunks>();

	public CoordinatorHttpGw(DatagramSocket s){
		this.socket=s;
	}

	public DatagramSocket getSocket(){
		return this.socket;
	}

	public void addServer(InetAddress i, int p){
		FFSInfo sv = new FFSInfo(i,p);
		this.ffsTable.put(this.tablekey,sv);
		this.tablekey+=1;
	}
	public int getTableSize(){
		return tablekey;
	}

	public void sendPacket(PacketUDP p1, InetAddress ipAddress, int port){
		this.socketWriteLock.lock();
		try{
			byte[] sendingDataBuffer = new byte[1024];

			ByteArrayOutputStream out = new ByteArrayOutputStream();
	   		ObjectOutputStream os = new ObjectOutputStream(out);		
	   		os.writeObject(p1);
			sendingDataBuffer = out.toByteArray();

			DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,sendingDataBuffer.length,ipAddress, port);
			this.socket.send(sendingPacket);
		}
		catch(Exception e){
			this.socketWriteLock.unlock();
			System.out.println("Error Sending packet");
			e.printStackTrace();
		}
		finally{
			this.socketWriteLock.unlock();
		}
	}

	public void sendPacketRandomFFS(PacketUDP p1) throws IllegalArgumentException{
		FFSInfo svinfo = this.getRandomFFSInfo();
		this.sendPacket(p1, svinfo.getIp(), svinfo.getPort());
	}
	public FFSInfo getRandomFFSInfo() throws IllegalArgumentException{
		try{
			Random rand = new Random();
			int n = rand.nextInt(this.tablekey);
			System.out.println("Random is "+n);
			return this.ffsTable.get(n);
		}
		catch(IllegalArgumentException e){
			throw e;
		}
	}

	public void sendPacketFFS(PacketUDP p1, int index) throws IllegalArgumentException{
		try{
			FFSInfo svinfo = this.ffsTable.get(index);
			this.sendPacket(p1, svinfo.getIp(), svinfo.getPort());
		}
		catch(IllegalArgumentException e){
			throw e;
		}
	}


	public int getRequestID(){
		this.requestIDLock.lock();
		try{
			return this.requestID;
		}
		finally{
			this.requestID+=1;
			this.requestIDLock.unlock();
		}
	}



	public void addChunks(int reqID, Chunks c){
		this.chunks.put(reqID,c);
	}
	public Chunks getChunks(int reqID){
		return this.chunks.get(reqID);
	}
	public void removeChunks(int reqID){
		this.chunks.remove(reqID);
	}

	public int getChunksSize(int reqID){
		if(this.chunks.get(reqID)!=null)
			return this.chunks.get(reqID).getSize();
		return 0;
	}



	public boolean FFSExists(InetAddress i, int p){
		FFSInfo aux = new FFSInfo(i,p);
		for(FFSInfo f: this.ffsTable.values()){
			if(f.equals(aux)) return true;
		}
		return false;
		//return this.ffsTable.containsValue(new FFSInfo(i,p)); 	// nao funciona
	}
}