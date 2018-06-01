package ro.cst.tsearch.search.filter.parser.address;

import java.io.Serializable;
import org.apache.log4j.Category;
import ro.cst.tsearch.utils.Log;

public class AddressParser implements Serializable {
    
    static final long serialVersionUID = 10000000;
	
	protected static final Category logger = Category.getInstance(AddressParser.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + AddressParser.class.getName());
	
	public static String parseAddress (String str){
		str = str.replaceAll("&nbsp;", " ");
		//logger.debug("adresa de parsat = " + str);
		int idx = str.indexOf(",");
		if (idx>0){
			str = str.substring(0,idx);
		}
		return str;
	}

}
