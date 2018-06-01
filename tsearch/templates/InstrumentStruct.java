package ro.cst.tsearch.templates;

import ro.cst.tsearch.utils.StringUtils;

public class InstrumentStruct {
	
	public String book ="";
	public String page ="";
	public String instNo ="";
	public String docNo ="";
	public String doctype = "";
	
	public boolean equals(Object inst){
		if (inst instanceof InstrumentStruct) {
			InstrumentStruct newinst = (InstrumentStruct) inst;
			
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				return book.equals(newinst.book)&&page.equals(newinst.page)&&doctype.equals(newinst.doctype);
			}
			else if(!StringUtils.isEmpty(instNo)){
				return instNo.equals(newinst.instNo)&&doctype.equals(newinst.doctype);
			}
			else if(!StringUtils.isEmpty(docNo)){
				return docNo.equals(newinst.docNo)&&doctype.equals(newinst.doctype);
			}
		}
		return false;
	}
	
	public String toString(){
		return "book = "+ book +"\npage = "+ page +"\ninst = "+ instNo;
	}
	
	public int hashCode(){
		if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
			return book.hashCode() + page.hashCode() + doctype.hashCode();
		}
		else if(!StringUtils.isEmpty(instNo)){
			return instNo.hashCode() + doctype.hashCode();
		}
		else if(!StringUtils.isEmpty(docNo)){
			return docNo.hashCode() + doctype.hashCode();
		}
		return 1;
	}

	public InstrumentStruct(){};
	
	public InstrumentStruct(String book, String page, String instNo, String docNo, String doctype) {
		super();
		this.book = book;
		this.page = page;
		this.instNo = instNo;
		this.docNo = docNo;
		this.doctype = doctype;
	}
}
