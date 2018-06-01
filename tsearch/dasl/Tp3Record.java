package ro.cst.tsearch.dasl;

import java.util.LinkedHashSet;
import java.util.Set;


import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import static ro.cst.tsearch.utils.XmlUtils.*;
import static org.apache.commons.lang.StringEscapeUtils.*;


public class Tp3Record {
		
	protected static Logger logger = Logger.getLogger(Tp3Record.class);
	
	private String amount = "";
	private String docNo = "";
	private String docType = "";
	private String caseNo = "";
	private XmlRecord recDate = null;
	private XmlRecord sigDate = null;
	
	private Set<String> comments = new LinkedHashSet<String>();
	private Set<String> remarks = new LinkedHashSet<String>();

	private Set<XmlRecord> legals = new LinkedHashSet<XmlRecord>();
	private Set<XmlRecord> addresses = new LinkedHashSet<XmlRecord>();
	private Set<XmlRecord> parties = new LinkedHashSet<XmlRecord>();
	
	/**
	 * Return XML representation of the Document
	 */
	@Override
	public String toString(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("<TitleDocument>");
		for(String comment : comments){
			sb.append("<Comment>" + escapeXml(comment) + "</Comment>");
		}
		for(XmlRecord record : legals){
			sb.append(record.toString());
		}
		sb.append("<Instrument>");
		if(!"".equals(amount)){
			sb.append("<ConsiderationAmount>" + escapeXml(amount) + "</ConsiderationAmount>");			
		}
		if(!"".equals(docNo)){
			sb.append("<DocumentNumber>" + escapeXml(docNo) + "</DocumentNumber>");			
		}
		if(!"".equals(docType)){
			sb.append("<Type>" + escapeXml(docType) + "</Type>");			
		}
		if(!"".equals(caseNo)){
			sb.append("<CaseNumber>" + escapeXml(caseNo) + "</CaseNumber>");			
		}
		if(recDate != null){			
			sb.append(recDate.toString());			
		}
		if(sigDate != null){
			sb.append(sigDate.toString());			
		}
		for(String remark : remarks){
			sb.append("<Remarks>" + escapeXml(remark) + "</Remarks>");
		}
		for(XmlRecord address : addresses){
			sb.append(address.toString());
		}
		for(XmlRecord party: parties){
			sb.append(party.toString());
		}
		sb.append("</Instrument>");
		sb.append("</TitleDocument>");
		return sb.toString();
	}
	
	/**
	 * Parse a document
	 * @param doc
	 * @param record
	 * @return
	 */
	public static Tp3Record parseDocument (Node doc, Tp3Record record){
		
		if(!"TitleDocument".equals(doc.getNodeName())){
			logger.error("Doc not doc.getNodeName()");
			return null;
		}
		
		if(record == null){
			record = new Tp3Record();
		}
		
		for(Node child: getChildren(doc)){			
			String childName = child.getNodeName();
			if("Comment".equals(childName)){				
				String childValue = getNodeValue(child);
				if(!"".equals(childValue)){
					record.comments.add(childValue);
				}
			} else if("LegalDescription".equals(childName)){
				XmlRecord legal = XmlRecord.parseXmlRecord(child);
				record.legals.add(legal);
			} else if("Instrument".equals(childName)){
				for(Node grand: getChildren(child)){
					String grandName = grand.getNodeName();					
					if("PropertyAddress".equals(grandName)){
						record.addresses.add(XmlRecord.parseXmlRecord(grand));
					} else if("PartyInfo".equals(grandName)){
						record.parties.add(XmlRecord.parseXmlRecord(grand));
					} else if("RecordedDate".equals(grandName)){
						record.recDate = XmlRecord.parseXmlRecord(grand);
					} else if("SignedDate".equals(grandName)){
						record.sigDate = XmlRecord.parseXmlRecord(grand);
					} else {							
						String grandValue = getNodeValue(grand);
						if(!"".equals(grandValue)){
							if("ConsiderationAmount".equals(grandName)){						
								record.amount = grandValue;
							} else if("DocumentNumber".equals(grandName)){
								record.docNo = grandValue;
							} else if("Remarks".equals(grandName)){
								record.remarks.add(getNodeValue(grand));
							} else if("Type".equals(grandName)){
								record.docType = grandValue;
							} else if("CaseNumber".equals(grandName)){
								record.caseNo = grandValue;
							}else {
								//logger.warn("Node :" + grandName + " ignored");
							}
						}
					}
				}
			} else {
				//logger.warn("Node :" + childName + " ignored");
			}
		}
		
		return record;
	}
}
