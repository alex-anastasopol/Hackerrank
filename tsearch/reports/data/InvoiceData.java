package ro.cst.tsearch.reports.data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

public class InvoiceData {

	private static String dateFormat = "EEE MMM dd, yyyy HH:mm:ss";
	private String logoFile;
	//these 4 fields are used with many meanings.... sometimes they refer to an agent...
	private String commName;
	private String commAddress;
	private String commPhone;
	private String commEmail;
	
	//these 4 fields are used only when sending a pdf as commAdmin and an agent is selected  
	private String agentName = "";
	private String agentAddress = "";
	private String agentPhone = "";
	private String agentEmail = "";
	
	private Date startInterval;
	private Date endInterval;
	private Date invoiceTimestamp;
	
	HashMap<String, HashMap<Double, Integer>> subTotals = new HashMap<String, HashMap<Double,Integer>>();
	
	private String details;
	
	
	private Products communityProducts  ;
	
	
	public String getCommAddress() {
		return commAddress;
	}
	
	public String getCommEmail() {
		return commEmail;
	}

	public String getCommName() {
		return commName;
	}

	public String getCommPhone() {
		return commPhone;
	}

	public String getEndInterval() {
		return new SimpleDateFormat(dateFormat).format(endInterval);
	}

	public String getInvoiceTimestamp() {
		return new SimpleDateFormat(dateFormat).format(invoiceTimestamp);
	}
	

	public String getStartInterval() {
		return new SimpleDateFormat(dateFormat).format(startInterval);
	}

	public void setCommAddress(String string) {
		commAddress = string;
	}

	public void setCommEmail(String string) {
		commEmail = string;
	}

	public void setCommName(String string) {
		commName = string;
	}

	public void setCommPhone(String string) {
		commPhone = string;
	}

	public void setEndInterval(Date date) {
		endInterval = date;
	}

	public void setInvoiceTimestamp(Date date) {
		invoiceTimestamp = date;
	}
	
	public void setStartInterval(Date date) {
		startInterval = date;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String string) {
		details = string;
	}

	public String getLogoFile() {
		return logoFile;
	}

	public void setLogoFile(String string) {
		logoFile = string;
	}
	
	public String getAgentAddress() {
		return agentAddress;
	}

	public void setAgentAddress(String agentAddress) {
		this.agentAddress = agentAddress;
	}

	public String getAgentEmail() {
		return agentEmail;
	}

	public void setAgentEmail(String agentEmail) {
		this.agentEmail = agentEmail;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentPhone() {
		return agentPhone;
	}

	public void setAgentPhone(String agentPhone) {
		this.agentPhone = agentPhone;
	}
	
	public void setInvoiceProduct(int commId){
		 communityProducts  = CommunityProducts.getProduct(commId);
	}

	/**
	 * @return the subTotals
	 */
	public HashMap<String, HashMap<Double, Integer>> getSubTotals() {
		return subTotals;
	}

	/**
	 * @param subTotals the subTotals to set
	 */
	public void setSubTotals(HashMap<String, HashMap<Double, Integer>> subTotals) {
		this.subTotals = subTotals;
	}

	public String getTableSubtotals() {
		StringBuilder table = new StringBuilder();
		table.append("<TABLE cellSpacing=\"0\" cellPadding=\"0\" width=\"100%\" border=\"0\">");
		
		int[] productsList = new int[] {
				Products.FULL_SEARCH_PRODUCT,
				Products.CURRENT_OWNER_PRODUCT,
				Products.CONSTRUCTION_PRODUCT,
				Products.COMMERCIAL_PRODUCT,
				Products.REFINANCE_PRODUCT,
				Products.OE_PRODUCT,
				Products.LIENS_PRODUCT,
				Products.ACREAGE_PRODUCT,
				Products.SUBLOT_PRODUCT,
				Products.UPDATE_PRODUCT,
				Products.INDEX_PRODUCT,
				Products.FVS_PRODUCT
		};
		double totalValue = 0;
		for (int i = 0; i < productsList.length; i++) {
			String key = getProductKey(productsList[i], false);
			if(StringUtils.isNotEmpty(key)) {
				table.append("<tr><td>");
				table.append(getProductTypeName(productsList[i], false));
				table.append(getComponentsForKey(key));
				double value = getTotalForKey(key);
				totalValue += value;
				table.append(" = US$ " + value);
				table.append("</td></tr>");
			}
		}
		
		for (int i = 0; i < productsList.length; i++) {
			String key = getProductKey(productsList[i], true);
			if(StringUtils.isNotEmpty(key)) {
				table.append("<tr><td>");
				table.append(getProductTypeName(productsList[i], true));
				table.append(getComponentsForKey(key));
				double value = getTotalForKey(key);
				totalValue += value;
				table.append(" = US$ " + value);
				table.append("</td></tr>");
			}
		}
		
		
		table.append("<tr><td>");
		table.append("Total : US$ " + totalValue);
		table.append("</td></tr>");
		
		return table.append("</table>").toString();
	}

	public String getProductTypeName(int productType, boolean withDiscount) {
		String key = communityProducts.getProductName(productType);
		String result = key;
		if(withDiscount) {
			key += "Discount";
			result += " with Discount : ";
		} else {
			result += " : ";
		}
		if(subTotals.containsKey(key)) {
			return result;
		} else {
			return null;
		}
	}
	
	public String getProductKey(int productType, boolean withDiscount) {
		String key = communityProducts.getProductName(productType);
		
		if(withDiscount) {
			key += "Discount";
		}
		if(subTotals.containsKey(key)) {
			return key;
		} else {
			return null;
		}
	}
	
	
	
	private String getComponentsForKey(String key) {
		HashMap<Double,Integer> components = subTotals.get(key);
		if(components == null || components.size() == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (Double price : components.keySet()) {
			if(sb.length() > 0) {
				sb.append(" + ");
			}
			sb.append(components.get(price) + " x US$ " + ATSDecimalNumberFormat.format(new BigDecimal(price)));
		}
		return sb.toString();
	}
	
	private double getTotalForKey(String key) {
		HashMap<Double,Integer> components = subTotals.get(key);
		if(components == null || components.size() == 0)
			return 0;
		double result = 0;
		for (Double price : components.keySet()) {
			result += price * components.get(price);
		}
		return result;
	}
		
}
