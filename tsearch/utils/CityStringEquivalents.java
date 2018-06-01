package ro.cst.tsearch.utils;

import java.util.Vector;

/**
 * 
 * @author mihaib
 * 
 */
public class CityStringEquivalents {
	private Vector<String[]> equivList = null;
	private static CityStringEquivalents cityStringEquivalents = null;

	private CityStringEquivalents() {
		equivList = new Vector<String[]>();
		equivList.add(new String[] { "NORTH", "N" });
		equivList.add(new String[] { "SOUTH", "S" });
		equivList.add(new String[] { "EAST", "E" });
		equivList.add(new String[] { "WEST", "W" });
		equivList.add(new String[] { "SAINT", "ST", "ST." });
		equivList.add(new String[] { "CHARLES", "CHAS" });
		equivList.add(new String[] { "CARPENTERSVILLE", "CVILLE" });
		equivList.add(new String[] { "MOUNT", "MT" });
	}

	public static CityStringEquivalents getInstance() {
		if (cityStringEquivalents == null) {
			cityStringEquivalents = new CityStringEquivalents();
		}
		return cityStringEquivalents;
	}

	public String getEquivalentToken(String string1, String string2) {
		if (string1 == null) {
			string1 = "";
		}
		if (string2 == null) {
			string2 = "";
		}

		String refTokens[] = string1.trim().toUpperCase().split("\\s+");
		String candTokens[] = string2.trim().toUpperCase().split("\\s+");

		if (refTokens.length != candTokens.length)
			return string1;

		String equivalent = "";
		boolean found = false;

		for (int i = 0; i < refTokens.length; i++) {
			found = false;

			for (int j = 0; j < equivList.size(); j++) {
				String[] equivalents = (String[]) equivList.elementAt(j);

				for (int k = 1; k < equivalents.length; k++) {

					if (refTokens[i].equalsIgnoreCase(equivalents[k])) {
						equivalent += equivalents[0].toUpperCase() + " ";
						found = true;
					}

				}
			}
			if (!found) {
				equivalent += " " + refTokens[i];
			}
		}

		if ("".equals(equivalent)) {
			equivalent = string1;
		}
		return equivalent.trim();
	}

}
