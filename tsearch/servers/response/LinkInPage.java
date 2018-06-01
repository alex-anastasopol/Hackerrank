package ro.cst.tsearch.servers.response;

import java.io.Serializable;

import ro.cst.tsearch.servers.info.TSServerInfoModule;


public class LinkInPage implements Serializable, Cloneable {
    
    static final long serialVersionUID = 10000000;
    
	private int actionType = -1;
	private String link = "";
	private String originalLink ="";
	private TSServerInfoModule module = null;

	//do not delete - kept for old search compatibility
	@SuppressWarnings("unused")
	private transient boolean isFake = false;
	
	public LinkInPage(String link,String originalLink,int actionType){
		this.link = link;
		this.originalLink = originalLink;
		this.actionType = actionType;
	}

	public LinkInPage(String link,String originalLink){
		this.link = link;
		this.originalLink = originalLink;
	}
	
	public LinkInPage(TSServerInfoModule module,int actionType){
		this.module = module;
		this.actionType = actionType;
	}
	
	public int getActionType() {
		return actionType;
	}

	public void setActionType(int ac) {
		this.actionType = ac;
	}

	public String getLink() {
		return link;
	}

	public void setOnlyLink(String s) {
		link = s;
	}
	
	public void setOnlyOriginalLink(String s) {
	    originalLink = s;
	}

	public String getOriginalLink() {
		return originalLink;
	}
	
	public String toString(){
		return "LinkInPage("+ link +";"
						 + originalLink + ";" 
						+ actionType +";"
						+ module +";"
						 + ")";
	}

	/**
	 * @return
	 */
	public TSServerInfoModule getModule() {
		return module;
	}

	public synchronized Object clone() {
		try {
			LinkInPage lp = (LinkInPage) super.clone();
			
			try {lp.link = new String(link);}catch(Exception e) {}
			try {lp.originalLink = new String(originalLink);}catch(Exception e) {}
			try {lp.actionType = actionType;}catch(Exception e) {}
			try {lp.module = (TSServerInfoModule)((TSServerInfoModule) module).clone();}catch(Exception e) {}
			return lp;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("clone() not supported for " + this.getClass().getName());
		}
	}

}
