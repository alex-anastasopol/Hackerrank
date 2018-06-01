package ro.cst.tsearch.data;

public class GenericCounty implements Comparable {

	private long id = 0, stateId = 0;
	private String name;
	private String countyFips;
	
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getStateId() {
		return stateId;
	}

	public void setId(long l) {
		id = l;
	}

	public void setName(String string) {
		name = string;
	}

	public void setStateId(long l) {
		stateId = l;
	}

	public int compareTo(Object o) {
		
		return name.compareTo(  ((GenericCounty) o).name  ) ;
	}

	/**
	 * @return the countyFips
	 */
	public String getCountyFips() {
		return countyFips;
	}

	/**
	 * @param countyFips the countyFips to set
	 */
	public void setCountyFips(String countyFips) {
		this.countyFips = countyFips;
	}

}
