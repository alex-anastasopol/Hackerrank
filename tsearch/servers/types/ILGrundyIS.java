package ro.cst.tsearch.servers.types;

public class ILGrundyIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
	
	public ILGrundyIS(long searchId) {
		super(searchId);
	}

	public ILGrundyIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0511351002)";
	
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
				"<option value=\" No Selection \"> No Selection </option>" +
				"<option value=\"16345\">16345</option>" +
				"<option value=\"60407\">60407</option>" +
				"<option value=\"60408\">60408</option>" +
				"<option value=\"60410\">60410</option>" +
				"<option value=\"60416\">60416</option>" +
				"<option value=\"60420\">60420</option>" +
				"<option value=\"60424\">60424</option>" +
				"<option value=\"60437\">60437</option>" +
				"<option value=\"60444\">60444</option>" +
				"<option value=\"60447\">60447</option>" +
				"<option value=\"60450\">60450</option>" +
				"<option value=\"60470\">60470</option>" +
				"<option value=\"60474\">60474</option>" +
				"<option value=\"60479\">60479</option>" +
				"<option value=\"60481\">60481</option>" +
				"<option value=\"60527\">60527</option>" +
				"<option value=\"60540\">60540</option>" +
				"<option value=\"60541\">60541</option>" +
				"<option value=\"60603\">60603</option>" +
				"<option value=\"60661\">60661</option>" +
				"<option value=\"60961\">60961</option>" +
				"<option value=\"61360\">61360</option>" +
				"<option value=\"61604\">61604</option>" +
				"<option value=\"62794\">62794</option>" +
				"<option value=\"6O410\">6O410</option>" +
				"<option value=\"75206\">75206</option>" +
			"</select>";
		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"11\">HOM-DWEL20G/FARM-20E - 11             </option>" +
				"<option value=\"21\">FARM LAND 20E - 21             </option>" +
				"<option value=\"30\">RES VAC LOTS LAND - 30             </option>" +
				"<option value=\"32\">20G-4 RES VAC LAND - 32             </option>" +
				"<option value=\"40\">RES IMPROVED - 40             </option>" +
				"<option value=\"41\">MODEL HOME 10-25 - 41             </option>" +
				"<option value=\"4500\">RR - STATE ASSESSED - 4500           </option>" +
				"<option value=\"50\">COMMVAC LOTS-LAND - 50             </option>" +
				"<option value=\"5000\">RAILROAD - 5000           </option>" +
				"<option value=\"52\">20G-4 COMM VAC LOTS - 52             </option>" +
				"<option value=\"60\">COMM IMPROVED - 60             </option>" +
				"<option value=\"62\">20G-4 VAC COMM LOTS - 62             </option>" +
				"<option value=\"70\">COMM OFFICES - 70             </option>" +
				"<option value=\"7100\">COAL ASSESSMENTS 20K - 7100           </option>" +
				"<option value=\"72\">20G-4 VAC COMM LOTS - 72             </option>" +
				"<option value=\"7200\">OIL LEASES - 7200           </option>" +
				"<option value=\"7300\">MINERAL LIMESTONE - 7300           </option>" +
				"<option value=\"7400\">MINERAL SAND-GRAVEL - 7400           </option>" +
				"<option value=\"7500\">MINERAL FLUORSPAR - 7500           </option>" +
				"<option value=\"7600\">MINERAL MISC! - 7600           </option>" +
				"<option value=\"80\">INDUSTRIAL - 80             </option>" +
				"<option value=\"82\">20G-4 VAC IND LOTS - 82             </option>" +
				"<option value=\"90\">TAX EXEMPT PROPERTY - 90             </option>" +
			"</select>";
		
	}
}
