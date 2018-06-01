package ro.cst.tsearch.propertyInformation;

public class PossessionEntity {

	private long id;
	private String addressNo;
	private String addressDirection;
	private String addressName;
	private String addressSuffix;
	private String addressUnit;
	private String addressCity;
	
	private long countyId;
	private long stateId;
	private String addressZip;
	
	private String legalInstrument;
	private String legalParcelId;
	private String legalPlatBook;
	private String legalPage;
	private String legalSubdivision;
	private String legalLotNo;

	private int isBootstrapped;
	
	public String getAddressCity() {
		return addressCity;
	}

	public String getAddressDirection() {
		return addressDirection;
	}

	public String getAddressName() {
		return addressName;
	}

	public String getAddressNo() {
		return addressNo;
	}

	public String getAddressSuffix() {
		return addressSuffix;
	}

	public String getAddressUnit() {
		return addressUnit;
	}

	public String getAddressZip() {
		return addressZip;
	}

	public long getCountyId() {
		return countyId;
	}

	public long getId() {
		return id;
	}

	public String getLegalInstrument() {
		return legalInstrument;
	}

	public String getLegalLotNo() {
		return legalLotNo;
	}

	public String getLegalPage() {
		return legalPage;
	}

	public String getLegalParcelId() {
		return legalParcelId;
	}

	public String getLegalPlatBook() {
		return legalPlatBook;
	}

	public String getLegalSubdivision() {
		return legalSubdivision;
	}

	public long getStateId() {
		return stateId;
	}

	public void setAddressCity(String string) {
		addressCity = string;
	}

	public void setAddressDirection(String string) {
		addressDirection = string;
	}

	public void setAddressName(String string) {
		addressName = string;
	}

	public void setAddressNo(String string) {
		addressNo = string;
	}

	public void setAddressSuffix(String string) {
		addressSuffix = string;
	}

	public void setAddressUnit(String string) {
		addressUnit = string;
	}

	public void setAddressZip(String string) {
		addressZip = string;
	}

	public void setCountyId(long l) {
		countyId = l;
	}

	public void setId(long l) {
		id = l;
	}

	public void setLegalInstrument(String string) {
		legalInstrument = string;
	}

	public void setLegalLotNo(String string) {
		legalLotNo = string;
	}

	public void setLegalPage(String string) {
		legalPage = string;
	}

	public void setLegalParcelId(String string) {
		legalParcelId = string;
	}

	public void setLegalPlatBook(String string) {
		legalPlatBook = string;
	}

	public void setLegalSubdivision(String string) {
		legalSubdivision = string;
	}

	public void setStateId(long l) {
		stateId = l;
	}

	public int getIsBootstrapped() {
		return isBootstrapped;
	}

	public void setIsBootstrapped(int isBootstrapped) {
		this.isBootstrapped = isBootstrapped;
	}

	
}
