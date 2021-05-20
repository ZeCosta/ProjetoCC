import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;

public class FFSInfo {
	InetAddress ip;
	int port;
	//HashSet<String> files = new HashSet<String>();

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
}