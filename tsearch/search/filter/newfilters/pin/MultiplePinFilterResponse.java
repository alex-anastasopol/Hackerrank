package ro.cst.tsearch.search.filter.newfilters.pin;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

public class MultiplePinFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 9101726265535465904L;
	
	public MultiplePinFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.9"));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void computeScores(Vector _rows){
		
		Vector<ParsedResponse> rows = (Vector<ParsedResponse>) _rows;
		
		// check that we have 2 or 3 hits
		if(rows.size() > 6 || rows.size() < 2){			
			super.computeScores(rows);
			return;
		}
		
		// check that all results have the PIS
		for(ParsedResponse row: rows){
			if(row.getPropertyIdentificationSetCount() != 1){				
				super.computeScores(rows);
				return;
			}
		}

		// identify reference data
		PropertyIdentificationSet pis = rows.get(0).getPropertyIdentificationSet(0);
		String refCity = pis.getAtribute("City");
		String refStrName = pis.getAtribute("StreetName");
		String refStrNo = pis.getAtribute("StreetNo");
		String refSfn = pis.getAtribute("SpouseFirstName");
		String refSmn = pis.getAtribute("SpouseMiddleName");
		String refSln = pis.getAtribute("SpouseLastName");
		String refOfn = pis.getAtribute("OwnerFirstName");
		String refOmn = pis.getAtribute("OwnerMiddleName");
		String refOln = pis.getAtribute("OwnerLastName");
		
		Collection<String> streetNames = new LinkedHashSet<String>();
		streetNames.add(refStrName);		
		
		// check that all have the same address and owners
		for(int i=1; i<rows.size(); i++){
			
			pis = rows.get(i).getPropertyIdentificationSet(0);
			String candCity = pis.getAtribute("City");
			String candStrName = pis.getAtribute("StreetName");
			String candStrNo = pis.getAtribute("StreetNo");
			String candSfn = pis.getAtribute("SpouseFirstName");
			String candSmn = pis.getAtribute("SpouseMiddleName");
			String candSln = pis.getAtribute("SpouseLastName");
			String candOfn = pis.getAtribute("OwnerFirstName");
			String candOmn = pis.getAtribute("OwnerMiddleName");
			String candOln = pis.getAtribute("OwnerLastName");
			
			streetNames.add(candStrName);			
			
			//B 4554
			boolean checkSpouse = (refSfn.equals(candSfn) &&  refSmn.equals(candSmn) && refSln.equals(candSln)) 
								|| ("".equals(candSfn) &&  "".equals(candSmn) && "".equals(candSln))
								|| ("".equals(refSfn) &&  "".equals(refSmn) && "".equals(refSln));
			boolean goOn = refCity.equals(candCity) /*&& refStrName.equals(candStrName)*/ && refStrNo.equals(candStrNo) && 
				checkSpouse &&
				refOfn.equals(candOfn) &&  refOmn.equals(candOmn) && refOln.equals(candOln);
			
			if(!goOn){				
				super.computeScores(rows);
				return;				
			}
		}
		
		// check street names for apt + garage
		if(streetNames.size() > 2){
			// more than two addresses: not a multiple PIN
			super.computeScores(rows);
			return;							
		} else if(streetNames.size() == 2){
			// exactly two addresses: check for apt + garage
			boolean hasApt = false;
			boolean hasGar = false;
			for(String strName: streetNames){
				if(strName.matches(".*#\\s*\\d+")){
					hasApt = true;
				} else if(strName.matches(".*#\\s*GU-\\d+")){
					hasGar = true;
				}
			}
			if(! hasApt || !hasGar){
				// not apt + garage: reject
				super.computeScores(rows);
				return;												
			}				
		}
		
		// mark that we have multiple PINs
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		search.setAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN, Boolean.TRUE);

		// log the info
		String pins = "";
		for(ParsedResponse row: rows){
			pins += row.getPropertyIdentificationSet(0).getAtribute("ParcelID") + "_"; 
		}
		pins = pins.replaceFirst("_$", "");
		SearchLogger.info("<br>Multiple PIN detected: <b>" + (pins.replace("_",", ")) + "</b></br>", searchId);
		
		// compute scores
		super.computeScores(rows);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row){
		return ATSDecimalNumberFormat.ONE;
	}
	
	@Override
    public String getFilterCriteria(){
		return "Multiple PIN for same property";
    }

}
