package ro.cst.tsearch.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class ATSDecimalNumberFormat {

	private static NumberFormat nrFormat = null;
	public  static BigDecimal   zeroBD   = null;
	
	public static BigDecimal ZERO = new BigDecimal("0.00");
	public static BigDecimal ONE = new BigDecimal("1.00");
	public static BigDecimal TWO = new BigDecimal("2.00");

	public static BigDecimal NA = new BigDecimal("-1.00");

	
	static {
	    nrFormat = NumberFormat.getInstance(Locale.US);
	    zeroBD   = new BigDecimal(0.0);
	}

    public static String format(BigDecimal bdToFormat) {
        String bdFormatted = nrFormat.format(bdToFormat);
        //must have exactly two decimal points
        int pointPos  = bdFormatted.indexOf(".");
        int strLength = bdFormatted.length();
        if (pointPos >= 0)
            switch (strLength-pointPos-1) {
                //switch after the number of decimal figures
                case 0 : bdFormatted += "00"; break;
                case 1 : bdFormatted += "0";  break;
                case 2 : break;
                default: bdFormatted = bdFormatted.substring(0,pointPos+3);
            }
        else
            bdFormatted += ".00";
        return bdFormatted;
    }
    
    public static double formats(double bdToFormat) {
        String bdFormatted = nrFormat.format(bdToFormat);
        bdFormatted = bdFormatted.replaceAll(",", "");
        //must have exactly two decimal points
        int pointPos  = bdFormatted.indexOf(".");
        int strLength = bdFormatted.length();
        if (pointPos >= 0)
            switch (strLength-pointPos-1) {
                //switch after the number of decimal figures
                case 0 : 
                case 1 : 
                case 2 : break;
                default: bdFormatted = bdFormatted.substring(0,pointPos+3);
            }
        return Double.parseDouble(bdFormatted);
    }
    

	public static String formatInt(BigDecimal bdToFormat) {
		
		String bdFormatted = nrFormat.format(bdToFormat);

		int pointPos  = bdFormatted.indexOf(".");
		if (pointPos >= 0)
			bdFormatted = bdFormatted.substring(0, pointPos); 
		return bdFormatted;
	}

}
