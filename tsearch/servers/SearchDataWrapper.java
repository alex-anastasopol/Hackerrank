/*
 * Created on Oct 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servers;

import javax.servlet.http.HttpServletRequest;

import com.stewart.ats.base.document.ImageI;

import ro.cst.tsearch.bean.SearchAttributes;

/**
 * @author elmarie
 *
 */
public class SearchDataWrapper {
	

	HttpServletRequest request = null;
	SearchAttributes sa = null;	
	ImageI image = null;
	
	public SearchDataWrapper() {
	}

	public SearchDataWrapper (HttpServletRequest request){
		this.request = request;
	}

	public SearchDataWrapper (SearchAttributes sa){
		this.sa = sa;
	}
	
	public void setImage(ImageI image){
		this.image = image;
	}

	public HttpServletRequest getRequest() {
		return request;
	}
	public SearchAttributes getSa() {
		return sa;
	}
	public ImageI getImage(){
		return image;
	}

}
