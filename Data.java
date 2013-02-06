import java.io.Serializable;

public class Data implements Serializable
{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	String msg;
	String key;
	String value;
	int src_port;
	String succ = null;
	String prev = null;
	String node;
	
	public Data(){}
	
	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getSrc_port() {
		return src_port;
	}

	public void setSrc_port(int src_port) {
		this.src_port = src_port;
	}

	public String getSucc() {
		return succ;
	}

	public void setSucc(String succ) {
		this.succ = succ;
	}

	public String getPrev() {
		return prev;
	}

	public void setPrev(String prev) {
		this.prev = prev;
	}
	
	public String getNode() {
		return node;
	}


	public void setNode(String node) {
		this.node = node;
	}

	
}
