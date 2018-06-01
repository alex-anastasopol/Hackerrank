package ro.cst.tsearch.search.bean;

import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.legal.TownShipI;

public class UnplattedSKLDIteratorEntry {
	
	private String arb;
	private String township;
	private String range;
	private String section;
	public String getArb() {
		return arb;
	}
	public void setArb(String arb) {
		this.arb = arb;
	}
	public String getTownship() {
		return township;
	}
	public void setTownship(String township) {
		this.township = township;
	}
	public String getRange() {
		return range;
	}
	public void setRange(String range) {
		this.range = range;
	}
	public String getSection() {
		return section;
	}
	public void setSection(String section) {
		this.section = section;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arb == null) ? 0 : arb.hashCode());
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((section == null) ? 0 : section.hashCode());
		result = prime * result
				+ ((township == null) ? 0 : township.hashCode());
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
		UnplattedSKLDIteratorEntry other = (UnplattedSKLDIteratorEntry) obj;
		if (arb == null) {
			if (other.arb != null)
				return false;
		} else if (!arb.equals(other.arb))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (section == null) {
			if (other.section != null)
				return false;
		} else if (!section.equals(other.section))
			return false;
		if (township == null) {
			if (other.township != null)
				return false;
		} else if (!township.equals(other.township))
			return false;
		return true;
	}
	public void setSubdivision(TownShipI townShipI) {
		if(townShipI != null) {
			arb = townShipI.getArb();
			township = townShipI.getTownship();
			range = townShipI.getRange();
			section = townShipI.getSection();
		}
		
	}
	public boolean isEntryValid() {
		return StringUtils.isNotEmpty(arb) || 
					StringUtils.isNotEmpty(township) || 
					StringUtils.isNotEmpty(range) || 
					StringUtils.isNotEmpty(section);
	}
	

}
