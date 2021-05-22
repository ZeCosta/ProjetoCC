import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class ChunkManager {
	//Lock + condition		-> Session asks for the size and waits. when received devides into x sized chunks and asks for them. Sleeps n time (depends on the number of chunks). evaluates if they're all there, if not asks again
	//name of file
	String fileName;
	//Size of file
	int fileSize;			//-> if its -1 it means the file does not exist
	//list/map of chunks 	-> If list/map size is size of file/size of chunks then its all there. alternatively Chunk can be a class with a boolean
	Map<Integer, byte[]> chunks = new HashMap<Integer, byte[]>();

	public ChunkManager(String fn, int fs){
		this.fileName=fn;
		this.fileSize=fs; 
	}

	public void setChunk(int i, byte[] c){
		this.chunks.put(i,c);
	}


	public int getSize(){
		return this.fileSize;
	}

	public Set<Map.Entry<Integer, byte[]>> getEntrySetChunks(){
		return this.chunks.entrySet();
	}

}