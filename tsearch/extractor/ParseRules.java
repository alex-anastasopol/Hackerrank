package ro.cst.tsearch.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ParseRules {
	DEFAULT_RULE("",""),
	APN_ALL_VALUES("","",
			new ParseRule("PropertyIdentificationSet.ParcelID", "\\d{17,18}",new int[]{}),
			new ParseRule("PropertyIdentificationSet.ParcelID", "\\d{15}",new int[]{15}),
			new ParseRule("PropertyIdentificationSet.ParcelID","[\\d*[A-Z]{0,2}\\d]{20}" ,new int[]{20}),
			new ParseRule("PropertyIdentificationSet.ParcelID", "\\d{12}",new int[]{12}),
			new ParseRule("PropertyIdentificationSet.ParcelID", "\\d{9,11}",new int[]{}),
			new ParseRule("PropertyIdentificationSet.ParcelID", "(?i)\\d+[A-Z]{1,2}\\d+{10}",new int[]{10}),
			new ParseRule("PropertyIdentificationSet.ParcelID", "\\d{8}",new int[]{8})
			),
	
	APN_FL_VOLUSIA("Volusia", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID3","\\d{19}",new int[]{19}),
					new ParseRule("PropertyIdentificationSet.ParcelID","\\d{12,14}",new int[]{12,14})
					),
					
	APN_FL_SARASOTA("Sarasota", "FL",
							new ParseRule("PropertyIdentificationSet.ParcelID","\\d+{10}",new int[]{10})
							),				
					
	APN_FL_SEMINOLE("Seminole", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","(?:XF)?((\\d+[A-Z0-9]{1,2}\\d+[A-Z0-9]+\\d+))",new int[]{18})
					),				
	APN_FL_POLK("Polk", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","(?:XF)?((\\d+[A-Z0-9]{1,2}\\d+[A-Z0-9]+\\d+))",new int[]{18})
					),		
	APN_FL_MIAMI_DADE("Miami-Dade", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","(?:XF)?((\\d+[A-Z0-9]{1,2}\\d+[A-Z0-9]+\\d+))",new int[]{13})
					),
	APN_FL_PINELLAS("Pinellas", "FL",
							new ParseRule("PropertyIdentificationSet.ParcelID","\\d{18,20}",new int[]{18,20})
							),					
	APN_FL_PASCO("Pasco", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","(?:XF)?((\\d+[A-Z0-9]{1,2}\\d+[A-Z0-9]+\\d+))",new int[]{19})
					),		
	APN_FL_PALM_BEACH("Palm Beach", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","\\d{17}",new int[]{17})
					),	
	APN_FL_LEE("Lee", "FL",
							new ParseRule("PropertyIdentificationSet.ParcelID","(\\d+[A-Z0-9]{1,2}\\d+[A-Z0-9]+)",new int[]{17,19})
					),
	APN_FL_DUVAL("Duval", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID","\\d{10}",new int[]{10})
					),		
	APN_FL_COLLIER("Collier", "FL",
					new ParseRule("PropertyIdentificationSet.ParcelID3","\\d{12}",new int[]{12}),
					new ParseRule("PropertyIdentificationSet.ParcelID","\\d{11}",new int[]{11})					
					),		
	APN_FL_BROWARD("Broward", "FL",
//					new ParseRule("/","/",new int[]{}),
					new ParseRule("PropertyIdentificationSet.ParcelID3","\\d+[A-Z0-9]{1,2}\\d+",new int[]{16}),
					new ParseRule("PropertyIdentificationSet.ParcelID","(?:XF)?((\\d+[A-Z0-9]{1,2}\\d+))",new int[]{10,12})					
					),		
	APN_FL_BREVARD("Brevard", "FL", 
					new ParseRule("PropertyIdentificationSet.ParcelID","[\\^\\d\\^\\d\\^\\d\\^\\d\\^C\\^A]\\d+[A-Z0-9]{1,2}\\d+",new int[]{7,9,10,12,14,20,21}),//\\d+[A-Z0-9]{1,2}\\d+
					new ParseRule("PropertyIdentificationSet.GeoNumber","(?is)\\b\\d+[A-Z0-9]+{2}\\d+\\b",new int[]{9,11})
					),		
	APN_FL_HILLSBOROUGH("Hillsborough", "FL",
			new ParseRule("-","-",new int[]{}),
			new ParseRule("PropertyIdentificationSet.ParcelID3","(?:XF)?(([A-Z]{1}\\d+[A-Z0-9]{1,2}\\d+))",new int[]{22}),
			new ParseRule("PropertyIdentificationSet.ParcelID","\\d{9,11}",new int[]{10})
			);
	
	public final static String REMOVE_DASH="-";		
    
		
//	APN_FL_OK("","","", new ParseRule());
	
	private String county;
	private String state;
	private List<ParseRule> rules=new ArrayList<ParseRule>();
	ParseRules(String county, String state, ParseRule... rules) {
		this.county = county;
		this.state = state;
		for (ParseRule parseRule : rules) {
			this.rules.add(parseRule);
		}
	}
	
	public String getCounty() {
		return county;
	}

	public String getState() {
		return state;
	}

	public List<ParseRule> getRules() {
		return rules;
	}

	
	public static Map<String,String> getPropertyFromPatterns(List<ParseRule> propertyPatterns, String stringToParse){
		Map<String,String> results= new HashMap<String,String>();
//		Set<String> propertSet = propertyPatterns.keySet();
		boolean alreadyFound = false;
//		for (String key : propertSet) {
//			List<ParseRule> patterns = propertyPatterns.get(key);
			for(Iterator<ParseRule> iterator = propertyPatterns.iterator(); iterator.hasNext()&&!alreadyFound; ){
				ParseRule pattern = iterator.next();
				if (pattern.getStringRule().equals(REMOVE_DASH)){
					stringToParse = stringToParse.replaceAll(REMOVE_DASH, "");
				}
				Pattern p = Pattern.compile(pattern.getStringRule());
				Matcher m = p.matcher(stringToParse);
				if (m.find() && !alreadyFound){
					String group = m.group();
					if (group.startsWith("XF")){
						group = m.group(1);
					}
					
					boolean b=false;
					int length = pattern.getStringLength().length;
					if (length==2){
					b = group.length() >= pattern.getStringLength()[0] && group
							.length() <= pattern.getStringLength()[1];
					}else if (length==1){
						b =  pattern.getStringLength()[0]==group.length();
//						results.put(pattern.getPropertyIdentification(), b);
					}else{
						for(int i=0;i<length;i++){
							if(pattern.getStringLength()[i]==group.length()){
								results.put(pattern.getPropertyIdentification(), group);
							}
						}
					}
					 
					if ((length == 0)
						|| b) {
					results.put(pattern.getPropertyIdentification(), group);
					alreadyFound = true;
					if (m.find()) {
						results.put(pattern.getPropertyIdentification(), "");
						alreadyFound = false;
					}
				}
				}
			}
//		}
		return results;
	}
	
	public static ParseRules getParseRuleByCountyAndState(String county,String state){
		ParseRules ruleToBeUsed=DEFAULT_RULE;
		for (ParseRules rules : ParseRules.values()) {
			if (rules.getCounty().equals(county) && rules.getState().equals(state)){
				ruleToBeUsed = rules;
			}	
		}
		return ruleToBeUsed;
	}
	
}

/*
 * public final String county; public final String state; public final
 * Map<String, List<String>> propertyPatterns;
 * 
 * public static class Builder { // Required parameters public final String
 * county; public final String state; // Optional parameters - initialized to
 * default values public Map<String, List<String>> propertyPatterns;
 * 
 * public Builder(String county, String state) { this.county = county;
 * this.state = state; }
 * 
 * public ParseRules build() { return new ParseRules(this); }
 * 
 * public Builder setPropertyPatterns(Map<String, List<String>>
 * propertyPatterns) { this.propertyPatterns = propertyPatterns; return this; }
 * }
 * 
 * public void addPropertyPatterns(String key,List<String> patterns){ if
 * (propertyPatterns.containsKey(key)){ List<String> list =
 * propertyPatterns.get(key); list.addAll(patterns); }else{
 * propertyPatterns.put(key, patterns); } }
 * 
 * private ParseRules(Builder builder) { this.state = builder.state; this.county
 * = builder.county; this.propertyPatterns = builder.propertyPatterns; }
 */