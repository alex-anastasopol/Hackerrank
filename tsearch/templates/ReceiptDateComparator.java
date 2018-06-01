package ro.cst.tsearch.templates;

import java.util.Comparator;
import java.util.Date;

import ro.cst.tsearch.generic.Util;

import com.stewart.ats.tsrindex.client.Receipt;

public class ReceiptDateComparator implements Comparator<Receipt> {
/*
SimpleDateFormat simpleDF1 = new SimpleDateFormat("MMMM d, yyyy");
Date date = Util.dateParser3( dateStr );
if( date==null ){
	return simpleDF1.format(new Date());
}
return simpleDF1.format( date ); */	  
	  
	  public int compare (Receipt r1, Receipt r2)
	  {
		  Date d1=null, d2=null;
		  
		  d1 = Util.dateParser3(r1.getReceiptDate().replace('-', '/'));
		  d2 = Util.dateParser3(r2.getReceiptDate().replace('-', '/'));
		  
		  if (d1!=null && d2!=null)
		  {
			  return d1.compareTo(d2);
		  }
		  else if (d1 == null && d2==null)
		  {
			  return 0;
		  }
		  else if (d1!=null && d2==null)
		  {
			  return 1;
		  }
		  else if (d1==null && d2!=null)
		  {
			  return -1;
		  }
		  return 0;
	  }
  }