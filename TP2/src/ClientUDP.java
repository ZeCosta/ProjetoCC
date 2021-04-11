import java.io.*;
import java.net.*;

class ClientUDP {
    public static void main(String args[]) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
                System.in));

        DatagramSocket clientSocket = new DatagramSocket();

        String servidor = "localhost";
        int porta = 8888;

        InetAddress IPAddress = InetAddress.getByName(servidor);

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        System.out.println("Escreve o que queres enviar ao servidor: ");
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData,
                sendData.length, IPAddress, porta);

        System.out
                .println("A enviar pacote UDP para " + servidor + ":" + porta);
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                receiveData.length);

        clientSocket.receive(receivePacket);
        System.out.println("Pacote UDP recebido...");

        String modifiedSentence = new String(receivePacket.getData());

        System.out.println("Texto recebido do servidor:" + modifiedSentence);
        clientSocket.close();
        System.out.println("Socket cliente fechado!");
    }
}