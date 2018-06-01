package ro.cst.tsearch.reports.throughputs;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.ObjectUtilities;

public class StandardLinkGenerator implements CategoryURLGenerator, Cloneable, Serializable {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = -1564554024003557216L;

	/** Prefix to the URL */
	private String prefix = "index.html";

	/** Suffix to the URL */
	private String suffix = "";

	/**
	 *  HashMap folosit pentru aflare corespondent 
	 *  pentru elementele de pe axa x (CategoryAxis)
	 *  Daca este null se folosesc direct elementele
	 */
	private HashMap<String, String> hashMap = null;
	
	
	public StandardLinkGenerator() {
		super();
	}

	public StandardLinkGenerator(String prefix, String suffix, HashMap<String, String> hashMap) {
		if (prefix == null) {
			throw new IllegalArgumentException("Null 'prefix' argument.");
		}
		this.prefix = prefix;
		if (suffix != null)
			this.suffix = suffix;
		this.hashMap = hashMap;
	}

	public String generateURL(CategoryDataset dataset, int series, int category) {
		String url = this.prefix;
		//Comparable seriesKey = dataset.getRowKey(series);
		Comparable categoryKey = dataset.getColumnKey(category);
		try {
			if(hashMap != null)
				url += URLEncoder.encode(hashMap.get(categoryKey.toString()), "UTF-8");
			else
				url += URLEncoder.encode(categoryKey.toString(), "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			if(hashMap != null)
				url += hashMap.get(categoryKey.toString());
			else
				url += categoryKey.toString();
		}
		if(suffix!=null)
			url += suffix;
		return url;
	}

	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
	
	public boolean equals (Object obj){
		if (obj==this)
			return true;
		if (!(obj instanceof StandardLinkGenerator))
			return false;
		StandardLinkGenerator that = (StandardLinkGenerator)obj;
		if(!ObjectUtilities.equal(this.prefix, that.prefix))
			return false;
		if(!ObjectUtilities.equal(this.suffix, that.suffix))
			return false;
		return true;
	}
}
