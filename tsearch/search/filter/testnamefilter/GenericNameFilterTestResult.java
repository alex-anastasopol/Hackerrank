package ro.cst.tsearch.search.filter.testnamefilter;

import ro.cst.tsearch.generic.tag.LoopTag;
import java.util.Vector;
import java.util.Iterator;
import ro.cst.tsearch.search.filter.testnamefilter.*;

public class GenericNameFilterTestResult extends LoopTag {
	private Object[] resultList;
	@Override
	protected Object[] createObjectList() throws Exception {
		// TODO Auto-generated method stub

		Vector rs;
		rs = (Vector)ses.getAttribute("results");
		if (rs != null){
			resultList = new Object[rs.size()];
			Iterator i = rs.iterator();
			int j = 0;
			while (i.hasNext()){
				resultList[j] = i.next();
				j++;
			}
		} else {
			resultList = new Object[0];
		}
		return resultList;
	}
	
	
	

}
