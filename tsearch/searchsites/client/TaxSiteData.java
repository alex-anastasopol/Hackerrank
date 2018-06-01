package ro.cst.tsearch.searchsites.client;


public class TaxSiteData extends GWTDataSite{
	
	public static final int TAX_YEAR_PD_YEAR = 0;
	public static final int TAX_YEAR_PD_YEAR_MINUS_1 = 1;
	public static final int TAX_YEAR_PD_YEAR_PLUS_1 = 2;
	
	private String payDate = null;
	private String dueDate = null;
	private int taxYearMode = TAX_YEAR_PD_YEAR;
	protected String cityName = null;
	
	public TaxSiteData(){
		super();
	}
	
	public TaxSiteData(String stateAbrv, String countyName, String siteTypeAbrv,
			boolean isNewentry, int countyId) {
		super(stateAbrv, countyName, siteTypeAbrv, "", "", "", "", 0, 30000,
				900000, 0, 1000, countyId);
		super.isNewRow = isNewentry;
	}

	public TaxSiteData(String stateAbrv, String countyName, String siteTypeAbrv, int countyId) {
		super(stateAbrv, countyName, siteTypeAbrv, "", "", "", "", 0, 30000,
				900000, 0, 1000, countyId);
	}

	public String getPayDate() {
		return payDate;
	}

	public void setPayDate(String payDate) {
		this.payDate = payDate;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public int getTaxYearMode() {
		return taxYearMode;
	}

	public void setTaxYearMode(int taxYearMode) {
		this.taxYearMode = taxYearMode;
	}
	
}
