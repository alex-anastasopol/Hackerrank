package ro.cst.tsearch.search.name.companyex;


import java.util.LinkedList;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement(name = "CompanyList") 
public class CompanyNameMarshal {
	private LinkedList<CompanyObject> companyObject=null;
	
	public CompanyNameMarshal(){
		this.companyObject=new LinkedList<CompanyObject>();
	}

	public LinkedList<CompanyObject> getCompanyObject() {
		return companyObject;
	}

	public void setCompanyObject(LinkedList<CompanyObject> companyObject) {
		this.companyObject = companyObject;
	}
	public void addCompanyObject(CompanyObject ob) {
		for(int i=0;i<this.companyObject.size();i++){
			if(this.companyObject.get(i).equals(ob)){
				this.companyObject.remove(i);
			}
		}
		this.companyObject.add(ob);
	}
	public void sort(){
		boolean isChange=true;
		String temp="";
		while(isChange){
			isChange=false;
			for(int i=0;i<this.companyObject.size()-1;i++)
				if(this.companyObject.get(i).getCompanyName().compareToIgnoreCase(this.companyObject.get(i+1).getCompanyName())>0){
					temp=this.companyObject.get(i).getCompanyName();
					this.companyObject.get(i).setCompanyName(this.companyObject.get(i+1).getCompanyName());
					this.companyObject.get(i+1).setCompanyName(temp);
					temp=this.companyObject.get(i).getCompanyValue();
					this.companyObject.get(i).setCompanyValue(this.companyObject.get(i+1).getCompanyValue());
					this.companyObject.get(i+1).setCompanyValue(temp);
					 isChange=true;
				}
		}
	}
}
