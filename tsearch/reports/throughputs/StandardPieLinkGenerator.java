package ro.cst.tsearch.reports.throughputs;

import java.io.Serializable;
import java.util.HashMap;

import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.data.general.PieDataset;

public class StandardPieLinkGenerator implements PieURLGenerator, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 268344441954880788L;
	/** Prefix to the URL */
	private String prefix = "index.html";
	
	/** Prefix used to change the URL if you press "Other" */
	private String prefix2 = null;

	/** Suffix to the URL */
	private String suffix = "";
	
	/**
	 *  HashMap folosit pentru aflarea corespondentului 
	 *  fiecarui slice din pie
	 *  Daca este null se folosesc direct elementele
	 */
	private HashMap<String, GraphicInfoStructure> hashMap = null;
	
	/**
	 * HashMap folosit pentru aflarea culorii
	 * elementului pe care se va face click
	 */
	private HashMap<String, String> colorHashMap = null;

	public StandardPieLinkGenerator() {
		super();
	}


	public String generateURL(PieDataset dataset, Comparable key, int arg2) {
		String url = prefix;
		if(key.toString().equals("Other") && prefix2!=null)
			url = prefix2;
//		if(key!=null){
//			url += key.toString();
//		}
		if(hashMap!=null){
			url+=hashMap.get(key.toString()).getId();
		}
		url += suffix;
		if(colorHashMap!=null){
			url += colorHashMap.get(key.toString());
		} else {
			url += key.toString();
		}
		if(prefix.contains("javascript"))
			url+="')";
		return url;
	}


	public StandardPieLinkGenerator(String prefix, String suffix, HashMap<String, GraphicInfoStructure> hashMap) {
		super();
		if (prefix == null) {
			throw new IllegalArgumentException("Null 'prefix' argument.");
		}
		this.prefix = prefix;
		if (suffix != null)
			this.suffix = suffix;
		this.hashMap = hashMap;
	}
	
	public StandardPieLinkGenerator(String prefix, String suffix, HashMap<String, GraphicInfoStructure> hashMap, HashMap<String, String> colorHashMap) {
		super();
		if (prefix == null) {
			throw new IllegalArgumentException("Null 'prefix' argument.");
		}
		this.prefix = prefix;
		if (suffix != null)
			this.suffix = suffix;
		this.hashMap = hashMap;
		this.colorHashMap = colorHashMap;
	}
		
	public StandardPieLinkGenerator(String prefix, String prefix2, String suffix, HashMap<String, GraphicInfoStructure> hashMap) {
		super();
		if (prefix == null || prefix2 == null) {
			throw new IllegalArgumentException("Null 'prefix' argument.");
		}
		this.prefix = prefix;
		this.prefix2 = prefix2;
		if (suffix != null)
			this.suffix = suffix;
		this.hashMap = hashMap;
	}
	
	

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final StandardPieLinkGenerator other = (StandardPieLinkGenerator) obj;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		if (suffix == null) {
			if (other.suffix != null)
				return false;
		} else if (!suffix.equals(other.suffix))
			return false;
		return true;
	}
	
	
	
	
}
