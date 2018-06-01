package ro.cst.tsearch.servers.bean;

import org.apache.commons.lang.builder.ToStringBuilder;

import ro.cst.tsearch.utils.StringUtils;

public class LegalDescription {
	// mandatory properties
	private String propertyId="";
	private String propertyType="";
	private DASLSimpleInstrumentInfo instrument;

	// optional attributes
	private String platBook="";
	private String platPage="";
	private String lot="";
	private String block="";

	private String lotThrough="";
	private String blockThrough="";

	public String getLotThrough() {
		return lotThrough;
	}

	public void setLotThrough(String lotThrough) {
		this.lotThrough = lotThrough;
	}

	public String getBlockThrough() {
		return blockThrough;
	}

	public void setBlockThrough(String blockThrough) {
		this.blockThrough = blockThrough;
	}

	private String acreage;
	private String platName;

	public static LegalDescription newInstance(DASLSimpleInstrumentInfo instrument, String propertyId,
			String propertyType) {
		boolean checkMandatoryAttributes = StringUtils.isNotEmpty(propertyId) && StringUtils.isNotEmpty(propertyType)
				&& instrument != null;
		LegalDescription legalDescription = null;
		if (checkMandatoryAttributes) {
			legalDescription = new LegalDescription();
			legalDescription.setPropertyId(propertyId);
			legalDescription.setPropertyType(propertyType);
			legalDescription.setInstrument(instrument);
		}
		return legalDescription;
	}

	public LegalDescription addFirstKey(String platBook, String platPage, String lot, String block) {
		this.setPlatBook(platBook);
		this.setPlatPage(platPage);
		this.setLot(lot);
		this.setBlock(block);
		return this;
	}

	public LegalDescription addSecondKey(String acreage, String platName) {
		this.setAcreage(acreage);
		this.setPlatName(platName);
		return this;
	}

	public boolean hasFirstKey() {
		boolean isFirstKey = isFirstKey();
		boolean isSecondKey = isSecondKey();
		return isFirstKey;
	}

	public boolean hasSecondKey() {
		boolean isFirstKey = isFirstKey();
		boolean isSecondKey = isSecondKey();
		return isSecondKey;
	}

	private boolean isFirstKey() {
		return StringUtils.isNotEmpty(getPlatBook()) || StringUtils.isNotEmpty(getPlatPage())
				|| StringUtils.isNotEmpty(getLot()) || StringUtils.isNotEmpty(getBlock());
	}

	private boolean isSecondKey() {
		return StringUtils.isNotEmpty(getAcreage()) || StringUtils.isNotEmpty(getPlatName());
	}

	public String getPropertyId() {
		return StringUtils.isNotEmpty(propertyId)?propertyId:"";
	}

	public void setPropertyId(String propertyId) {
		this.propertyId = propertyId;
	}

	public String getPropertyType() {
		return StringUtils.isNotEmpty(propertyType)?propertyType:"";
	}

	public void setPropertyType(String propertyType) {
		this.propertyType = propertyType;
	}

	public DASLSimpleInstrumentInfo getInstrument() {
		return instrument;
	}

	public void setInstrument(DASLSimpleInstrumentInfo instrument) {
		this.instrument = instrument;
	}

	public String getPlatBook() {
		return StringUtils.isNotEmpty(platBook)?platBook:"";
	}

	public void setPlatBook(String platBook) {
		this.platBook = platBook;
	}

	public String getPlatPage() {
		return StringUtils.isNotEmpty(platPage)?platPage:"";
	}

	public void setPlatPage(String platPage) {
		this.platPage = platPage;
	}

	public String getLot() {
		return StringUtils.isNotEmpty(lot)?lot:"";
	}

	public void setLot(String lot) {
		this.lot = lot;
	}

	public String getBlock() {
		return StringUtils.isNotEmpty(block)?block:"";
	}

	public void setBlock(String block) {
		this.block = block;
	}

	public String getAcreage() {
		return StringUtils.isNotEmpty(acreage)?acreage:"";
	}

	public void setAcreage(String acreage) {
		this.acreage = acreage;
	}

	public String getPlatName() {
		return StringUtils.isNotEmpty(platName)?platName:"";
	}

	public void setPlatName(String platName) {
		this.platName = platName;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("instrument",instrument).
		append("propertyId",propertyId).append("propertyType",propertyType).append("lot",lot).append("lotThrough",lotThrough).
		append("block",block).append("blockThrough",blockThrough).append("platBook",platBook).append("platPage",platPage).
		append("platName",platName).append("propertyType",propertyType).toString();
	}

	@Override
	public boolean equals(Object obj) {
		boolean returnValue = false;
		if (obj instanceof LegalDescription && obj!=null){
			LegalDescription  ld  = (LegalDescription) obj;;
			
			boolean bothWithFirstKey = this.hasFirstKey() && ld.hasFirstKey();
			boolean bothWithSecondKey = this.hasSecondKey() && ld.hasSecondKey();
			boolean bothKeys = bothWithFirstKey && bothWithSecondKey;
			
			if (bothKeys){
				return this.getInstrument().equals(ld.getInstrument()) && firstKeyEquals(ld)&& secondKeyEquals(ld);
			}
			else if (bothWithFirstKey){
				returnValue = this.getInstrument().equals(ld.getInstrument()) && firstKeyEquals(ld); 
			}else if (bothWithSecondKey){
				returnValue = this.getInstrument().equals(ld.getInstrument()) && secondKeyEquals(ld);
			}
		}
		
		
		return returnValue;
	}
	
	@Override
	public int hashCode() {
		if (isFirstKey()){
			return  5*getPlatBook().hashCode() + 4*getPlatPage().hashCode() + 3* getBlock().hashCode() + 2* getLot().hashCode() + getInstrument().hashCode() ; 
		}else if (isSecondKey()){
			return 3 * getPlatName().hashCode() + 2 * getAcreage().hashCode() + getInstrument().hashCode();
		}
		return getInstrument().hashCode();
	}

	private boolean secondKeyEquals(LegalDescription ld) {
		boolean acreageEquals  = StringUtils.specialStringCompare(this.getAcreage(), ld.getAcreage());
		boolean platNameEquals  = StringUtils.specialStringCompare(this.getPlatName(), ld.getPlatName());
		return this.getInstrument().equals(ld.getInstrument()) && acreageEquals && platNameEquals;
	}

	private boolean firstKeyEquals(LegalDescription ld) {
//		ld.getAcreage()
/*		boolean notEmptyThis = StringUtils.isNotEmpty(getPlatBook())  
				&& StringUtils.isNotEmpty(getPlatPage());
		
		boolean notEmptyThat = StringUtils.isNotEmpty(ld.getPlatBook())  
		&& StringUtils.isNotEmpty(ld.getPlatPage());
		
		if (notEmptyThis && notEmptyThat) {
			 boolean platBookPageEqual = this.getPlatBook().equals(ld.getPlatBook()) && this.getPlatPage().equals(ld.getPlatPage());
			 boolean lotEquals = (StringUtils.isNotEmpty(getLot()))?  getLot().equals( ld.getLot() ): false;
			 boolean blockEquals =(StringUtils.isNotEmpty(s) ) getBlock().equals( ld.getBlock() );
			 	 
		}
	*/
		
		boolean platBookEquals = StringUtils.specialStringCompare(ld.getPlatBook(), this.getPlatBook());
		boolean platPageEquals = StringUtils.specialStringCompare(ld.getPlatPage(), this.getPlatPage());
		boolean lotEquals = StringUtils.specialStringCompare(ld.getLot(), this.getLot());
		boolean blockEquals = StringUtils.specialStringCompare(ld.getBlock(), this.getBlock());
		if (platBookEquals&&platPageEquals&&lotEquals&&blockEquals){
			return true;
		}
//		StringUtils.isNotEmpty(getPlatBook()) || StringUtils.isNotEmpty(getPlatPage())
//		|| StringUtils.isNotEmpty(getLot()) || StringUtils.isNotEmpty(getBlock())

		return false;
	}
	
	
	/*
	 * public static class Builder { //required parameters private String
	 * propertyId; private String propertyType; private DASLSimpleInstrumentInfo
	 * instrument;
	 * 
	 * //optional parameters private String platBook; private String platPage;
	 * private String lot; private String block;
	 * 
	 * private String acreage; private String platName; public Builder(String
	 * propertyId, String propertyType, DASLSimpleInstrumentInfo instrument) {
	 * this.propertyId = propertyId; this.propertyType = propertyType;
	 * this.instrument = instrument; }
	 * 
	 * public Builder setPlatBook(String input){ platBook = input; return this;
	 * }
	 * 
	 * public Builder setPlatPage(String input){ platPage = input; return this;
	 * }
	 * 
	 * public Builder setLot(String input){ lot = input; return this; }
	 * 
	 * public Builder setAcreage(String input){ acreage = input; return this; }
	 * 
	 * public Builder setPlatName(String input){ platName = input; return this;
	 * } }
	 * 
	 * private LegalDescription(Builder builder){ instrument =
	 * builder.instrument; propertyId = builder.propertyId; propertyType =
	 * builder.propertyType;
	 * 
	 * }
	 */
}
