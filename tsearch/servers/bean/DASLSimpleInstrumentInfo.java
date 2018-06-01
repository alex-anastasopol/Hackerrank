package ro.cst.tsearch.servers.bean;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.utils.StringUtils;

public class DASLSimpleInstrumentInfo {
	private String book;
	private String page;
	private String intrumentNo;
	private String dateString;
	private Date date;
	private String type;
	private Calendar calendar;
	/**
	 * @return the book
	 */
	public String getBook() {
		return book;
	}
	/**
	 * @param book the book to set
	 */
	public void setBook(String book) {
		this.book = book;
	}
	/**
	 * @return the page
	 */
	public String getPage() {
		return page;
	}
	/**
	 * @param page the page to set
	 */
	public void setPage(String page) {
		this.page = page;
	}
	/**
	 * @return the intrumentNo
	 */
	public String getIntrumentNo() {
		return intrumentNo;
	}
	/**
	 * @param intrumentNo the intrumentNo to set
	 */
	public void setIntrumentNo(String intrumentNo) {
		this.intrumentNo = intrumentNo;
	}
	/**
	 * @return the date
	 */
	public Date getDate() {
		if(date!=null) {
			return date;
		} 
		try {
			if(getDateString() != null && !getDateString().isEmpty())
				date = SearchAttributes.DATE_FORMAT_MMddyyyy.parse(getDateString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return date;
		
	}
	
	public Calendar getCalendar() {
		if(calendar != null)
			return calendar;
		if(getDate() == null)
			return null;
		calendar = Calendar.getInstance();
		calendar.setTime(getDate());
		return calendar;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the dateString
	 */
	public String getDateString() {
		return dateString;
	}
	/**
	 * @param dateString the dateString to set
	 */
	public void setDateString(String dateString) {
		this.dateString = dateString;
	}
	
	public String getUniqueKey(){
		String key = getIntrumentNo();
		if(StringUtils.isEmpty(key)) {
			if(StringUtils.isNotEmpty(getBook()) && StringUtils.isNotEmpty(getPage())) {
				key = getBook() + "_" + getPage();
			} else {
				return null;
			}
		}
		if(StringUtils.isNotEmpty(getType())) {
			key += "_" + getType();
		}
		if(getCalendar() != null) {
			key += "_" + getCalendar().get(Calendar.YEAR);
		}
		return key;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DASLSimpleInstrumentInfo) {
			DASLSimpleInstrumentInfo inst = (DASLSimpleInstrumentInfo) obj;
			return  inst.getUniqueKey().equals(this.getUniqueKey()); 
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getUniqueKey().hashCode();
	}
	
	@Override
	public String toString() {
			return new ToStringBuilder(this).reflectionToString(this);
	}

}
