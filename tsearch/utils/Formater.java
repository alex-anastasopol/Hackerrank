package ro.cst.tsearch.utils;

import java.text.*;
import java.util.*;


/**
* Class for converting data to/from Strings in a proper format.
*/

public class Formater {


    /**
    *  Gives address to be displayed in account information page i.e. address 1, 2, 3 + zip code
    */
    public static String addressFormat(String addr[], String zipCode, String lineSep) {
        String ret = "";
        for (int i = 0; i < addr.length; i++)
            if (addr[i] != null && addr[i].trim().length() > 0)
                if (ret.length() > 0)
                    ret = ret + lineSep + addr[i].trim();
                else
                    ret = ret + addr[i].trim();
        if (zipCode != null && zipCode.trim().length() > 0)
            if (ret.length() > 0)
                ret = ret + " " + zipCode.trim();
            else
                ret = ret + zipCode.trim();

        return(ret);
    }

    /**
    * Trim last zeroes
    */
    public static String trimLastZeroes( String str ) {
        int i;
        for (i=str.length()-1; i>1; i--)
            if (str.charAt(i) != '0')
                break;
        return str.substring(0, i+1);
    }

	/**
	* Transform spaces in %20
	*/

	public static String escape( String str ) {
        String ret="";
        for( int i = 0; i < str.length(); i++) {
            if(str.charAt(i) != ' ')
                ret = ret + str.substring(i,i+1);
            else
                ret = ret + "%20";
        }
        return ret;
    }

    /**
    * Perform a percent format
    */

    public static String percentFormat( String str )
                                throws NumberFormatException {
        DecimalFormat df;
        df=new DecimalFormat("0.00");
        double d = Double.parseDouble(str);
        d = ((double)Math.round(d * 100))/100.0;
        return df.format(d)+"%";
    }

    /**
    *	Put commas and zecimals.
    */

    public static String convertToAmount( String stupidString ) {
        int pointIndex = stupidString.indexOf('.');
        String decimals = "";
        if(pointIndex!=-1)
            decimals=stupidString.substring(pointIndex);
        if(!decimals.equals(""))
            stupidString=stupidString.substring(0, pointIndex);
        if (decimals.length() == 2)
           decimals = decimals + "0";
        String amount = "";
	int i;
	int minLen=3;
	if (stupidString.charAt(0) == '-')
	   minLen=4;
	for( i = stupidString.length(); i > minLen; i = i-3)
	    if(!amount.equals(""))
	        amount = stupidString.substring(i-3, i) + "," + amount;
	    else
		amount = stupidString.substring(i-3, i);
        if( i == 0 ) 
            return amount+decimals;
	if( i < stupidString.length() ) {
	    amount = stupidString.substring(0, i) + "," + amount;
            return amount+decimals;
        }
	return stupidString+decimals;
    }

	/**
	* Get a amount of money from a value.
	*/
	public static String mortgageFormat( String sign, String value ) {
	    if(sign.equals("-")) {
                if(value.indexOf('.')==-1) {
                    if(value.length()>=2)
                        return "$-" + convertToAmount(value.substring(0, value.length()-2)
                                              + "." + value.substring(value.length()-2, value.length()));
                    else
                       return "$-0.0" + value;
                }
                else
                    return "$-" + convertToAmount(value);
            }
            else {
                if(value.indexOf('.')==-1) {
                    if(value.length()>=2)
	                return "$" + convertToAmount(value.substring(0, value.length()-2)
                                              + "." + value.substring(value.length()-2, value.length()));
                    else
                        return "$0.0" + value;
                }
                else
                    return "$" + convertToAmount(value);
            }
	}

    /**
    * Return a string without the leading zeroes.
    */
    public static String trimZeroes( String str ) {
        for(int i = 0; i < str.length(); i++)
            if(str.charAt(i) != '0')
                return str.substring(i);
        return "0";
    }

    /**
    *   Parameter s has to be a string which represents an unsigned number precedeed by "$" and followed by sign.
    *   That the way mainfreame sends money amounts.
    *   Return a string having the sign in the correct position in order to be displayed.
    */
    public static String moveSignInFront( String s ) {
        s = s.replace('$', ' ');
        s = s.replace('+', ' ');
        s = s.trim();
        if(s.indexOf('-') == s.length()-1) {
                return  ( "-" + s.substring(0, s.length()-1) );
              }
		return s;
	}


    /** 
    * converts a double in a rounded one with exact to decimal digits
    */
    public static double doubleToAmount (double d) {
        double ret;
        if (d>=0)
            ret=d;
        else
            ret=-d;
        int i=(int)(ret*1000);
	if (i % 10 >= 5)
	    i += 1;
	i = i/10;
	ret = (double)i/100.0;
	if (d<0)
	    ret = -ret;
	return ret;
    }

    /** 
    * converts a string in format amount (###,###,###.##) in double format by removing comas
    */
    public static String amountToDouble(String sum1) {
        while (sum1.charAt(0) == ' ')
            sum1 = sum1.substring(1, sum1.length());
        while (sum1.charAt(0) == '$')
            sum1 = sum1.substring(1, sum1.length());
        while (sum1.charAt(0) == '0')
            sum1 = sum1.substring(1, sum1.length());
        if (sum1.charAt(sum1.length() - 1) == '.')      //iexplore != netscape
            sum1 = sum1.substring(0, sum1.length() - 1);
        if (sum1.length() == 0)
            sum1 = "0";
        if (sum1.charAt(0) == '.')
           sum1="0"+sum1;
        String sum2=sum1;
        if (sum1.indexOf('.')>=1) {
            sum2=sum1.substring(sum1.lastIndexOf('.')+1);
            sum1=sum1.substring(0, sum1.lastIndexOf('.'));
        }
        else
            sum2="00";
        int j=0;
        boolean first=true;
        String sum3="";
        for (int i=sum1.length()-1; i>=0; i--) {
            if (!first && j % 3 == 0)
                if (sum1.charAt(i) == '.' || sum1.charAt(i) == ',')
                   continue;
            if (sum1.charAt(i)>='0' && sum1.charAt(i)<='9')
               sum3=sum1.substring(i, i+1)+sum3;
            else
               return null;
            j++;
            first=false;
        }
        return sum3+"."+sum2;
    }

    /**
    *   converts a double in a string with two decimal digits
    */
    public static String doubleToString(double d) {
       String sret;
       int intr, dec;
       if (d<1)
           sret="0";
       else
           sret="";
       intr = (int) d;
       dec = (int)(((d-(double)intr))*1000);
       if (dec%10 == 9)
          dec++;
       dec = dec/10;
       while (intr > 0) {
           sret = (intr % 10)+sret;
           intr = intr/10;
       }
       sret = sret + "."+(dec/10) +dec %10;
       return sret;
    }

    /**
    * add leading zeroes to the string s in order to have exact 16 characters. 
    */
    public static String leftPadding( String s ) {

         String s1,s2;
         s = s.trim();
         s1="0000000000000000";
         if (s1.length()<=s.length())
            return s;
         s2=s1.substring(s.length());
         return (s2.concat(s));

	}
    
    /**
    * add leading zeroes to the string s in order to have exactly n characters. 
    */
    public static String leftPadding( String s, int n ) {
        s = s.trim();
        while (s.length() < n)
            s = "0"+s;
        return s;
	}

    /**
    * format = 99V99999%
    */
    
    public static String interestRate(String s) {
        String rez;
        
        rez = trimZeroes(s.substring(0, 2));
        rez = rez + ".";
        rez = rez + trimLastZeroes(s.substring(2));
        rez = rez + "%";
        
        return rez;
    }
    
    /**
    * remove $ sign from a string representing money amounts
    */
    public static String removeDollarSign(String s) {
        return s.replace('$', ' ').trim();
    }    

    /**
    * mortgage data format
    */
    public static String mortgageData(String s) {
        return s.substring(0, 2)+"/"+s.substring(2, 4)+"/"+s.substring(4);
    }    

    /**
    * convert a string to double
    */
    public static double stringToDouble2(String s) throws NumberFormatException {
        s = s.replace('$',' ').trim();
        for (int j=s.indexOf(','); j != -1; j=s.indexOf(','))
            s = s.substring(0, j)+s.substring(j+1);
        return Double.parseDouble(s);    
    }    
    
    /**
    * convert a double to string
    */
    public static String doubleToString2(double d) {
        return (new DecimalFormat("###,###,##0.00")).format(d);
    }

    /**
    * amount format
    */
    public static String amountFormat(String s) throws NumberFormatException {
        return doubleToString2(stringToDouble2(s));
    }

    /**
    * remove dashes from a string
    */
    public static String removeDashes(String s) {
        if (s == null)
            return null;
        for (int i=s.indexOf('-'); i != -1; i=s.indexOf('-'))
            s = s.substring(0, i)+s.substring(i+1);
        return s;
    }

    /**
    * add dashes in SSN represented by 9 digits
    */
    public static String addDashes(String s) {
        if (s.length() == 9)
            s = s.substring(0, 3)+"-"+s.substring(3, 5)+"-"+s.substring(5);
        return s;
    }
    
    /**
    * get last Month and Year for previous noMonth starting from today
    */

    public static String[] getLastMonth (int noMonths) {
        Calendar rightNow = Calendar.getInstance();
        int month = rightNow.get(Calendar.MONTH), 
            year  = rightNow.get(Calendar.YEAR), i;
        String months[]={ "January","February","March","April","May","June","July","August","September","October","November","December"};
        String[] result = new String[noMonths];
        for(i=0;i<noMonths;i++) {
            result[i]= months[month] + " " + year;
            month=month-1;
            if(month==-1) {
                year--;
                month=11;
            }
        }
        return result;
    }

    /**
    * return true if password is like username#### where #### are four digits. These passwords are changed by agents for
    * customers. At first login customer must change pasword
    */

    public static boolean isAdminPassword (String userName, String password) {
        if (password.length() < userName.length() )
            return false;
        if (!password.substring(0, userName.length()).equals(userName))
            return false;
        password=password.substring(userName.length());
        if (password.length() != 4)
            return false;
        for (int i=0; i<4; i++)
            if (! Character.isDigit(password.charAt(i)))
                return false;
        return true;
    }


    public static String trimN(String s, int n) {
        if (s.length() <= n)
            return s;
        else
            return s.substring(0, n);
    }

    /**
    * converts string 'hh:mm' by removing leading zero and in format 'hh:mmaa'
    **/

    public static String mainToUpibTime(String s) {
        String dateString = "01/01/2000 "+s;
        Date dateTest = (new SimpleDateFormat("MM/dd/yyyy h:mm")).parse(dateString, new ParsePosition(0));
        s = (new SimpleDateFormat("MM/dd/yyyy h:mmaa")).format(dateTest);
        return s.substring(11);
    }

    /**
    *  Replaces ' by '' in a string
    */

    public static String doubleQuotes(String s) {
    	
    	if (s == null)
    		return s;
    	
    	int newIndex=0;
        while ((newIndex=s.indexOf ('\'', newIndex))>=0) {
           s=s.substring(0, newIndex)+"''"+s.substring(newIndex+1);
           newIndex+=2;
        }
        //return quotes(s);
        return s;
    }
    
    /**
    *  Replaces ' by \' in a string
    */

    public static String backSlashQuote(String s) {
    	int newIndex=0;
        while ((newIndex=s.indexOf ('\'', newIndex))>=0) {
           s=s.substring(0, newIndex)+"\\'"+s.substring(newIndex+1);
           newIndex+=2;
        }        
        return s;
    }    
    
    /**
    *  Replaces \ by \\ in a string
    */

    public static String backSlash(String s) {
    	int newIndex=0;
        while ((newIndex=s.indexOf ('\\', newIndex))>=0) {
           s=s.substring(0, newIndex)+"\\\\"+s.substring(newIndex+1);
           newIndex+=2;
        }        
        return s;
    } 
    
    public static int[] Percentage(int nr){
        int[] percentage=new int[nr];
        int percTotal=10000;
        int percR=percTotal%nr;
        for (int i=0; i<nr; i++) {
            percentage[i]=percTotal/nr;
            if (percR>0) {
                percentage[i]++;
                percR--;
            }
        }
        return(percentage);
        
    }
        

}
