package ro.cst.tsearch.searchsites.client;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ClickListener;



public class CountyNamePanel extends HeaderFocusPanel implements Comparable<CountyNamePanel>{

	private String counTname ="";

	private HandlerRegistration handlerRegistration;
	
	public HandlerRegistration getHandlerRegistration() {
		return handlerRegistration;
	}

	public CountyNamePanel(String countyName, String messageOnMouseEnter, boolean showMessage,String linkId) {
		super("<font color = 'green' ><a href ='#"+linkId+countyName+"''>" +countyName+"</a></font>", messageOnMouseEnter, showMessage,20,15);
		this.counTname = countyName;
	}
	
	public void setCountyName(String countyName,String linkId){
		super.setHTML("<font color = 'green' ><a href ='#"+linkId+countyName+"''>" +countyName+"</a></font>");
		this.counTname = countyName;
	}
	public int compareTo(CountyNamePanel o) {
		return counTname.compareTo(o.counTname);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null){
			return false;
		}
		if(!(obj instanceof CountyNamePanel)){
			return false;
		}
		return counTname.equals(((CountyNamePanel)obj).counTname);
	}
	
	@Override
	public int hashCode() {
		return counTname.hashCode();
	}
	
	public String getCountyName(){
		return counTname;
		
	}
	
	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		HandlerRegistration handlerRegistration = super.addClickHandler(handler);
		this.handlerRegistration = handlerRegistration;
		return handlerRegistration;
	}
	
}
