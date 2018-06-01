package ro.cst.tsearch.servers.response;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.utils.StringUtils;

public class InfSet implements Serializable {

    static final long serialVersionUID = 10000000;
    
    protected Map map = new TreeMap();

    public InfSet(String[] a) {
       // for (int i=0; i<a.length; i++) 
       //     map.put(a[i], "");
    }
    
    public void addAttribute(String key) {
        map.put(key, "");
    }

    
	public String getAtribute(String key) {
        if (!map.containsKey(key))
            //throw new IllegalArgumentException("InfSet atribute unknown : "+key);
        	return "";
        return (String)map.get(key);
    }

	public void setAtribute(String key, String value) {
        map.put(key, value);
    }
	
	public void removeAtribute(String key){
		if(map.containsKey(key)){
			map.remove(key);
		}
	}
	
	public boolean hasAtribute(String key) {
        if (!map.containsKey(key))
        	return false;
        return true;
    }

    public Iterator keyIterator() {
        return new ro.cst.tsearch.extractor.xml.ReadOnlyIterator(map.keySet().iterator());
    }

    public String toString() {
//        return map.toString();
        StringBuffer sb=new StringBuffer();
        Iterator it=map.keySet().iterator();
        int i;
        for (i=0; it.hasNext(); i++) {
            String key=(String)it.next();
            String s=key+"="+map.get(key);
            sb.append(s);
            if (s.length()<=38)
                sb.append("                                      ".substring(s.length()));
            else
                sb.append('\n');
            if (i%2==1)
                sb.append('\n');
        }
//        if (!(i%2==1))
//            sb.append('\n');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (!(o instanceof InfSet))
            return false;
        return equals((InfSet)o);
    }

    public boolean equals(InfSet is) {
        if (is.map.size()!=map.size())
            return false;
        Iterator it=map.entrySet().iterator();
        while (it.hasNext()) {
            Entry e=(Entry)it.next();
            String key=(String)e.getKey();
            String value=(String)e.getValue();
            if (!is.map.containsKey(key) || !value.replaceAll("\\r?\\n", " ").trim().equals(((String)is.map.get(key)).replaceAll("\\r?\\n", " ").trim()))
                return false;
        }
        return true;
    }

    /**
     * display infset, only non-empty fields
     * @return text representation of the infset
     */
    public String display(){    	   
    	// don't want to print owner details set
    	
    	StringBuilder sb = new StringBuilder();        
        for(Object keyObj: map.keySet()) {
        	Object valObj = map.get(keyObj);
        	if(keyObj instanceof String && valObj instanceof String){
	        	String key = (String)keyObj;
	        	String value = (String)valObj;
	        	if(!"".equals(value)) {
	        		sb.append(key); sb.append(": "); sb.append(value);sb.append(' ');
	        	}
        	}
        }
        return sb.toString();
   }
    
    /**
     * Return a text representation of the two persons in a NameSet infset
     * @param infset
     * @return
     */
    public String displayNameSet(){
    	
    	InfSet infset = this;
    	
    	String f1 = infset.getAtribute("OwnerFirstName");    	
    	String m1 = infset.getAtribute("OwnerMiddleName");
    	String l1 = infset.getAtribute("OwnerLastName");
    	
    	String f2 = infset.getAtribute("SpouseFirstName");    	
    	String m2 = infset.getAtribute("SpouseMiddleName");
    	String l2 = infset.getAtribute("SpouseLastName");

    	String p1 = (f1 + " " + m1 + " " + l1).trim();
    	String p2 = (f2 + " " + m2 + " " + l2).trim();
    	    	
    	boolean ep1 = StringUtils.isEmpty(p1);
    	boolean ep2 = StringUtils.isEmpty(p2) || (StringUtils.isEmpty(m2) && StringUtils.isEmpty(f2) && l2.equals(l1));
    	
    	if(!ep1){
    		if(!ep2){
    			return p1 + " / " + p2;
    		} else {
    			return p1;
    		}
    	}else{
    		if(!ep2){
    			return p2;
    		}
    	}    	
    	return "";
    }

    /**
     * Return a text represenation of a crossreference set
     * @return
     */
    public String displayCrossRefSet(){
    	
    	InfSet infset = this; 
    	String book = infset.getAtribute("Book");
    	String page = infset.getAtribute("Page");
    	String instr = infset.getAtribute("InstrumentNumber");
    	String bookPage =  infset.getAtribute("Book_Page");
    	
    	// determine book_page
    	String bp = "";
    	if(!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){
    		if(!"0".equals(book) && ! "0".equals(page)){
    			bp += book + "_" + page;
    		}
    	}
    	if(StringUtils.isEmpty(bp) && !StringUtils.isEmpty(bookPage) && !"0_0".equals(bookPage)){
    		bp = bookPage;
    	}
    	
    	// construct return value
    	String retVal = "";
    	if(!StringUtils.isEmpty(instr)){
    		if(!StringUtils.isEmpty(bp)){
    			retVal = instr + " (" + bp + ")";
    		} else {
    			retVal = instr;
    		}
    	} else {
    		if(!StringUtils.isEmpty(bp)){
    			retVal = bp;
    		}
    	}
    	 
    	return retVal;
    }
    
    public boolean isEmpty(){
    	if(map == null || map.isEmpty()) {
    		return true;
    	}
    	for (Object iterable_element : map.values()) {
			if (iterable_element instanceof String) {
				String new_name = (String) iterable_element;
				if(StringUtils.isNotEmpty(new_name)) {
					return false;
				}
			} else {
				return false;
			}
		}
    	return true;
    }
    
}