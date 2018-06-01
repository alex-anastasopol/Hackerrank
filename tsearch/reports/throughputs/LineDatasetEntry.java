package ro.cst.tsearch.reports.throughputs;

public class LineDatasetEntry {
	private String key;
	private Double value = new Double(0);
	private int numberOfElements = 0;
	public LineDatasetEntry(String key) {
		this.key = key;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public void updateInfo(long count, long value) {
		this.numberOfElements += count;
		this.value += value;
		
	}
	public double getSimpleLineValue(int denominator) {
		if(numberOfElements == 0 || denominator == 0) 
			return 0;
		return value/numberOfElements/denominator;
	}
	
}
