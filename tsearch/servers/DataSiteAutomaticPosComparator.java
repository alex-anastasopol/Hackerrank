package ro.cst.tsearch.servers;

import java.util.Comparator;

import ro.cst.tsearch.servers.bean.DataSite;

public class DataSiteAutomaticPosComparator implements Comparator<DataSite> {

	@Override
	public int compare(DataSite o1, DataSite o2) {
		if(o1 != null) {
			if (o2 != null) {
				return Integer.valueOf(o1.getAutpos()).compareTo(o2.getAutpos());
			}
			else {
				return 1;
			}
		} else {
			if( o2 != null) {
				return -1;
			} else {
				return 0;
			}
		}
	}

}
