import java.io.Serializable;



public class PacketUDP implements Serializable{ 
	private int packettype; //1-Subscribe|2-Unsubscribe|3-Size|4-Chunk
	private int packetid;  //Se for um chunk vai ter de ter id
	private int chunkid;  //Se for um chunk vai ter de ter identificacao do chunk
	private byte[] chunk;

	public PacketUDP(){
		this.packettype=-1;
		this.packetid=-1;
		this.chunkid=-1;
		this.chunk=null;
	}
	public PacketUDP(int pt, int pi, int ci, byte[] bt){
		this.packettype=pt;
		this.packetid=pi;
		this.chunkid=ci;
		this.chunk=bt;
	} 

	public int getPackettype(){return this.packettype;}
	public int getPacketid(){return this.packetid;}
	public int getChunkid(){return this.chunkid;}
	public byte[] getChunk(){return this.chunk;}


	public void setPackettype(int pt){this.packettype=pt;}
	public void setPacketid(int pi){this.packetid=pi;}
	public void setChunkid(int ci){this.chunkid=ci;}
	public void setchunk(byte[] bts){this.chunk=bts;}



}