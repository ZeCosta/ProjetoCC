import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;

public class FFSInfo {
	InetAddress ip;
	int port;

	//Variaveis pensadas para o HttpGw poder dar Unsub nos FFS que não respondiam o suficiente
	int requested=0;
	int received=0;


	public FFSInfo(InetAddress i, int p){
		this.ip=i;
		this.port=p;
	}

	public InetAddress getIp(){
		return this.ip;
	}
	public int getPort(){
		return this.port;
	}

	public void setIp(InetAddress i){
		this.ip=i;
	}
	public void getPort(int p){
		this.port=p;
	}

	public boolean equals(FFSInfo f){
		return this.getIp().equals(f.getIp());// && this.getPort()==f.getPort();
	}







	//Não utilizado

	public boolean tooManyMisses(){
		return (this.received/this.requested)<75;
	}

	public void addRequested(){
		this.requested+=1;
	}
	public void addReceived(){
		this.received+=1;
	}

}