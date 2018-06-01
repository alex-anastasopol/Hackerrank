package ro.cst.tsearch.reports.throughputs;

public class BarDatasetEntry {
	private String key;
	private Double valueFirstColumn = new Double(0);
	private Double valueSecondColumn = new Double(0);
	public BarDatasetEntry(String key) {
		this.key = key;
	}
	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * @return the valueFirstColumn
	 */
	public Double getValueFirstColumn() {
		return valueFirstColumn;
	}
	/**
	 * @param valueFirstColumn the valueFirstColumn to set
	 */
	public void setValueFirstColumn(Double valueFirstColumn) {
		this.valueFirstColumn = valueFirstColumn;
	}
	/**
	 * @return the valueSecondColumn
	 */
	public Double getValueSecondColumn() {
		return valueSecondColumn;
	}
	/**
	 * @param valueSecondColumn the valueSecondColumn to set
	 */
	public void setValueSecondColumn(Double valueSecondColumn) {
		this.valueSecondColumn = valueSecondColumn;
	}
	
	public Double increaseValueFirstColumn(Number valueToIncrease) {
		if(valueFirstColumn == null) {
			valueFirstColumn = valueToIncrease.doubleValue();
		} else {
			valueFirstColumn += valueToIncrease.doubleValue();
		}
		return valueFirstColumn;
	}
	public Double increaseValueSecondColumn(Number valueToIncrease) {
		if(valueSecondColumn == null) {
			valueSecondColumn = valueToIncrease.doubleValue();
		} else {
			valueSecondColumn += valueToIncrease.doubleValue();
		}
		return valueSecondColumn;
	}

}
