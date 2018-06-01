package ro.cst.tsearch.search.filter.testnamefilter;

public final class GenericNameFilterTestConf {
		//negative value for unlimited wrong candidates 
		public final static int nAcceptedWrongCandidates = -1;
		
		public final static boolean debug = false;
		//max length for a part of the name 
		public final static int maxCharacters = 300;
		//separator in csv files
		public final static char separator = ',';
		public final static int maxFileLength = 200000;
		// files smaller then maxThreshold bytes will be kept in memory
		public final static int maxThreshold = maxFileLength;
		//how many characters should be displayed from result
		public final static int maxDecimals = 4;
		//errors
		public final static String ERROR_REF = "Please fill some fields for reference.";
		public final static String ERROR_FIELD_NAMES = "Some names provided in forms are in wrong format";
		public final static String ERROR_CAND = "Please fill some sources for candidates.";
		public final static String ERROR_TOO_BIG = "The file you try to upload is too large.";
		public final static String ERROR_CSV_ONLY = "We accept only CSV files, delimited with \",\".";
		public final static String ERROR_WRONG_ROW = "This row has problems:";
		public final static String ERROR_WRONG_FIELD = "Wrong field name.";
		public final static String ERROR_NO_TSADMIN = "Only TSAdmins have access to this tool!";		
		public final static String ERROR_BAD_FILE = "The uploaded file has too many bad names!";		

}
