package ro.cst.tsearch.search.name;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

/**
 * BasicNameParser.
 *
 * @author catalinc
 */
public class BasicNameParser implements INameParser {

	protected static final Category logger= Category.getInstance(BasicNameParser.class.getName());

	/**
	 * Parsing schema for person name.
	 */
	String parseSchema = "LMF";
	
	/**
	 * Default constructor.
	 * parsing schema used is LFM
	 */
	public BasicNameParser() {
		parseSchema = "LFM";
	}

	/**
	 * @param schema Parsing schema used [LMF,FML etc]
	 */	
	public BasicNameParser(String schema) {
		parseSchema = schema;
	}

	/**
	 * @see ro.cst.tsearch.search.name.INameParser#parseNames(java.lang.String)
	 */
	public ArrayList parseNames(String input) {
		ArrayList l = new ArrayList(); 
		if(input.toUpperCase().trim().equalsIgnoreCase("N/A")) {
			return l;
		}
		String[] names = NameNormalizer.normalize(input).split(NameNormalizer.SPOUSE_DELIM);
		for (int i = 0; i < names.length; i++) {
			l.add(parseName(names[i].trim(), (i==0 ? null : (Name)l.get(i-1))));
		}		
		return l;
	}
	
	/**
	 * Parse one name.
	 */
	protected Name parseName(String s,Name prevName) {
		
		if(NameNormalizer.isCompanyName(s)) {
			return parseCompany(s);
		}
		
		String notParsed 	= "";
		String[] tok 		= s.split("\\s+");
		Name name 			= new Name();
		for (int i = 0; i < tok.length; i++) {
			if(NameNormalizer.isNamePrefix(tok[i])) {
				name.setNameElement(Name.NAME_PREFIX,tok[i]);
			} else if(NameNormalizer.isNameSuffix(tok[i])) {
				name.setNameElement(Name.NAME_SUFFIX,tok[i]);
			} else if(NameNormalizer.isNameDegree(tok[i])) {
				name.setNameElement(Name.NAME_DEGREE,tok[i]);
			} else {
				notParsed += tok[i] + " ";
			}
		}
	
		String[] tok2 = notParsed.trim().split("\\s");
		
		int fIndex,mIndex,lIndex;
		
		fIndex = parseSchema.indexOf("F");
		mIndex = parseSchema.indexOf("M");
		lIndex = parseSchema.indexOf("L");

		switch(tok2.length)
		{
			case 0: 
				break;
			case 1:
				if(prevName != null && !tok2[0].equals(prevName.getNameElement(Name.LAST_NAME))) {
					name.setNameElement(Name.LAST_NAME,prevName.getNameElement(Name.LAST_NAME));
					name.setNameElement(Name.FIRST_NAME,tok2[0]);
				} else {
					name.setNameElement(Name.LAST_NAME,tok2[0]);
				}
				break;
			case 2:
				if(prevName != null 
					&& !tok2[0].equals(prevName.getNameElement(Name.LAST_NAME)) 
					&& !tok2[1].equals(prevName.getNameElement(Name.LAST_NAME))
					&& !NameNormalizer.isLastName(tok2[0])) {
					
					fIndex = (fIndex >= 2 ? fIndex = 1 :  fIndex);
					mIndex = (mIndex >=2 ? mIndex = 1 : mIndex);
					if(mIndex == fIndex) {
						if(parseSchema.indexOf("M") > parseSchema.indexOf("F")) {
							fIndex--;
						} else {
							mIndex--;
						}
					}
					
					if(tok2[0].length() == 1) { mIndex = 0; fIndex = 1;}
					else if(tok2[1].length() == 1) { mIndex = 1; fIndex = 0; }
					
					name.setNameElement(Name.FIRST_NAME,tok2[fIndex]);
					name.setNameElement(Name.MIDDLE_NAME,tok2[mIndex]);
					name.setNameElement(Name.LAST_NAME,prevName.getNameElement(Name.LAST_NAME));
				} else {
					fIndex = (fIndex >= 2 ? 1 : fIndex);
					lIndex = (lIndex >=2 ? 1 : lIndex);
					if(prevName != null 
						&& (prevName.getNameElement(Name.LAST_NAME).startsWith(tok2[0])
						|| NameNormalizer.isLastName(tok2[0]))) {
						lIndex = 0;
						fIndex = 1;
					} else if(prevName != null 
						&& prevName.getNameElement(Name.LAST_NAME).startsWith(tok2[1])) {
						lIndex = 1;
						fIndex = 0;
					}
					
					if(NameNormalizer.isLastName(tok2[0])) {lIndex = 0; fIndex = 1;}
					
					name.setNameElement(Name.FIRST_NAME,tok2[fIndex]);
					name.setNameElement(Name.LAST_NAME,tok2[lIndex]);
				}
				break;
			default:
				
				if(prevName != null) {
					for(int i = 0; i < 3; i++) {
						if(prevName.getNameElement(Name.LAST_NAME).startsWith(tok2[i])
							&& tok2[i].length() >= 2) {
								tok2[i] = prevName.getNameElement(Name.LAST_NAME);
								if (lIndex != i){
									int tmpIndex = lIndex;
									lIndex = i;
									fIndex = (fIndex == i ? tmpIndex:fIndex);
									mIndex = (mIndex == i ? tmpIndex:mIndex);
								}
							}
					}
					for(int i = 0; i < 3; i++) {
						if(prevName.getNameElement(Name.FIRST_NAME).startsWith(tok2[i])
							&& tok2[i].length() >= 2) {
								//tok2[i] = prevName.getNameElement(Name.FIRST_NAME);
								if (fIndex != i){
									int tmpIndex = fIndex;
									fIndex = i;
									lIndex = (lIndex == i ? tmpIndex:lIndex);
									mIndex = (mIndex == i ? tmpIndex:mIndex);
								}
							}
					}
				}
						
				name.setNameElement(Name.FIRST_NAME,tok2[fIndex]);
				name.setNameElement(Name.MIDDLE_NAME,tok2[mIndex]);
				name.setNameElement(Name.LAST_NAME,tok2[lIndex]);
				if(tok2.length > 3) {
					String xx = "";
					for(int i=3; i < tok2.length; i++) {
						xx += tok2[i] + " ";
					}
					name.setNameElement(Name.MIDDLE_NAME,
							(name.getNameElement(Name.MIDDLE_NAME) + " " + xx).trim());					
				}				
		}

		for(int i = 0; i < 6; i++) {
			name.setNameElement(i,
				name.getNameElement(i).replaceAll(",",""));					
		}
		
		return name;		
	}		

	/**
	 * Parse company name. 
	 */
	protected Name parseCompany(String s) {		
		String notParsed 	= "";
		String[] tok 		= s.split("\\s+");
		Name name 			= new Name();
		for (int i = 0; i < tok.length; i++) {
			if(NameNormalizer.isCompanySuffix(tok[i])) {
				name.setNameElement(Name.NAME_SUFFIX,
					(name.getNameElement(Name.NAME_SUFFIX) + " " + tok[i]).trim());
			} else {
				notParsed += tok[i] + " ";
			}
		}		
		
		name.setNameElement(Name.LAST_NAME,notParsed.trim());
		name.setCompany(true);

		for(int i = 0; i < 6; i++) {
			name.setNameElement(i,
				name.getNameElement(i).replaceAll(",",""));					
		}
		
		return name;
	}

	/**
	 * @return Parsing schema.
	 */
	public String getParseSchema() {
		return parseSchema;
	}

	/**
	 * @param string Parsing schema.
	 */ 
	public void setParseSchema(String string) {
		parseSchema = string;
	}

	/**
	 * Test case for name parsing.
	 */
	public void testCase(String[] arr) {
		for (int i = 0; i < arr.length; i++) {
			logger.info("INPUT [" + arr[i] + "]");
			logger.info("NORMALIZED INPUT [" + NameNormalizer.normalize(arr[i]) + "]");
			logger.info("------- PARSED NAMES --------");
			List l = parseNames(arr[i]);
			for (Iterator iter = l.iterator(); iter.hasNext();) {
				Name n = (Name) iter.next();
				logger.info(n);
			}
			logger.info("");
		}
	}

	public static void main(String[] args) {
		(new BasicNameParser()).testCase(new String[] {
				"MISTER JOHN Smith III ETUX JOHN LIDIA FIRST",
				"THE HONORABLE JOHN M SMITH J.R."}
		);
	}
}
