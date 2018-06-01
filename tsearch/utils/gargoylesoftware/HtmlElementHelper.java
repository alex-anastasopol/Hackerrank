package ro.cst.tsearch.utils.gargoylesoftware;

import java.util.List;

import ro.cst.tsearch.servers.response.ServerResponse;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class HtmlElementHelper {

	public static HtmlTextInput setHtmlTextInputValueByName(HtmlPage page,String value, String inputTextName ) {
		HtmlTextInput htmlTextInput = null;
		try {
			htmlTextInput = page.getElementByName(inputTextName);
			htmlTextInput.setValueAttribute(value);
		}catch(Exception e){
			e.printStackTrace();
		}
		return htmlTextInput;
	}

	public static HtmlSelect setHtmlSelectSelectedValueByOptionValue(HtmlPage page, String selectName, String optionValue) {
		HtmlSelect htmlSelect = null;
		try{
			htmlSelect = page.getElementByName(selectName);
			HtmlOption optionByValue = htmlSelect.getOptionByValue(optionValue);
			htmlSelect.setSelectedAttribute(optionByValue, true);
		}catch(Exception e){
			e.printStackTrace();
		}
		return htmlSelect;
	}
	
	public static HtmlCheckBoxInput setHtmlCheckBoxInputValueByOptionValue(HtmlPage page, String checkBoxInputName, boolean optionValue) {
		HtmlCheckBoxInput htmlCheckBox = null;
		try{
			htmlCheckBox = page.getElementByName(checkBoxInputName);
			htmlCheckBox.setChecked(optionValue);
		}catch(Exception e){
			e.printStackTrace();
		}
		return htmlCheckBox;
	}
		
	public static ServerResponse getServerResponseByUrl(String url) {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		ServerResponse resp = new ServerResponse();
		try{
			HtmlPage page = webClient.getPage(url);			
			resp.setPage(page);			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			webClient.closeAllWindows();
		}
		return resp;
	}
	
	public static Page getHtmlPageByURL(String url) {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		Page page = null;
		try {
			page = webClient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			webClient.closeAllWindows();
		}
		return page;
	}
	
	public static void removeElementByXpath(DomNode detailsNode,String xPath) {
		try{
			List<?> footerTR = detailsNode.getByXPath(xPath);
			if (footerTR.size() > 0){
				((DomNode) footerTR.get(0)).remove();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void removeElementWithTagName(String tagName,HtmlElement main){
		 List<HtmlElement> allImages = main.getHtmlElementsByTagName(tagName);
			for(HtmlElement el:allImages){
				el.remove();
			}
	 }

}
