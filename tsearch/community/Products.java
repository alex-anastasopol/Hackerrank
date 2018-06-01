package ro.cst.tsearch.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class Products {
	
	
	private HashMap<Integer, HashMap> communityProducts = new HashMap<Integer, HashMap>();
	public static String[] productListNames = null;
	public static List<ProductsMapper> products = null;
	
	public static final int UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS = 20;
	
	public static final int FULL_SEARCH_PRODUCT = 1;
	public static final int CURRENT_OWNER_PRODUCT = 2;
	public static final int CONSTRUCTION_PRODUCT = 3;
	public static final int COMMERCIAL_PRODUCT = 4;
	public static final int REFINANCE_PRODUCT = 5;
	public static final int OE_PRODUCT = 6;
	public static final int LIENS_PRODUCT = 7;
	public static final int ACREAGE_PRODUCT = 8;
	public static final int SUBLOT_PRODUCT = 9;
	//updates and other type with was_opened set or closed
	public static final int UPDATE_PRODUCT = 10;
	public static final int INDEX_PRODUCT = 11;	//not really a search
	//only searches with search_type = update
	public static final int UPDATE_PURE_PRODUCT = 14;
	
	public static final int FVS_PRODUCT = 12;
	
	public static final int INTERNAL_ONLY = 0;
	public static final int RPC_ASSOCIATE_ONLY = 15;
	
	
	public static String Product1Name = "Sale";
	public static String PRODUCT_FAKE_INDEX = "IndexRatio";
	
	public static String PRODUCT_SHORT_ABBREV 		= "short";
	public static String PRODUCT_SEARCH_FROM_ABBREV = "searchFrom";
	public static String PRODUCT_TSR_VIEW_FROM_ABBREV = "tsrViewFilter";
	
	
	public static int FIELD_NAME = 1;
	public static int FIELD_SHORT_NAME = 2;
	public static int FIELD_TSR_VIEW_FILTER = 3;
	public static int FIELD_SEARCH_FROM									= 4;
	
	public static enum TsrViewFilterOptions {
		DEF,
		LTD,
		PTD
	}
	
	public static enum SearchFromOptions {
		DEF,
		LTD,
		PTD,
		CD
	}
	
	public static enum UPDATE_PRODUCTS{
        UPDATE(UPDATE_PRODUCT),
        FVS(FVS_PRODUCT);
        
        private final int value;

        private UPDATE_PRODUCTS(final int newValue) {
            value = newValue;
        }
        public int getValue() { return value; }
    }
	
	public static boolean isOneOfUpdateProductType(int productType){
    	for (UPDATE_PRODUCTS updp : UPDATE_PRODUCTS.values()){
    		if (updp.getValue() == productType){
    			return true;
    		}
    	}
    	
    	return false;
    }
	
	public Products(){}
	
	public Products(HashMap products){
		this.communityProducts  = products; 
	}
			
	public void setProduct(int productId, HashMap productInfo){		
		communityProducts.put(productId, productInfo);
	}

	public String getProductName(int productId){
		if(productId == INDEX_PRODUCT) {
			return "Index";
		}
		return  communityProducts.get(productId).get(FIELD_NAME).toString();
	}
	
	public String getShortProductName(int productId){
		return communityProducts.get(productId).get(FIELD_SHORT_NAME).toString();
	}
	
	public String getTsrViewFilter(int productId){
		String ret = TsrViewFilterOptions.DEF.toString();
		try {
			String value = communityProducts.get(productId).get(FIELD_TSR_VIEW_FILTER).toString();
			ret = (value.trim().isEmpty())?ret:value.trim();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public String getSearchFrom(int productId){
		String ret = SearchFromOptions.DEF.toString();
		try {
			String value = communityProducts.get(productId).get(FIELD_SEARCH_FROM).toString();
			ret = (value.trim().isEmpty())?ret:value.trim();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static List<ProductsMapper> getProductList(){
		
		if (products == null){
			products = DBManager.getProducts(false);
		}
		
		return products;
	}
	
	public static String[] getProductListNames(){
		if (productListNames == null){
			if (products == null){
				products = getProductList();
			}
			List<Integer> prodIds = new ArrayList<Integer>();
			for (ProductsMapper prod : products) {
				prodIds.add(prod.getProductId());
			}
			Collections.sort(prodIds);
			productListNames = new String[prodIds.get(prodIds.size() - 1) + 1];

			for (int i = 0; i < productListNames.length; i++) {
				for (ProductsMapper prod : products) {
					if (i == prod.getProductId()){
						productListNames[i] = prod.getAlias();
						break;
					}
				}
				if (org.apache.commons.lang.StringUtils.isEmpty(productListNames[i])){
					productListNames[i] = "";
				}
			}
		}
		return productListNames;
	}
	
	public static ProductsMapper getProductById(int productId){
		List<ProductsMapper> productList = getProductList();
		
		for (ProductsMapper productsMapper : productList) {
			if (productsMapper.getProductId() == productId){
				return productsMapper;
			}
		}
		return null;
	}

	public HashMap<Integer, HashMap> getProducts() {
		return communityProducts;
	}
	
	
	/**
	 * build products list used for insert operation
	 * @return String
	 */
	public static String buildProductList(){

		List<ProductsMapper> products = DBManager.getProducts(true);
					
		  String out = "";
		  for (int i = 0; i < products.size(); i++){
			  
			  ProductsMapper product = products.get(i);
				
			  String productDefaultName 	= product.getAlias();
			  String productShortName    	= product.getShortName();
			  int orderId					= product.getOrder();
			  int product_id 				= product.getProductId();
			    
			  out += "<tr class='row" + ((i%2) + 1) + "'>" + 
					  "	<td width='3'><span> &nbsp;" + orderId + "&nbsp;</span></td>"+
					  "	<td ><span> &nbsp;" + productDefaultName + "&nbsp;</span></td>"+
					  "	<td align='left' width='60%'>" + 
					  "   <input size='25%' maxlength='50' name='" + productDefaultName + "' value='" + productDefaultName + "' style='width:100%'>" +
					  "	</td> "+
					  "	<td align='left' width='20%'>" +
					  "   <input size='25%' maxlength='50' name='" + PRODUCT_SHORT_ABBREV + product.getAlias() + "' value='" + productShortName + "' style='width:100%'>" +
					  "	</td> "+                        	
					  "  </tr>";
		  }
		  
	 return out; 
	}
	
	/**
	 * builds product list used for update operation
	 * @param commId
	 * @return String
	 */
	public static String buildProductList(long commId){

		List<ProductsMapper> products = DBManager.getProducts(true);
		
		
		Products currentCommunityProducts = CommunityProducts.getProduct(commId);
		String out = "";
		for (int i = 0; i < products.size(); i++){
			
			ProductsMapper product = products.get(i);
			
			String productDefaultName 	= product.getAlias();			    
			int orderId					= product.getOrder();
			int product_id 				= product.getProductId();
			    
			out += "<tr class='row" + ((i%2) + 1) + "'>" + 
					"	<td width='3'><span> &nbsp;" + orderId + "&nbsp;</span></td>" +
					"	<td ><span> &nbsp;" + productDefaultName + "&nbsp;</span></td>" +
					"	<td ><span> &nbsp;" + product_id + "&nbsp;</span></td>" +
					"	<td align='left' width='40%'>" + 
					"	   <input size='25%' maxlength='50' name='" + productDefaultName + "' value='" + currentCommunityProducts.getProductName(product_id) + "' style='width:100%'>" +
					"	</td> "+
					"	<td align='left' width='20%'>" +
					"   	<input size='25%' maxlength='50' name='" + PRODUCT_SHORT_ABBREV + product.getAlias() + "' value='" + currentCommunityProducts.getShortProductName(product_id) + "' style='width:100%'>" +
					"	</td> "+  
					"	<td align='left' width='20%'>" +
					"   	<input size='25%' maxlength='50' name='" + PRODUCT_SEARCH_FROM_ABBREV + product.getAlias() + "' value='" + currentCommunityProducts.getSearchFrom(product_id) + "' style='width:100%'>" +
					"	</td> "+
					"	<td align='left' width='20%'>" +
					"   	<input size='25%' maxlength='50' name='" + PRODUCT_TSR_VIEW_FROM_ABBREV + product.getAlias() + "' value='" + currentCommunityProducts.getTsrViewFilter(product_id) + "' style='width:100%'>" +
					"	</td> "+      
					"  </tr>";
		  }
		  
	 return out; 
	}
	
	/**
	 * builds product list used for update operation
	 * @param commId
	 * @return String
	 */
	public static String buildViewProductList(long commId){

		List<ProductsMapper> products = DBManager.getProducts(true);
		
		
		Products currentCommunityProducts = CommunityProducts.getProduct(commId);
		String out = "";
		for (int i = 0; i < products.size(); i++){
			
			ProductsMapper product = products.get(i);
			
			String productDefaultName 	= product.getAlias();			    
			int orderId					= product.getOrder();
			int product_id 				= product.getProductId();
			    
			out += "<tr class='row" + ((i%2) + 1) + "'>" + 
					"	<td width='3'><span> &nbsp;" + orderId + "&nbsp;</span></td>" +
					"	<td ><span> &nbsp;" + productDefaultName + "&nbsp;</span></td>" +
					"	<td ><span> &nbsp;" + product_id + "&nbsp;</span></td>" +
					"	<td align='left' width='40%'>" + currentCommunityProducts.getProductName(product_id) + " " + "</td> "+
					"	<td align='left' width='20%'>" + currentCommunityProducts.getShortProductName(product_id) + " " + "</td> "+ 
					"	<td align='left' width='20%'>" + currentCommunityProducts.getSearchFrom(product_id) + " " + "</td> "+
					"	<td align='left' width='20%'>" + currentCommunityProducts.getTsrViewFilter(product_id) + " " + "</td> "+      
					"  </tr>";
		  }
		  
	 return out; 
	}
	
	public void updateProductList(long commId){
		
		Vector<Vector<Object>> allValues = new Vector<Vector<Object>>();
		
		Iterator it = communityProducts.entrySet().iterator();
		CommunityProducts.setProduct(commId, (new Products(communityProducts)));
		while (it.hasNext()) {
			Map.Entry element   = (Map.Entry)it.next();
			
			Vector<Object> insertValues = new Vector<Object>();
			insertValues.add(commId);
			insertValues.add(element.getKey());
			insertValues.add(((HashMap)(element.getValue())).get(FIELD_NAME));
			insertValues.add(((HashMap)(element.getValue())).get(FIELD_SHORT_NAME));
			insertValues.add(((HashMap)(element.getValue())).get(FIELD_TSR_VIEW_FILTER));
			insertValues.add(((HashMap)(element.getValue())).get(FIELD_SEARCH_FROM));
			allValues.add(insertValues);
		}

		  DBManager.updateCommunityProducts(commId, allValues);
	}
	
	public static String getProductShortNameStringLength3(long searchId){	
		String ret ="";
		try{
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			SearchAttributes sa = global.getSa();
			String typeString = sa.getAtribute(SearchAttributes.SEARCH_PRODUCT);

			int typeId = Integer.parseInt(typeString);
			
			UserAttributes ua = global.getAgent();
			
			Products currentProduct = CommunityProducts.getProduct(ua.getCOMMID().longValue());		    					
			ret = currentProduct.getShortProductName( typeId);
			
			ret = ret+"AAA";
			ret = ret.substring(0,3);
			
		} catch(Exception e){
			//e.printStackTrace();
		}
		
		if(!StringUtils.isEmpty(ret)){
			return ret;
		}
		
		try{
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			SearchAttributes sa = global.getSa();
			String typeString = sa.getAtribute(SearchAttributes.SEARCH_PRODUCT);

			int typeId = Integer.parseInt(typeString);
			
			long commId  = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getCOMMID().longValue();		
			Products currentProduct = CommunityProducts.getProduct(commId);		    					
			ret = currentProduct.getShortProductName( typeId);
			
			ret = ret+"AAA";
			ret = ret.substring(0,3);
			
			return ret;
		} catch(Exception e){
			//e.printStackTrace();
			return "TSR";
		}
	}
	
	public static String[] getAllProductShortNameStringLength3(long searchId){
		String []temp = {"TSR"};
		try{
			long commId  = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getCOMMID().longValue();		
			Products currentProduct = CommunityProducts.getProduct(commId);		    					
			Set<Entry<Integer, HashMap>>  set = currentProduct .communityProducts.entrySet();
			String []types = new String[set.size()];
			int i=0;
			for(Entry<Integer, HashMap>el:set){
			    HashMap value = el.getValue();
			    String ret =(String)value.get(2)+"AAA"; 
			    types[i] = ret.substring(0,3);
			    i++;
			}
			
			return types;
		} catch(Exception e){
			e.printStackTrace();
			return temp ;
		}
	}
	
	public static String getTsrViwFilter(int communityId, int productId){
		try {
			return CommunityProducts.getProduct(communityId).getTsrViewFilter(productId);		    					
		}catch(Exception e) {
			e.printStackTrace();
		}
		return TsrViewFilterOptions.DEF.toString();
	}
	
	public static String getSearchFrom(int communityId, int productId){
		try {
			return CommunityProducts.getProduct(communityId).getSearchFrom(productId);		    					
		}catch(Exception e) {
			e.printStackTrace();
		}
		return SearchFromOptions.DEF.toString();
	}
	
	@SuppressWarnings("rawtypes")
	public static Map<Long,String> getAllProductShortNameLength3ForCommunity(int communityId){
		Map<Long,String> types = new TreeMap<Long, String>();
		try{
			Set<Entry<Integer, HashMap>>  set = new HashSet<Entry<Integer, HashMap>>(); 
			
			if (communityId==-1) {
				Set<Long> communities = CommunityProducts.getAllCommunities();
				for (Long comm: communities) {
					Products currentProduct = CommunityProducts.getProduct(comm.intValue());		    					
					Set<Entry<Integer, HashMap>> partialSet = currentProduct.communityProducts.entrySet();
					set.addAll(partialSet);
				}
			} else {
				Products currentProduct = CommunityProducts.getProduct(communityId);		    					
				set = currentProduct.communityProducts.entrySet();
			}
			
			for(Entry<Integer, HashMap>el:set){
			    HashMap value = el.getValue();
			    String ret =(String)value.get(2)+"AAA";
			    types.put(el.getKey().longValue(), ret.substring(0,3));
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
		return types;
	}
	
	public static Map<Long,String> getAllProductShortNameLength3ForCommunity(int[] communityIds){
		Map<Long,String> result = new TreeMap<Long, String>();
		for (int i=0;i<communityIds.length;i++) {
			Map<Long,String> partialResult = getAllProductShortNameLength3ForCommunity(communityIds[i]);
			result.putAll(partialResult);
		}
		return result;
	}
	
	public static Map<Long,String> getAllProductShortNameForCommunity(int communityId){
		Map<Long,String> types = new TreeMap<Long, String>();
		try{
			Products currentProduct = CommunityProducts.getProduct(communityId);		    					
			Set<Entry<Integer, HashMap>>  set = currentProduct.communityProducts.entrySet();
			
			for(Entry<Integer, HashMap>el:set){
			    HashMap value = el.getValue();
			    types.put(el.getKey().longValue(), (String)value.get(1));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return types;
	}
	
	public static Map<Long,String> getAllProductShortNameForCommunity(int[] communityIds){
		Map<Long,String> result = new TreeMap<Long, String>();
		for (int i=0;i<communityIds.length;i++) {
			Map<Long,String> partialResult = getAllProductShortNameForCommunity(communityIds[i]);
			result.putAll(partialResult);
		}
		return result;
	}
	
}
