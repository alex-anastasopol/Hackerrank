/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.jsp.utils;

import ro.cst.tsearch.utils.URLMaping;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * Button class holds the functionality related to a HTML representation of 
 * a button
 */
public class Button{
	
	/** The HTML representation of this object*/
	private String html = "";
	/**
	 * Creates the Button instance from supplied label and href
	 * @param label is the label on the Button instance
	 * @param href is the link to be followed on "click" events
	 */
	public Button(String label,String href){
		html = getButton(label,href,"");
	}

	public Button(String label,String href,String tooltip){
		html = getButton(label,href,tooltip);
	}

	/**
	 * gets the HTML representation of this element enveloped into a 
	 * <table> tag.
	 * @param text is the label to be displayed in to the button
	 * @param link is the href location to be loaden on click event
	 */

	public static String getButton(String text,String link,String tooltip) {
		String button = ""
				  + "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n"
				  + "<tr>"                          
				  + "<td height=\"1\"><img src=\""+ URLMaping.IMAGES_DIR + "/dot.gif\" border=\"0\"></td>"
				  + "<td class=\"buttonborder\" width=\"1\" height=\"1\" rowspan=\"3\"><img src=\""+ URLMaping.IMAGES_DIR + "/dot.gif\" border=\"0\"></td>"
				  + "</tr>"
				  + "<tr><td nowrap class=\"buttoncolor\"><div class=buttontext><a class=\"button\" href=\"" 
				  + link + "\" title=\"" + tooltip + "\"  onMouseOver=\"status='" + tooltip + "';return true;\" onMouseOut=\"status=''\">&nbsp;&nbsp;" 
				  + text + "&nbsp;&nbsp;</a></div></td></tr>"
				  + "<tr><td class=\"buttonborder\"><img src=\""+ URLMaping.IMAGES_DIR + "/dot.gif\" border=\"0\"></td></tr>"
				  + "</table>";
		return button;
	}

	/**
	 * Gets the String representing this instances
	 * @return the HTML internal representation of this element
	 */
	public String toString(){
		return html;
	}
}