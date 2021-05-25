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
	int MAXCHUNKSIZE=400;			//Máximo número de bytes por chunk										

	int MAXTHREADS=10;				//Máximo número de threads que podem estar ativas de uma vez para sessões tcp + lock e condition
	ReentrantLock threadLock = new ReentrantLock();
	Condition threadCondition = threadLock.newCondition();


	DatagramSocket socket;			//socket udp para que a thread ReceiverUDP possa ler do socket e as threads SessionTCP possam enviar pacotes
	ReentrantLock socketWriteLock = new ReentrantLock();		//lock para que apenas uma thread SessionTCP possa ecrever de cada vez


	HashMap<String, FFSInfo> ffsTable = new HashMap<String, FFSInfo>();	//tabela com a os FFServers. A chave é a concatenação do ip + porta
	int numberFFS=0; 													//numero de FFServers que estão subscritos
	ReentrantLock ffsLock = new ReentrantLock();						//lock para o acesso concorrente ao mapa
	

	int requestID=0;				//numero da proxima sessãoTCP 	-> é usado para o mapa de chunkmanagers
	int MAXREQUESTID=2147483647;	//numero máximo de requestID 	->não é preciso haver controlo de ids porque não vai haver colisão
	ReentrantLock requestIDLock = new ReentrantLock();					//lock para que apenas uma sessãotCP possa pedir o número de cada vez


	HashMap<Integer, ChunkManager> chunkmanagers = new HashMap<Integer, ChunkManager>(); 	//mapa com os chunkManagers de cada ficheiro

	//Função para criar o objeto CoordinatorHttpGw
	public CoordinatorHttpGw(DatagramSocket s){
		this.socket=s;
	}

	//Função que retorna o socket
	public DatagramSocket getSocket(){
		return this.socket;
	}


	//Função que adiciona um FFS ao mapa:
	// 1.Lock ao mapa
	// 2.Criação da entrada
	// 3.Inserção no mapa, incremento do numero de servidores e retorno do mesmo
	// 4.Unlock ao mapa
	public int addServer(InetAddress i, int p){
		this.ffsLock.lock();
		try{
			String key=i.getHostAddress()+String.valueOf(p);
			FFSInfo sv = new FFSInfo(i,p);
			this.ffsTable.put(key,sv);
			this.numberFFS+=1;
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
	}


	//Função que remove um FFS ao mapa:
	// 1.Lock ao mapa
	// 2.Criação da chave do servidor
	// 3.Remoção so mapa, decremento do numero de servidores e retorno do mesmo
	// 4.Unlock ao mapa
	public int removeServer(InetAddress i, int p){
		this.ffsLock.lock();
		try{
			String key=i.getHostAddress()+String.valueOf(p);
			this.ffsTable.remove(key);
			this.numberFFS-=1;
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
	}


	//Função que retorna o numero de FFS:
	// 1.Lock ao mapa
	// 2.Retorno do numero de FFS
	// 4.Unlock ao mapa
	public int getNumberFFS(){
		this.ffsLock.lock();
		try{
			return this.numberFFS;
		}finally{
			this.ffsLock.unlock();
		}
	}



	//Função que retorna true se o FFS existe e false se não existe:
	// 1.Criação da chave do FFS
	// 2.Lock ao mapa de FFS
	// 3.Retorno do Contains do mapa
	// 3.Unlock ao mapa de FFS
	public boolean FFSExists(InetAddress i, int p){
		String key=i.getHostAddress()+String.valueOf(p);
		this.ffsLock.lock();
		try{
			return this.ffsTable.containsKey(key);
		}finally{
			this.ffsLock.unlock();
		}
	}




	//Função que envia um pacote p1 por UDP ao servidor com ip=ipAddress e a porta=port:
	// 1.Lock ao socket
	// 2.Envio do pacote
	// 3.Unlock do socket
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



	
	//Função que escolhe um FFS aleatório e chama a função sendPacket para enviar o pacote:
	// 1.Lock ao mapa de FFS
	// 2.Escolha aleatória do servidor
	// 3.Chamada da função sendPacket
	// 4.Unlock ao mapa de FFS
	public void sendPacketRandomFFS(PacketUDP p1) throws IllegalArgumentException{
		this.ffsLock.lock();
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
		finally{
			this.ffsLock.unlock();
		}
	}


	//Função que retorna o id de sessão à SessionTCP:
	// 1.Lock ao requestID
	// 2.Retorno do requestID
	// 3.Se o requestID for menor que o número máximo, o requestID é incrementado, se não é colocado a 0
	// 4.Unlock ao requestID
	public int getRequestID(){
		this.requestIDLock.lock();
		try{
			return this.requestID;
		}
		finally{
			if(this.requestID<MAXREQUESTID)
				this.requestID+=1;
			else this.requestID=0;
			this.requestIDLock.unlock();
		}
	}



	//Função que adiciona um chunk manager ao mapa  de chunkmanagers
	public void addChunkManager(int reqID, ChunkManager c){
		this.chunkmanagers.put(reqID,c);
	}
	//Função que adiciona o tamanho e a metadata a um chunkmanager
	public void setChunkManagerSizeAndMetadata(int reqID, int s, String m){
		this.chunkmanagers.get(reqID).setSizeAndMetadata(s,m);
	}

	//Função que retorna o chunkmanager de uma sessão
	public ChunkManager getChunkManager(int reqID){
		return this.chunkmanagers.get(reqID);
	}
	//Função que remove o chunkmanager de uma sessão
	public void removeChunkManager(int reqID){
		this.chunkmanagers.remove(reqID);
	}

	//Função que retorna o tamanho do ficheiro que vai ser/está guardado no chunkmanager
	public int getChunkManagerSize(int reqID){
		return this.chunkmanagers.get(reqID).getSize();
	}
	//Função que retorna a metadata do ficheiro que está guardado no chunkmanager
	public String getChunkManagerMetadata(int reqID){
		return this.chunkmanagers.get(reqID).getMetadata();
	}


	//Função que cria espaço para os chunks no chunkmanager
	// 1. Enquanto o tamanho atual (countsize) for menor que o tamanho do ficheiro tem de se colocar espaço para mais um chunk
	public void createChunksSpace(int reqID){
		int size = this.chunkmanagers.get(reqID).getSize();
		int key=0, countsize=0;
		
		while(countsize<size){
			this.chunkmanagers.get(reqID).setChunk(key,null);
			
			countsize+=MAXCHUNKSIZE;
			key+=1;
		}
	}


	//Função que pede os chunks do ficheiro e retorna true se os chunks já chegaram todos, false se ainda não chegaram
	// 1. Para cada entrada no mapa de chunks que está guardado no chunkmanager erifica-se se a variavel de bytes está a null
	// 2. Se estiver é alterado o pacote que é recebido nos argumentos para indicar qual é o d do chunk que falta
	// 3. o chunk é pedido
	// 4. Retorna-se se foi pedido algum chunk ou não
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



	//Função que coloca um chunk no chunkmanager
	// 1. se o chunkmanager com o id de sessão existir coloca-se lá o chunk
	public void setChunk(int reqID, int chunkid, byte[] bytes){
		if(this.chunkmanagers.get(reqID)!=null)
			this.chunkmanagers.get(reqID).setChunk(chunkid,bytes);
	}



	//Função que escreve no OutputStream do socket os bytes de cada chunk
	// 1. Para cada MapEntry escreve-se os chunks no out
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
	

}