package ro.cst.tsearch.search.filter.testnamefilter;
import java.util.Vector;
import java.math.BigDecimal;
public class GenericNameFilterTestBean {
	String ref_first = "";
	String ref_middle = "";
	String ref_last = "";
	String cand_first = "";
	String cand_middle = "";
	String cand_last = "";
	String generateAranjaments = "";
	String error = "";
	Vector fileList = new Vector<BigDecimal>();
	
	
	public void setFileList(String s){
		fileList.add((BigDecimal) new BigDecimal(s));
	}
	
	public Vector<BigDecimal> getFileList(){
		return fileList;
	}
	public void reset(){
		fileList = new Vector<BigDecimal>();
		setGenerateAranjaments(null);
		setError("");
	}
	public void setError(String e){
		error = e;
	}
	
	public void setRef_first(String value){
		ref_first = value;
	}
	
	public void setRef_middle(String value){
		ref_middle = value;
	}
	
	public void setRef_last(String value){
		ref_last = value;
	}
	
	public void setCand_first(String value){
		cand_first = value;
	}
	
	public void setCand_middle(String value){
		cand_middle = value;
	}
	
	public void setCand_last(String value){
		cand_last = value;
	}
	
	public void setGenerateAranjaments(String value){
		generateAranjaments =  (value != null?value:"");
	}
	
	public String getRef_first(){
		return ref_first;
	}
	public String getRef_middle(){
		return ref_middle;
	}
	public String getRef_last(){
		return ref_last;
	}
	public String getCand_first(){
		return cand_first;
	}
	public String getCand_middle(){
		return cand_middle;
	}
	public String getCand_last(){
		return cand_last;
	}
		
	public String getGenerateAranjaments(){
		return generateAranjaments;
	}

	public String getError(){
		return error;
	}
}
