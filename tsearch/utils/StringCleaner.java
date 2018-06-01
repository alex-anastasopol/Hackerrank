package ro.cst.tsearch.utils;

import ro.cst.tsearch.extractor.xml.StringFormats;

public class StringCleaner {
	public static final int NO_CLEAN = 0;
	public static final int JACKSON_SUBDIV = 1;
	public static final int JOHNSON_SUBDIV = 2;
	public static final int HAMILTON_SUBDIV = 3;
	public static final int MO_CLAY_OR_SUB = 4;
	public static final int MO_CLAY_RO_SUB = 5;

	public static String cleanString(int cleanerId, String input) {
		switch (cleanerId) {
		case JACKSON_SUBDIV:
			input = SubdivisionMatcher.cleanJackson(input);
			break;
		case JOHNSON_SUBDIV:
			break;
		case HAMILTON_SUBDIV:
			break;
		case MO_CLAY_OR_SUB:
			input = StringFormats.SubdivisionMOClayOR(input);
			break;
		case MO_CLAY_RO_SUB:
			input = StringFormats.SubdivisionEquivMOClayRO(input);
			break;
		}

		return input;
	}
}