package ro.cst.tsearch.utils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.document.sort.RecordedDateComparator;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;




public class GBManager {
	public static final String ERR_INVALID_SEARCH_NAME="No valid grantor to go back with";
	private List<String> gbTransfers		= Collections.synchronizedList(new ArrayList<String>());
	private List<String> gbTransferHistory	= Collections.synchronizedList(new ArrayList<String>());
	private List<String> gbDocsEvidence	= Collections.synchronizedList(new ArrayList<String>());
	private transient HashMap<String,String> err=new HashMap<String,String>();
	public List<String> getGbTransfers() {
		return gbTransfers;
	}

	public void addGbTransfers(String id) {
		this.gbTransfers .add(id);
	}
	
	public List<String> getGbTransferHistory() {
		return gbTransferHistory;
	}
	
	public void addGbTransferHistory(String id) {
		this.gbTransferHistory .add(id);
	}
	
	public boolean containsId(String id){
		return gbTransferHistory.contains(id);
	}
	
	public String getDateForSearch(String id,String format,long searchId) {
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
			int i = allTransfers.lastIndexOf((TransferI)documentsManager.getDocument(id));
			
			if (i+1<allTransfers.size()){
				tr=(Transfer)allTransfers.get(i+1);
			    cal.add(Calendar.DATE, -1);
			    if (tr.getRecordedDate()!=null&&tr.getRecordedDate().before(cal.getTime())){
			    	d=tr.getRecordedDate();
			    }
			    else{
			        d=tr.getInstrumentDate();	
			    }
			}
		}
		catch(Exception e){
			  e.printStackTrace(); 
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

	public String getDateForSearchBrokenChain(String id,String format,long searchId){
	   DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		Calendar cal = Calendar.getInstance();
		String ret=null;
		Date d = null;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		try{
			documentsManager.getAccess();
			Transfer tr=(Transfer)documentsManager.getDocument(id);
			d=tr.getRecordedDate();
		}
		catch(Exception e){
			  e.printStackTrace(); 
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

	public Set<NameI> getNamesForSearch(String id,long searchId){
		   Set<NameI> allNames	= new HashSet<NameI>();
		   Set<NameI> ret	= new HashSet<NameI>();   
		   Set<NameI> cand  = new HashSet<NameI>();
		   DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		   boolean add=false;
		   try{
		 	   documentsManager.getAccess();  			
		 	   
		 	   allNames.addAll(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getOwners().getNames());
		 	   RegisterDocumentI rd=(RegisterDocumentI)documentsManager.getDocument(id);
		 	   if(rd!=null)
		 	   	cand=rd.getGrantor().getNames(); 	       	   
		 	   
		 	   for (NameI cname : cand) {
					add=true;  
		 		    for (NameI rname : allNames) {
						 
						  if (GenericNameFilter.calculateScore(cname, rname)>0.90) {
							  add=false;
							  break;
						  }
					  }
		 		   if (add)ret.add(cname);
		 	     }
		 	      	  
		   } 
		   catch(Exception e){
			  e.printStackTrace(); 
		   }
		   finally{
		       documentsManager.releaseAccess();
		   }
		   return ret;
	}
	
	
	
	
	
	
	
	
	
	
	public Set<NameI> getNamesForBrokenChain(String id,long searchId){
		   Set<NameI> allNames	= new HashSet<NameI>();
		   Set<NameI> ret	= new HashSet<NameI>();   
		   Set<NameI> cand  = new HashSet<NameI>();
		   DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		   boolean add=false;
		   try{
		 	   documentsManager.getAccess();  			
		 	   for (String  obj:getGbTransferHistory() ) {
		 			  allNames.addAll(((RegisterDocumentI)documentsManager.getDocument(obj)).getGrantor().getNames()); 
		 			  
		 	   }	  
		 	   allNames.addAll(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getOwners().getNames());
		 	   RegisterDocumentI rd=(RegisterDocumentI)documentsManager.getDocument(id);
		 	   if(rd!=null)
		 		   cand=rd.getGrantee().getNames(); 	       	   
		 	   
		 	   for (NameI cname : cand) {
					add=true;  
		 		    for (NameI rname : allNames) {
						 
						  if (GenericNameFilter.calculateScore(cname, rname)>0.90) {
							  add=false;
							  break;
						  }
					  }
		 		   if (add)ret.add(cname);
		 	     }
		 	     
		 	   
		 	  TransferI transForCurrentOwner = documentsManager.getLastTransfer(true);
			   if( transForCurrentOwner!=null && id.equalsIgnoreCase(transForCurrentOwner.getId()) ){
				 
				 List<RegisterDocumentI> list =   documentsManager.getDocumentsBefore(transForCurrentOwner.getRecordedDate(), true);
				 
				 TransferI previousTransfer = null;
				 
				 for(RegisterDocumentI regDoc:list){
					 if(regDoc instanceof Transfer && DocumentTypes.TRANSFER.equalsIgnoreCase(regDoc.getDocType())){
						 TransferI curentTransfer = (TransferI)regDoc;
						 if(previousTransfer==null){
							 previousTransfer = curentTransfer;
						 }else{
							 if(previousTransfer.before(curentTransfer)){
								 previousTransfer = curentTransfer;
							 }
						 }
					 }
				 }
				  
				 if(previousTransfer!=null && previousTransfer.before(transForCurrentOwner)){
					 cand = previousTransfer.getGrantee().getNames(); 
					 for (NameI cname : cand) {
							add=true;  
				 		    for (NameI rname : allNames) {
								 
								  if (GenericNameFilter.calculateScore(cname, rname)>0.90) {
									  add=false;
									  break;
								  }
							  }
				 		   if (add)ret.add(cname);
				 	     }
					 
				 }
			   }
		 	   
		   }
		   catch(Exception e){
				  e.printStackTrace(); 
			   }
		   finally{
		       documentsManager.releaseAccess();
		   }
		   
		   return ret;
	}
	
	public RegisterDocument getDoc(String id,long searchId){
		 DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		 RegisterDocument doc=null;  
		   try{
			   documentsManager.getAccess(); 
			   doc=(RegisterDocument) documentsManager.getDocument(id);	   
		   } 
		   catch(Exception e){
				  e.printStackTrace(); 
			   }
		   finally{
			documentsManager.releaseAccess();
		   }
		  return doc;  
	}

	public Set<NameI> getGBHistoryNames(long searchId){
		Set<NameI> ret	= new HashSet<NameI>();
		DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		try{
		   documentsManager.getAccess(); 
		   for (String id : getGbTransferHistory()) {
			ret.addAll(((RegisterDocument) documentsManager.getDocument(id)).getGrantor().getNames());
		   }
		}
		catch(Exception e){
			  e.printStackTrace(); 
		   }
		finally{
			documentsManager.releaseAccess();
		}
		return ret; 
	}

	public List<String> getGbDocsEvidence() {
		return gbDocsEvidence;
	}

	public void setGbDocsEvidence(List<String> gbDocsEvidence) {
		this.gbDocsEvidence = gbDocsEvidence;
	}
	public void addGbDocsEvidence(String id) {
		if(!this.gbDocsEvidence.contains(id))this.gbDocsEvidence.add(id);
	}
	public void clearGbDocsEvidence() {
		this.gbDocsEvidence.clear();
	}

	public HashMap<String, String> getErr() {
		if (err==null) {
			err = new HashMap<String, String>();
		}
		return err;
		}

		public void setErr(String key ,String value) {
		if (!getErr().containsKey(key))
			err.put(key, value);

		}
}
