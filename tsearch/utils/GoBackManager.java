package ro.cst.tsearch.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;

import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.document.sort.RecordedDateComparator;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;


public class GoBackManager {

	List<Instrument> gbInstList=new ArrayList<Instrument>();
	List<Instrument> historyInstList=new ArrayList<Instrument>();
	public List<Instrument> getGbInstList() {
		return gbInstList;
	}
	public void addGbInstList(Instrument i){
		gbInstList.add(i);
	}
	public void setGbInstList(List<Instrument> gbInstList) {
		this.gbInstList = gbInstList;
	}
	public List<Instrument> getHistoryInstList() {
		return historyInstList;
	}
	public void setHistoryInstList(List<Instrument> historyInstList) {
		this.historyInstList = historyInstList;
	}
	public void addHistoryInstList(Instrument i){
		historyInstList.add(i);
	}
    public boolean containsInst(Instrument i){
    	for (Instrument element : getHistoryInstList()) {
			if (i.equals(getHistoryInstList())) return true;
		}
        return false;  
    }
    public String getDateForSearch(Instrument ins,String format,long searchId) {
    	DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getDocManager();
		Calendar cal = Calendar.getInstance();
		String ret=null;
		Date d = null;
		Transfer tr=null;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try{
		documentsManager.getAccess();
		List<TransferI> allTransfers =documentsManager.getTransferList(true);
		Collections.sort(allTransfers,new RecordedDateComparator());
		int i = allTransfers.lastIndexOf((TransferI)documentsManager.getDocument(ins));
		
		if (i+1<allTransfers.size()){
			tr=(Transfer)allTransfers.get(i+1);
		    cal.add(Calendar.DATE, -1);
		    if (tr.getInstrumentDate()!=null&&tr.getInstrumentDate().before(cal.getTime())){
		    	d=tr.getInstrumentDate();
		    }
		    else{
		        d=tr.getRecordedDate();	
		    }
		}
				
    }
	finally{
	  documentsManager.releaseAccess();
    }
	if (d!=null){
			cal.setTime(d);
		    ret=sdf.format(cal.getTime());
	}
	return ret;
   }
   public String getDateForSearchBrokenChain(Instrument ins,String format,long searchId){
	   DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getDocManager();
		Calendar cal = Calendar.getInstance();
		String ret=null;
		Date d = null;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		try{
		documentsManager.getAccess();
		Transfer tr=(Transfer)documentsManager.getDocument(ins);
		if (tr.getRecordedDate().before(tr.getInstrumentDate()))
			d=tr.getRecordedDate();
		else 
			d=tr.getInstrumentDate();
		}
		finally
		{
	    documentsManager.releaseAccess();
		}
		if (d!=null){
			cal.setTime(d);
		    ret=sdf.format(cal.getTime());
	     }
		return ret;	
   }
   
   public Set<NameI> getNamesForBrokenChain(Instrument ins,long searchId){
	   Set<NameI> allNames	= new HashSet<NameI>();
	   Set<NameI> ret	= new HashSet<NameI>();   
	   Set<NameI> cand  = new HashSet<NameI>();
	   DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
	   
	   try
    	  {
    	   documentsManager.getAccess();  			
    	   for (Instrument  obj: getHistoryInstList()) {
    			  allNames.addAll(((RegisterDocumentI)documentsManager.getDocument(obj)).getGrantor().getNames()); 
    	   }	  
    	   cand=((RegisterDocumentI)documentsManager.getDocument(ins)).getGrantee().getNames(); 	       	   
    	   
    	   
    	   
    	   for (NameI cname : cand) {
			  for (NameI rname : allNames) {
				 
				  if (GenericNameFilter.calculateScore(cname, rname)>=NameFilterFactory.NAME_FILTER_THRESHOLD)
					  ret.add(cname);
			    
			  }
		
    	     }
    	      	  
    	  } 
    	  finally
  		  {
  	       documentsManager.releaseAccess();
  		  }
     return ret;
   
   
   }
  public Instrument getInstrument(int pos,long searchId){
	  DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();  
	 Instrument ins=getGbInstList().get(pos);
	 return ins;   
  }
   
   
 }   