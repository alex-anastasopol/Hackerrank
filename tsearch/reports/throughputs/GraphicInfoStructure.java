package ro.cst.tsearch.reports.throughputs;

public class GraphicInfoStructure {
	
	private String id = null;
	private String realValue = null;
	
	public GraphicInfoStructure(String id, String realValue) {
		super();
		this.id = id;
		this.realValue = realValue;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the realValue
	 */
	public String getRealValue() {
		return realValue;
	}
	/**
	 * @param realValue the realValue to set
	 */
	public void setRealValue(String realValue) {
		this.realValue = realValue;
	}

}
