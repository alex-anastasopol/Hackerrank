package ro.cst.tsearch.search.filter.testnamefilter;
import java.util.Vector;
import java.util.Iterator;
public class GenericNameFilterTestError {
	private Vector<String> errorMessage = null;
	
	public GenericNameFilterTestError (){
		errorMessage = new Vector<String>();
	}
	
	public boolean isError(){
		return errorMessage.size() > 0;
	}
	
	public void setError(String s){
		errorMessage.add(s);
	}
	
	public String toString(){
		String em = "";
		Iterator i = errorMessage.iterator();
		if (i.hasNext()) em += i.next();
		while (i.hasNext()){
			em += "<br>" + i.next();
		}
		return em;
		
	}
}
