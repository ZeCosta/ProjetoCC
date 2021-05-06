import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PacketTest {
    public static void main(String[] args) {
    	try{
    		//readFileToByteArray
	    	String s = "ola";
	    	//File index = new File("index.html");
	    	//byte[] bt = readFileToByteArray(index);

			String filePath = "index.html";

			// file to byte[], Path
			byte[] bt= Files.readAllBytes(Paths.get(filePath));
			System.out.println(bt.length);


			PacketUDP pk = new PacketUDP(1,2,3,bt);


			FileOutputStream file = new FileOutputStream("test");
	        ObjectOutputStream out = new ObjectOutputStream(file);

	        // Method for serialization of object
	        out.writeObject(pk);

	        out.close();
	        file.close();
    	}catch(Exception e){
    		System.out.println("AAAAAAAAAA");
    	}


    	try{

			PacketUDP pk2;

			// Reading the object from a file
            FileInputStream file = new FileInputStream
                                         ("test");
            ObjectInputStream in = new ObjectInputStream
                                         (file);
  
            // Method for deserialization of object
            pk2 = (PacketUDP)in.readObject();
  
            in.close();
            file.close();
            System.out.println("Object has been deserialized\n"
                                + "Data after Deserialization.");
            //printdata(pk2);
            System.out.println(pk2.getPackettype());
            System.out.println(pk2.getPacketid());
            System.out.println(pk2.getChunkid());
            String output = new String(pk2.getChunk());
            System.out.println(output);
    	}catch(Exception e){
    		System.out.println("AAAAAAAAAA");
    	}
	}
}