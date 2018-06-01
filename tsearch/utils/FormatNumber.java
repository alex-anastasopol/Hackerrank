/*
 * Text here
 */

package ro.cst.tsearch.utils;

import java.util.Locale;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 *
 */
public class FormatNumber {

    /**     */
    public static final String ONE_DECIMAL;

    /**     */
    public static final String TWO_DECIMALS;

    /**     */
    public static final String TWO_DECIMALS_COMA;

    /**     */
    public static final String THREE_DECIMALS;

    /**     */
    public static final String INTEGER;

    /**     */
    public static final String PERCENTAGE;

    /**     */
    static {
        
        ONE_DECIMAL = new String ("0.0");
        TWO_DECIMALS = new String ("0.00");
        THREE_DECIMALS = new String("0.000");
        PERCENTAGE = new String ("0.00%");
        INTEGER = new String ("0");
        TWO_DECIMALS_COMA = new String ("0,00#");
    }
    
     /**    */
     private String formatString;



    /**     */
    public FormatNumber(String formatString){
        this.formatString = formatString;
    }
    


    /**     */
    public String getNumber(double nr){
        
        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
            ((DecimalFormat) f).applyPattern(formatString);
  
            
            return ((DecimalFormat) f).format(nr);
        }
        
        return formatString+" error!!!";        
    }
    public String getNumberComma(double nr){
        
        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
            ((DecimalFormat) f).applyPattern(formatString);
  
            
            return (((DecimalFormat) f).format(nr)).replace(".",",");
        }
        
        return formatString+" error!!!";        
    }
    public String getNumber(long nr){
        
        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).applyPattern(formatString);

            return ((DecimalFormat) f).format(nr);
        }
        
        return formatString+" error!!!";        
    }
    public String getNumber(int nr){
        
        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).applyPattern(formatString);

            return ((DecimalFormat) f).format(nr);
        }
        
        return formatString+" error!!!";        
    }
    /**
     * Formats the supplied BigDecimal instance acording with internal pattern
     */
    public String getNumber(BigDecimal nr){
        return getNumber(nr.doubleValue());
    }
    /**
     * Formats the supplied Double instance according with internal pattern
     */
    public String getNumber(Double nr){
        return getNumber(nr.doubleValue());
    }
}	 
