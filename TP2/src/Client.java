import javax.swing.*;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Date;

public class Client {
    public static void main(String[] args){
        try{
            Socket client = new Socket("localhost",8080);
            ObjectInputStream entrada = new ObjectInputStream(client.getInputStream());
            Date dataAtual = (Date)entrada.readObject();
            JOptionPane.showMessageDialog(null,"Data recebida do servidor: " + dataAtual.toString());
            entrada.close();
            System.out.println("Conexão encerrada");

        } catch(Exception e){
            System.out.println("Erro: " + e.getMessage());
        }
    }
}
