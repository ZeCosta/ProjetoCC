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
	int tablekey=0;

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

	public void sendPacketRandomFFS(PacketUDP p1){
		FFSInfo svinfo = this.getRandomFFSInfo();
		this.sendPacket(p1, svinfo.getIp(), svinfo.getPort());
	}
	public FFSInfo getRandomFFSInfo(){
		Random rand = new Random();
		int n = rand.nextInt(this.tablekey);
		System.out.println("Random is "+n);
		return this.ffsTable.get(n);
	}

}