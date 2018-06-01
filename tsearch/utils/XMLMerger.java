package ro.cst.tsearch.utils;

import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ch.elca.el4j.services.xmlmerge.AbstractXmlMergeException;
import ch.elca.el4j.services.xmlmerge.ConfigurationException;
import ch.elca.el4j.services.xmlmerge.config.AttributeMergeConfigurer;
import ch.elca.el4j.services.xmlmerge.config.ConfigurableXmlMerge;
import ch.elca.el4j.services.xmlmerge.merge.DefaultXmlMerge;

/**
 * Merging two xml-like files.
 * @author mihaib
 */

public class XMLMerger {
	public static final Double threshold = 0.90d;
	public static final Double thresholdSpecial = 0.90d;
	private static final Pattern patternContentToAIM = Pattern.
									compile("(?is)<\\s*(\\w+)\\s*>\\s*(\\w+\\s*=\\s*\\\")\\s*<!--(.*?)-->\\s*\\\"\\s*(\\2)\\s*<!--(.*?)-->\\s*\\\"\\s*</\\1>");
	private static final Pattern patternContentTitleDesk = Pattern.compile("(?is)(<(\\w+)([^>]*)>)\\s*(<!--(.*?))-->\\s*(<!--(.*?)-->)?(</\\2>)");
	private static final Pattern patternSpecialTag = Pattern.compile("(?is)<([A-Z\\d_]+)>\\s*([A-Z].*)</\\1>");
	private static final Pattern patternInnerAttribs = Pattern.compile("(?is)(\\w+)\\s*=\\s*\\\"\\s*<!--(.*?)-->\\\"");
	/**
	 * merging two .ats templates
	 * @param templateSourceOriginal template from Copied Search
	 * @param templateSourceOriginal2 template of the current search
	 * @return {@link String}
	 */
	public static String templateMerge(String templateSourceOriginal, String templateSourceOriginal2) 
		throws ConfigurationException, AbstractXmlMergeException, Exception 
		{//i hate this
			double score;
			boolean isNotTitleDesk = false;
			String templateString = templateSourceOriginal;
			String templateString2 = templateSourceOriginal2;
			if (!templateString.contains("TitleDesk") && !templateString2.contains("TitleDesk")){
				
				templateString = templateString.replaceAll("(?is)<#", "&lt;#");
				templateString = templateString.replaceAll("(?is)/#>", "/#&gt;");
				templateString = templateString.replaceAll("(?is)<(a\\s+href[^>]+)>", "&lt;$1&gt;");
				templateString = templateString.replaceAll("(?is)<(/a)>", "&lt;$1&gt;");
				
				templateString2 = templateString2.replaceAll("(?is)<#", "&lt;#");
				templateString2 = templateString2.replaceAll("(?is)/#>", "/#&gt;");
				templateString2 = templateString2.replaceAll("(?is)<(a\\s+href[^>]+)>", "&lt;$1&gt;");
				templateString2 = templateString2.replaceAll("(?is)<(/a)>", "&lt;$1&gt;");
				
				templateString2 = templateString2.replaceAll("(?is)<(\\w+)\\s+(\\1=\"<!--[^>]*>\\\")/>", "<$1> $2 </$1>");//change tags
				templateString = templateString.replaceAll("(?is)<(\\w+)\\s+(\\1=\"<!--[^>]*>\\\")/>", "<$1> $2 </$1>");
				
				templateString = templateString.replaceAll("\\$", "DDDOOOLAR");
				templateString2 = templateString2.replaceAll("\\$", "DDDOOOLAR");
				
				Pattern pat1 = Pattern.compile("(?is)(<(\\w+)>\\s*)(<\\2\\w+)\\s+(\\w+=.*?)/>");
				Matcher mat1 = pat1.matcher(templateString);
				while (mat1.find()){
					String bucata = mat1.group(4);
					Pattern pat2 = Pattern.compile("(?is)((\\w+)=\\\"<!--[^>]*>\\\")");
					Matcher mat2 = pat2.matcher(bucata);
					if (mat2.find()){
						bucata = bucata.replaceAll("(?is)((\\w+)=\\\"<!--[^>]*>\\\")", "<$2WWW333RRRR>$1</$2WWW333RRRR>");
					}
					templateString = templateString.replaceFirst("(?is)(<(\\w+)>\\s*)<(\\2\\w+)\\s+(\\w+=.*?)/>", "$1<$3XXX11YYY>" + bucata + "</$3XXX11YYY>");
				}
				
				mat1.reset();
				mat1 = pat1.matcher(templateString2);
				while (mat1.find()){
					String bucata = mat1.group(4);
					Pattern pat2 = Pattern.compile("(?is)((\\w+)=\\\"<!--[^>]*>\\\")");
					Matcher mat2 = pat2.matcher(bucata);
					if (mat2.find()){
						bucata = bucata.replaceAll("(?is)((\\w+)=\\\"<!--[^>]*>\\\")", "<$2WWW333RRRR>$1</$2WWW333RRRR>");
					}
					templateString2 = templateString2.replaceFirst("(?is)(<(\\w+)>\\s*)<(\\2\\w+)\\s+(\\w+=.*?)/>", "$1<$3XXX11YYY>" + bucata + "</$3XXX11YYY>");
				}

				templateString = cleanString(templateString);
				templateString2 = cleanString(templateString2);
				isNotTitleDesk = true;
			}
			templateString = templateString.replaceAll("(?is)(<!--\\s*)(-->)", "$1WWW####EEE$2");//to have something in empty <!---->
			templateString2 = templateString2.replaceAll("(?is)(<!--\\s*)(-->)", "$1WWW####EEE$2");
			
			String[] sources = {templateString, templateString2};

			String result = new DefaultXmlMerge().merge(sources);
			result = result.replaceAll("(?is)<\\?xml[^>]+>(.*)", "$1");;
			result = result.replaceAll("WWW####EEE", "");
			if (isNotTitleDesk){
				result = result.replaceAll("(?is)</?\\w+WWW333RRRR>", "");
				result = result.replaceAll("(?is)(<\\w+)XXX11YYY>\\s*", "$1 ");
				result = result.replaceAll("(?is)</\\w+XXX11YYY>", "/>");
				result = remakeResult(result);
			}
			
			String cand = "";
			String ref = "";
			Set<String> candSet = new HashSet<String>();
			Matcher ma = patternContentTitleDesk.matcher(result);
			
			while (ma.find()) {
				String tag = ma.group(1);
				if (ma.group(1).contains("Exceptions") || ma.group(1).contains("Requirements") || ma.group(1).contains("Legal Description") || ma.group(1).contains("Derivation")){
					candSet = new HashSet<String>();
					cand = ma.group(5).trim();
					candSet.add(cand);
					if (ma.group(7) != null ) {
						ref = ma.group(7).trim();
						score = GenericNameFilter.
									calculateMatchForCompanyOrSubdivision(candSet, ref.replaceAll("\\s", "").toUpperCase(), threshold.doubleValue(), null);
						if (score > threshold.doubleValue() || ref.equals(cand)){
							result = result.replaceFirst("(?is)(" + tag + ")\\s*(<!--(.*?))-->\\s*(<!--(.*?)-->)", "$1$4");
						} else {
							result = result.replaceFirst("(?is)(" + tag + ")\\s*(<!--(.*?))-->\\s*(<!--(.*?)-->)", "$1$2\r\n\r\n$4");
						}
					}
				} else {
					result = result.replaceFirst("(?is)(" + tag + ")\\s*([^<]*<!--[^>]+>)\\s*([^<]*?<!--[^>]+>)", "$1\r\n$3");
				}
			}
			ma.reset();
			ma = patternContentToAIM.matcher(result);
			
			while (ma.find()) {
				if (ma.group(1).contains("Legal") || ma.group(1).contains("DerivationInformation")) {
					candSet = new HashSet<String>();
					cand = ma.group(3).trim();
					candSet.add(cand);
					if (ma.group(5) != null ) {
						ref = ma.group(5).trim();
						score = GenericNameFilter.
									calculateMatchForCompanyOrSubdivision(candSet, ref.replaceAll("\\s", "").toUpperCase(), threshold.doubleValue(), null);
						if (score > threshold.doubleValue() || ref.equals(cand)){
							result = result.
								replaceFirst("(?is)<\\s*(\\w+)\\s*>\\s*(\\w+\\s*=\\s*\\\")\\s*<!--(.*?)-->\\\"\\s*(\\2)\\s*<!--(.*?)-->\\s*\\\"\\s*</\\1>", "<$1 $2<!--$3-->\"/>");
						} else {
							result = result.
								replaceFirst("(?is)<\\s*(\\w+)\\s*>\\s*(\\w+\\s*=\\s*\\\")\\s*<!--(.*?)-->\\\"\\s*(\\2)\\s*<!--(.*?)-->\\s*\\\"\\s*</\\1>", "<$1 $2<!--$3\r\n\r\n$5-->\"/>");
						}
					}
				} else {
					result = result.
						replaceFirst("(?is)<\\s*(\\w+)\\s*>\\s*(\\w+\\s*=\\s*\\\")\\s*<!--(.*?)-->\\\"\\s*(\\2)\\s*<!--(.*?)-->\\s*\\\"\\s*</\\1>", "<$1 $4<!--$5-->\"/>");
				}
			}
			ma.reset();
			ma = Pattern.compile("(?is)(<(\\w+)>\\s*)(<\\2\\w+)\\s+(\\w+=.*?)/>").matcher(result);//<Exceptions>, <Requirements>
			
			Matcher mat = null;
			Matcher matcher = null;
			Pattern pat = null;
			
			while (ma.find()) {
				String tag = ma.group(2);
				String tagContent = ma.group(4);
				mat = patternInnerAttribs.matcher(tagContent);
				if (ma.group(1).contains("Exceptions") || ma.group(1).contains("Requirements") || ma.group(1).contains("Derivation")) {
					while (mat.find()){
						String attrib = mat.group(1);
						pat = Pattern.compile("(?is)("+attrib+")\\s*=\\s*\\\"\\s*<!--(.*?)-->\\\"");
						matcher = pat.matcher(tagContent);
						String value1 = "";						
						String value2 = "";	
						int counter = 0;
						while (matcher.find()){
							if (counter == 0) {
								value1 = matcher.group(2);
							} else {
								value2 = matcher.group(2);
								tagContent = tagContent.replaceFirst("(?is)("+attrib+")\\s*=\\s*\\\"\\s*<!--(.*?)-->\\\"", "");
							}
							counter++;
						}
						if (StringUtils.isNotEmpty(value1) && StringUtils.isNotEmpty(value2)) {
							candSet = new HashSet<String>();
							cand = value1.trim();
							candSet.add(cand);
							ref = value2.trim();
							score = GenericNameFilter.
								calculateMatchForCompanyOrSubdivision(candSet, ref.replaceAll("\\s", "").replaceAll("(?is)&amp;dummy=[^\\\"]*", "").toUpperCase(), thresholdSpecial.doubleValue(), null);
							if (score > thresholdSpecial.doubleValue() || cand.equalsIgnoreCase(ref.replaceAll("(?is)&amp;dummy=[^\\\"]*", ""))){
								tagContent = tagContent.replaceFirst("(?is)("+attrib+"\\s*=\\s*\\\"\\s*<!--).*?-->\\\"", "$1" + value1 +"-->\"\r\n");
							} else {
								tagContent = tagContent.replaceFirst("(?is)("+attrib+"\\s*=\\s*\\\"\\s*<!--).*?-->\\\"", "$1" + value1 +"\r\n\r\n" + value2 + "-->\"");
							}
						}
					}
				} else {
					while (mat.find()){
						String attrib = mat.group(1);
						pat = Pattern.compile("(?is)("+attrib+")\\s*=\\s*\\\"\\s*<!--(.*?)-->\\\"");
						matcher = pat.matcher(tagContent);
						String value1 = "";	
						String value2 = "";	
						int counter = 0;
						while (matcher.find()){
							if (counter == 0) {
								value1 = matcher.group(2);
							} else {
								tagContent = tagContent.replaceFirst("(?is)("+attrib+")\\s*=\\s*\\\"\\s*<!--(.*?)-->\\\"", "");
								value2 = matcher.group(2);
							}
							counter++;
						}
						if (StringUtils.isNotEmpty(value2)) {
							tagContent = tagContent.replaceFirst("(?is)("+attrib+"\\s*=\\s*\\\"\\s*<!--).*?-->\\\"", "$1" + value2 +"-->\"");
						}
					}
				}
				result = result.replaceFirst("(?is)(<(" + tag + ")>\\s*)(<\\2\\w+)\\s+(\\w+=.*?)/>", "$1 $3 " + tagContent + "/>");
			}
			//System.out.println(result);
			result = result.replaceAll("DDDOOOLAR", "\\$");
			
			result = result.replaceAll("(?is)&lt;(a\\s+href.*?)&gt;", "<$1>");
			result = result.replaceAll("(?is)&lt;(/a)&gt;", "<$1>");
			
			return result;
	}

	/**
	 * merging two xml files. must use this with writeResultToXml(String result, String fileName) method to create the xml result file
	 */
	public static String xmlMerge(File inputFileName, File inputFileName2) throws ConfigurationException, AbstractXmlMergeException, Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		Document doc = documentBuilder.parse(inputFileName);
		Document doc2 = documentBuilder.parse(inputFileName2);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		//initialize StreamResult with File object to save to file
		StreamResult resultStream = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		
		transformer.transform(source, resultStream);
		String xmlString = resultStream.getWriter().toString();
		//System.out.println(xmlString);
		
		resultStream = new StreamResult(new StringWriter());
		source = new DOMSource(doc2);
		transformer.transform(source, resultStream);
		String xmlString2 = resultStream.getWriter().toString();
		//System.out.println(xmlString2);
		
		String[] sources = {xmlString, xmlString2};
		String result = new ConfigurableXmlMerge(new AttributeMergeConfigurer()).merge(sources);
		//System.out.println(result);
	    
		return result;
	}
	
	public static String cleanString(String string) {
		string = string.replaceAll("(?is)<(Exceptions_ML)\\s*<", "\"$1=\"<");
		string = string.replaceAll("<!--<", "TTT111EEE");
		string = string.replaceAll("/>-->", "WWW111");
		string = string.replaceAll("<!--", "RRR555TTTT");
		string = string.replaceAll("-->", "RRR777TTTT");
		return string;
	}
	
	public static String remakeResult(String string) {
		
		string = string.replaceAll("\\\"(Exceptions_ML)=\\\"", "<$1 ");
		string = string.replaceAll("TTT111EEE", "<!--<");
		string = string.replaceAll("WWW111", "/>-->");
		string = string.replaceAll("RRR555TTTT", "<!--");
		string = string.replaceAll("RRR777TTTT", "-->");
		return string;
	}
}
