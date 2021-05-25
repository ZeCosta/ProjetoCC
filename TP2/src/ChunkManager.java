import java.util.Map;
import java.util.HashMap;
import java.util.Set;

// Objeto que guarda toda a informação relativa ao pedido do cliente

public class ChunkManager {
	String fileName;		//nome do ficheiro que o cliente pediu
	String fileMetadata;	//metadata 		"			"


	int fileSize;			//Tamanho do ficheiro
								//começa a 0
								//se for -1 significa que o ficheiro não existe
	
	//map de <chunkId,bytes desse chunk>
	Map<Integer, byte[]> chunks = new HashMap<Integer, byte[]>();

	//função para criar um ChunkManager
	public ChunkManager(String fn, int fs){
		this.fileName=fn;
		this.fileSize=fs; 
	}

	//Função para inserir um chunk
	public void setChunk(int i, byte[] c){
		this.chunks.put(i,c);
	}

	//Função que insere o tamanho e a metadata de um ficheiro
	public void setSizeAndMetadata(int s, String m){
		this.fileSize=s;
		this.fileMetadata=m;
	}

	//Função que retorna o tamanho de um ficheiro
	public int getSize(){
		return this.fileSize;
	}
	//Função que retorna a metadata de um ficheiro
	public String getMetadata(){
		return this.fileMetadata;
	}

	//Função que retorna um set de mapEntries dos chunks do ficheiro
	public Set<Map.Entry<Integer, byte[]>> getEntrySetChunks(){
		return this.chunks.entrySet();
	}

}