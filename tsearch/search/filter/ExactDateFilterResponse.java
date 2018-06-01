package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 22, 2011
 */

/**
 * filters instruments by recorded date or instrument date against the filterDates list, only exact matches are retained
 * 
 */
public class ExactDateFilterResponse extends FilterResponse {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -8330191294358980077L;

	private ArrayList<Calendar>	filterDates			= new ArrayList<Calendar>();

	private boolean				withRecordedDate	= true;
	private boolean				withInstrumentDate	= true;
	private boolean				justYear			= false;

	public ExactDateFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.90"));
	}

	public ExactDateFilterResponse(long searchId, Date filterDate) {
		super(searchId);
		setThreshold(new BigDecimal("0.90"));
		Calendar c = Calendar.getInstance();
		c.setTime(filterDate);
		this.filterDates.add(c);
	}

	public ExactDateFilterResponse(long searchId, Date filterDate, boolean withRecordedDate, boolean withInstrumentDate, boolean justYear) {
		super(searchId);
		setThreshold(new BigDecimal("0.90"));
		Calendar c = Calendar.getInstance();
		c.setTime(filterDate);
		this.filterDates.add(c);
		this.withInstrumentDate = withInstrumentDate;
		this.withRecordedDate = withRecordedDate;
		this.justYear = justYear;
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		// pass if no sale data set
		if (row.getSaleDataSet() == null || row.getSaleDataSet().size() == 0 || filterDates.isEmpty()) {
			return ATSDecimalNumberFormat.ONE;
		}
		SaleDataSet sds = (SaleDataSet) row.getSaleDataSet().elementAt(0);
		// check interval
		Date recDate = Util.dateParser3(sds.getAtribute("RecordedDate"));
		Date insDate = Util.dateParser3(sds.getAtribute("InstrumentDate"));

		for (Calendar filterDate : filterDates) {
			if (filterDate == null)
				return BigDecimal.ONE;

			int year = filterDate.get(Calendar.YEAR);
			int month = filterDate.get(Calendar.MONTH);
			int day = filterDate.get(Calendar.DAY_OF_MONTH);

			if (withRecordedDate && recDate != null) {
				Calendar c = Calendar.getInstance();
				c.setTime(recDate);

				if (justYear) {
					if (c.get(Calendar.YEAR) == year)
						return BigDecimal.ONE;
				} else {
					if (c.get(Calendar.DAY_OF_MONTH) == day && c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year)
						return BigDecimal.ONE;
				}
			}

			if (withInstrumentDate && insDate != null) {
				Calendar c = Calendar.getInstance();
				c.setTime(insDate);

				if (justYear) {
					if (c.get(Calendar.YEAR) == year)
						return BigDecimal.ONE;
				} else {
					if (c.get(Calendar.DAY_OF_MONTH) == day && c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year)
						return BigDecimal.ONE;
				}
			}
		}
		return BigDecimal.ZERO;
	}

	@Override
	public String getFilterCriteria() {
		
		if(filterDates == null || filterDates.isEmpty()) {
			return "exact date. No reference date. All will pass";
		}
		
		StringBuffer sb = new StringBuffer();
		if (!justYear) {
			for (Calendar c : filterDates) {
				sb.append(c.get(Calendar.MONTH) + 1 + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR) + ", ");
			}
			sb.delete(sb.length() - 2, sb.length());
			return "exact date(s):" + sb.toString();
		} else {
			for (Calendar c : filterDates) {
				sb.append(c.get(Calendar.YEAR) + ", ");
			}
			sb.delete(sb.length() - 2, sb.length());
			return "exact date(s) (just year(s)): " + sb.toString();
		}
	}

	public void addFilterDate(Date filterDate) {
		if (filterDate != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(filterDate);
			if(!this.filterDates.contains(c)) {
				this.filterDates.add(c);
			}
		}
	}

	public ArrayList<Calendar> getFilterDates() {
		return filterDates;
	}

	public void setWithRecordedDate(boolean withRecordedDate) {
		this.withRecordedDate = withRecordedDate;
	}

	public boolean isWithRecordedDate() {
		return withRecordedDate;
	}

	public void setWithInstrumentDate(boolean withInstrumentDate) {
		this.withInstrumentDate = withInstrumentDate;
	}

	public boolean isWithInstrumentDate() {
		return withInstrumentDate;
	}

	public void setJustYear(boolean justYear) {
		this.justYear = justYear;
	}

	public boolean isJustYear() {
		return justYear;
	}
}
