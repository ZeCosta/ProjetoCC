import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class echoSV_udp{
	// Server UDP socket runs at this port
	public final static int SERVICE_PORT=8888;

	public static void main(String[] args){
		DatagramSocket socket;
		boolean running;

        try{
        	//DatagramSocket(int port, InetAddress laddr)
	        InetAddress addr =InetAddress.getByName("localhost");
			socket = new DatagramSocket(SERVICE_PORT,addr);

			running = true;
		    while (running) {
		    	byte[] buf = new byte[256];
		        DatagramPacket packet = new DatagramPacket(buf, buf.length);
		        socket.receive(packet);
		        
		        InetAddress address = packet.getAddress();
		        int port = packet.getPort();
		        packet = new DatagramPacket(buf, buf.length, address, port);
		        
		        System.out.println(packet.getData());
		        System.out.println(packet.getLength());
		        System.out.println();
		        String received = new String(packet.getData(), 0, packet.getLength());
		        
		        /*
			        //String teste = "end";
			        System.out.println(received + "!");
			        System.out.println(received.equals("end"));
			        //System.out.println(teste.equals("end"));
			        if (received.equals("end")) {
			            running = false;
			            System.out.println("AAAAAAA");
			            continue;
			        }
		        */

		        socket.send(packet);
	            System.out.println();
	            System.out.println();
	            System.out.println();
		    }
		    socket.close();
        
        }catch(Exception e){
            System.out.println("BBBBBB");
        }
	}
}