package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LotInterval {

    public static final int		TYPE_NUMERIC		= 0;
    public static final int		TYPE_CHARACTER		= 1;
    
    int type = TYPE_NUMERIC;
    
    int nLow, nHigh;
    String cLow, cHigh;
    
    
    boolean expandImproperInterval = true;
    
    public static final String COMPOSITE_NUMBER = "([0-9]*)([a-zA-Z]?[a-zA-Z]?)";
    
    static Pattern p = Pattern.compile("(\\d+)-?([a-zA-Z])");
    static Pattern pCharNumber = Pattern.compile("([a-zA-Z])-?(\\d+)");
    
    protected Set<String> extraLots = new TreeSet<String>();
    
    public LotInterval(String lot) {
    	
    	lot = lot.trim();
    	lot = lot.replaceAll("\\s*-\\s*", "-");
    	lot = lot.replaceAll(",", " ").replaceAll("\\s{2,}", " ").trim(); 	//let's support "," not just space as separator
    	if(lot.contains(" ")) {
    		String[] lots = lot.split(" ");
    		for (String singleLot : lots) {
				LotInterval intermediaryInterval = new LotInterval(singleLot);
				extraLots.addAll(intermediaryInterval.getLotList());
			}
    	} else {
    		Matcher m = p.matcher(lot);
	    	if (m.matches()) { //we have something like 101-J
	    		nLow = nHigh = Integer.parseInt(m.group(1));
				cLow = cHigh = m.group(2);
				type = TYPE_CHARACTER;
				return;
	    	}
	    	else {
	    		m = pCharNumber.matcher(lot);
	    		if(m.matches()){	//we have something la J-101
	    			nLow = nHigh = Integer.parseInt(m.group(2));
	    			cLow = cHigh = m.group(1);
	    			type = TYPE_CHARACTER;
	    			return;
	    		}
	    	}
	    	
	    	String[] a = lot.split("-");
	    	
	    	if (a.length == 2) {
		    	
	    		String rangeMatch = "(^" + COMPOSITE_NUMBER + "-" + COMPOSITE_NUMBER + "$)";
				Matcher candidateRangeTokens = Pattern.compile(rangeMatch).matcher(lot);
				
				if (candidateRangeTokens.matches()) {
					
					try {
						if (candidateRangeTokens.group(2).length() != 0){
							nLow = Integer.parseInt(candidateRangeTokens.group(2));
						}
						if (candidateRangeTokens.group(4).length() != 0){
							nHigh   = Integer.parseInt(candidateRangeTokens.group(4));
						}
	
						cLow = candidateRangeTokens.group(3); 
						boolean startExist = !(cLow == null || "".equals(cLow));
						cHigh   = candidateRangeTokens.group(5);
						boolean endExist = !(cHigh == null || "".equals(cHigh));
	
						if (startExist || endExist) {
							type = TYPE_CHARACTER;
	
							cLow = startExist ? cLow : cHigh;
							cHigh   = endExist   ? cHigh   : cLow;
						}
						
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Invalid range format " + e);
					}
				} else {
					extraLots.add(lot);	//put the original lot. example lot: M3-51
				}
				
	    	} else {
	    		
	    		if(lot.isEmpty()) {
	    			extraLots.add("");
					return;
	    		} else {
	    			String rangeMatch = "(^" + COMPOSITE_NUMBER + "$)";
					Matcher candidateRangeTokens = Pattern.compile(rangeMatch).matcher(lot);
					
					if (candidateRangeTokens.matches()) {
						
						try {
							
							nLow = nHigh = 0;
							
							String numberGroup = candidateRangeTokens.group(2);
							if (numberGroup != null && !"".equals(numberGroup))
								nLow = nHigh = Integer.parseInt(numberGroup);
		
							cLow = candidateRangeTokens.group(3); 
							boolean startExist = !(cLow == null || "".equals(cLow));
							cHigh = cLow;
		
							if (startExist) {
								type = TYPE_CHARACTER;
							}
							
						} catch (NumberFormatException e) {
							//throw new IllegalArgumentException("Invalid range format " + e);
							nLow = nHigh = 0;
						}
					} else {
						extraLots.add(lot); //put the original lot
					}
				
	    		}
	    		
	    	}
    	}
    }
    
    public int getLow() {
    	return nLow;
    }
    
    public int getHigh() {
    	return nHigh;
    }
    
    public boolean contains(LotInterval other) {
    	
    	if ( (nLow <= other.nHigh && nHigh >= other.nHigh) || 
				(nLow <= other.nLow && nHigh >= other.nLow) || 
				(nLow <= other.nLow && nHigh >= other.nHigh) ||
				(nLow >= other.nLow && nHigh <= other.nHigh) ) {
    		
    		if (type == TYPE_CHARACTER) {
    			
    			return (cLow.compareToIgnoreCase(other.cHigh) <= 0 && cHigh.compareToIgnoreCase(other.cHigh) >= 0) || 
				    		(cLow.compareToIgnoreCase(other.cLow) <= 0 && cHigh.compareToIgnoreCase(other.cLow) >= 0) || 
				    		(cLow.compareToIgnoreCase(other.cLow) <= 0 && cHigh.compareToIgnoreCase(other.cHigh) >= 0) ||
				    		(cLow.compareToIgnoreCase(other.cLow) >= 0 && cHigh.compareToIgnoreCase(other.cHigh) <= 0);
    		}
    		
    		return true;
    	}
    	
    	return false;
    }
    
    public String toString() {
    	
    	if (type == TYPE_NUMERIC)
    		return "Lot [" + nLow + "] - [" + nHigh + "]";
    	else
    		return "Lot [" + nLow + cLow + "] - [" + nHigh + cHigh + "]";
    }
    
    public boolean related(LotInterval newInterval) {
    	
    	if (type != newInterval.type)
    		return false;
    	
    	if ( (nLow - 1 <= newInterval.nLow && newInterval.nLow <= nHigh + 1) ||
    			(nLow - 1 <= newInterval.nHigh && newInterval.nHigh <= nHigh + 1) ||
    			(nLow - 1 >= newInterval.nLow && newInterval.nHigh >= nHigh + 1) ) {
    		
    		if (type == TYPE_CHARACTER) {
    			if (cLow == null || cHigh == null || newInterval.cLow == null || newInterval.cHigh == null)
    				return false;
    			
    			return (cLow.compareToIgnoreCase(newInterval.cLow) <= -1  && newInterval.cLow.compareToIgnoreCase(cHigh) <= 1) ||
		    			(cLow.compareToIgnoreCase(newInterval.cHigh) <= -1 && newInterval.cHigh.compareToIgnoreCase(cHigh) <= 1) ||
		    			(cLow.compareToIgnoreCase(newInterval.cLow) >= - 1 && newInterval.cHigh.compareToIgnoreCase(cHigh) >= 1);
    		}
    		
    		return true;
    	}
    	
	    return false;
    }
    
    public List<String> getLotList() {
    	return getLotList(100);
    }
    
    public List<String> getLotList(int limitForList) {
    	
    	if(!extraLots.isEmpty()) {
    		return new ArrayList<String>(extraLots);
    	}
    	
    	if(limitForList < 0) {
    		limitForList = 100;
    	}
    	List<String> lotList = new ArrayList<String>();
    	
    	if(type == TYPE_CHARACTER && nLow == nHigh && cLow.equals(cHigh)){
    		
    		if(nLow == 0) {
    			lotList.add(cLow);
    		} else {
	    		lotList.add(nLow + cLow);
	    		lotList.add(nLow + "-" + cLow);
	    		lotList.add(cLow + nLow);
	    		lotList.add(cLow + "-" + nLow);
    		}
    			
    		return lotList;
    	}
    	
    	for (int i = nLow; i <= nHigh && lotList.size() < limitForList; i ++) {
    		if (type == TYPE_CHARACTER) {
    			for (char c = cLow.charAt(0); c <= cHigh.charAt(0); c ++) {
    				String first=String.valueOf(i);
    				if (i == 0) {
    					lotList.add( ""+ c);
    				}
    				else{
    					lotList.add(first+  c);
    				}	
    			}
    		} else {
    			lotList.add(String.valueOf(i));
    		}
    	}
    	return lotList;
	}
    
    public void add(LotInterval newInterval) {
    	
    	nLow = nLow < newInterval.nLow ? nLow : newInterval.nLow;
    	nHigh = nHigh > newInterval.nHigh ? nHigh : newInterval.nHigh;
    	
    	if (type == TYPE_CHARACTER) {
    		cLow = cLow.compareToIgnoreCase(newInterval.cLow) < 0 ? cLow : newInterval.cLow;
    		cHigh = cHigh.compareToIgnoreCase(newInterval.cHigh) > 0 ? cHigh : newInterval.cHigh;
    	}
    }
    
    public static void main(String[] args) {
    	
    	//"1, 2-3, 5 thru 10, 6&7, 10 & 11, 44- 45 46";
    	// "1a-10c, 10b-12b, 5a, 5x, 12d";
	    /*String ref = "1-10, 10-12, 5, 15, 13";
	    String cand = "10-11";
	    
	    System.out.println("REF : [" + ref + "]");
	    System.out.println("CAND : [" + cand + "]");
	    
	    Vector refs = LotMatchAlgorithm.prepareLotInterval(ref);
	    Vector cands = LotMatchAlgorithm.prepareLotInterval(cand);
		
	    for (int i = 0; i < cands.size(); i++) {
	        
	        LotInterval candInterval = (LotInterval) cands.elementAt(i);
	        
	        boolean ok = false;
	        for (int j = 0; j < refs.size(); j++) {
	            
	        	LotInterval refInterval = (LotInterval) refs.elementAt(j);
	            
	            if (refInterval.contains(candInterval)) {
	            	
	            	System.out.println("OK 	- " + candInterval.toString());
	            	ok = true;
	            }
	        }
	        
	        if (!ok) 
	        	System.out.println("FAIL 	- " + candInterval.toString());
	    }*/
    	
    	
    	List<String> list = new ArrayList<String>();
    	
    	list.add(" ");
    	list.add("1 2");
    	list.add("101 -J ");
    	list.add("101J");
    	list.add("M3-51");
    	list.add("1- 2");
    	list.add("1-3");
    	list.add("1 2 3");
    	list.add("1, 2 4");
    	list.add("1, 2 3- 5");
    	list.add("AAA");
    	
    	for (String lotAsString : list) {
        	LotInterval li = new LotInterval(lotAsString);
        	System.err.println("\"" + lotAsString + "\" -> " + li.getLotList());	
		}
    	
    	
    }

	
}