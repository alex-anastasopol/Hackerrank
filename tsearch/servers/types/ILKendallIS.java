package ro.cst.tsearch.servers.types;

public class ILKendallIS extends GenericISI {
	
	
	public static final long serialVersionUID = 10000000L;
	
	public ILKendallIS(long searchId) {
		super(searchId);
	}

	public ILKendallIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0121230029)";
	
	
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
				"<option value=\" No Selection \"> No Selection </option>" +
				"<option value=\"06038\">06038</option>" +
				"<option value=\"60008\">60008</option>" +
				"<option value=\"60014\">60014</option>" +
				"<option value=\"60048\">60048</option>" +
				"<option value=\"60056\">60056</option>" +
				"<option value=\"60081\">60081</option>" +
				"<option value=\"60108\">60108</option>" +
				"<option value=\"60115\">60115</option>" +
				"<option value=\"60118\">60118</option>" +
				"<option value=\"60119\">60119</option>" +
				"<option value=\"60123\">60123</option>" +
				"<option value=\"60134\">60134</option>" +
				"<option value=\"60142\">60142</option>" +
				"<option value=\"60148\">60148</option>" +
				"<option value=\"60172\">60172</option>" +
				"<option value=\"60174\">60174</option>" +
				"<option value=\"60185\">60185</option>" +
				"<option value=\"60187\">60187</option>" +
				"<option value=\"60195\">60195</option>" +
				"<option value=\"60403\">60403</option>" +
				"<option value=\"60420\">60420</option>" +
				"<option value=\"60427\">60427</option>" +
				"<option value=\"60431\">60431</option>" +
				"<option value=\"60432\">60432</option>" +
				"<option value=\"60435\">60435</option>" +
				"<option value=\"60438\">60438</option>" +
				"<option value=\"60441\">60441</option>" +
				"<option value=\"60447\">60447</option>" +
				"<option value=\"60448\">60448</option>" +
				"<option value=\"60450\">60450</option>" +
				"<option value=\"60454\">60454</option>" +
				"<option value=\"60457\">60457</option>" +
				"<option value=\"60491\">60491</option>" +
				"<option value=\"60503\">60503</option>" +
				"<option value=\"60504\">60504</option>" +
				"<option value=\"60505\">60505</option>" +
				"<option value=\"60506\">60506</option>" +
				"<option value=\"60510\">60510</option>" +
				"<option value=\"60511\">60511</option>" +
				"<option value=\"60512\">60512</option>" +
				"<option value=\"60517\">60517</option>" +
				"<option value=\"60520\">60520</option>" +
				"<option value=\"60521\">60521</option>" +
				"<option value=\"60522\">60522</option>" +
				"<option value=\"60523\">60523</option>" +
				"<option value=\"60527\">60527</option>" +
				"<option value=\"60534\">60534</option>" +
				"<option value=\"60536\">60536</option>" +
				"<option value=\"60537\">60537</option>" +
				"<option value=\"60538\">60538</option>" +
				"<option value=\"60541\">60541</option>" +
				"<option value=\"60542\">60542</option>" +
				"<option value=\"60543\">60543</option>" +
				"<option value=\"60544\">60544</option>" +
				"<option value=\"60545\">60545</option>" +
				"<option value=\"60548\">60548</option>" +
				"<option value=\"60551\">60551</option>" +
				"<option value=\"60554\">60554</option>" +
				"<option value=\"60555\">60555</option>" +
				"<option value=\"60560\">60560</option>" +
				"<option value=\"60564\">60564</option>" +
				"<option value=\"60565\">60565</option>" +
				"<option value=\"6056O\">6056O</option>" +
				"<option value=\"60580\">60580</option>" +
				"<option value=\"60585\">60585</option>" +
				"<option value=\"60586\">60586</option>" +
				"<option value=\"60603\">60603</option>" +
				"<option value=\"60610\">60610</option>" +
				"<option value=\"60646\">60646</option>" +
				"<option value=\"60690\">60690</option>" +
				"<option value=\"61001\">61001</option>" +		
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
				"<option value=\"7600\">MINERAL MISC - 7600           </option>" +
				"<option value=\"80\">INDUSTRIAL - 80             </option>" +
				"<option value=\"82\">20G-4 VAC IND LOTS - 82             </option>" +
				"<option value=\"90\">TAX EXEMPT PROPERTY - 90             </option>" +		
			"</select>";
		TOWNSHIP_SELECT =
			"<select tabindex=\"200\" id=\"ddlTownship\" name=\"ddlTownship\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"BG\">BIG GROVE</option>" +
				"<option value=\"BR\">BRISTOL</option>" +
				"<option value=\"FX\">FOX</option>" +
				"<option value=\"KE\">KENDALL</option>" +
				"<option value=\"LI\">LISBON</option>" +
				"<option value=\"LR\">LITTLE ROCK</option>" +
				"<option value=\"NS\">NA-AU-SAY</option>" +
				"<option value=\"OS\">OSWEGO</option>" +
				"<option value=\"SE\">SEWARD</option>" +
			"</select>";

	}
}
