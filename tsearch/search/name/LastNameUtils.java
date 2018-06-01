package ro.cst.tsearch.search.name;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servlet.BaseServlet;

/**
 * Provides name utility methods, like checking if a name token list or a string 
 * are last names
 * @author mihaib
 *
 */
public class LastNameUtils {

	protected static final Category logger = Logger.getLogger(LastNameUtils.class);
	
	public final static Pattern firstAndMiddleInitialsWithSeparator = Pattern.compile("[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]*[,][ \t]*");
	public final static Pattern firstAndMiddleInitials = Pattern.compile("[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]+");
		
	public final static Pattern posiblleNameToken = Pattern.compile("[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z&]+");
	
	public final static Pattern posiblleNameTokenWithFirstAndMiddleInitials = Pattern.compile("[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]+");
	
	public final static Pattern patLastInitialLastInitial = Pattern.compile("([a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+)([ \t]+[a-zA-Z][ \t]+)([a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+)(([ \t]+[a-zA-Z])+)[ \t]+");
	
	
	private static String [] lastNameExpressions = {""};
	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF" + File.separator + "classes"  + File.separator 
						+ "resource" + File.separator + "names" +  File.separator);
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Last Names list not loaded!");
		}
		try {
			List<String> lastNamesList = org.apache.commons.io.FileUtils.readLines
				(new File(folderPath + "LastNames.txt"));
			lastNameExpressions = new String[lastNamesList.size()];
			int i=0;
			for (String s: lastNamesList) {
				lastNameExpressions[i++] = s;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static List<Pattern> lastNamePatterns = new ArrayList<Pattern>();
	
	private static Set<String> lastNameWords = new HashSet<String>();
	
	/**
	 * Pattern used for splitting a string into words
	 */
	private static Pattern splitPattern = Pattern.compile("[^0-9a-zA-Z_\\.]+");
	
	
	static{
		//int expr =0;
		for(int i=0; i<lastNameExpressions.length; i++){
			String expression = lastNameExpressions[i];
			String [] words = splitPattern.split(expression);
			if(words.length == 1){
				lastNameWords.add(words[0]);
			}else{
				lastNamePatterns.add(Pattern.compile("(?i)\\b"+expression+"\\b"));
				//expr++;
			}
		}
		
	}

	
	private static boolean isLastNameString(String s) {               
		s = s.toUpperCase();
            
        String [] words = splitPattern.split(s);
        for(int i=0; i<words.length; i++){
        	if(lastNameWords.contains(words[i]))
        		return true;
        }
        for(int i=0; i<lastNamePatterns.size(); i++){
        	if(lastNamePatterns.get(i).matcher(s).find())
        		return true;
        }
        return false;
	}
	
	
	public static boolean isLastName(String s) {
        return isLastNameString(s);
	}
	
	public static boolean isNotLastName(String s) {
		return !isLastNameString(s);
	}
	
	public static boolean isLAstName(NameTokenList ntl) {
		String s = ntl.getFirstNameAsString() + " " + ntl.getMiddleNameAsString() + " " + ntl.getLastNameAsString();
		return isLastNameString(s);
	}

	public static String[] getLastNameExpressions() {
		return lastNameExpressions;
	}

	
	public static boolean isInCompanyList(String temp){
		boolean  testBreak = false;
		for(int i=0;i<lastNameExpressions.length;i++){
			   if(temp.equalsIgnoreCase(lastNameExpressions[i])){
				   testBreak = true;
				   break;
			   }
		}
		return testBreak;
	}
	
	private  final static String [] noNameOwnerExpressions = {
		"APT",
		"BANKFIELD",
		"BRANDEN",
		"CLOSE",
		"CLUB",
		"CORP",
		"DOWNSHILL",
		"FARM",
		"FL",
		"FOEL",
		"HOUSE",
		"INN",
		"LANE",
		"LODGE",
		"LOT",
		"MOELFRYN",
		//"OAK", This can be middle name: SMITH, DANIEL OAK 
		"PARLOR",
		"PLACE",
		"RD",
		"RIVERSTONE",
		"SCHOOL",
		"SPRINGFIELD",
		"TREE",
		"VALE",
		"VILLA",
		"WALES",
		"WHARF"
		
	};
	private static List<Pattern> noNameOwnerPatterns = new ArrayList<Pattern>();
	
	private static Set<String> noNameOwnerWords = new HashSet<String>();
	
	/**
	 * Pattern used for splitting a string into words
	 */
	private static Pattern splitNoOwnerNamePattern = Pattern.compile("[^0-9a-zA-Z_\\.]+");
	
	
	static{
		//int expr =0;
		for(int i=0; i<noNameOwnerExpressions.length; i++){
			String expression = noNameOwnerExpressions[i];
			String [] words = splitNoOwnerNamePattern.split(expression);
			if(words.length == 1){
				noNameOwnerWords.add(words[0]);
			}else{
				noNameOwnerPatterns.add(Pattern.compile("(?i)\\b"+expression+"\\b"));
				//expr++;
			}
		}
		
	}
	private static boolean isNoNameOwnerString(String s) {               
		s = s.toUpperCase();
            
        String [] words = splitNoOwnerNamePattern.split(s);
        for(int i=0; i<words.length; i++){
        	if(noNameOwnerWords.contains(words[i]))
        		return true;
        }
        for(int i=0; i<noNameOwnerPatterns.size(); i++){
        	if(noNameOwnerPatterns.get(i).matcher(s).find())
        		return true;
        }
        return false;
	}
	
	public static boolean isNoNameOwner(String s) {
			return isNoNameOwnerString(s);
	}
}
