package ro.cst.tsearch.generic.template;


//java
import java.util.*;

import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.reports.data.InvoiceData;
import ro.cst.tsearch.utils.RequestParams;


public class InvoiceTemplate extends BaseTemplate implements RequestParams {    
    
	InvoiceData data = null;
	                   
	public InvoiceData getData() {
		return data;
	}

	public void setData(InvoiceData data) {
		this.data = data;
	}
    /**
     *  get replacements 
     */   
	@Override
    public Hashtable<String, String> getReplacements() {

        Hashtable<String, String> ht = super.getReplacements();
        if (data==null) {
            return ht;
        }
        
		try {
			//ht.put(RequestParams.LOGO_PATH, data.getLogoFile());
			ht.put(RequestParams.COMM_NAME, data.getCommName());
			ht.put(RequestParams.COMM_ADDRESS, data.getCommAddress());
			ht.put(RequestParams.COMM_EMAIL, data.getCommEmail());
			ht.put(RequestParams.COMM_PHONE, data.getCommPhone());
			ht.put(RequestParams.AGENT_NAME, data.getAgentName());
			ht.put(RequestParams.AGENT_ADDRESS, data.getAgentAddress());
			ht.put(RequestParams.AGENT_EMAIL, data.getAgentEmail());
			ht.put(RequestParams.AGENT_PHONE, data.getAgentPhone());
			ht.put(RequestParams.START_INTERVAL, data.getStartInterval());
			ht.put(RequestParams.END_INTERVAL, data.getEndInterval());
			ht.put(RequestParams.INV_TIMESTAMP, data.getInvoiceTimestamp());
			
			ht.put(RequestParams.INV_DETAILS, data.getDetails());
			ht.put(RequestParams.TABLE_SUBTOTALS, data.getTableSubtotals());
			//ht.put(RequestParams.COST_TOTAL, data.getTotalCost());
			
			/*
			ht.put(RequestParams.FULL_SEARCHES_PRODUCT, 
					data.getProductTypeName(Products.FULL_SEARCH_PRODUCT, false));
			ht.put(RequestParams.CURRENTOWNER_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.CURRENT_OWNER_PRODUCT, false));
			ht.put(RequestParams.CONSTRUCTION_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.CONSTRUCTION_PRODUCT, false));
			ht.put(RequestParams.COMMERCIAL_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.COMMERCIAL_PRODUCT, false));
			ht.put(RequestParams.REFINANCE_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.REFINANCE_PRODUCT, false));
			ht.put(RequestParams.OE_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.OE_PRODUCT, false));
			ht.put(RequestParams.LIENS_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.LIENS_PRODUCT, false));
			ht.put(RequestParams.ACREAGE_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.ACREAGE_PRODUCT, false));
			ht.put(RequestParams.SUBLOT_SEARCH_PRODUCT, 
					data.getProductTypeName(Products.SUBLOT_PRODUCT, false));
			ht.put(RequestParams.SEARCH_UPDATES_PRODUCT, 
					data.getProductTypeName(Products.UPDATE_PRODUCT, false));
			ht.put(RequestParams.PRODUCT_INDEX, 
					data.getProductTypeName(Products.INDEX_PRODUCT, false));
			
			
			ht.put(RequestParams.SUBTOTAL_SEARCHES, data.getSubtotalSearches());
			ht.put(RequestParams.SUBTOTAL_CURRENTOWNER, data.getSubTotalCurrentOwner());
			ht.put(RequestParams.SUBTOTAL_CONSTRUCTION, data.getSubTotalConstruction());
			ht.put(RequestParams.SUBTOTAL_COMMERCIAL, data.getSubTotalCommercial());
			ht.put(RequestParams.SUBTOTAL_REFINANCE, data.getSubTotalRefinance());
			ht.put(RequestParams.SUBTOTAL_OE, data.getSubTotalOe());
			ht.put(RequestParams.SUBTOTAL_LIENS, data.getSubTotalLiens());
			ht.put(RequestParams.SUBTOTAL_ACREAGE, data.getSubTotalAcreage());
			ht.put(RequestParams.SUBTOTAL_SUBLOT, data.getSubTotalSublot());
			ht.put(RequestParams.SUBTOTAL_UPDATES, data.getSubtotalUpdates());
			
			
			
			ht.put(RequestParams.COST_TOTAL_SEARCHES, data.getTotalSearchesCost());
			ht.put(RequestParams.COST_TOTAL_CURRENTOWNER, data.getTotalCurrentOwnerCost());
			ht.put(RequestParams.COST_TOTAL_CONSTRUCTION, data.getTotalConstructionCost());
			ht.put(RequestParams.COST_TOTAL_COMMERCIAL, data.getTotalCommercialCost());
			ht.put(RequestParams.COST_TOTAL_REFINANCE, data.getTotalRefinanceCost());
			ht.put(RequestParams.COST_TOTAL_OE, data.getTotalOeCost());
			ht.put(RequestParams.COST_TOTAL_LIENS, data.getTotalLiensCost());
			ht.put(RequestParams.COST_TOTAL_ACREAGE, data.getTotalAcreageCost());
			ht.put(RequestParams.COST_TOTAL_SUBLOT, data.getTotalSublotCost());	
			ht.put(RequestParams.COST_TOTAL_UPDATES, data.getTotalUpdatesCost());
			
			
			
			
			
			ht.put(RequestParams.NR_SEARCHES_DISCOUNT, data.getNrSearchesDiscount());
			ht.put(RequestParams.NR_CURRENTOWNER_DISCOUNT, data.getNrCurrentOwnerDiscount());
			ht.put(RequestParams.NR_CONSTRUCTION_DISCOUNT, data.getNrConstructionDiscount());
			ht.put(RequestParams.NR_COMMERCIAL_DISCOUNT, data.getNrCommercialDiscount());
			ht.put(RequestParams.NR_REFINANCE_DISCOUNT, data.getNrRefinanceDiscount());
			ht.put(RequestParams.NR_OE_DISCOUNT, data.getNrOeDiscount());
			ht.put(RequestParams.NR_LIENS_DISCOUNT, data.getNrLiensDiscount());
			ht.put(RequestParams.NR_ACREAGE_DISCOUNT, data.getNrAcreageDiscount());
			ht.put(RequestParams.NR_SUBLOT_DISCOUNT, data.getNrSublotDiscount());
			ht.put(RequestParams.NR_UPDATES_DISCOUNT, data.getNrUpdatesDiscount());
			
			
			ht.put(RequestParams.COST_SEARCHES_DISCOUNT, data.getSearchCostDiscount());
			ht.put(RequestParams.COST_CURRENTOWNER_DISCOUNT, data.getCurrentOwnerCostDiscount());
			ht.put(RequestParams.COST_CONSTRUCTION_DISCOUNT, data.getConstructionCostDiscount());
			ht.put(RequestParams.COST_COMMERCIAL_DISCOUNT, data.getCommercialCostDiscount());
			ht.put(RequestParams.COST_REFINANCE_DISCOUNT, data.getRefinanceCostDiscount());
			ht.put(RequestParams.COST_OE_DISCOUNT, data.getOeCostDiscount());
			ht.put(RequestParams.COST_LIENS_DISCOUNT, data.getLiensCostDiscount());
			ht.put(RequestParams.COST_ACREAGE_DISCOUNT, data.getAcreageCostDiscount());
			ht.put(RequestParams.COST_SUBLOT_DISCOUNT, data.getSublotCostDiscount());						
			ht.put(RequestParams.COST_UPDATES_DISCOUNT, data.getUpdateCostDiscount());
			
			
			ht.put(RequestParams.SUBTOTAL_SEARCHES_DISCOUNT, data.getSubtotalSearchesDiscount());			
			ht.put(RequestParams.SUBTOTAL_CURRENTOWNER_DISCOUNT, data.getSubTotalCurrentOwnerDiscount());
			ht.put(RequestParams.SUBTOTAL_CONSTRUCTION_DISCOUNT, data.getSubTotalConstructionDiscount());
			ht.put(RequestParams.SUBTOTAL_COMMERCIAL_DISCOUNT, data.getSubTotalCommercialDiscount());
			ht.put(RequestParams.SUBTOTAL_REFINANCE_DISCOUNT, data.getSubTotalRefinanceDiscount());
			ht.put(RequestParams.SUBTOTAL_OE_DISCOUNT, data.getSubTotalOeDiscount());
			ht.put(RequestParams.SUBTOTAL_ACREAGE_DISCOUNT, data.getSubTotalAcreageDiscount());
			ht.put(RequestParams.SUBTOTAL_LIENS_DISCOUNT, data.getSubTotalLiensDiscount());
			ht.put(RequestParams.SUBTOTAL_SUBLOT_DISCOUNT, data.getSubTotalSublotDiscount());			
			ht.put(RequestParams.SUBTOTAL_UPDATES_DISCOUNT, data.getSubtotalUpdatesDiscount());
			
			
			
			
			ht.put(RequestParams.COST_TOTAL_SEARCHES_DISCOUNT, data.getTotalSearchesCostDiscount());
			ht.put(RequestParams.COST_TOTAL_CURRENTOWNER_DISCOUNT, data.getTotalCurrentOwnerCostDiscount());
			ht.put(RequestParams.COST_TOTAL_CONSTRUCTION_DISCOUNT, data.getTotalConstructionCostDiscount());
			ht.put(RequestParams.COST_TOTAL_COMMERCIAL_DISCOUNT, data.getTotalCommercialCostDiscount());
			ht.put(RequestParams.COST_TOTAL_REFINANCE_DISCOUNT, data.getTotalRefinanceCostDiscount());
			ht.put(RequestParams.COST_TOTAL_OE_DISCOUNT, data.getTotalOeCostDiscount());
			ht.put(RequestParams.COST_TOTAL_LIENS_DISCOUNT, data.getTotalLiensCostDiscount());
			ht.put(RequestParams.COST_TOTAL_ACREAGE_DISCOUNT, data.getTotalAcreageCostDiscount());
			ht.put(RequestParams.COST_TOTAL_SUBLOT_DISCOUNT, data.getTotalSublotCostDiscount());			
			ht.put(RequestParams.COST_TOTAL_UPDATES_DISCOUNT, data.getTotalUpdatesCostDiscount());
			
			
			ht.put(RequestParams.FULL_SEARCHES_PRODUCT_DISCOUNT, data.getProductTypeName(Products.FULL_SEARCH_PRODUCT, true));
			ht.put(RequestParams.CURRENTOWNER_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.CURRENT_OWNER_PRODUCT, true));
			ht.put(RequestParams.CONSTRUCTION_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.CONSTRUCTION_PRODUCT, true));
			ht.put(RequestParams.COMMERCIAL_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.COMMERCIAL_PRODUCT, true));
			ht.put(RequestParams.REFINANCE_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.REFINANCE_PRODUCT, true));
			ht.put(RequestParams.OE_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.OE_PRODUCT, true));
			ht.put(RequestParams.LIENS_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.LIENS_PRODUCT, true));
			ht.put(RequestParams.ACREAGE_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.ACREAGE_PRODUCT, true));
			ht.put(RequestParams.SUBLOT_SEARCH_PRODUCT_DISCOUNT, data.getProductTypeName(Products.SUBLOT_PRODUCT, true));
			ht.put(RequestParams.SEARCH_UPDATES_PRODUCT_DISCOUNT, data.getProductTypeName(Products.UPDATE_PRODUCT, true));
			ht.put(RequestParams.PRODUCT_INDEX_DISCOUNT, data.getProductTypeName(Products.INDEX_PRODUCT, true));
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}                            
        
        return ht;
    }

}        
