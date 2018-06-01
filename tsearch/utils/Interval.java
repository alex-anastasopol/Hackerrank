package ro.cst.tsearch.utils;

public class Interval {
	private String low;
	private String high;
	public String getLow() {
		return low;
	}
	public void setLow(String low) {
		this.low = low;
	}
	public String getHigh() {
		return high;
	}
	public void setHigh(String high) {
		this.high = high;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((high == null) ? 0 : high.hashCode());
		result = prime * result + ((low == null) ? 0 : low.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interval other = (Interval) obj;
		if (high == null) {
			if (other.high != null)
				return false;
		} else if (!high.equals(other.high))
			return false;
		if (low == null) {
			if (other.low != null)
				return false;
		} else if (!low.equals(other.low))
			return false;
		return true;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Interval [low=");
		builder.append(low);
		builder.append(", high=");
		builder.append(high);
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * For now supports only Integer intervals
	 * @param value to be checked
	 * @return <code>true</code> if the value is the same as one of the limits of the interval or between if the interval is Integer
	 */
	public boolean contains(String value){
		if(value == null) {
			return false;
		}
		if(value.equals(low) || value.equals(high)) {
			return true;
		}
		
		if(value.matches("\\d+")) {
			int valueInt = Integer.parseInt(value);
			int lowInt = -1;
			int highInt = -1;
			if(low != null && low.matches("\\d+")) {
				lowInt = Integer.parseInt(low);
			}
			if(high != null && high.matches("\\d+")){
				highInt = Integer.parseInt(high);
			}
			if(lowInt > 0 && highInt > 0){
				if(lowInt <= valueInt && valueInt <= highInt) {
					return true;
				}
			}
		}
		
		return false;
	}
	protected boolean justNextOfHigh(int value) {
		if(high != null) {
			try {
				int highValue = Integer.parseInt(high);
				if(highValue + 1 == value) {
					return true;
				}
			} catch (NumberFormatException e) {
			}
		} else {
			try {
				int lowValue = Integer.parseInt(low);
				if(lowValue + 1 == value) {
					return true;
				}
			} catch (NumberFormatException e) {
			}
		}
		return false;
		
	}
}
