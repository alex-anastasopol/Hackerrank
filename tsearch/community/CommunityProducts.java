package ro.cst.tsearch.community;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;

public class CommunityProducts {

	public static HashMap<Long, Products> communityProducts ;
	
	public static final String FIELD_ID = "id";
	public static final String FIELD_COMM_ID = "comm_id";
	public static final String FIELD_PRODUCT_ID = "product_id";
	public static final String FIELD_PRODUCT_NAME = "product_name";
	public static final String FIELD_SHORT_NAME = "shortName";
	public static final String FIELD_TSR_VIEW_FILTER = "tsrViewFilter";
	public static final String FIELD_SEARCH_FROM = "searchFrom";
	
	public CommunityProducts(){}
	
	public static void addProduct(long commId, Products product){
		communityProducts.put(Long.valueOf(commId), product);
	}
	
	public static void setProduct(long commId, Products product){
		communityProducts.put(Long.valueOf(commId), product);
	}
	
	public static Set<Long> getAllCommunities() {
		return communityProducts.keySet();
	}
	
	public static Products getProduct (long commId){
		if(communityProducts == null || communityProducts.size() == 0){
			fillComunityProducts();
		}
		return communityProducts.get(Long.valueOf(commId));
	}
	
	public static LinkedHashMap<Integer,HashMap> getProductsMap(long commId){
		
		LinkedHashMap<Integer,HashMap> orderedProducts = new LinkedHashMap<Integer, HashMap>(); 
		
		List<ProductsMapper> products = DBManager.getProducts(true);
		
		Products currentCommunityProducts = CommunityProducts.getProduct(commId);
		
		  for (int i = 0; i < products.size(); i++){
			  ProductsMapper product = products.get(i);
			  
			  int product_id = product.getProductId();
			  orderedProducts.put(product_id, currentCommunityProducts.getProducts().get(product_id));
		  }
		  
		return orderedProducts;
	}
	
	public static void fillComunityProducts(){
		
		DatabaseData data = DBManager.getCommunitiesProducts();

		///reset community products 
		communityProducts = new HashMap<Long, Products>();		
		
		  Products currentProduct = null;		
		  int currentCommId=0,prevCommunityId = 0;		  
		  String productName = "", productShortName = "", tsrViewFilter="", searchFrom = "";
		  int productId = 0;
		  
		  
		  for (int i=0;i<data.getRowNumber();i++){
			  currentCommId = Integer.parseInt(data.getValue(FIELD_COMM_ID,i).toString());
			  productId		    		= Integer.parseInt(data.getValue(FIELD_PRODUCT_ID, i).toString());
			  productName			= data.getValue(FIELD_PRODUCT_NAME,i).toString();
			  productShortName	= data.getValue("product_short_name",i).toString();
			  tsrViewFilter	= data.getValue(FIELD_TSR_VIEW_FILTER,i).toString();
			  searchFrom = data.getValue(FIELD_SEARCH_FROM, i).toString();
			  
			  if ( currentCommId != prevCommunityId){				  				  
				    if (prevCommunityId !=0){
				    	///add new community product object
				    	addProduct(prevCommunityId, currentProduct);
				    }
				    currentProduct = new Products();
				    prevCommunityId = currentCommId;
			  }
			  
			  ///add in current loop product
			  HashMap<Integer,String> tmp   = new HashMap<Integer,String>();
			  tmp.put(Products.FIELD_NAME, productName);
			  tmp.put(Products.FIELD_SHORT_NAME, productShortName);
			  tmp.put(Products.FIELD_TSR_VIEW_FILTER, tsrViewFilter);
			  tmp.put(Products.FIELD_SEARCH_FROM, searchFrom);
			  currentProduct.setProduct(productId, tmp);			  
			  
		  }
		  //add last community product
		  addProduct(currentCommId, currentProduct);
	}		
}
