package ro.cst.tsearch.data;

public class GenericState extends GenericCounty {

	private long id = 0;
	private String name, stateAbv;
	private int stateFips;
	
	
	public int getStateFips() {
		return stateFips;
	}

	public void setStateFips(int stateFips) {
		this.stateFips = stateFips;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getStateAbv() {
		return stateAbv;
	}

	public void setId(long l) {
		id = l;
	}

	public void setName(String string) {
		name = string;
	}

	public void setStateAbv(String string) {
		stateAbv = string;
	}

}
