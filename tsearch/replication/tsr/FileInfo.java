package ro.cst.tsearch.replication.tsr;

/**
 * Immutable class holding file info
 * @author radu bacrau
 */
public class FileInfo {
	
	public final int id;
	public final String name;
	public final int size;
	public final String timestamp;

	public FileInfo(int id, String name, int size, String timestamp){
		this.id  = id;
		this.name = name;
		this.size = size;	
		this.timestamp = timestamp;
	}
	
	public String toString() {
		return "FileInf(id=" + id + ",name=" + name + ",size=" + size + ",timestamp=" + timestamp + ")";
	}
}