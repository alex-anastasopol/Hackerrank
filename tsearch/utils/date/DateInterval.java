package ro.cst.tsearch.utils.date;

import java.util.Calendar;

public class DateInterval {
	private Calendar intervalStart;
	private Calendar intervalEnd;

	public DateInterval(Calendar minDate, Calendar maxDate) {
		this.intervalStart = minDate;
		this.intervalEnd = maxDate;
	}

	public Calendar getIntervalStart() {
		return intervalStart;
	}

	public void setIntervalStart(Calendar intervalStart) {
		this.intervalStart = intervalStart;
	}

	public Calendar getIntervalEnd() {
		return intervalEnd;
	}

	public void setIntervalEnd(Calendar intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

	public boolean isInInterval(Calendar value) {
		boolean isInInterval = false;
		if (intervalStart.before(value) && intervalEnd.after(value)) {
			isInInterval = true;
		}
		return isInInterval;
	}
	
	@Override
	public String toString() {
		return String.format("[%s; %s]", getIntervalStart().getTime(),getIntervalEnd().getTime());
	}

}
