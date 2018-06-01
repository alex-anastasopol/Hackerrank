package ro.cst.tsearch.bean.templates;


import java.util.HashMap;
import java.util.Iterator;

import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.templates.GlobalTemplateFactory;
import ro.cst.tsearch.templates.Template;

public class GlobalTemplatesSelect extends SelectTag{
		
	protected String createOptions() throws Exception {

		HashMap<String, Template> allTemplates = GlobalTemplateFactory.getInstance().getTemplatesHash();

		StringBuffer sb = new StringBuffer(3000);
		Iterator<String> i = allTemplates.keySet().iterator();
		while (i.hasNext()){
			Template template = allTemplates.get(i.next());
			sb.append(
				"<option "
					+ " value='"
					+ template.getTemplateName()
					+ "'>"
					+ template.getTemplateName()
					+ "</option>");
		}
		return sb.toString();
	}	
}
