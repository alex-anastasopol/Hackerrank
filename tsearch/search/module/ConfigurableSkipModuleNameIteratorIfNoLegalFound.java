package ro.cst.tsearch.search.module;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class ConfigurableSkipModuleNameIteratorIfNoLegalFound extends
		ConfigurableNameIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] skipModuleIfSourceTypesAvailable = null;

	public ConfigurableSkipModuleNameIteratorIfNoLegalFound(long searchId,
			String[] derivPatern, String ... skipModuleIfSourceTypesAvailable) {
		super(searchId,derivPatern);
		this.skipModuleIfSourceTypesAvailable = skipModuleIfSourceTypesAvailable;
		setInitAgain(true);
	}
	
	@Override
	public void init(TSServerInfoModule initial) {
		Search search = getSearch();
		DocumentsManagerI managerI = search.getDocManager();
		try {
			managerI.getAccess();
			boolean hasLegal = false;
			boolean hasTownship = false;
			if(managerI.getDocumentsWithDataSource(false, skipModuleIfSourceTypesAvailable).size() > 0) {
				List<RegisterDocumentI> listRodocs = managerI.getRoLikeDocumentList(false);
				DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
				for( RegisterDocumentI reg: listRodocs){
					if( reg.isOneOf("TRANSFER", "MORTGAGE")){
						for (PropertyI prop: reg.getProperties()){
							if(prop.hasLegal()){
								LegalI legal = prop.getLegal();
								if(legal.hasSubdividedLegal()){
									SubdivisionI subdiv = legal.getSubdivision();
									if (StringUtils.isNotEmpty(subdiv.getName())){
										hasLegal = true;
										break;
									}
								}
							}
						}
					}
				}
				if (!hasLegal){
					for( RegisterDocumentI reg: listRodocs){
						if( reg.isOneOf("TRANSFER", "MORTGAGE")){
							for (PropertyI prop: reg.getProperties()){
								if(prop.hasLegal()){
									LegalI legal = prop.getLegal();
									if(legal.hasTownshipLegal()){
										TownShipI township = legal.getTownShip();
										if (StringUtils.isNotEmpty(township.getTownship())){
											hasTownship = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			if (!hasLegal && !hasTownship){
				super.init(initial);
			} else {
				Set<NameI> derivNames = new LinkedHashSet<NameI>();
				StatesIterator si = new DefaultStatesIterator(derivNames);
				setStrategy(si);
			}
		} catch (Throwable t) {
			logger.error("Error while initializing ConfigurableSkipModuleNameIteratorIfNoLegalFound!", t);
		} finally {
			if(managerI != null) {
				managerI.releaseAccess();
			}
		}
		
	}

}
