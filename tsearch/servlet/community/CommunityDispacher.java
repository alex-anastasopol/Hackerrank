package ro.cst.tsearch.servlet.community;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CategoryAttributes;
import ro.cst.tsearch.community.CategoryManager;
import ro.cst.tsearch.community.CategoryUtils;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.TSParameters;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserManagerI;
/**
 * @author nae
 *
 */
public class CommunityDispacher extends BaseServlet {
	/**
	 * 
	 *
	 * @param request The servlet request we are processing
	 * @param response The servlet response we are producing
	 *
	 * @exception IOException if an input/output error occurs
	 * @exception ServletException if a servlet error occurs
	 */
	public void doRequest(HttpServletRequest request, HttpServletResponse response)
		throws IOException, BaseException {
		String fowardlink = URLMaping.path;
		
		//HttpSession session =  request.getSession();
		//User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		
		long searchId = Search.SEARCH_NONE;
		MultipartParameterParser mpp = null;
		ParameterParser pp = null;
		String ret="OK";	
		int opCode=-1;
		String comm_id="-1";		
		
		String contentType = request.getContentType();
		if (contentType != null && contentType.indexOf("multipart/form-data") > -1) {
		//try{			
			mpp = new MultipartParameterParser(request);
			
		    //mpp = InstanceManager.getCurrentInstance().getMultipartParameterParser();
			opCode = mpp.getMultipartIntParameter(TSOpCode.OPCODE, -1);
			searchId = mpp.getMultipartLongParameter(RequestParams.SEARCH_ID, -1);
			comm_id = mpp.getMultipartStringParameter(CommunityAttributes.COMMUNITY_ID, "-1");
		} else {
		//}catch (IOException e){
			pp = new ParameterParser(request);
			opCode = pp.getIntParameter(TSOpCode.OPCODE, -1);
			comm_id = pp.getStringParameter(CommunityAttributes.COMMUNITY_ID, "-1");
			searchId = pp.getLongParameter(RequestParams.SEARCH_ID, -1);
		}
		if(!comm_id.matches("\\-?[0-9]+")
				&& opCode != TSOpCode.DEL_CATEGORY 
				&& opCode != TSOpCode.SAVE_CATEGORY
				&& opCode != TSOpCode.HIDE_COMMUNITY
				&& opCode != TSOpCode.UNHIDE_COMMUNITY) {
			System.err.println("Invalid parrameter: " + "commId = " + comm_id);
			return;
		}
		Hashtable hash = new Hashtable();
		String[] privacyStatement= null;
		// based on opCode
		switch (opCode) {
			case TSOpCode.MANAGE_POLICYS :
			//	UploadPolicyDoc.updateDB(null, UploadPolicyDoc.COMMUNITY_EXISTS, Long.parseLong(comm_id));
			//	fowardlink = URLMaping.path + URLMaping.StartTSPage + "?" + RequestParams.SEARCH_ID + "=" + searchId;
				
				privacyStatement= request.getParameterValues(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT);
				
				CommunityAttributes communityAttributes = CommunityUtils.getCommunityFromId(Long.valueOf(comm_id));
				if(privacyStatement!=null && privacyStatement.length==1){
					communityAttributes.setIgnorePrivacyStatement(Boolean.TRUE);
				}else{
					communityAttributes.setIgnorePrivacyStatement(Boolean.FALSE);
				}
				
				
				hash = CommunityUtils.getCommunityInfo(communityAttributes);
				CommunityUtils.modifyCommunity(hash);
				fowardlink = URLMaping.path + URLMaping.COMMUNITY_POLICY_MANAGE+ "?"
							+ CommunityAttributes.COMMUNITY_ID + "="+comm_id + "&"
							+ RequestParams.SEARCH_ID + "=" + searchId;
				break;
			case TSOpCode.NEW_COMMUNITY :			
			String name="";				
				name =mpp.getMultipartStringParameter(CommunityAttributes.COMMUNITY_NAME, "");
			long admin =
				mpp.getMultipartLongParameter(
					CommunityAttributes.COMMUNITY_ADMIN,
					-1);
			long category =
				mpp.getMultipartLongParameter(
					CommunityAttributes.COMMUNITY_CATEGORY,
					-1);
				
				if(name.equals("")){
					fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_ADD						
						+ "?" + RequestParams.SEARCH_ID + "=" + searchId
						+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADD_VIEW
						+ "&ret=Please Enter all Required Paramters";						
				}else{
					hash.put(CommunityAttributes.COMMUNITY_ID, comm_id);
					hash.put(CommunityAttributes.COMMUNITY_NAME, name);
					hash.put(
						CommunityAttributes.COMMUNITY_CATEGORY,
						new Long(category));
					hash.put(
						CommunityAttributes.COMMUNITY_DESCRIPTION,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_DESCRIPTION,
							"N/A"));
					hash.put(
						CommunityAttributes.COMMUNITY_SEE_ALSO,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_SEE_ALSO,
							"N/A"));
					hash.put(
						CommunityAttributes.COMMUNITY_ADMIN,
						new Long(admin));
					hash.put(
						CommunityAttributes.COMMUNITY_ADDRESS,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_ADDRESS,
							"N/A"));
					hash.put(
						CommunityAttributes.COMMUNITY_PHONE,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_PHONE,
							"N/A"));
					hash.put(
						CommunityAttributes.COMMUNITY_EMAIL,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_EMAIL,
							"N/A"));
					hash.put(
						CommunityAttributes.COMMUNITY_CONTACT,
						mpp.getMultipartStringParameter(
							CommunityAttributes.COMMUNITY_CONTACT,
							"N/A"));
					hash.put( CommunityAttributes.COMMUNITY_CTIME,
								new BigInteger( "" + System.currentTimeMillis() ));
					
					hash.put( CommunityAttributes.COMMUNITY_CODE,
					        mpp.getMultipartStringParameter(CommunityAttributes.COMMUNITY_CODE, "N/A"));
					
					hash.put( CommunityAttributes.COMMUNITY_OFFSET,
					        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_OFFSET, 0)));
					
					hash.put( CommunityAttributes.COMMUNITY_AUTOFILEID,
					        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_AUTOFILEID, 0)));
					
					hash.put(RequestParams.SESSION_ID, request.getSession().getId());
					privacyStatement= mpp.getRequest().getParameterValues(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT);
					if(privacyStatement!=null && privacyStatement.length==1){
						hash.put(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT, Boolean.TRUE);
					}else{
						hash.put(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT, Boolean.FALSE);
					}
					
					hash.put(
							CommunityAttributes.COMMUNITY_PLACEHOLDER,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_PLACEHOLDER,
								""));
					
					File commLogo=null, termsOfUse = null, privacyState = null;
					if(mpp!=null)
						commLogo= mpp.getFileParameter(CommunityAttributes.COMMUNITY_LOGO, null);
						termsOfUse = mpp.getFileParameter(CommunityAttributes.COMMUNITY_TERMS_OF_USE, null);
						privacyState = mpp.getFileParameter(CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT, null);
					if (commLogo != null)
								hash.put(CommunityAttributes.COMMUNITY_LOGO, commLogo);	
					if (termsOfUse != null)
								hash.put(CommunityAttributes.COMMUNITY_TERMS_OF_USE, termsOfUse);
					if (privacyState != null)
								hash.put(CommunityAttributes.COMMUNITY_TERMS_OF_USE, privacyState);
					try{							
						CommunityAttributes canew = CommunityUtils.createCommunity(hash);
						comm_id = canew.getID().toString();
						
						///set the new values for products
						Products product = new Products();
						List<ProductsMapper> productList 				= Products.getProductList();
												  
						//set the products
						   for (ProductsMapper prod : productList){	
							     HashMap<Integer,String> tmp  = new HashMap<Integer,String>();
							     String productName = mpp.getMultipartStringParameter(prod.getAlias(),"");
							     tmp.put(Products.FIELD_NAME, productName);
							     tmp.put(Products.FIELD_SHORT_NAME, mpp.getMultipartStringParameter(Products.PRODUCT_SHORT_ABBREV + prod.getAlias(),""));
							     
							     String tsrViewFilter = mpp.getMultipartStringParameter(Products.PRODUCT_TSR_VIEW_FROM_ABBREV + prod.getAlias(),"");
							     if(!(Products.TsrViewFilterOptions.DEF.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || Products.TsrViewFilterOptions.PTD.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || NumberUtils.isDigits(tsrViewFilter))) {
							    	 tsrViewFilter = "DEF";
							     }
							     tmp.put(Products.FIELD_TSR_VIEW_FILTER,tsrViewFilter );
							     
							     String searchFrom = mpp.getMultipartStringParameter(Products.PRODUCT_SEARCH_FROM_ABBREV + prod.getAlias(),"");
							     if (Products.isOneOfUpdateProductType(prod.getProductId())){//Update
								     if(!(Products.SearchFromOptions.DEF.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.LTD.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.PTD.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.CD.toString().equalsIgnoreCase(searchFrom)
								    	 || NumberUtils.isDigits(searchFrom))) {
								    	 searchFrom = "DEF";
								     }
								 } else {
									 if(!(Products.SearchFromOptions.DEF.toString().equalsIgnoreCase(searchFrom)
									    	 || Products.SearchFromOptions.LTD.toString().equalsIgnoreCase(searchFrom)
									    	 || Products.SearchFromOptions.PTD.toString().equalsIgnoreCase(searchFrom)
									    	 || NumberUtils.isDigits(searchFrom))) {
										 searchFrom = "DEF";
									 }
								 }
							     tmp.put(Products.FIELD_SEARCH_FROM,searchFrom );
							     product.setProduct(prod.getProductId(),tmp);
						   }
						   
					  //updates database
						   product.updateProductList(Long.parseLong(comm_id));
						
						
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_VIEW
							+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id
							+ "&" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMM_VIEW;						
						
					}catch(Exception e){
						String errMsg = e.getMessage();
						e.printStackTrace();
						if(errMsg.startsWith("SQLException:SQLException: ORA-00001: unique constraint"))
							ret= "The Community Already exist! Please Choose another name!\n";
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_ADD
								+ "?" + RequestParams.SEARCH_ID + "=" + searchId								
								+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADD_VIEW
								+ "&ret=" + ret + e.getMessage();
								
					}							

				}
				
			break;
			case TSOpCode.SAVE_COMMUNITY :
					name =mpp.getMultipartStringParameter(CommunityAttributes.COMMUNITY_NAME, "");
					if(name.equals("")){
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_ADD							
							+ "?" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADD_VIEW
							+ "&ret=Please Enter all Required Paramters";						
					}else{
						admin =
							mpp.getMultipartLongParameter(
								CommunityAttributes.COMMUNITY_ADMIN,
								-1);
						category =
						mpp.getMultipartLongParameter(
							CommunityAttributes.COMMUNITY_CATEGORY,
							-1);
						hash.put(CommunityAttributes.COMMUNITY_ID, comm_id);
						hash.put(CommunityAttributes.COMMUNITY_NAME, name);
						hash.put(
							CommunityAttributes.COMMUNITY_CATEGORY,
							new Long(category));
						hash.put(
							CommunityAttributes.COMMUNITY_DESCRIPTION,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_DESCRIPTION,
								"N/A"));
						hash.put(
							CommunityAttributes.COMMUNITY_SEE_ALSO,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_SEE_ALSO,
								"N/A"));
						hash.put(
							CommunityAttributes.COMMUNITY_ADMIN,
							new Long(admin));
						hash.put(
							CommunityAttributes.COMMUNITY_ADDRESS,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_ADDRESS,
								"N/A"));
						hash.put(
							CommunityAttributes.COMMUNITY_PHONE,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_PHONE,
								"N/A"));
						hash.put(
							CommunityAttributes.COMMUNITY_EMAIL,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_EMAIL,
								"N/A"));
						hash.put(
							CommunityAttributes.COMMUNITY_CONTACT,
							mpp.getMultipartStringParameter(
								CommunityAttributes.COMMUNITY_CONTACT,
								"N/A"));
						hash.put( CommunityAttributes.COMMUNITY_CTIME,
									new BigInteger("" + System.currentTimeMillis()));
						
						hash.put( CommunityAttributes.COMMUNITY_CODE,
						        mpp.getMultipartStringParameter(CommunityAttributes.COMMUNITY_CODE, "N/A"));
						
						hash.put( CommunityAttributes.COMMUNITY_OFFSET,
						        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_OFFSET, 0)));
						
						hash.put( CommunityAttributes.COMMUNITY_AUTOFILEID,
						        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_AUTOFILEID, 0)));
						
                        hash.put( CommunityAttributes.COMMUNITY_DEFAULT_SLA, 
                                new BigInteger("" + mpp.getMultipartLongParameter( CommunityAttributes.COMMUNITY_DEFAULT_SLA, 0 )));

                        hash.put(
    							CommunityAttributes.COMMUNITY_SUPPORT_EMAIL,
    							mpp.getMultipartStringParameter(
    								CommunityAttributes.COMMUNITY_SUPPORT_EMAIL,
    								MailConfig.getTicketSupportEmailAddress()));
                        
                        File commLogo=null, termsOfUse = null, privacySt = null;
						if(mpp!=null)
							commLogo= mpp.getFileParameter(CommunityAttributes.COMMUNITY_LOGO, null);
							termsOfUse= mpp.getFileParameter(CommunityAttributes.COMMUNITY_TERMS_OF_USE, null);
							privacySt= mpp.getFileParameter(CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT, null);
						if (commLogo != null) {
							hash.put(CommunityAttributes.COMMUNITY_LOGO, commLogo);
						}
						if (termsOfUse != null) {
							hash.put(CommunityAttributes.COMMUNITY_TERMS_OF_USE, termsOfUse);
						}
						if (privacySt != null) {
							hash.put(CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT, privacySt);
						}
					
						privacyStatement= mpp.getRequest().getParameterValues(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT);
						
						if(privacyStatement!=null && privacyStatement.length==1){
							hash.put(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT, Boolean.TRUE);
						}else{
							hash.put(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT, Boolean.FALSE);
						}
						
						hash.put(
								CommunityAttributes.COMMUNITY_PLACEHOLDER,
								mpp.getMultipartStringParameter(
									CommunityAttributes.COMMUNITY_PLACEHOLDER,
									""));
						
						hash.put( CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES,
						        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES, 0)));
						
						hash.put( CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS,
						        new Integer(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS, 0)));
						
						///set the new values for products
						Products product = new Products();
						List<ProductsMapper> productList 				= Products.getProductList();
						
						//set the products
						   for (ProductsMapper prod : productList){	
							     HashMap tmp  = new HashMap();
							     String productName = mpp.getMultipartStringParameter(prod.getAlias(),"");
							     tmp.put(Products.FIELD_NAME, productName);
							     tmp.put(Products.FIELD_SHORT_NAME, mpp.getMultipartStringParameter(Products.PRODUCT_SHORT_ABBREV + prod.getAlias(),""));
							     String tsrViewFilter = mpp.getMultipartStringParameter(Products.PRODUCT_TSR_VIEW_FROM_ABBREV + prod.getAlias(),"");
							     if(!(Products.TsrViewFilterOptions.DEF.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || Products.TsrViewFilterOptions.PTD.toString().equalsIgnoreCase(tsrViewFilter)
							    	 || NumberUtils.isDigits(tsrViewFilter))) {
							    	 tsrViewFilter = "DEF";
							     }
							     tmp.put(Products.FIELD_TSR_VIEW_FILTER,tsrViewFilter );
							     
							     String searchFrom = mpp.getMultipartStringParameter(Products.PRODUCT_SEARCH_FROM_ABBREV + prod.getAlias(),"");
							     if (Products.isOneOfUpdateProductType(prod.getProductId())){//Update
								     if(!(Products.SearchFromOptions.DEF.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.LTD.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.PTD.toString().equalsIgnoreCase(searchFrom)
								    	 || Products.SearchFromOptions.CD.toString().equalsIgnoreCase(searchFrom)
								    	 || NumberUtils.isDigits(searchFrom))) {
								    	 searchFrom = "DEF";
								     }
								 } else {
									 if(!(Products.SearchFromOptions.DEF.toString().equalsIgnoreCase(searchFrom)
									    	 || Products.SearchFromOptions.LTD.toString().equalsIgnoreCase(searchFrom)
									    	 || Products.SearchFromOptions.PTD.toString().equalsIgnoreCase(searchFrom)
									    	 || NumberUtils.isDigits(searchFrom))) {
										 searchFrom = "DEF";
									 }
								 }
							     tmp.put(Products.FIELD_SEARCH_FROM,searchFrom );
							     product.setProduct(prod.getProductId(),tmp);
						   }
						   
					  //updates database
						   product.updateProductList(Long.parseLong(comm_id));
						
						
						
					   try{
							CommunityUtils.modifyCommunity(hash);
							fowardlink= URLMaping.path + URLMaping.COMMUNITY_PAGE_VIEW
									+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id
									+ "&" + RequestParams.SEARCH_ID + "=" + searchId
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMM_VIEW;							
					   }
					   catch(Exception e){
						   fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_EDIT
								   + "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id
								   + "&" + RequestParams.SEARCH_ID + "=" + searchId
								   + "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_EDIT_VIEW
								   +  "&ret="+ e.getMessage();	
						   logger.error("Something happened while modifing community", e);
					   }

					   CommunityUtils.invalidateLogo(new BigDecimal(comm_id));
					   
				}
			break;
			case TSOpCode.COMM_DEL :
				try {
					CommunityAttributes ca = CommunityUtils.getCommunityFromId(Long.parseLong(comm_id));
					if(!ca.getNAME().equals(CommunityAttributes.DEFAULT_COMMUNITY)) {// the default community can't be deleted
						int nrofUsers = CommunityUtils.getNrUsers( ca);						 						
						if(nrofUsers<=1){
							CommunityUtils.delete(comm_id);
						}else
							throw new Exception("The community is not empty!");
						
						fowardlink = 
							URLMaping.path
								+ URLMaping.COMMUNITY_PAGE_ADMIN
								+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id
								+ "&" + RequestParams.SEARCH_ID + "=" + searchId
								+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADMIN_VIEW;
					}else{
					
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_VIEW
								+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id								
								+ "&"+ RequestParams.SEARCH_ID + "=" + searchId
								+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMM_VIEW
								+ "&ret=" + "The default community can't be deleted!";
					}
						
				} catch (Exception e) {
					//tratare eroare ca nu poate fi sters
					fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_VIEW
							+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + comm_id							
							+ "&" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMM_VIEW
							+ "&ret=" + e.getMessage();
							
				}
				break;
			case TSOpCode.DEL_CATEGORY :
					try{
						Hashtable hashd = pp.getSetParameter(TSParameters.CBXC);
						if(hashd.isEmpty()){
							ret = "Please select at list one group for deletion!";
							fowardlink = URLMaping.path + URLMaping.CATEGORY_PAGE_ADMIN
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId									
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADMIN_VIEW
									+ "&ret=" + ret;
						}else{
							Enumeration delcateg = hashd.keys();
							while(delcateg.hasMoreElements()){
								String categId = (String)delcateg.nextElement();
								int getNrComm = CategoryManager.getNrComm(Long.parseLong(categId));
								if(getNrComm==0){
									CategoryUtils.deleteCategory(categId);
								}else{
									throw new Exception("This Group is not empty!");
								}
																
							}
							fowardlink = URLMaping.path + URLMaping.CATEGORY_PAGE_ADMIN
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADMIN_VIEW;
							}
					
					} catch (Exception e2){
						fowardlink = URLMaping.path + URLMaping.CATEGORY_PAGE_ADMIN
								+ "?" + RequestParams.SEARCH_ID + "=" + searchId
								+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADMIN_VIEW
								+ "&ret=" + e2.getMessage();
					}
				break;
				case TSOpCode.SAVE_CATEGORY:
					String categName = pp.getStringParameter(CategoryAttributes.CATEGORY_NAME,"");

					
					try{
					    CategoryAttributes ca = new CategoryAttributes(categName);
						CategoryUtils.createCategory(ca);
						fowardlink = URLMaping.path + URLMaping.CATEGORY_PAGE_ADMIN
							+ "?"+ RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADMIN_VIEW;
						
					} catch (Exception e){
						String error= e.getMessage();
						String errRet="";
						if(error.startsWith("SQLException:SQLException: ORA-00001: unique constraint"))
							errRet= "The Group Already exist! Please Choose another name!\n";					
						
						fowardlink = URLMaping.path + URLMaping.CATEGORY_PAGE_ADD
							+ "?" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADD_VIEW
							+ "&ret=" + errRet;
						e.printStackTrace();
					}
				break;
				case TSOpCode.HIDE_COMMUNITY:
					try{
						Hashtable hashd = pp.getSetParameter(TSParameters.CBXC);
						if(hashd.isEmpty()){
							ret = "Please select at list one community for hiding!";
							fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId									
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.HIDE_COMMUNITY
									+ "&ret=" + ret;
						}else{
							Enumeration e = hashd.keys();
							StringBuilder commIds = new StringBuilder();
							while(e.hasMoreElements()){
								commIds.append((String)e.nextElement()).append(",");
							}
							commIds.deleteCharAt(commIds.length()-1);	//delete the last comma
							String commIdStr = commIds.toString();
							
							CommunityManager.hideCommunities(commIdStr);
							
							UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
							userManager.hideUsersFromCommunities(commIdStr);
							
							fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.HIDE_COMMUNITY;
							}
					} catch (Exception e){
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
							+ "?" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.HIDE_COMMUNITY
							+ "&ret=" + e.getMessage();
						e.printStackTrace();
					}
				break;	
				case TSOpCode.UNHIDE_COMMUNITY:
					try{
						Hashtable hashd = pp.getSetParameter(TSParameters.CBXC);
						if(hashd.isEmpty()){
							ret = "Please select at list one community for unhiding!";
							fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId									
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.UNHIDE_COMMUNITY
									+ "&ret=" + ret;
						}else{
							Enumeration e = hashd.keys();
							StringBuilder commIds = new StringBuilder();
							while(e.hasMoreElements()){
								commIds.append((String)e.nextElement()).append(",");
							}
							commIds.deleteCharAt(commIds.length()-1);	//delete the last comma
							String commIdStr = commIds.toString();
							
							CommunityManager.unhideCommunities(commIdStr);
							
							UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
							userManager.unhideUsersFromCommunities(commIdStr);
							
							fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
									+ "?" + RequestParams.SEARCH_ID + "=" + searchId
									+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.UNHIDE_COMMUNITY;
							}
					} catch (Exception e){
						fowardlink = URLMaping.path + URLMaping.COMMUNITY_PAGE_HIDE
							+ "?" + RequestParams.SEARCH_ID + "=" + searchId
							+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.UNHIDE_COMMUNITY
							+ "&ret=" + e.getMessage();
						e.printStackTrace();
					}
				break;
				
				}
			response.sendRedirect(fowardlink);	
		}
			
	}
