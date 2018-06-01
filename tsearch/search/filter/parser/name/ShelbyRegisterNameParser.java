/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.parser.name;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ShelbyRegisterNameParser extends NameParser {
	private boolean lastFirst = false;



	protected static final Category logger = Category.getInstance(ShelbyRegisterNameParser.class.getName());
	
	
	public ShelbyRegisterNameParser (){
		delimitsSeveralNames = new String[]{
									"\\bAND\\b", "," 
									};
		delimitsOwnerSpouseNames = new String[]{"&"};
										
		abbreviations = new String[]{
									"ETUX",
									"ETX",
									"ETUXX",
									"ETVIR",
									"ETAL"
											
		};
	}

	protected void hookPosition(int i) {
		if (i==0){
			lastFirst = true;
		}else{
			lastFirst = false;
		}
	}



	public NameTokenList parseOwnerName(String input){
		NameTokenList owner ;
		if (lastFirst){
			owner = NameParser.parseNameLFM(input);
		}else{
			owner = NameParser.parseNameFML(input);
		}
		return owner;
	}

}
