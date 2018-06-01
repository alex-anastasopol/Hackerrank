/*
 * Created on Nov 7, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.utils;

import org.apache.log4j.Category;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class StringIntUtil {

	private static final Category logger = Category.getInstance(StringIntUtil.class.getName());

	public static String stringLetStrip(String stringToStrip) {
		int pozFinal = stringToStrip.length();
		for (int i = 0; i < stringToStrip.length(); i++) {
			if (stringToStrip.charAt(i) < 48 || stringToStrip.charAt(i) > 57) {
				pozFinal = i;
				break;
			}
		}
		return stringToStrip.substring(0, pozFinal);
	}

	public static String varFormating(String varToFormat)
		throws NumberFormatException {
		String retString = new String();
		try {
			int vr_simplu = Integer.parseInt(varToFormat);
			double vr = new Double(varToFormat).doubleValue();
			retString = varToFormat;

			if (vr / 10 < 1)
				retString = "00" + vr_simplu;
			if (vr / 100 < 1 && vr / 10 > 1)
				retString = "0" + vr_simplu;
			retString = "book" + retString;
		} catch (NumberFormatException e) {
			logger.error("Error to book string!", e);
		}
		return retString;
	}
}
