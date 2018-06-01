package ro.cst.tsearch.extractor.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ro.cst.tsearch.utils.StringUtils;

public class ResultMap {

	protected static Logger logger = Logger.getLogger(ResultMap.class);
	
    protected Map m;
    private boolean read_only;

    public ResultMap() {
        m=new HashMap();
        read_only=false;
    }

    public ResultMap(Map m2) {
        m=m2;
        read_only=true;
    }

    protected void setReadOnly() {;}

    public int size() {
        return m.size();
    }

    public Iterator entrySetIterator() { 
        Iterator ret=m.entrySet().iterator();
        if (read_only)
            ret=new ReadOnlyIterator(ret);
        return ret;
    }

    public Object get(Object key) {
        return m.get(key);
    }

    public void put(Object key, Object val) throws RuntimeException {
        if (read_only)
            throw new RuntimeException("This object is read-only");
        m.put(key, val);
    }

    public void remove(Object key) throws RuntimeException {
        if (read_only)
            throw new RuntimeException("This object is read-only");
        m.remove(key);
    }
    
    public Map getMap(){
    	return m;
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        Iterator it=m.entrySet().iterator();
        while (it.hasNext()) {
            Entry e=(Entry)it.next();
            sb.append(" ------------------------- \n");
            sb.append(e.getKey());
            sb.append('\n');
            sb.append(" ------------------------- \n");
            sb.append(e.getValue());
            sb.append('\n');
        }
        return sb.toString();
    }
    
   private static final String legalFields [] = {
	   "PropertyIdentificationSet.ParcelID",
	   "PropertyIdentificationSet.PlatBook",
	   "PropertyIdentificationSet.PlatNo",
	   "PropertyIdentificationSet.CondominiumPlatBook",
	   "PropertyIdentificationSet.CondominiumPlatPage",
	   "PropertyIdentificationSet.SubdivisionLotNumber",
	   "PropertyIdentificationSet.Subdivision",
	   "PropertyIdentificationSet.SubdivisionBlock",
	   "PropertyIdentificationSet.SubdivisionUnit",
	   "PropertyIdentificationSet.SubdivisionBldg",
	   "PropertyIdentificationSet.SubdivisionPhase",
	   "PropertyIdentificationSet.SubdivisionTract",
	   "PropertyIdentificationSet.SubdivisionSection",
	   "PropertyIdentificationSet.SubdivisionTownship",
	   "PropertyIdentificationSet.SubdivisionRange",
	   "PropertyIdentificationSet.SubdivisionCond",
	   "PropertyIdentificationSet.SubdivisionName",
	   "PropertyIdentificationSet.SubdivisionNo",
	   "PropertyIdentificationSet.NcbNo",
	   "PropertyIdentificationSet.AbsNo",
	   "PropertyIdentificationSet.ARB",
	   "CrossRefSet.Book",
	   "CrossRefSet.Page",
	   "CrossRefSet.InstrumentNumber",
	   "CrossRefSet.Book_Page_Type"	   
	   };
 
   public String displayLegal(){	   
	   StringBuilder sb = new StringBuilder();
	   for(String key: legalFields){
		   String value = (String)m.get(key);
		   if(!StringUtils.isEmpty(value)){
			   sb.append(key + " = " + value + "\n");
		   }
	   }
	   ResultTable cr = (ResultTable)m.get("CrossRefSet");
	   if(cr != null){
		   sb.append("CrossRefSet=");
		   sb.append(cr.toString());
		}
	   ResultTable sd = (ResultTable)m.get("SaleDataSet");
	   if(sd != null){
		   sb.append("SaleDataSet=");
		   sb.append(sd.toString());
		}
	   ResultTable pis = (ResultTable)m.get("PropertyIdentificationSet");
	   if(pis != null){
		   sb.append("PropertyIdentificationSet=");
		   sb.append(pis.toString());
		}
	   return sb.toString();
   }
   
   private static final String addressFields [] = {
	   "PropertyIdentificationSet.StreetNo", 
	   "PropertyIdentificationSet.StreetName", 
	   "PropertyIdentificationSet.City", 
	   "PropertyIdentificationSet.Zip", 
   };
   
   public String displayAddress(){	   
	   StringBuilder sb = new StringBuilder();
	   for(String key: addressFields){
		   String value = (String)m.get(key);
		   if(!StringUtils.isEmpty(value)){
			   sb.append(key + " = " + value + "\n");
		   }
	   }

	   ResultTable pis = (ResultTable)m.get("PropertyIdentificationSet");
	   if(pis != null){
		   sb.append("PropertyIdentificationSet=");
		   sb.append(pis.toString());
		}
	   return sb.toString();
   }
   
   private static final String ownerFields [] = {
	   "PropertyIdentificationSet.OwnerLastName",
	   "PropertyIdentificationSet.OwnerFirstName",
	   "PropertyIdentificationSet.OwnerMiddleName",
	   "PropertyIdentificationSet.SpouseLastName",
	   "PropertyIdentificationSet.SpouseFirstName",
	   "PropertyIdentificationSet.SpouseMiddleName"	   	   
	   };
   
   private static final String partyFields [] = {	  
	   "PartyNameSet.OwnerFirstName",
	   "PartyNameSet.OwnerMiddleName",
	   "PartyNameSet.OwnerLastName",
	   "PartyNameSet.Suffix",
	   "PartyNameSet.isCompany",
	   "PartyNameSet.OwnerMaidenName"
	   };
   
   public String displayOwner(){	   
	   StringBuilder sb = new StringBuilder();
	   for(String key: ownerFields){
		   String value = (String)m.get(key);
		   if(!StringUtils.isEmpty(value)){
			   sb.append(key + " = " + value + "\n");
		   }
	   }
	   return sb.toString();
   }
   
   public String displayParty(){	   
	   StringBuilder sb = new StringBuilder();
	   ResultTable rt = (ResultTable) this.get("PartyNameSet");
	   if (rt != null && rt.body.length != 0){
		   for (int i=0; i<rt.body.length; i++){
			   int j = 0;
			   for(String key: partyFields){
				   if(rt.body[i].length > j && rt.body[i][j].length() != 0){
					   sb.append(key + " = " + rt.body[i][j] + "\n");
				   }
				   j++;
			   }
		   }
	   }	   
	   return sb.toString();
   }
   
   
   public String displayPartyAsXml(){	   
	   StringBuilder sb = new StringBuilder();
	   ResultTable rt = (ResultTable) this.get("PartyNameSet");
	   if (rt != null && rt.body.length != 0){
		   sb.append("<PartyNameSet>\n");
		   for (int i=0; i<rt.body.length; i++){
			   int j = 0;
			   for(String key: partyFields){
				   if(rt.body[i].length > j && rt.body[i][j].length() != 0){
					   sb.append("<" + key + ">\n");
					   sb.append(rt.body[i][j]+ "\n" );
					   sb.append("</" + key + ">");
				   }
				   j++;
			   }
		   }
	   }	   
	   sb.append("</PartyNameSet>");
	   return sb.toString();
   }
   
   public String displayParty2(){	   
	   StringBuilder sb = new StringBuilder();
	   ResultTable rt = (ResultTable) this.get("PartyNameSet");
	   if (rt != null && rt.body.length != 0){
		   for (int i=0; i<rt.body.length; i++){
			   int j = 0;
			   for(String key: partyFields){
				   if(rt.body[i].length > j && rt.body[i][j].length() != 0){
					   sb.append(key + " = " + rt.body[i][j] + "\n");
				   }
				   j++;
			   }
			   sb.append("\n");
		   }
	   }	   
	   return sb.toString();
   }
   
   public String displayKey(String key){
	   StringBuilder sb = new StringBuilder();
	   String[] s;
	   if (this.get(key) == null) return "";
	   if (this.get(key) instanceof String){
		   s = new String[1];
		   s[0] = (String) this.get(key);
	   } else {
		   s = new String[1];
		   s[0] = this.get(key).toString();
	   }
	   if (s != null){
		   for (String s1:s){
			   if (s1 != null){
				   sb.append(s1 + "\n");
			   }
		   }
	   }
	   return sb.toString();
	   
   }
   public void removeTempDef() {
	   Iterator it= entrySetIterator();
	   while (it.hasNext()) {
	       Entry e=(Entry)it.next();
	       try {
				if (((String) e.getKey()).startsWith("tmp")){
					it.remove();
				}
			} catch (Exception e2) {
				if (e2 instanceof ClassCastException){
					logger.error(e2 + " on key " + e.getKey());
				}
			}
	   }
   }
}
