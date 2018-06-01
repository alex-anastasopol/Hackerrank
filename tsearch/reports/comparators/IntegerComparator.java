package ro.cst.tsearch.reports.comparators;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

public class IntegerComparator implements Comparator<String> {

	@Override
	public int compare(String arg0, String arg1) {
		if (StringUtils.isEmpty(arg0) || StringUtils.isEmpty(arg1)){
			return 0;
		}
		int a = !StringUtils.isNumeric(arg0) ? Integer.MAX_VALUE
				: Integer.valueOf(arg0);
		int b = !StringUtils.isNumeric(arg1) ? Integer.MAX_VALUE
				: Integer.valueOf(arg1);
		if (a > b)
			return 1;
		else if (a < b)
			return -1;
		else
			return 0;

	}

}
