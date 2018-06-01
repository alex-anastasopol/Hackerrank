package ro.cst.tsearch.data;

public class CountyState {

	private long countyId = Long.MIN_VALUE;
	private String countyName = "";
	private String stateAbv = "";
	
	public long getCountyId() {
		return countyId;
	}
   
	public void setCountyId(long l) {
		countyId = l;
	}
	
	public String getCountyName() {
		return countyName;
	}
   
	public void setCountyName(String s) {
		countyName = s;
	}

	public String getStateAbv() {
		return stateAbv;
	}
   
	public void setStateAbv(String s) {
		stateAbv = s;
	}
	
	public String getCountyFullName() {
		return countyName + " " + stateAbv;
	}
	
}
