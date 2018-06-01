package ro.cst.tsearch.data;

public class SearchToArchive {

	private int id;
	private String TSRname;
	private String TSRlink;
	
	/**
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return
	 */
	public String getTSRlink() {
		return TSRlink;
	}

	/**
	 * @return
	 */
	public String getTSRname() {
		return TSRname;
	}

	/**
	 * @param i
	 */
	public void setId(int i) {
		id = i;
	}

	/**
	 * @param string
	 */
	public void setTSRlink(String string) {
		TSRlink = string;
	}

	/**
	 * @param string
	 */
	public void setTSRname(String string) {
		TSRname = string;
	}

	public String toString() {
		return "SearchToArchive: [ id=" + id + "; TSRLINK=" + TSRlink + "; TSRName=" + TSRname +"]";  
	}
}
