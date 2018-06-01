package ro.cst.tsearch.data.sort;

import java.util.Comparator;

import ro.cst.tsearch.data.County;

public class StateCountyComparator implements Comparator<County>{

	@Override
	public int compare(County c1, County c2) {
		if(c1 == null && c2 == null) {
			return 0;
		} else if(c2 == null) {
			return 1;
		} else if(c1 == null) {
			return -1;
		}
		/*
		if(c1.getStateId() == c2.getStateId()) {
			return c1.getName().compareTo(c2.getName());
		}
		if(c1.getStateId() < c2.getStateId()) {
			return -1;
		} else {
			return 1;
		}
		*/
		switch (c1.getStateAbv().compareTo(c2.getStateAbv())) {
			case 0:
				return c1.getName().compareTo(c2.getName());
			default:
				return c1.getStateAbv().compareTo(c2.getStateAbv());
		}
		
	}

}
