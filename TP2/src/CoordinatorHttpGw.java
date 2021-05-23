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

import java.io.DataOutputStream;
import java.io.OutputStream;


public class CoordinatorHttpGw {
	int MAXCHUNKSIZE=400;

	int MAXTHREADS=10;
	ReentrantLock threadLock = new ReentrantLock();
	Condition threadCondition = threadLock.newCondition();


	ReentrantLock socketWriteLock = new ReentrantLock();
	DatagramSocket socket;

	//reentrant lock for table manage
	HashMap<InetAddress, FFSInfo> ffsTable = new HashMap<InetAddress, FFSInfo>();
	int numberFFS=0; //number of FFServers
	ReentrantLock ffsLock = new ReentrantLock();
	

	ReentrantLock requestIDLock = new ReentrantLock();
	int requestID=0;	//number of the request -> key for the chunkmanagers map/list

	HashMap<Integer, ChunkManager> chunkmanagers = new HashMap<Integer, ChunkManager>();

	public CoordinatorHttpGw(DatagramSocket s){
		this.socket=s;
	}

	public DatagramSocket getSocket(){
		return this.socket;
	}

	public int addServer(InetAddress i, int p){
		this.ffsLock.lock();
		try{
			FFSInfo sv = new FFSInfo(i,p);
			this.ffsTable.put(i,sv);
			this.numberFFS+=1;
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
	}

	public int removeServer(InetAddress i){
		this.ffsLock.lock();
		try{
			this.ffsTable.remove(i);
			this.numberFFS-=1;
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
	}

	public int getNumberFFS(){
		this.ffsLock.lock();
		try{
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
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



	//Colocar locks
	public void sendPacketRandomFFS(PacketUDP p1) throws IllegalArgumentException{
		try{
			Random generator = new Random();
			FFSInfo[] svinfos = new FFSInfo[this.numberFFS];
			svinfos = this.ffsTable.values().toArray(svinfos);
			FFSInfo svinfo = svinfos[generator.nextInt(svinfos.length)];
			
			//FFSInfo svinfo = this.getRandomFFSInfo();
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



//no need for locks because only the tcp session reads and the udp receiver writes
	public void addChunkManager(int reqID, ChunkManager c){
		this.chunkmanagers.put(reqID,c);
	}
	public void setChunkManagerSizeAndMetadata(int reqID, int s, String m){
		this.chunkmanagers.get(reqID).setSizeAndMetadata(s,m);
	}

	public ChunkManager getChunkManager(int reqID){
		return this.chunkmanagers.get(reqID);
	}
	public void removeChunkManager(int reqID){
		this.chunkmanagers.remove(reqID);
	}

	public int getChunkManagerSize(int reqID){
		return this.chunkmanagers.get(reqID).getSize();
	}
	public String getChunkManagerMetadata(int reqID){
		return this.chunkmanagers.get(reqID).getMetadata();
	}

	public void createChunksSpace(int reqID){
		int size = this.chunkmanagers.get(reqID).getSize();
		int key=0, countsize=0;
		
		while(countsize<size){
			this.chunkmanagers.get(reqID).setChunk(key,null);
			
			countsize+=MAXCHUNKSIZE;
			key+=1;
		}
	}

	public boolean requestChunks(int reqID, PacketUDP p1){
		boolean complete = true;
		//int size=this.chunkmanagers.get(reqID).getSize();
		for (Map.Entry<Integer,  byte[]> chunk: this.chunkmanagers.get(reqID).getEntrySetChunks()) {
			if(chunk.getValue()==null){
				//System.out.println("requeste chunk "+ chunk.getKey());
				p1.setChunkid(chunk.getKey());
				this.sendPacketRandomFFS(p1);
				complete=false;
			}// else if complete write in tcpsocket		<- o problema é se nao se consegue ir buscar o ficheiro todo
		}
		return complete;
	}


	public void setChunk(int reqID, int chunkid, byte[] bytes){
		if(this.chunkmanagers.get(reqID)!=null)
			this.chunkmanagers.get(reqID).setChunk(chunkid,bytes);
	}


	public void getChunksToSocketAsString(int reqID, PrintWriter out){
		for (Map.Entry<Integer,  byte[]> chunk: this.chunkmanagers.get(reqID).getEntrySetChunks()) {
			System.out.println(new String(chunk.getValue()));
			out.write(new String(chunk.getValue()));
		}
	}


	public void getChunksToSocketAsBytes(int reqID, OutputStream out){
		try{
			for (Map.Entry<Integer,  byte[]> chunk: this.chunkmanagers.get(reqID).getEntrySetChunks()) {
				out.write(chunk.getValue());
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
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