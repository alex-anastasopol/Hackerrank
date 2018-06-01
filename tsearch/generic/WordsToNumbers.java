package ro.cst.tsearch.generic;

import java.util.StringTokenizer;

import ro.cst.tsearch.utils.StringUtils;

/**
 * Transform a number from words to digits
 * @author mihaib
 *
 */

public class WordsToNumbers {

	/**
	 * replace literals with digits
	 */
	protected static String replaceOnlyNumbers(String s){
		   s = s.replaceAll("\\bONE\\b", "1");
		   s = s.replaceAll("\\bTWO\\b", "2");
		   s = s.replaceAll("\\bTHREE\\b", "3");
		   s = s.replaceAll("\\bFOUR\\b", "4");
		   s = s.replaceAll("\\bFIVE\\b", "5");
		   s = s.replaceAll("\\bSIX\\b", "6");
		   s = s.replaceAll("\\bSEVEN\\b", "7");
		   s = s.replaceAll("\\bEIGHT\\b", "8");
		   s = s.replaceAll("\\bNINE\\b", "9");
		   s = s.replaceAll("\\bTEN\\b", "10");
		   s = s.replaceAll("\\bELEVEN\\b", "11");
		   s = s.replaceAll("\\bTWELVE\\b", "12");
		   s = s.replaceAll("\\bTHIRTEEN\\b", "13");
		   s = s.replaceAll("\\bFOURTEEN\\b", "14");
		   s = s.replaceAll("\\bFIFTEEN\\b", "15");
		   s = s.replaceAll("\\bSIXTEEN\\b", "16");
		   s = s.replaceAll("\\bSEVENTEEN\\b", "17");
		   s = s.replaceAll("\\bEIGHTEEN\\b", "18");
		   s = s.replaceAll("\\bNINETEEN\\b", "19");
		   s = s.replaceAll("\\bTWENTY\\b", "20");
		   s = s.replaceAll("\\bTHIRTY\\b", "30");
		   s = s.replaceAll("\\bFORTY\\b", "40");
		   s = s.replaceAll("\\bFIFTY\\b", "50");
		   s = s.replaceAll("\\bSIXTY\\b", "60");
		   s = s.replaceAll("\\bSEVENTY\\b", "70");
		   s = s.replaceAll("\\bEIGHTY\\b", "80");
		   s = s.replaceAll("\\bNINETY\\b", "90");
		   s = s.replaceAll("\\bHUNDRED\\b", "100");
		   s = s.replaceAll("\\bTHOUSAND\\b", "1000");
		   s = s.replaceAll("\\bMILLION\\b", "1000000");
		   	   	   	   	   
		   return s;
	   }
	
	/**
	 * Transform numbers from words to digits. Only the integer part. Up just to millions. 
	 */
	public static double transformWordsToNumbers(String myString) { 
		//String myString = "Two Hundred Twenty-Seven Thousand Eight Hundred & 00/100 Dollars";
		
		StringTokenizer st = new StringTokenizer(myString);
   	 	double wholeNumber = 0.00;
   	 	double partialNumber = 0.00;
   	    double bigNumber = 0.00;
   	 	String termen = "";
		while (st.hasMoreTokens()) {
	    	 String number = st.nextToken();
	    	 if (!"HUNDRED".equals(number) && !"THOUSAND".equals(number) && !"MILLION".equals(number)) {
	    		 termen = replaceOnlyNumbers(number);
	    		 partialNumber = partialNumber + Double.parseDouble(termen);
	    	 }
	    	 if ("HUNDRED".equals(number)){
	    		 termen = replaceOnlyNumbers(number);
	    		 partialNumber = partialNumber * Double.parseDouble(termen);
	    	 }
	    	 if ("THOUSAND".equals(number)){
	    		 termen = replaceOnlyNumbers(number);
	    		 partialNumber = partialNumber * Double.parseDouble(termen);
	    		 wholeNumber = partialNumber;
	    		 partialNumber = 0;
	    	 }
	    	 if ("MILLION".equals(number)){
	    		 termen = replaceOnlyNumbers(number);
	    		 partialNumber = partialNumber * Double.parseDouble(termen);
	    		 bigNumber = partialNumber;
	    		 partialNumber = 0.00;
	    	 }
	         //System.out.println(replaceOnlyNumbers(st.nextToken()));
	     }
		wholeNumber = wholeNumber + partialNumber;
		if (myString.contains("MILLION")){
			wholeNumber = wholeNumber + bigNumber;
		}
		//System.out.println(wholeNumber);
		return wholeNumber;
	}
	
	/**
	 * Calculates the decimal part. For now only for this form: 09/100
	 */
	
	public static double transformDecimals(String string){
		double number = 0.00;
		if (StringUtils.isEmpty(string))
			return number;
			
		if (!string.matches("00/100")) {
			try {
				double beforeSlash = Double.parseDouble(string.replaceAll("(.+)/(.+)", "$1"));
				double afterSlash = Double.parseDouble(string.replaceAll("(.+)/(.+)", "$2"));
				number = beforeSlash / afterSlash;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return number;
	}
	/**
	 * Transform amount from words to digits. Return a double. 
	 */
	public static String transformAmountFromWords(String amount){

		if (amount.matches("[\\d\\.,]+")) // 356,000.00
			amount = "$" + amount;
		
		if (amount.contains("$"))
			return amount;
		
		amount = amount.toUpperCase();
		amount = amount.replaceAll("&", "AND");
		amount = amount.replaceAll("(?is)\\bNO/1.00", "00/100");
		amount = amount.replaceAll("(?is)\\bno\\b", "00");
		amount = amount.replaceAll("(?is)(AND\\s+00)1(100)", "$1/$2");
		amount = amount.replaceAll("(?is)(AND\\s+00)(100)", "$1/$2");
		String intregNumber = amount.replaceAll("(?is)(.+)\\s+and\\s*(.+)", "$1").trim();
		intregNumber = intregNumber.replaceAll("-", " ");
		String decimalPart = amount.replaceAll("(?is)(.+)\\s*and\\s*(.+100).*", "$2").trim();
		double number = 0.00;
		
		number = transformWordsToNumbers(intregNumber) + transformDecimals(decimalPart);
		return "$" + Double.toString(number);
	}	
}
