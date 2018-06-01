package ro.cst.tsearch.datatrace;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.utils.URLMaping;

/**
 * 
 * @author radu bacrau
 *
 */
public class Utils {

	/**
	 * load an XML file and concatenate all lines, trimming beginning and ending spaces
	 * @param fileName name of the file
	 * @return file contents on a single line
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String loadXMLTemplate(String fileName) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		StringBuffer sb = new StringBuffer();		
		try{
			String line;
			while((line = br.readLine()) != null){
				sb.append(line.trim());
			}
		} finally {
			br.close();
		}		
		return sb.toString();			
	}
	
	/**
	 * load an HTML file
	 * @param fileName name of the file
	 * @return file contents on a single line
	 * @throws FileNotFoundException
	 * @throws IOException
	 */	
	public static String loadHTMLTemplate(String fileName) throws FileNotFoundException, IOException {

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		StringBuffer sb = new StringBuffer();
		try{
			String line;
			while((line = br.readLine()) != null){
				sb.append(line);
			}
		} finally {
			br.close();	
		}
		
		return sb.toString();			
	}
	/**
	 * Replace all "@@parameterName@@" with "parameterValue" 
	 * @param xml
	 * @param map
	 * @return
	 */
	public static String fillXMLTemplate(String xml, Map<String,String> map){
		for(String key : map.keySet()){
			xml = fillXMLParameter(xml,key,map.get(key));
		}
		return xml;
	}
	
	/**
	 * Replace "@@parameterName@@" with "parameterValue" 
	 * @param xml
	 * @param parameterName
	 * @param parameterValue
	 * @return
	 */
	public static String fillXMLParameter(String xml, String parameterName, String parameterValue){
		return xml.replace("@@" + parameterName + "@@", parameterValue);
	}
	
	/**
	 * Replace "@@parameterName@@" with "parameterValue"
	 * @param xml
	 * @param parameterName
	 * @param parameterValue
	 * @return
	 */
	public static String fillFirstXMLParameter(String xml, String parameterName, String parameterValue){
		String pattern = "@@" + parameterName + "@@";
		if(xml.contains(pattern)){
			xml = xml.replaceFirst(pattern, parameterValue);
		}
		return xml;
	}
	/**
	 * Load parameters from file
	 * @param params
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void loadParams(Map<String,String> params, String fileName) throws FileNotFoundException, IOException{
		params.clear();
    	BufferedReader br = new BufferedReader(new FileReader(fileName));
    	String line;
    	try {
	    	while((line = br.readLine()) != null){
	    		int idx = line.indexOf('=');
	    		if((idx != -1) && (idx != (line.length()-1))){
	    			String key = line.substring(0,idx).trim();
	    			String value = line.substring(idx+1).trim();
	    			if(!"".equals(key)){
	    				params.put(key, value);
	    			}
	    		}
	    	}
    	} finally {
    		br.close();
    	}
    }
	
	/**
	 * extract $1 parameter using a regex
	 * @param data
	 * @param regex
	 * @return
	 */
	public static String extractParameter(String data, String regex){
		try{
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(data);
			if(m.find()){
				return m.group(1);
			} else {
				return "";
			}
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * read a byte array from an input stream
	 * @param is
	 * @return
	 */
	public static byte[] readByteArray(InputStream is)
	{
		byte buffer[] = new byte[4096];
		
		try{
			List<byte[]> responseList = new ArrayList<byte[]>();
			int retValLength = 0;		
			int readLength = 0;
			do {
				readLength = is.read(buffer);
				
				if(readLength > 0) {
					byte [] chunk = new byte[readLength];
					System.arraycopy(buffer, 0, chunk, 0, readLength);					
					responseList.add(chunk);
					retValLength += chunk.length;
				}
	        } while(readLength != -1);
			
			if(responseList.size() == 0){
				return null;
			}
			if(responseList.size() == 1){
				return responseList.get(0);
			}
			byte [] retVal = new byte[retValLength];
			int position = 0;
			for(byte[]  chunk : responseList){
				System.arraycopy(chunk, 0, retVal, position, chunk.length);
				position += chunk.length;
			}
			return retVal;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * sleep for a number of miliseconds
	 * @param ms
	 */
	public static void sleep(int ms){
		try{
			Thread.sleep(ms);
		}catch(InterruptedException ignored){}
	}
		
	/**
	 * 
	 * @param fct
	 * @param html
	 */
	public static void setupSelectBox(TSServerInfoFunction fct, String html){
	   fct.setHtmlformat(html);
	   fct.setParamType(TSServerInfoFunction.idSingleselectcombo);		
	}
	
	public static void setupMultipleSelectBox(TSServerInfoFunction fct, String html) {
		if (fct != null) {
			fct.setHtmlformat(html);
			fct.setParamType(TSServerInfoFunction.idMultipleSelectCombo);
		}
	}
	
	/**
	 * 
	 * @param is
	 * @return
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Node parseXMLDocument(InputStream is) throws SAXException, IOException, ParserConfigurationException {		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Node doc = builder.parse(is);		
		return doc;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Node parseXMLDocument(String fileName) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {		
		return parseXMLDocument(new FileInputStream(fileName));
	}
	
	/**
	 * 
	 * @param bytes
	 * @return
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Node parseXMLDocument(byte [] bytes) throws SAXException, IOException, ParserConfigurationException {		
		return parseXMLDocument(new ByteArrayInputStream(bytes));
	}
	
	/**
	 * 
	 * @param doc
	 * @param query
	 * @return
	 * @throws XPathExpressionException
	 */
	public static NodeList getAllNodes(Node doc, String query) throws XPathExpressionException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile(query);
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		return nodes;
	}
	
	/**
	 * 
	 * @param doc
	 * @param dataSite
	 * @return
	 */
	public static DTRecord parseTitleRec(Node doc, DataSite dataSite){				
	
		Map<String,String> instInfo = null;
		Map<String,String> aliasInfo = null;
		Map<String,String> caseInfo = null;
		
		Set<List<Map<String,String>>> partyList = new LinkedHashSet<List<Map<String,String>>>();
		Set<List<Map<String,String>>> grantorList = new LinkedHashSet<List<Map<String,String>>>();
		Set<List<Map<String,String>>> granteeList = new LinkedHashSet<List<Map<String,String>>>();
		Set<Map<String,String>> partyRefList = new LinkedHashSet<Map<String,String>>();
		Set<Map<String,String>> propertyList = new LinkedHashSet<Map<String,String>>();		
		Set<Map<String,String>> refInstList  = new LinkedHashSet<Map<String,String>>();		
		List<DTRecord> referencedDocs = new ArrayList<DTRecord>();
		
		Set<String> remarks = new LinkedHashSet<String>();	
		
		Node newDoc = null;
		if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
			if (doc.getNodeName().equals("title_rec")){
				Node attribType = doc.getAttributes().getNamedItem("type");
				if (attribType != null && "REFERRING".equals(attribType.getTextContent())){
					try {
						newDoc = getAllNodes(doc, "title_doc//referring_docs/title_rec/title_doc").item(0);
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					}
				} else{
					try {
						newDoc = getAllNodes(doc, "title_doc").item(0);
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if (newDoc != null){
			doc = newDoc;
		}
		for(Node child: getChildren(doc)){
			
			String name = child.getNodeName();

			if("inst".equals(name)){								
				
				instInfo = parseGeneric(child, true);
				
			}if("ref".equals(name)){								
				
				for(Node child1: getChildren(child)){
					if("inst".equalsIgnoreCase(child1.getNodeName())){
						if (dataSite.getCountyId() == CountyConstants.OH_Lorain){
							instInfo = parseGeneric(child1, true);
						} else{
							Map<String,String> refInst = parseGeneric(child1, true);
							if (refInst != null){
								refInstList.add(refInst);
							}
						}
					}
				}
				
			} else if("alias".equals(name)){				
				
				aliasInfo = parseGeneric(child, "inst", true);
				
			} else  if("case".equals(name)){								
				
				caseInfo = parseGeneric(child, true);	
				
			} else if("party_info".equals(name)){

				List<Map<String,String>> personList = new ArrayList<Map<String,String>>();
				
				for(Node nephew : getChildren(child)){	
					
					String nephewName = nephew.getNodeName();

					if("party".equals(nephewName)){						
						Map<String,String> person = parseGeneric(nephew, false);
						personList.add(person);						
					} else if("name_xref".equals(nephewName)){						
						Map<String,String> person = parseGeneric(nephew, false);
						personList.add(person);						
					} else if("ref".equals(nephewName)){						
						Map<String,String> instrument = parseGeneric(nephew, "inst", false);
						if(instrument != null){
							Map<String,String> instrument2 = parseGeneric(nephew, "alias", false);
							if (instrument2 != null){
								instrument.putAll(instrument2);
							}
							partyRefList.add(instrument);	
						}
					} else {
						dbgIgnored (nephewName);
					}					
				}
				
				if(personList.size() != 0){
					partyList.add(personList);
					if("PARTY1".equals(personList.get(0).get("role")) || "GRNTOR".equals(personList.get(0).get("role"))){
						grantorList.add(personList);
					} else {
						granteeList.add(personList);
					}
				}

					
			} else if("remarks".equals(name)){
				
				Map<String,String> map = parseGeneric(child, false);
				if(map.containsKey("text")){
					remarks.add(map.get("text"));
				}
				
			} else if ("property".equals(name)){
							
				Map<String,String> property = new HashMap<String,String>();
				
				for(Node nephew : getChildren(child)){
					
					String nephewName = nephew.getNodeName();
					String nephewValue = getNodeValue(nephew);
					
					if("property_legal".equals(nephewName)){						
						Map<String,String> propertyInfo = parseGeneric(nephew, false);
						if (propertyInfo != null){
							property.putAll(propertyInfo);
						}
					} else if ("prior_arb".equals(nephewName)){					
						property.put("prior_arb", nephewValue);						
					} else {						
						dbgIgnored(nephewName);						
					}
				}
				
				if(property.size() != 0){
					propertyList.add(property);
				}	
				
			} else if ("referring_docs".equals(name)){
				// /title_rec/title_doc/inst : works much better than XPath in this case				
				for(Node l1: getChildren(child)){
					if("title_rec".equals(l1.getNodeName())){
						// skip to the title_doc
						Node titleDoc = null;
						for(Node n : getChildren(l1)){
							if("title_doc".equals(n.getNodeName())){
								//this is for CA DTG
								Node attrType = n.getAttributes().getNamedItem("outcome");
								if (attrType != null){
									String type = attrType.getTextContent();
									if ("NOT_FOUND".equalsIgnoreCase(type)){
										remarks.add("REFERRING:NOT_FOUND");
										continue;
									}
								}
								titleDoc = n;
							}
						}
						if(titleDoc ==  null){ continue; }
						// parse the ref
						DTRecord rec = parseTitleRec(titleDoc, dataSite);
						if(rec == null){ continue; }
						
						referencedDocs.add(rec);
						
						for(Node l2: getChildren(l1)){
							if("title_doc".equals(l2.getNodeName())){									
								for(Node l3: getChildren(l2)){
									if("inst".equals(l3.getNodeName())){
										Map<String,String> refInst = parseGeneric(l3, true);
										if(refInst != null){
											refInstList.add(refInst);
										};
										break;
									}
								}								
								break;
							}
						}
					}
				}				
			} else {
				dbgIgnored(name);
			}
		}		
		
		if(instInfo == null && aliasInfo == null && caseInfo == null){
			return null;		
		}		
		
		return new DTRecord(	
				instInfo,
				aliasInfo,
				caseInfo,
				grantorList,
				granteeList,
				propertyList,
				partyRefList,
				refInstList,
				referencedDocs,
				remarks);
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public static DTTaxRecord parseTaxReport(Node doc){				
	
		Map<String,String> parcelInfo = null;
		Map<String,String> currentAssessmentInfo = null;
		
		Set<List<Map<String,String>>> partyList = new LinkedHashSet<List<Map<String,String>>>();
		
		Map<String,String> taxDueInfo = null;
		Set<Map<String,String>> taxInstallmentList = new LinkedHashSet<Map<String,String>>();
		Set<Map<String,String>> specialAssessmentList = new LinkedHashSet<Map<String,String>>();
		
		for (Node child: getChildren(doc)){
			
			String name = child.getNodeName();

			if ("tax_parcel".equals(name) && parcelInfo == null){
				
				parcelInfo = parseGeneric(child, true);
				StringBuffer legal = new StringBuffer();
				for (Node nephew : getChildren(child)){
					String nephewName = nephew.getNodeName();
					if ("property".equals(nephewName)){
						for (Node nephewLvl2 : getChildren(nephew)){	
							String nephewLvl2Name = nephewLvl2.getNodeName();
							if ("property_legal".equals(nephewLvl2Name)){
								for (Node nephewLvl3 : getChildren(nephewLvl2)){
									String freeform = "";
									try {
										freeform = nephewLvl3.getFirstChild().getTextContent();
									} catch (Exception e) {
									}
									if (StringUtils.isNotEmpty(freeform)){
										if (StringUtils.isNotEmpty(freeform)){
											if (legal.length() == 0){
												legal.append(freeform);
											} else{
												legal.append("#/#").append(freeform);
											}
										}
									}
								}
							}
						}
					}
				}
				if (parcelInfo != null && legal.length() > 0){
					parcelInfo.put("property.property_legal.freeform", legal.toString());
				}
			} else if ("current_taxes".equals(name)){

				List<Map<String,String>> personList = new ArrayList<Map<String,String>>();
				
				for (Node nephew : getChildren(child)){	
					
					String nephewName = nephew.getNodeName();
					if ("current_assessment".equals(nephewName)){
						currentAssessmentInfo = parseGeneric(child, true);
						
						for (Node nephewLvl2 : getChildren(nephew)){	
							
							String nephewLvl2Name = nephewLvl2.getNodeName();
							if ("ownership".equals(nephewLvl2Name)){
								for (Node nephewLvl3 : getChildren(nephewLvl2)){
									Map<String, String> person = parseGeneric(nephewLvl3, false);
									if (person != null && person.size() > 0){
										personList.add(person);
									}
								}
							} else {
								dbgIgnored(nephewName);
							}
						}
					} else if ("current_tax".equals(nephewName)){	
							for (Node nephewLvl2 : getChildren(nephew)){	
								
								String nephewLvl2Name = nephewLvl2.getNodeName();
								if ("tax_due".equals(nephewLvl2Name)){
									
									taxDueInfo = parseGeneric(child, true);
									
									String taxPeriod = taxDueInfo.get("current_tax.tax_due.tax_period");
									if (StringUtils.isNotEmpty(taxPeriod)){
										Map<String, String> installment = new HashMap<String, String>();
										installment.put("tax_period", taxPeriod);
										taxInstallmentList.add(installment);
									}
									for (Node nephewLvl3 : getChildren(nephewLvl2)){
										
										String nephewLvl3Name = nephewLvl3.getNodeName();
										if ("tax_installment".equals(nephewLvl3Name)){
											Map<String, String> installment = parseGeneric(nephewLvl3, false);
											Node attrStatus = nephewLvl3.getAttributes().getNamedItem("status");
											if (attrStatus != null){
												String status = attrStatus.getTextContent();
												installment.put("tax_installment.status", status);
											}
											Node attrType = nephewLvl3.getAttributes().getNamedItem("type");
											if (attrType != null){
												String type = attrType.getTextContent();
												installment.put("tax_installment.type", type);
											}
											taxInstallmentList.add(installment);
										} else {
											dbgIgnored(nephewLvl3Name);
										}
									}
								} else if ("special_assessments".equals(nephewLvl2Name)){
									for (Node nephewLvl3 : getChildren(nephewLvl2)){
										
										String nephewLvl3Name = nephewLvl3.getNodeName();
										if ("special_assessment".equals(nephewLvl3Name)){
											Map<String, String> assessment = parseGeneric(nephewLvl3, false);
											specialAssessmentList.add(assessment);
										}
									}
								} else {
									dbgIgnored(nephewName);
								}
							}
					}				
				}
				
				if(personList.size() != 0){
					partyList.add(personList);
				}	
			}
		}		
		
		if (parcelInfo == null){
			return null;		
		}		
		
		return new DTTaxRecord(	
				parcelInfo,
				currentAssessmentInfo,
				taxDueInfo,
				partyList,
				taxInstallmentList,
				specialAssessmentList);
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public static DTTaxRecord parseTaxDelinqReport(Node doc){				
	
		Set<Map<String,String>> priorYearsDelinqList = null;
		Set<Map<String,String>> priorYearsRedemptionList = new LinkedHashSet<Map<String,String>>();
		
		for (Node child: getChildren(doc)){
			
			String name = child.getNodeName();

			if ("delinquency".equals(name)){
				priorYearsDelinqList = new LinkedHashSet<Map<String,String>>();
				for (Node nephew : getChildren(child)){	
					String nephewName = nephew.getNodeName();
					if ("delinquency_detail".equals(nephewName)){
						Map<String, String> eachYearDelinq = parseGeneric(nephew, false);
						priorYearsDelinqList.add(eachYearDelinq);
					} else {
						dbgIgnored(nephewName);
					}
				}
			} else if ("redemption".equals(name)){
					Map<String, String> eachRedemp = parseGeneric(child, false);
					priorYearsRedemptionList.add(eachRedemp);
			} else {
				dbgIgnored(name);
			}
		}		
		
		if (priorYearsDelinqList == null && priorYearsRedemptionList.size() == 0){
			return null;		
		}		
		
		return new DTTaxRecord(priorYearsDelinqList, priorYearsRedemptionList);
	}
	
	public static Map<String,String> parseIntermediary(Node doc){				
		
		Map<String,String> results = new LinkedHashMap<String, String>();
		
		for (Node child: getChildren(doc)){
			
			String name = child.getNodeName();
			if ("apn".equals(name)){
				for (Node nephew : getChildren(child)){
					String nephewName = nephew.getNodeName();
					if ("freeform".equalsIgnoreCase(nephewName)){
						results.put(name, nephew.getTextContent());
					} else {
						dbgIgnored(nephewName);
					}
				}
			} else if ("property".equals(name)){
				for (Node nephew : getChildren(child)){
					String nephewName = nephew.getNodeName();
					if ("ownership".equalsIgnoreCase(nephewName)){
						for (Node nephewLvl2 : getChildren(nephew)){	
							String nephewLvl2Name = nephewLvl2.getNodeName();
							if ("owner".equalsIgnoreCase(nephewLvl2Name)){
								for (Node nephewLvl3 : getChildren(nephewLvl2)){
									String nephewLvl3Name = nephewLvl3.getNodeName();
									if ("freeform".equalsIgnoreCase(nephewLvl3Name)){
										results.put(nephewLvl2Name, nephewLvl3.getTextContent());
									} else {
										dbgIgnored(nephewName);
									}
								}
							} else {
								dbgIgnored(nephewName);
							}
						}
					} else {
						dbgIgnored(nephewName);
					}
				}
			} else if ("situs".equals(name)){
				for (Node nephew : getChildren(child)){
					String nephewName = nephew.getNodeName();
					if ("freeform".equalsIgnoreCase(nephewName)){
						results.put(name, nephew.getTextContent());
					} else {
						dbgIgnored(nephewName);
					}
				}
			}  else {
				dbgIgnored(name);
			}
		}		
		
		
		return results;
	}
	
	public static String formatDate(Map<String,String> map, String prefix){
		String year = map.get(prefix + ".year");
		String month = map.get(prefix + ".month");
		String day = map.get(prefix + ".day");
		if(year == null && month == null && day == null ){
			return null;
		}
		if(year == null){ year = "";}
		if(month == null) { month = "";}
		if(day == null) { day = "";}		
		return month + "/" + day + "/" + year;
	}
	
	public static String formatCase(Map<String,String> instr){
		
		String number = instr.get("number");
		String type = instr.get("type");
		String filed = formatDate(instr, "filed");
		String posted = formatDate(instr, "posted");
		
		String retVal = "";
		if(type != null){
			retVal += "Doc Type: " + type + " ";
		}
		if(number != null){
			retVal += "Number: " + number + " ";
		}
		if(filed != null){
			retVal += "Filed: " + filed + " ";
		}
		if(posted != null){
			retVal += "Posted: " + posted + " ";
		}	
		return retVal;
	}
	
	public static String formatInstrument(Map<String,String> instr){
		
		String bp 		= formatPair(instr.get("book"),instr.get("page"),"-");
		if (StringUtils.isEmpty(bp)){
			bp = formatPair(instr.get("inst.book"), instr.get("inst.page"), "-");
		}
		String posted 	= formatDate(instr, "posted");
		String recorded = formatDate(instr, "recorded");
		String type 	= instr.get("type");
		String year 	= instr.get("year");
		String number 	= instr.get("number");
		String amount 	= instr.get("amount");
		
		if (StringUtils.isEmpty(number) && StringUtils.isNotEmpty(instr.get("inst.number"))){
			number 	= instr.get("inst.number");
		}
		if (StringUtils.isEmpty(year) && StringUtils.isNotEmpty(instr.get("inst.year"))){
			year 	= instr.get("inst.year");
		}
		
		String retVal 	= "";
		
		if(type != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;DocType:</b> " + type + " ";
		}
		if(recorded != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Recorded:</b> " + recorded + " ";
		}
		if(posted != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Posted:</b> " + posted + " ";
		}
		if(bp != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Book-Page:</b> " + bp + " ";
		}
		if(year != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Year:</b> " + year + " ";
		}
		if(number != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Number:</b> " + number + " ";
		}		
		if(amount != null){
			retVal += "<b>&nbsp;&nbsp;&nbsp;Amount:</b> " + amount + " ";
		}			
		return retVal;
	}
	
	private static String formatName(Map<String,String> name){
		String last = name.get("name");
		String first = name.get("first");
		String middle = name.get("middle");
		if(middle == null){
			middle = name.get("mi");
		}
		String suffix = name.get("suffix");
		if(last == null){
			return null;
		}
		String retVal = last;
		if(first != null){
			retVal += ", " + first;
		}
		if(middle != null){
			retVal += " " + middle;
		}
		if(suffix != null){
			retVal += " " + suffix;
		}		
		return retVal;
	}
	
	public static String formatNamePair(List<Map<String,String>> couple){
		//String retVal = couple.get(0).toString();
		String retVal = formatName(couple.get(0));
		if(couple.size() == 2){
			String wife = formatName(couple.get(1));
			if(wife != null){
				retVal += " and " + wife;
			} else {
				if("HW".equals(couple.get(1).get("relation"))){
					retVal += " ETUX ";
				}
			}
		}
		return retVal;
	}
	
	public static String  formatPair(String s1, String s2, String connector){
		String retVal = null;
		if(s1 != null){
			retVal = s1;
		}
		if(s2 != null){
			if(retVal == null){
				retVal = "";
			}
			retVal += connector + s2;
		}
		return retVal;
	}
	public static String formatLegal(Map<String,String> legal){
		String retVal = "";		
		{
			// subdivided		
			// <!ELEMENT property_legal ( arb | block | lot | plat | quarter | range | section | sub_lot | township )* >
			String pbk 		= legal.get("plat.book_page.book");
			String ppg 		= legal.get("plat.book_page.page");
			String pn 		= legal.get("plat.number");
			String platInstYear = legal.get("plat.year");
			if(StringUtils.isBlank(pn)){
				pn 		= legal.get("plat.plat_inst.number");
				platInstYear 	= legal.get("plat.plat_inst.year");
			}
			
			String lot 		= legal.get("lot");
			String lotThru  = legal.get("thru_lot");
			String block 	= legal.get("block");
			String section 	= legal.get("section");
			String qtr 	    = legal.get("quarter.half_quarter");
			String twn      = formatPair(legal.get("township.number"), legal.get("township.dir"), "");
			String rng      = formatPair(legal.get("range.number"), legal.get("range.dir"), "");
			
			//for MO St Louis CityDTG
        	String cityBlock 		= notNull(legal.get("plat.city_block_name.city_block"));
			String cityBlockName	= notNull(legal.get("plat.city_block_name.name"));
			
			if(pbk != null || ppg != null || pn!=null){
				retVal += "Plat: ";
				if(pbk != null || ppg != null ){
					if (ppg == null){
						retVal +=  pbk + " ";
					} else{
						retVal +=  pbk + "-" + ppg + " ";
					}
				}
				if(pn!=null){
					retVal +=  pn + " ";
					if(platInstYear!=null){
						retVal +=  "Year: " + platInstYear +" ";
					}
				}
			}
			
			if(lot != null){
				if(lotThru != null){
					lot = lot + "-" + lotThru;
				}				
				retVal += "Lot: " + lot + " ";
			}		
			if(block != null){
				retVal += "Block: " + block + " ";
			}
			if (StringUtils.isNotEmpty(cityBlock)){
				retVal += "City Block: " + cityBlock + " ";
			}
			if (StringUtils.isNotEmpty(cityBlockName)){
				retVal += "City Block Name: " + cityBlockName + " ";
			}
			if(section != null){
				retVal += "Section: " + section + " ";
			}
			if(twn != null){
				retVal += "Township: " + twn + " ";
			}
			if(rng != null){
				retVal += "Range: " + rng + " ";
			}		
			if(qtr != null){
				retVal += "Quarter: " + qtr + " ";
			}
			//for CA
			String tract = notNull(legal.get("plat.tract.number"));
			if (StringUtils.isNotEmpty(tract)){
				retVal += "Tract: " + tract + " ";
			}
		}{
			
			String arb = "";
			
			String arbNr    = legal.get("arb.arb_number");
			String bkpg     = formatPair(legal.get("arb.book"), legal.get("arb.page"), "-");
			String lot 		= legal.get("arb.lot");
			String block 	= legal.get("arb.block");
			String section 	= legal.get("arb.section");
			String qtr 	    = legal.get("arb.quarter.half_quarter");
			String twn      = formatPair(legal.get("arb.township.number"), legal.get("arb.township.dir"), "");
			String rng      = formatPair(legal.get("arb.range.number"), legal.get("arb.range.dir"), "");
						
			if(bkpg != null){
				arb += "Book-page: " + bkpg + " ";
			}
			if(lot != null){
				arb += "Lot: " + lot + " ";
			}		
			if(block != null){
				arb += "Block: " + block + " ";
			}
			if(section != null){
				arb += "Section: " + section + " ";
			}
			if(twn != null){
				arb += "Township: " + twn + " ";
			}
			if(rng != null){
				arb += "Range: " + rng + " ";
			}		
			if(qtr != null){
				arb += "Quarter: " + qtr + " ";
			}		
			if(arbNr != null){
				arb += "Arb:" + arbNr + " ";
			}
			//OHFairfield
			String district = notNull(legal.get("arb.district"));
			if (StringUtils.isNotEmpty(district)){
				arb += "District:" + district + " ";
			}
        	String parcel = notNull(legal.get("arb.parcel"));
        	if (StringUtils.isNotEmpty(parcel)){
				arb += "Parcel:" + parcel + " ";
			}
        	String splitCode = notNull(legal.get("arb.split_code"));
        	if (StringUtils.isNotEmpty(splitCode)){
				arb += "Parcel Split:" + splitCode + " ";
			}
			
			if(!"".equals(arb)){
				retVal += "<i>ARB</i>: " + arb;
			}
			
		}
		return retVal;
	}
	/**
	 * 
	 * @param map
	 * @return
	 */
	static Map<String,String> checkEmpty(Map<String,String> map){
		if(map != null){
			if(map.keySet().size() == 0){
				return null;
			}
		}
		return map;
	}
		
	public static String notNull(String str){
		if(str == null){ str = ""; }
		return str;
	}
	
	public static boolean isNotEmpty(String s){
		return s != null && !"".equals(s.trim());
	}
	public static boolean isEmpty(String s){
		return s == null || "".equals(s.trim());
	}
	
	/**
	 * 
	 * @param doc
	 * @param childName
	 * @return
	 */
	private static Map<String,String> parseGeneric(Node doc, String childName, boolean getAttributes){
		for(Node child : getChildren(doc)){
			if(childName.equals(child.getNodeName())){
				return parseGeneric(child, getAttributes);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	private static Map<String,String> parseGeneric(Node doc, boolean getAttributes){
		Map<String,String> map = new HashMap<String,String>();
		fillHierarchyMap(doc, map, "", getAttributes);
		return checkEmpty(map);		
	}
			
	/**
	 * get value of a node
	 * @param node
	 * @return value
	 */
	private static String getNodeValue(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}

	
	/**
	 * 
	 * @param doc
	 * @param map
	 * @param prefix
	 */
	private static void fillHierarchyMap(Node doc, Map<String,String> map, String prefix, boolean getAttributes){
		
		// update prefix
		if(!"".equals(prefix)){
			prefix = prefix + ".";
		}
		
		// put the attributes
		if(getAttributes){
			NamedNodeMap attrMap = doc.getAttributes();
			if(attrMap != null){
				for(int i=0 ; i<attrMap.getLength(); i++){
					Node attr = attrMap.item(i);
					String name = attr.getNodeName();
					String value = attr.getNodeValue();
					map.put(prefix + name, value);
				}
			}
		}
		
		// process the children
		for(Node child: getChildren(doc)){			
			
			String name = child.getNodeName();
			String value = getNodeValue(child);	
			
			if(!"".equals(value)){				
				map.put(prefix + name, value);				
			} else if (!name.startsWith("QS")) {							
				fillHierarchyMap(child, map, prefix + name, getAttributes);
			}
		}
	}

	/**
	 * return an iterable object with all children of a DOM node
	 * @param doc
	 * @return iterable object with all children
	 */
	private static Iterable<Node> getChildren(final Node doc){
		return new Iterable<Node>() {			
			public Iterator<Node> iterator(){ 
				List<Node> nodeList = new ArrayList<Node>();
				Node child = doc.getFirstChild();
				while (child != null){
					nodeList.add(child);
					child = child.getNextSibling();
				}			
				return nodeList.iterator();
			}			
		};
	}
	
	/**
	 * Parameter sent to datatrace must not contain the '/' characters
	 * @param function
	 * @param defaultValue
	 */
    public static void fixDate(TSServerInfoFunction function, String defaultValue){
    	if(function == null){ return; }
        String str = function.getParamValue().replace("/", "");
        if(!str.matches("\\d\\d\\d\\d\\d\\d\\d\\d")){
            str = defaultValue;
        }
        function.setParamValue(str);        
    }
    
    private static String potentialEmptyFields []  =  {
        "Lot", 
        "LotThru", 
        "Block", 
        "Book", 
        "Page", 
        "Sublot",
        "SublotThru",
        "DocType1",
        "DocType2",
        "NameLast",
        "NameFirst",
        "NameMI",
        "NameRole",
        "Section", 
        "Township", 
        "Range", 
        "Quarter",
        "HalfQuarter",
        "Parcel2",
        "Year2",
        "Instrument2",
        "SpouseFirst",
        "SpouseMI",
        "Parcel",
        "SubdivisionName"
    };
    
    /**
     * remove empty query parameters
     * @param query
     * @return
     */
    public static String removeEmpty(String query){
    	// <field name="Year2" value="" label="Year" pid="instrument"/>
        for(String field : potentialEmptyFields){
            query = query.replaceAll("<field name=\"" + field + "\" value=\"\"" + "[^>]+>", "");
        }
        return query;
    }	

	/**
	 * Write a byte array to a file
	 * @param fileName file name
	 * @param content data to be written
	 */
	public static void writeByteArray2File(String fileName, byte [] content){
		try{
			OutputStream fos = new FileOutputStream(fileName);
			try{
				fos.write(content);
			} finally {
				fos.close();
			}
		}catch(Exception ignored){	
			ignored.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param buffer
	 * @return
	 */
	public static boolean isXML(byte [] buffer){
		return buffer != null && 
			   buffer.length >= 5 && 
			   buffer[0] == '<' && buffer[1] == '?' && buffer[2] == 'x' && buffer[3]== 'm' && buffer[4] == 'l';
	}

	/**
	 * 
	 * @param buffer
	 * @return
	 */
	public static boolean isZIP(byte [] buffer){
		return buffer != null && buffer.length > 100 &&
		       buffer[0] == 'P' && buffer[1] == 'K';
	}

	/**
	 * Create a string of letters. Some uppercase, some lowercase
	 * @param length dessired length of the string 
	 * @return created string
	 */
	public static String createRandomLetterString(int length){
		StringBuffer sb = new StringBuffer(length);
		Random rand = new Random();
		for(int completed=0; completed < length; ){
			char c = (char)rand.nextInt();
			if( (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ) { 
				sb.append(c);
				completed++;
			}
		}
		return sb.toString();
	}
	
	private static long sequence = 10000000;
	public static synchronized long getUniqueTemporaryIdNano(){
		return (sequence++) + System.nanoTime();
	}	
	public static synchronized long getUniqueTemporaryIdMilli(){
		return (sequence++) + System.currentTimeMillis();
	}
	
	public static void dbgPrintRecords(Collection<DTRecord> collection, String fileName){
		/*
		fileName = "d:/" + fileName;
		try{
	        PrintWriter pw = new PrintWriter(new FileOutputStream(fileName), true);
	        int i = 1;
	        try{
		        for(DTRecord record: collection){
		        	pw.println("<hr>" + i + "<br>" + record.toString());
		        	i++;
		        }
	        } finally{
	        	pw.close();
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
		*/
	}	
	
	//private static Set<String> ignored = new HashSet<String>();
	
	private static void dbgIgnored(String name){
		/*
		if(!ignored.contains(name)){
			System.err.println("Ignored: <" + name + ">");
			ignored.add(name);
		}
		*/
	}

	public static void dbgWriteFile(String fileName, byte [] content){
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		boolean contains = inputArguments.contains("Ddebug");
		
		if (contains){
			try{
				OutputStream fos = new FileOutputStream("D:/xml/" + fileName);
				fos.write(content);
				fos.close();			
			}catch(Exception ignored){	
				ignored.printStackTrace();
			}
		}
	}	
	
	public static boolean hasJvmArgument(String argument) {
		  List arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		  return arguments.contains(argument);
	}
	/**
	 * Checks if a locally debug vm argument is defined. 
	 * e.g. Utils.isJvmArgumentTrue("debugForATSProgrammer") -DdebugForATSProgrammer=true
	 * @param argument
	 * @return
	 */
	public static boolean isJvmArgumentTrue(String argument) {
		String property = System.getProperty(argument);
		Boolean valueOf = Boolean.valueOf(property);
		return valueOf;
	}
	
	public static boolean isApplicationRunningOnLocal() {
		String property = URLMaping.INSTANCE_DIR;
		Boolean valueOf = Boolean.valueOf(property);
		return valueOf;
	}
	
	/**
	 * get value of a node
	 * @param node
	 * @return value
	 */
	public static String getNodeCdataOrTextValue(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.CDATA_SECTION_NODE && child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}
	
	/**
	 * Extract the text data of a node
	 * @param expr XPath expression for selecting the node
	 * @param node the node
	 * @return contents of desired node, or null if none found
	 */
	public static String extractData(String expr, Node node){
		String data = null;
		try{			
			NodeList nl = getAllNodes(node, expr);
			for(int i=0; i<nl.getLength(); i++) {
		        Node child = nl.item(i);
		        data = getNodeCdataOrTextValue(child);
			}					
		}catch(XPathExpressionException e){
			
		}
		return data;
	}
	
}
