package ro.cst.tsearch.search.iterator.data;

import ro.cst.tsearch.utils.StringUtils;

public class LegalStructDTG implements Cloneable{
	
	private String type 			= "";
	private String block 			= "";
	private String unit 			= "";
	private String platBook			= "";
	private String platPage			= "";
	private String platInst			= "";
	private String section			= "";
	private String township			= "";
	private String range			= "";
	private String arb				= "";
	private String arbLot			= "";
	private String arbBlock			= "";
	private String arbBook			= "";
	private String arbPage			= "";
	private String arbDtrct 		= "";
	private String arbParcel		= "";
	private String arbParcelSplit 	= "";
	private String quarterOrder		= "";
	private String quarterValue		= "";
	private String lot				= "";
	private String lotThru 			= "";
	private String subLot 			= "";
	private String platInstrYear 	= "";
	private String ncbNumber 		= "";
	private String subdivisionName 	= "";
	private String tract 			= "";
	
	public LegalStructDTG(){}
	
	public LegalStructDTG(String type){
		this.setType(type);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	private static boolean equalsIgnoreStartLetter(char letter, String a, String b){
		String letString = (letter + "").toUpperCase();
		a = a.toUpperCase();
		b = b.toUpperCase();
		
		if (a.equals(b)){
			return true;
		}
		
		if (a.length() > 1 && a.startsWith(letString)){
			a = a.substring(1);
		}
		
		if (b.length() > 1 && b.startsWith(letString)){
			b = b.substring(1);
		}
		
		return a.equals(b);
	}
	
	public boolean equalsSubdivided(LegalStructDTG struct) {
		boolean checkSublot = true;
		
		//ignore sublot equivalence if any of the sublots is empty
		if (StringUtils.isNotEmpty(struct.getSubLot()) && StringUtils.isNotEmpty(this.getSubLot()))
			checkSublot = this.getSubLot().equals(struct.getSubLot());
			
		return 
			equalsIgnoreStartLetter('B', this.block, struct.block)
			&& (equalsIgnoreStartLetter('L', this.lot, struct.lot) || equalsIgnoreStartLetter('U', this.lot, struct.lot))
			&& (equalsIgnoreStartLetter('P', this.getPlatBook(), struct.getPlatBook()) 
					|| equalsIgnoreStartLetter('C', this.getPlatBook(), struct.getPlatBook())
					|| equalsIgnoreStartLetter('U', this.getPlatBook(), struct.getPlatBook()))
			&& this.getPlatPage().equals(struct.getPlatPage())
			&& this.getUnit().equals(struct.getUnit())
			&& checkSublot
			&& this.getPlatInstrYear().equals(struct.getPlatInstrYear());
	}
	
	public boolean equalsStrictSubdivided(LegalStructDTG struct) {
		boolean checkSublot = true;
		
		//ignore sublot equivalence if any of the sublots is empty
		if (StringUtils.isNotEmpty(struct.getSubLot()) && StringUtils.isNotEmpty(this.getSubLot()))
			checkSublot = this.getSubLot().equals(struct.getSubLot());
			
		return 
			this.block.equals(struct.block)
			&&	(this.lot.equals(struct.lot) || this.lot.equals(struct.lot))
			&&	(this.getPlatBook().equals(struct.getPlatBook()) || this.getPlatBook().equals(struct.getPlatBook())
					|| this.getPlatBook().equals(struct.getPlatBook()))
			&& this.getPlatPage().equals(struct.getPlatPage())
			&& this.getUnit().equals(struct.getUnit())
			&& checkSublot
			&& this.getPlatInstrYear().equals(struct.getPlatInstrYear());
	}
	
	public boolean equalsSectional(LegalStructDTG struct) {
		return this.getSection().equals(struct.getSection())
				&& this.getTownship().equals(struct.getTownship())
				&& this.getRange().equals(struct.getRange())
				&& this.getQuarterOrder().equals(struct.getQuarterOrder())
				&& this.getQuarterValue().equals(struct.getQuarterValue());
	}
	
	public boolean equalsSectionalAndPlat(LegalStructDTG struct) {
		return this.getSection().equals(struct.getSection()) 
				&& this.getTownship().equals(struct.getTownship()) 
				&& this.getRange().equals(struct.getRange())
				&& this.getQuarterOrder().equals(struct.getQuarterOrder()) 
				&& this.getQuarterValue().equals(struct.getQuarterValue())
				&& this.platBook.equals(struct.platBook) 
				&& this.platPage.equals(struct.platPage);
	}
	
	public boolean equalsArb(LegalStructDTG struct) {
		return this.getSection().equals(struct.getSection())
				&& this.getTownship().equals(struct.getTownship())
				&& this.getRange().equals(struct.getRange())
				&& this.getQuarterOrder().equals(struct.getQuarterOrder())
				&& this.getArb().equals(struct.getArb());
	}

	public boolean equalsArbExtended(LegalStructDTG struct) {
		return this.getArbBlock().equals(struct.getArbBlock())
				&& this.getArbLot().equals(struct.getArbLot())
				&& this.getArbBook().equals(struct.getArbBook())
				&& this.getArbPage().equals(struct.getArbPage());
	}
	
	public boolean isPlated() {
		return !StringUtils.isEmpty(getPlatBook())
				&& !StringUtils.isEmpty(getPlatPage())
				&& (!StringUtils.isEmpty(lot) || !StringUtils.isEmpty(block) || !StringUtils.isEmpty(getSubLot()));
	}

	public boolean isArb() {
		return !StringUtils.isEmpty(getSection())
				&& !StringUtils.isEmpty(getTownship())
				&& !StringUtils.isEmpty(getRange())
				&& !StringUtils.isEmpty(getQuarterOrder())
				&& !StringUtils.isEmpty(getArb());
	}

	public boolean isArbOnly() {
		return StringUtils.isEmpty(getSection())
				&& StringUtils.isEmpty(getTownship())
				&& StringUtils.isEmpty(getRange())
				&& StringUtils.isEmpty(getQuarterOrder())
				&& StringUtils.isNotEmpty(getArb());
	}
	
	public boolean isArbExtended() {
		return !StringUtils.isEmpty(getArbBlock()) && !StringUtils.isEmpty(getArbLot()) && !StringUtils.isEmpty(getArbBook()) && !StringUtils.isEmpty(getArbPage());
	}
	
	public boolean isSectional() {
		return !StringUtils.isEmpty(getSection())
				&& !StringUtils.isEmpty(getTownship())
				&& !StringUtils.isEmpty(getRange())
				&& !StringUtils.isEmpty(getQuarterOrder())
				&& StringUtils.isEmpty(getArb());
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

	public String getBlock() {
		return block;
	}

	public void setBlock(String block) {
		this.block = block;
	}

	public String getNcbNumber() {
		return ncbNumber;
	}

	public void setNcbNumber(String ncbNumber) {
		this.ncbNumber = ncbNumber;
	}
	
	public String getTract(){
		return tract;
	}
	
	public void setTract(String tract){
		this.tract = tract;
	}
	
	public String getArbDtrct(){
		return arbDtrct;
	}
	
	public void setArbDtrct(String arbDtrct){
		this.arbDtrct = arbDtrct;
	}
	
	public String getArbParcel(){
		return arbParcel;
	}
	
	public void setArbParcel(String arbParcel){
		this.arbParcel = arbParcel;
	}
	
	public String getArbParcelSplit(){
		return arbParcelSplit;
	}
	
	public void setArbParcelSplit(String arbParcelSplit){
		this.arbParcelSplit = arbParcelSplit;
	}
	
	public String getSubdivisionName() {
		return subdivisionName;
	}

	public void setSubdivisionName(String subdivisionName) {
		this.subdivisionName = subdivisionName;
	}
	
	public String getLot() {
		return lot;
	}

	public void setLot(String lot) {
		this.lot = lot;
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

	public String getArb() {
		return arb;
	}

	public void setArb(String arb) {
		this.arb = arb;
	}

	public String getLotThru() {
		return lotThru;
	}

	public void setLotThru(String lotThru) {
		this.lotThru = lotThru;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPlatInst() {
		return platInst;
	}

	public void setPlatInst(String platInst) {
		this.platInst = platInst;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getArbLot() {
		return arbLot;
	}

	public void setArbLot(String arbLot) {
		this.arbLot = arbLot;
	}

	public String getArbBlock() {
		return arbBlock;
	}

	public void setArbBlock(String arbBlock) {
		this.arbBlock = arbBlock;
	}

	public String getArbBook() {
		return arbBook;
	}

	public void setArbBook(String arbBook) {
		this.arbBook = arbBook;
	}

	public String getArbPage() {
		return arbPage;
	}

	public void setArbPage(String arbPage) {
		this.arbPage = arbPage;
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

	public String getSubLot() {
		return subLot;
	}

	public void setSubLot(String subLot) {
		this.subLot = subLot;
	}

	public String getPlatInstrYear() {
		return platInstrYear;
	}

	public void setPlatInstrYear(String platInstrYear) {
		this.platInstrYear = platInstrYear;
	}
}