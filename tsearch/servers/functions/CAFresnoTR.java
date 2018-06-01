package ro.cst.tsearch.servers.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CAFresnoTR {
	private static final Pattern	pinPattern1	= Pattern.compile("(\\d{3})-?(\\d{3})-?(\\d{2})");

	/**
	 * Extract the pin parts from 111-111-11 or 11111111
	 * 
	 * @param pin
	 * @return 3 pin parts
	 */
	public static String[] extractPins(String pin) {
		Matcher m = pinPattern1.matcher(pin);
		if (!m.matches()) {
			return null;
		}
		String p1 = m.group(1);
		if (p1 == null) {
			p1 = "";
		}
		String p2 = m.group(2);
		if (p2 == null) {
			p2 = "";
		}
		String p3 = m.group(3);
		if (p3 == null) {
			p3 = "";
		}
		return new String[] { p1, p2, p3 };
	}
}
