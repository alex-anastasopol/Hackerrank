package ro.cst.tsearch.search.iterator.data;

import ro.cst.tsearch.utils.StringUtils;

public class LegalStruct implements Cloneable{
	
	public boolean sectional = false;
	
	private String lot		=	"";
	private String block		=	"";
	private String platBook	=	"";
	private String platPage	=	"";
	private String platBookType 	= 	"";		//like a source or something...
	private String platBookSuffix = null;
	private String platPageSuffix = null;
	
	
	private String platInst	=	"";
	private String platYear = null;
	private String platInstType = null;
	private String platInstSuffix = null;
	private String platInstCode = null;
	
	private String section	=	"";
	private String township	=	"";
	private String range	=	"";
	private String arb		=	"";
	private String addition	=	"";
	private String parcel	=	"";
	private String quarterOrder	=	"";
	private String quarterValue	=	"";
	
	private String abs	=	"";
	private String unit	=	"";
	private String tract	=	"";
	private String district	=	"";
	
	
	public LegalStruct(boolean isSectional){
		sectional = isSectional;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addition == null) ? 0 : addition.hashCode());
		result = prime * result + ((getArb() == null) ? 0 : getArb().hashCode());
		result = prime * result + ((block == null) ? 0 : block.hashCode());
		result = prime * result + ((lot == null) ? 0 : lot.hashCode());
		result = prime * result + ((getParcel() == null) ? 0 : getParcel().hashCode());
		result = prime * result + ((getPlatBook() == null) ? 0 : getPlatBook().hashCode());
		result = prime * result + ((platBookSuffix == null) ? 0 : platBookSuffix.hashCode());
		result = prime * result + ((getPlatInst() == null) ? 0 : getPlatInst().hashCode());
		result = prime * result + ((platInstCode == null) ? 0 : platInstCode.hashCode());
		result = prime * result + ((platInstSuffix == null) ? 0 : platInstSuffix.hashCode());
		result = prime * result + ((platInstType == null) ? 0 : platInstType.hashCode());
		result = prime * result + ((getPlatPage() == null) ? 0 : getPlatPage().hashCode());
		result = prime * result + ((platPageSuffix == null) ? 0 : platPageSuffix.hashCode());
		result = prime * result + ((getPlatBookType() == null) ? 0 : getPlatBookType().hashCode());
		result = prime * result + ((platYear == null) ? 0 : platYear.hashCode());
		result = prime * result + ((getQuarterOrder() == null) ? 0 : getQuarterOrder().hashCode());
		result = prime * result + ((getQuarterValue() == null) ? 0 : getQuarterValue().hashCode());
		result = prime * result + ((getRange() == null) ? 0 : getRange().hashCode());
		result = prime * result + ((getSection() == null) ? 0 : getSection().hashCode());
		result = prime * result + (sectional ? 1231 : 1237);
		result = prime * result + ((getTownship() == null) ? 0 : getTownship().hashCode());
		result = prime * result + ((getAbs() == null) ? 0 : getAbs().hashCode());
		result = prime * result + ((getUnit() == null) ? 0 : getUnit().hashCode());
		result = prime * result + ((getTract() == null) ? 0 : getTract().hashCode());
		result = prime * result + ((getDistrict() == null) ? 0 : getDistrict().hashCode());
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
		LegalStruct other = (LegalStruct) obj;
		if (addition == null) {
			if (other.addition != null)
				return false;
		} else if (!addition.equals(other.addition))
			return false;
		if (getArb() == null) {
			if (other.getArb() != null)
				return false;
		} else if (!getArb().equals(other.getArb()))
			return false;
		if (block == null) {
			if (other.block != null)
				return false;
		} else if (!block.equals(other.block))
			return false;
		if (lot == null) {
			if (other.lot != null)
				return false;
		} else if (!lot.equals(other.lot))
			return false;
		if (getParcel() == null) {
			if (other.getParcel() != null)
				return false;
		} else if (!getParcel().equals(other.getParcel()))
			return false;
		if (getPlatBook() == null) {
			if (other.getPlatBook() != null)
				return false;
		} else if (!getPlatBook().equals(other.getPlatBook()))
			return false;
		if (platBookSuffix == null) {
			if (other.platBookSuffix != null)
				return false;
		} else if (!platBookSuffix.equals(other.platBookSuffix))
			return false;
		if (getPlatInst() == null) {
			if (other.getPlatInst() != null)
				return false;
		} else if (!getPlatInst().equals(other.getPlatInst()))
			return false;
		if (platInstCode == null) {
			if (other.platInstCode != null)
				return false;
		} else if (!platInstCode.equals(other.platInstCode))
			return false;
		if (platInstSuffix == null) {
			if (other.platInstSuffix != null)
				return false;
		} else if (!platInstSuffix.equals(other.platInstSuffix))
			return false;
		if (platInstType == null) {
			if (other.platInstType != null)
				return false;
		} else if (!platInstType.equals(other.platInstType))
			return false;
		if (getPlatPage() == null) {
			if (other.getPlatPage() != null)
				return false;
		} else if (!getPlatPage().equals(other.getPlatPage()))
			return false;
		if (platPageSuffix == null) {
			if (other.platPageSuffix != null)
				return false;
		} else if (!platPageSuffix.equals(other.platPageSuffix))
			return false;
		if (getPlatBookType() == null) {
			if (other.getPlatBookType() != null)
				return false;
		} else if (!getPlatBookType().equals(other.getPlatBookType()))
			return false;
		if (platYear == null) {
			if (other.platYear != null)
				return false;
		} else if (!platYear.equals(other.platYear))
			return false;
		if (getQuarterOrder() == null) {
			if (other.getQuarterOrder() != null)
				return false;
		} else if (!getQuarterOrder().equals(other.getQuarterOrder()))
			return false;
		if (getQuarterValue() == null) {
			if (other.getQuarterValue() != null)
				return false;
		} else if (!getQuarterValue().equals(other.getQuarterValue()))
			return false;
		if (getRange() == null) {
			if (other.getRange() != null)
				return false;
		} else if (!getRange().equals(other.getRange()))
			return false;
		if (getSection() == null) {
			if (other.getSection() != null)
				return false;
		} else if (!getSection().equals(other.getSection()))
			return false;
		if (sectional != other.sectional)
			return false;
		if (getTownship() == null) {
			if (other.getTownship() != null)
				return false;
		} else if (!getTownship().equals(other.getTownship()))
			return false;
		if (!getAbs().equals(other.getAbs())){
			return false;
		}
		if (!getUnit().equals(other.getUnit())){
			return false;
		}
		if (!getTract().equals(other.getTract())){
			return false;
		}
		if (!getDistrict().equals(other.getDistrict())){
			return false;
		}
		return true;
	}

	public boolean isPlated() {
		return !StringUtils.isEmpty(getPlatBook())&&!StringUtils.isEmpty(getPlatPage())&&(!StringUtils.isEmpty(getLot())||!StringUtils.isEmpty(getBlock()));
	}
	
	public boolean isSubdivision() {
		return !StringUtils.isEmpty(getAddition()) && (!StringUtils.isEmpty(getLot())||!StringUtils.isEmpty(getBlock()));
	}

	public boolean isArb() {
		return !StringUtils.isEmpty(getSection())&&!StringUtils.isEmpty(getTownship())&&!StringUtils.isEmpty(getRange())&&!StringUtils.isEmpty(getQuarterOrder())
		&&!StringUtils.isEmpty(getArb());
	}

	public boolean isSectional() {
		return !StringUtils.isEmpty(getSection())&&!StringUtils.isEmpty(getTownship())&&!StringUtils.isEmpty(getRange())&&!StringUtils.isEmpty(getQuarterOrder())
		&&StringUtils.isEmpty(getArb());
	}
	
	public boolean isAbs(){
		return StringUtils.isNotEmpty(getAbs());
	}

	public String getLot() {
		return lot;
	}

	public void setLot(String lot) {
		if(lot == null) {
			this.lot = "";
		} else if("ALL".equalsIgnoreCase(lot.trim())) {
			this.lot = "";
		} else {
			this.lot = lot.trim();
		}
	}

	public String getBlock() {
		return block;
	}

	public void setBlock(String block) {
		if(block == null) {
			this.block = "";
		} else if("ALL".equalsIgnoreCase(block.trim())) {
			this.block = "";
		} else {
			this.block = block.trim();
		}
	}

	public String getAddition() {
		return addition;
	}

	public void setAddition(String addition) {
		if(addition != null) {
			addition = addition.toUpperCase();
		}
		this.addition = addition;
	}

	public String getPlatBook() {
		return platBook;
	}

	public void setPlatBook(String platBook) {
		this.platBook = platBook;
	}

	public String getPlatPage() {
		return platPage;
	}

	public void setPlatPage(String platPage) {
		this.platPage = platPage;
	}

	public String getPlatBookType() {
		return platBookType;
	}

	public void setPlatBookType(String platBookType) {
		this.platBookType = platBookType;
	}

	public String getPlatInst() {
		return platInst;
	}

	public void setPlatInst(String platInst) {
		this.platInst = platInst;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
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

	public String getArb() {
		return arb;
	}

	public void setArb(String arb) {
		this.arb = arb;
	}

	public String getParcel() {
		return parcel;
	}

	public void setParcel(String parcel) {
		this.parcel = parcel;
	}

	public String getQuarterOrder() {
		return quarterOrder;
	}

	public void setQuarterOrder(String quarterOrder) {
		this.quarterOrder = quarterOrder;
	}

	public String getQuarterValue() {
		return quarterValue;
	}

	public void setQuarterValue(String quarterValue) {
		this.quarterValue = quarterValue;
	}

	public String getPlatBookSuffix() {
		return platBookSuffix;
	}

	public void setPlatBookSuffix(String platBookSuffix) {
		this.platBookSuffix = platBookSuffix;
	}

	public String getPlatPageSuffix() {
		return platPageSuffix;
	}

	public void setPlatPageSuffix(String platPageSuffix) {
		this.platPageSuffix = platPageSuffix;
	}

	public String getPlatYear() {
		return platYear;
	}

	public void setPlatYear(String platYear) {
		this.platYear = platYear;
	}

	public String getPlatInstType() {
		return platInstType;
	}

	public void setPlatInstType(String platInstType) {
		this.platInstType = platInstType;
	}

	public String getPlatInstSuffix() {
		return platInstSuffix;
	}

	public void setPlatInstSuffix(String platInstSuffix) {
		this.platInstSuffix = platInstSuffix;
	}

	public String getPlatInstCode() {
		return platInstCode;
	}

	public void setPlatInstCode(String platInstCode) {
		this.platInstCode = platInstCode;
	}

	public void setSectional(boolean sectional) {
		this.sectional = sectional;
	}

	/**
	 * @return the tract
	 */
	public String getTract(){
		return tract;
	}

	/**
	 * @param tract the tract to set
	 */
	public void setTract(String tract){
		if (tract == null){
			tract = "";
		}
		this.tract = tract;
	}
	
	/**
	 * @return the district
	 */
	public String getDistrict(){
		return district;
	}

	/**
	 * @param district the district to set
	 */
	public void setDistrict(String district){
		if (district == null){
			district = "";
		}
		this.district = district;
	}
	
	/**
	 * @return the abs
	 */
	public String getAbs(){
		return abs;
	}

	/**
	 * @param abs the abs to set
	 */
	public void setAbs(String abs){
		if (abs == null){
			abs = "";
		}
		this.abs = abs;
	}

	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * @param unit the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}
}