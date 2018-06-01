package ro.cst.tsearch.utils.date;

import java.util.List;

public enum MonthFormat {
		NO_MONTH(0,""),
		JANUARY(1,"JANUARY","JAN"),
		FEBRUARY(2,"FEBRUARY", "FEB"),
		MARCH(3,"MARCH", "MAR"),
		APRIL(4,"APRIL", "APR"),
		MAY(5,"MAY", "MAY"),
		JUNE(6,"JUNE","JUN"),
		JULY(7,"JULY","JUL"),
		AUGUST(8,"AUGUST","AUG"),
		SEPTEMBER(9,"SEPTEMBER","SEP"),
		OCTOBER(10,"OCTOBER","OCT"),
		NOVEMBER(11,"NOVEMBER","NOV"),
		DECEMBER(12,"DECEMBER","DEC");
		
	private final int monthIndex; // starts with 1
	private final String[] monthNames;

	MonthFormat(int monthIndex, String... monthName) {
		this.monthIndex = monthIndex;
		this.monthNames = monthName;
	}

	public String[] getMonthNames() {
		return monthNames;
	}

	public String getMonthName(int monthIndex) {
		return monthNames[monthIndex];
	}

	public int value() {
		return monthIndex;
	}

	public static MonthFormat getMonthByName(String monthName) {
		for (MonthFormat date : MonthFormat.values()) {
			for (String d : date.monthNames) {
				if (d.equals(monthName)) {
					return date;
				}
			}
		}
		return NO_MONTH;
	}
}
