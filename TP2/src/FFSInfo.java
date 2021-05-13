import java.net.InetAddress;

public class FFSInfo {
	InetAddress ip;
	int port;

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
}