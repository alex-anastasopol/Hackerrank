package ro.cst.tsearch.utils;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.servers.types.MOJacksonRO;

public class MatchEquivalents
{
    private Vector<String[]> equivList = null;
    private Vector<String[]> equivListILKane = null;
    
    private Hashtable<String,String> directReplacements = null;
    private LinkedHashMap<String,String> directReplacements2 = null;
    
    protected static final Category logger = Logger.getLogger(MatchEquivalents.class);
    
    private static MatchEquivalents equivInstance = null;    
    
    public static Pattern directReplacementsPattern = Pattern.compile( "(?i)\\b(\\w)\\s+(\\w)\\b" );
    private long searchId = -1;
    private MatchEquivalents(long searchId)
    {	this.searchId = searchId;
        equivList = new Vector<String[]>();
        equivListILKane = new Vector<String[]>();
        directReplacements = new Hashtable<String,String>();
        directReplacements2 = new LinkedHashMap<String,String>();
        
        equivList.add( new String[] {"and", "&"} );
        equivList.add( new String[] {"at", "@"} );
        
        equivList.add( new String[] {"number", "no.", "no", "#"} );
        equivList.add( new String[] {"1", "I"} );
        equivList.add( new String[] {"first", "1st"} );
        equivList.add( new String[] {"second", "2nd"} );
        equivList.add( new String[] {"third", "3rd"} );
        equivList.add( new String[] {"corporation", "corp"} );
        equivList.add( new String[] {"fourth", "4th"} );
        equivList.add( new String[] {"fifth", "5th"} );
        equivList.add( new String[] {"sixth", "6th"} );
        equivList.add( new String[] {"seventh", "7th"} );
        equivList.add( new String[] {"eighth", "8th"} );
        equivList.add( new String[] {"ninth", "9th"} );
        equivList.add( new String[] {"tenth", "10th"} );
        equivList.add( new String[] {"eleventh", "11th"} );
        equivList.add( new String[] {"twelfth", "12th"} );
        equivList.add( new String[] {"thirteenth", "13th"} );
        equivList.add( new String[] {"fourteenth", "14th"} );
        equivList.add( new String[] {"fifteenth", "15th"} );
        equivList.add( new String[] {"sixteenth", "16th"} );
        equivList.add( new String[] {"seventeenth", "17th"} );
        equivList.add( new String[] {"eighteenth", "18th"} );
        equivList.add( new String[] {"nineteenth", "19th"} );
        equivList.add( new String[] {"twentieth", "20th"} );
        equivList.add( new String[] {"17TH", "seventeenth"} );
        equivList.add( new String[] {"85TH", "EIGHTY FIFTH"} );
        equivList.add( new String[] {"EIGHTY FIFTH", "85TH"} );
        equivList.add( new String[] {"resurvey", "res"} );
        equivList.add( new String[] {"replat", "rep", "repl"} );
        equivList.add( new String[] {"village", "vlg"} );
        equivList.add( new String[] {"townhomes", "townh"} );
        equivList.add( new String[] {"town", "city"} ); // fix for bug #1663
        equivList.add( new String[] {"office", "ofc"} );
        equivList.add( new String[] {"center", "cntr"} );
        equivList.add( new String[] {"heights", "hgts", "hts"} );
        equivList.add( new String[] {"associates", "assoc"} );
        equivList.add( new String[] { "management", "manage", "managenebt" } );
        equivList.add( new String[] {"", "'",","} );
        equivList.add( new String[] {"", "inc", "inc.","ltd", "llc", "l.l.c.", "co", "co.", "company", "the", "of", "firm"} );
        equivList.add( new String[] {"", "add", "addition", "ad", "addn"} ); // fix for bug #996
        equivList.add( new String[] {"cor", "corr", "corrected"} );
        equivList.add( new String[] {"indus", "industrial"} );
        equivList.add( new String[] {"bus", "busn", "business"} );
        equivList.add( new String[] {"dev", "devel", "development", "developmt" , "devl"} );
        equivList.add( new String[] {"pk", "park"} );
        equivList.add( new String[] {"sub", "subdivision"} );
        equivList.add( new String[] {"", "est", "ests", "estates"} );		//B2214 - added the "" replacement
        equivList.add( new String[] {"","FIL","FILING","FILLING"} );
        equivList.add( new String[] {"","trust"} );
        equivList.add( new String[] {"bldg", "building"} );
        equivList.add( new String[] {"r", "rnch", "ranch"} );
        equivList.add( new String[] {"pnt", "point"} );
        equivList.add( new String[] {"rdg", "ridge"} );
        equivList.add( new String[] {"hill", "hills"} );
        equivList.add( new String[] {"contruction", "construction"} );
        equivList.add( new String[] {"blvd", "boulevard"} );
        equivList.add( new String[] {"inv", "invests", "invest", "investment", "investments"} );
        equivList.add( new String[] {"worldwide", "world wide"} );
        equivList.add( new String[] {" ", "-"} ); //equivalates '-' with space to fix bug #892
        equivList.add( new String[] {"southside", "s/s"} ); // fix for bug #996
        equivList.add( new String[] {"place", "pl"} ); //fix for bug #1160
        equivList.add(new String[] {"north kansas city", "nkc"}); //fix for bug #1678
        equivList.add(new String[] {"85TH", "EIGHTY FIFTH"});
        equivList.add(new String[] {"17TH", "SEVENTEENTH"});
        equivList.add(new String[] {"MTN", "Mountain"});
        equivList.add( new String[] {"FED", "FEDERAL"} );
        equivListILKane.add(new String[] {"street", "st"});
        equivListILKane.add(new String[] {"way", "wa"});
        equivListILKane.add(new String[] {"center", "ctr"});
        
        directReplacements.put("L P", "LTD PARTNERSHIP");
        directReplacements2.put("LIVING TRUST", "LIVING");//4382
        directReplacements2.put( "MCELROY" , "MC ELROY");
        directReplacements2.put("CEDARCREST","CEDAR CREST");//1971
        directReplacements2.put("1ST INCR", ""); //6425
        
        directReplacements2.put("THE SECRETARY OF HOUSING & URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("HUD HOUSING OF URBAN DEV", "HUD");
        directReplacements2.put("HUD-HOUSING OF URBAN DEV", "HUD");
        directReplacements2.put("HUD-HOUSING OF URBAN DEVELOPMENT", "HUD");
        
        directReplacements2.put("SECRETARY OF HOUSING & URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("SECRETARY OF HOUSING AND URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("UNITED STATES SEC OF HOUSING & URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("US DEPARTMENT OF HOUSING & URBAN DEVELOPMENT", "HUD");
        
        directReplacements2.put("HOUSING & URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("HOUSING AND URBAN DEVELOPMENT", "HUD");
        directReplacements2.put("UNITED STATES", "US");
        
        directReplacements2.put("(?is)\\ba\\s+([^\\s]*)\\s*limited\\s+liability\\s+company", "LLC");
        directReplacements2.put("(?is)\\blimited\\s+liability\\s+company", "LLC");
        
        directReplacements2.put("REVOCABLE TRUST", "");		//9783
        
        
        
        for(Entry<String,String> equivMoJackson : MOJacksonRO.ROTREquivalences.entrySet()) {
        	directReplacements2.put(equivMoJackson.getKey(),equivMoJackson.getValue());
        }
        
        directReplacements2.put("'", "");
    }
    
    public static synchronized MatchEquivalents getInstance(long searchId)
    {
        if( equivInstance == null )
        {
            equivInstance = new MatchEquivalents(searchId);
        }
        
        return equivInstance;
    }
    
    public String makeDirectReplacements( String originalStr )
    {
        Matcher directReplacementMatcher = directReplacementsPattern.matcher( originalStr );
        while( directReplacementMatcher.find() )
        {
            String key = directReplacementMatcher.group( 1 ).toUpperCase() + " " + directReplacementMatcher.group( 2 ).toUpperCase();
            String replaceWith = directReplacements.get( key );
            if( replaceWith != null )
            {
                originalStr = originalStr.replaceAll( key, replaceWith );
            }
        }
        
        for(Map.Entry<String,String> entry: directReplacements2.entrySet()){
        	originalStr = originalStr.replaceAll(entry.getKey(), entry.getValue());
        }
        
        return originalStr;
    }
    
    static String []identifiers={ "SECTION", "SEC" , "BLO?C?KS?" , "LO?TS?", "PHASE" };
    private String cleanSubdivision(String str){
    	if( str==null)
    		str="";
    	for(	int i=0; i<identifiers.length; i++	){
    		String regEx = "\\b"+identifiers[i]+"\\b";
			boolean containsIdentifier = RegExUtils.matches(regEx, str);
    		if (containsIdentifier){
    			String[] splitArray = str.split(regEx);
    			if(splitArray.length > 0) {
    				str = splitArray[0];
    			}
    		}
    		
    	}
    	return str.trim();
    }
    
    
    public String getEquivalent( String originalStr){
    	return getEquivalent(originalStr, true);
    }
    
    public String getEquivalent( String originalStr, boolean requiresClean )
    {
    	if(originalStr == null)
    		return "";
        /*
         * returns the equivalent of originalStr using the equivalent lists
         */
        StringBuilder equivalent = new StringBuilder();
        
        if (requiresClean){
        	originalStr = cleanSubdivision( originalStr  );
        }
        originalStr = makeDirectReplacements( originalStr );
        
        StringTokenizer strTok = new StringTokenizer( originalStr.toUpperCase(), " &@#-,", true ); //added '-' as tokens delimitator to fix bug #892
        while( strTok.hasMoreTokens())
        {
            String token = strTok.nextToken();
            boolean found = false;
            
            for( int i = 0 ; i < equivList.size() ; i ++ )
            {
                String[] equivalents = (String[]) equivList.elementAt( i );
                
                for( int j = 1 ; j < equivalents.length ; j++ )
                {
                    /*
                     * if any of the equivalents found, replace them with equivalent[0]
                     */
                    if( token.equalsIgnoreCase( equivalents[j] ) )
                    {
                        equivalent.append(equivalents[0].toUpperCase());
                        found = true;
                        break;
                    }
                }
            }
            
            if(!found)
            {
                equivalent.append(token);
            }
        }
        
        if( equivalent.length() == 0 )
        {
            equivalent.append(originalStr);
        }
        
        String equivalentString = equivalent.toString().replaceAll("[ ]+", " ");
        
        if( !equivalentString.equalsIgnoreCase( originalStr ) ) {
            IndividualLogger.info( "MatchEquivalents: " + originalStr + " replaced with " + equivalentString,searchId );
        }
        
        return equivalentString;
    }
    //because don't need to replace all the equivalences, just some of them
    public String getEquivalentILKaneSubdiv( String originalStr )
    {
    	if(originalStr == null)
    		return "";
        /*
         * returns the equivalent of originalStr using the equivalent lists
         */
        StringBuilder equivalent = new StringBuilder();
        
        originalStr = cleanSubdivision( originalStr  );
        originalStr = makeDirectReplacements( originalStr );
        
        StringTokenizer strTok = new StringTokenizer( originalStr.toUpperCase(), " &@#-", true ); 
        while( strTok.hasMoreTokens())
        {
            String token = strTok.nextToken();
            boolean found = false;
            
            for( int i = 0 ; i < equivListILKane.size() ; i ++ )
            {
                String[] equivalents = (String[]) equivListILKane.elementAt( i );
                
                for( int j = 1 ; j < equivalents.length ; j++ )
                {
                    /*
                     * if any of the equivalents found, replace them with equivalent[0]
                     */
                    if( token.equalsIgnoreCase( equivalents[j] ) )
                    {
                        equivalent.append(equivalents[0].toUpperCase());
                        found = true;
                        break;
                    }
                }
            }
            
            if(!found)
            {
                equivalent.append(token);
            }
        }
        
        if( equivalent.length() == 0 )
        {
            equivalent.append(originalStr);
        }
        
        String equivalentString = equivalent.toString().replaceAll("[ ]+", " ");
        
        if( !equivalentString.equalsIgnoreCase( originalStr ) ) {
            IndividualLogger.info( "MatchEquivalents: " + originalStr + " replaced with " + equivalentString,searchId );
        }
        
        return equivalentString;
    }
    
    public static void main(String[] args) {
		MatchEquivalents matchEquivalents = MatchEquivalents.getInstance(10);
		
		String equivRefToken = matchEquivalents.getEquivalent("Deer Ridge, LLC", true).replaceAll("\\s", "");
		
		
		System.out.println(equivRefToken);
		
	}
}