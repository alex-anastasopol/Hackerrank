package ro.cst.tsearch.data;

public class InvoiceSearchStatus {
	
	private int id = 0;
	private String name = "";
	
	public InvoiceSearchStatus(int idParam, String nameParam){
		id = idParam;
		name = nameParam;
	}
	
	/**
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
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
	public void setName(String string) {
		name = string;
	}

}
