package ro.cst.tsearch.pdftiff.util;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;


public class SynchHTMLEditorKit extends HTMLEditorKit {
	class SynchHTMLFactory extends HTMLFactory {
		public View create(Element elem) {
			Object o = elem.getAttributes().getAttribute(
					StyleConstants.NameAttribute);
			if (o instanceof HTML.Tag) {
				HTML.Tag kind = (HTML.Tag) o;
				if (kind == HTML.Tag.IMG) {
					ImageView v = new ImageView(elem);
					v.setLoadsSynchronously(true);
					return v;
				}
			}
			return super.create(elem);
		}
	}

	ViewFactory fac = new SynchHTMLFactory();

	public ViewFactory getViewFactory() {
		return fac;
	}
}