package ro.cst.tsearch.search.name;

import java.io.BufferedReader;
import java.io.File;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.URLMaping;

public class NameFactory {
	public static int MIN_PREFIX_LEN = 6;
	//the key is the name
	//the values are an array 0 - Frequency in percent 
	//1 - Cumulative Frequency in percent
	//2 - rank
	static TreeMap<String, Double[]> last = new TreeMap<String, Double[]>();
	static TreeMap<String, Double[]> male = new TreeMap<String, Double[]>();
	static TreeMap<String, Double[]> female = new TreeMap<String, Double[]>();
	static String  lastNames = "dist.all.last";
	static String  maleNames = "dist.male.first";
	static String  femaleNames = "dist.female.first";
	static NameFactory nfInstance = null;
	public static synchronized NameFactory getInstance(){
		if (nfInstance == null){
			nfInstance = new NameFactory();
			//read the files
			String path = NameFactory.getDefaultPath();
			last = loadFiles(path + File.separator + lastNames);
			male = loadFiles(path + File.separator + maleNames);
			female = loadFiles(path + File.separator + femaleNames);

		}
		return nfInstance;
	}
	
	public static String getDefaultPath(){
		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		String basePath = BaseServlet.REAL_PATH;
		if (basePath.equals("")){
			if (Utils.isJvmArgumentTrue("parametruTestareLocala"))
				 basePath = "/workspace/TSEARCH/";	
			else basePath = "/work/eclipse/TSEARCH_RESIN/web/";
		}
		return basePath + rbc.getString("lists.names.path").trim();
	}
	
	private static TreeMap<String, Double[]>loadFiles(String fileName){

		TreeMap<String, Double[]> tmpTree = new TreeMap<String, Double[]>();
		String content = "";
		
		try {
			BufferedReader in = new BufferedReader(new java.io.FileReader(fileName));
			while((content = in.readLine()) != null){
				String[] s = content.trim().split("\\s+");
				if (s.length == 4){
					try {
						tmpTree.put(s[0], new Double[] {Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3])});
					} catch (Exception e){
						e.printStackTrace();
					}
				}
				
			}
		} catch (Exception e){
			e.printStackTrace();
			return tmpTree;
		}

		return tmpTree;
	}
	
	private String cleanName(String name){
		name = name.toUpperCase();
		name = name.replaceAll("[^A-Z]", "");	
		return name;
	}
	
	public boolean isLast(String name){
		name = cleanName(name);
		if (last.get(name) != null){
			return true;
		} else {
			return false;
		}
	}

	public boolean isLastOnly(String name){
		name = cleanName(name);
		if (last.get(name) != null && !isMale(name) && !isFemale(name)){
			return true;
		} else {
			return false;
		}
	}	

	public boolean isName(String name){
		name = cleanName(name);
		return isMale(name) || isFemale(name) || isLast(name);
	}	
	
	public boolean isMale(String name){
		name = cleanName(name);
		if (male.get(name) != null){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isFirstMiddle(String name){
		return isMale(name) || isFemale(name);
	}
	
	public boolean isFirstMiddleOnly(String name){
		return (isMale(name) || isFemale(name)) && !isLast(name);
	}
	
	public boolean isFemale(String name){
		name = cleanName(name);
		if (female.get(name) != null){
			return true;
		} else {
			return false;
		}
	}

	public boolean isFemaleOnly(String name){
		name = cleanName(name);
		if (isFemale(name) && !isMale(name)){
			return true;
		} else {
			return false;
		}
	}	
	
	public boolean isMaleOnly(String name){
		name = cleanName(name);
		if (!isFemale(name) && isMale(name)){
			return true;
		} else {
			return false;
		}
	}	
	
	public boolean isSameSex(String name1, String name2){
		return (isMale(name1) && isMale(name2)) 
				|| (isFemale(name1) && isFemale(name2));
	}
	
	public boolean isSameSexOnly(String name1, String name2){
		return (isMaleOnly(name1) && isMaleOnly(name2)) 
				|| (isFemaleOnly(name1) && isFemaleOnly(name2));
	}
	
	/**
	 * if a word at least 3 charaters long is last but not first is considered maiden name
	 * if more words matches, only the last one is maiden
	 * @param middleName
	 * @return String[2] - 0 = maiden
	 * 						1 = rest of the middle name
	 */
	public String[] extractMaiden(String firstName, String middleName){
		String[] tmpS = {"", ""};
		Pattern pa = Pattern.compile("(\\w{3,})");
		String maiden = "";
		Matcher ma = pa.matcher(middleName);
		while (ma.find()){
			String name = ma.group();
			if (isLast(name) && !isSameSex(firstName, name)){
				maiden = name.trim();
			}
		}


		if (maiden.length() > 2){
			tmpS[0] = maiden;
			tmpS[1] = middleName.replaceAll("\\s*" + maiden + "\\s*", " ");
			tmpS[1] = tmpS[1].replaceFirst("\\s{2,}", " ").trim();
		} else {
			tmpS[1] = middleName;
		}
		return tmpS;
	}
	
	public String[] extractMaiden(String[] names){
		
		String[] tmps = { "" ,  ""};
		String[] maiden = {"", ""};
		if (!names[2].equals("") && isFemale(names[0]) && names[1].length()>3){
			tmps = extractMaiden(names[0], names[1]);
			maiden[0] = tmps[0];
			names[1] = tmps[1];
			
		}
		
		if (names.length > 5 && names[4].length() > 3 && isFemale(names[3])){
			tmps = extractMaiden(names[3], names[4]);
			maiden[1] = tmps[0];
			names[4] = tmps[1];
		}
		return maiden;
	}
	
	public static boolean isSuffix(String s){
		String[] parts = GenericFunctions.extractSuffix(s);
		return parts[1].equals(s);
	}
	/**
	 * check if we have the prefix in a particular list
	 * @param prefix
	 * @return
	 */
	public boolean findPrefix(String prefix, TreeMap<String, Double[]> where){
		SortedMap<String, Double[]> sub = where.subMap(prefix, prefix + "Z\0");
		if (sub.size() != 0){
			if (sub.firstKey().contains(prefix)){
				return true;
			} 
		}
		return false;
	}	
	
	public boolean isLastPrefix(String prefix){
		return findPrefix(prefix, last);
	}

	public boolean isMalePrefix(String prefix){
		return findPrefix(prefix, male);
	}
	
	public boolean isFemalePrefix(String prefix){
		return findPrefix(prefix, female);
	}
		
	
	public  static void main(String[] a){
		if (NameFactory.getInstance().isLastPrefix("ABRAMOVIT")){
			System.out.println("este");
		} else {
			System.out.println("nu este");
		}
		/*
		last = loadFiles("/work/eclipse/TSEARCH_RESIN/web/" + getDefaultPath() + File.separator + lastNames);
		male = loadFiles("/work/eclipse/TSEARCH_RESIN/web/" + getDefaultPath() + File.separator + maleNames);
		female = loadFiles("/work/eclipse/TSEARCH_RESIN/web/" + getDefaultPath() + File.separator + femaleNames);
		*/
	}
	
}
