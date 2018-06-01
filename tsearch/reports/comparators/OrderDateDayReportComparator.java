package ro.cst.tsearch.reports.comparators;

import java.util.Comparator;

import ro.cst.tsearch.reports.data.DayReportLineData;

/**
 * Used to sort a DayReportLineData objects
 * @author aandrei
 *
 */
public class OrderDateDayReportComparator implements Comparator<DayReportLineData> {

	@Override
	public int compare(DayReportLineData o1, DayReportLineData o2) {
		if(o1 == null){
			if (o2 == null)
				return 0;
			else 
				return -1;
		} else {
			if(o2 == null)
				return 1;
			return o1.getSearchTimeStamp().compareTo(o2.getSearchTimeStamp());
		}
	}

}
