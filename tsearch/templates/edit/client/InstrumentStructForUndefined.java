package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class InstrumentStructForUndefined implements IsSerializable{
	
		public String book ="";
		public String page ="";
		public String instNo ="";
		public String docNo ="";
		public String doctype = "";
		
		public InstrumentStructForUndefined() {
				super();
		}
		
		public InstrumentStructForUndefined(String book, String page, String instNo, String docNo, String doctype) {
			super();
			this.book = book;
			this.page = page;
			this.instNo = instNo;
			this.docNo = docNo;
			this.doctype = doctype;
		}
		
}
