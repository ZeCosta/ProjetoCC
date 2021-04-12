import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class myip {
    public static void main(String[] args) {
        String ip = new String();
        try{
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    System.out.println(iface.getDisplayName() + " " + ip);
                }
            }
            System.out.println(ip);
            try {
                System.out.println(InetAddress.getByName("127.0.0.1"));
            }catch(Exception e){
                System.out.println("BBBBBB");
            }

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        try {
            InetAddress iAddress = InetAddress.getLocalHost();
            String client_IP = iAddress.getHostAddress();
            System.out.println("Current IP address : " +client_IP);
        }catch(Exception e){
            System.out.println("AAAAAAAAAAA00");
        }
    }
}