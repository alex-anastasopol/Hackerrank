package ro.cst.tsearch.reports.data;

public class DiscountData {
	private String[] searches ;
	private float discountValue ;
	private int commId ;
	private String page;
	private int fromDay;
	private int fromMonth;
	private int fromYear;
	private int toDay;
	private int toMonth;
	private int toYear;
	
	public int getCommId() {
		return commId;
	}
	public void setCommId(int commId) {
		this.commId = commId;
	}
	public float getDiscountValue() {
		return discountValue;
	}
	public void setDiscountValue(float discountValue) {
		this.discountValue = discountValue;
	}
	public int getFromDay() {
		return fromDay;
	}
	public void setFromDay(int fromDay) {
		this.fromDay = fromDay;
	}
	public int getFromMonth() {
		return fromMonth;
	}
	public void setFromMonth(int fromMonth) {
		this.fromMonth = fromMonth;
	}
	public int getFromYear() {
		return fromYear;
	}
	public void setFromYear(int fromYear) {
		this.fromYear = fromYear;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public String[] getSearches() {
		return searches;
	}
	public void setSearches(String[] searches) {
		this.searches = searches;
	}
	public int getToDay() {
		return toDay;
	}
	public void setToDay(int toDay) {
		this.toDay = toDay;
	}
	public int getToMonth() {
		return toMonth;
	}
	public void setToMonth(int toMonth) {
		this.toMonth = toMonth;
	}
	public int getToYear() {
		return toYear;
	}
	public void setToYear(int toYear) {
		this.toYear = toYear;
	}
	
}
