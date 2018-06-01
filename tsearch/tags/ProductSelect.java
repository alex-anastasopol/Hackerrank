package ro.cst.tsearch.tags;

import java.util.List;

import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.servlet.user.ManageCountyList;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.RequestParams;

public class ProductSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int[] productType = {-1};
	public void setProductType(String s) {
		productType = Util.extractArrayFromString(s);
	}	
	public String getProductType() {
		return Util.getStringFromArray(productType);
	}
	
	private boolean readOnly = false;
	public void setReadOnly(String s) {
		if("true".equalsIgnoreCase(s))
			readOnly = true;
	}
	public String getReadOnly(){
		if(readOnly)
			return "true";
		return "false";
	}
	
	@Override
	protected String createOptions() throws Exception {
		if( getRequest().getParameter(RequestParams.SEARCH_PRODUCT_TYPE) == null) {
			try {
				setProductType(UserFilterMapper.getAttributesAsString(
						Integer.parseInt(getRequest().getParameter(UserAttributes.USER_ID)), 
						UserManager.dashboardFilterTypes.get("ProductType")));
			} catch (Exception e) {
				loadAttribute(RequestParams.SEARCH_PRODUCT_TYPE);
			}
		} else {
			if(ManageCountyList.RESET_ALL_FILTERS.equals(getRequest().getParameter("operation")) || 
					ManageCountyList.RESET_PRODUCTS.equals(getRequest().getParameter("operation"))) {
				setProductType("-1");
			} else {
				loadAttribute(RequestParams.SEARCH_PRODUCT_TYPE);	
			}
			
		}
		
		StringBuilder sb = new StringBuilder(allOption(productType));
		List<ProductsMapper> products = Products.getProductList();
		for (ProductsMapper prod : products) {
			
			if(readOnly) {
				if (Util.isValueInArray(prod.getProductId(), productType)) {
					sb.append( "<option selected value='" + prod.getProductId() + "'>" + prod.getAlias() + "</option>\n");
				}
			} else {
				sb.append(
						"<option "
							+ (Util.isValueInArray(prod.getProductId(), productType) ? "selected" : "")
							+ " value='" + prod.getProductId() + "'>" + prod.getAlias() + "</option>\n");
			}
		}
		return sb.toString();
	}
	
	protected String allOption(int[] id) throws Exception {
		if(all ) {
			if(readOnly) {
				if(Util.isValueInArray(-1, id)) {
					return "<option selected value='-1'>See All Transactions</option>" ;
				}
			} else {
				return "<option "+(Util.isValueInArray(-1, id)?"selected":"") +
					" value='-1'>All Transactions</option>" ;
			}
		}
		return "";
	}

}
