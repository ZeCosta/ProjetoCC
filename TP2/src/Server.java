import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server {
    public static void main(String[] args) throws IOException {
        try{
            // cria serversocket a escutar porta 8080 (TCP)
            ServerSocket server = new ServerSocket(8080);
            System.out.println("Servidor está a escutar a porta 8080");

            while(true){
                // comunicação com o cliente (execução bloqueada até que o servidor
                // reveba um pedido de conexão
                Socket client = server.accept();
                System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());
                ObjectOutputStream saida = new ObjectOutputStream(client.getOutputStream());
                saida.flush();
                saida.writeObject(new Date());
                saida.close();
                client.close();
            }

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }

    }

}
