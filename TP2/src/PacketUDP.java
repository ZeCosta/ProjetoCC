import java.io.Serializable;


// o tamanho do packote em bytes deve ser <= 512bytes para não ser fragmentado
// depois de alguns testes descobrimos que no máximo o array para os chunks deveria ter 400bytes
// a password depois de feito o hash tem menos que 400bytes

public class PacketUDP implements Serializable{ 
	private int packettype;	//1-Subscribe|2-Unsubscribe|3-Size|4-Chunk
	private int packetid;	//ID do packet = ID da sessão TCP -> Necessário para guardar o Chunk no sitio certo no map
	private int chunkid;	//Se for um chunk vai ter de ter identificacao do chunk
							//Se for um pedido de size vai ter o tamanho do ficheiro -> -1 se mão existe
	private byte[] chunk;	//Chunk ou Metadata(String) ou password

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
	public void setChunk(byte[] bts){this.chunk=bts;}

	public PacketUDP clone(){
		return new PacketUDP(this.getPackettype(),this.getPacketid(),this.getChunkid(),this.getChunk());
	}
}