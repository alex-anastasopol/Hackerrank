package ro.cst.tsearch.servers.types;

public class ILBooneIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
	
	public ILBooneIS(long searchId) {
		super(searchId);
	}

	public ILBooneIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0327127033)";
	
	
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
				"<option value=\" No Selection \"> No Selection </option>" +
				"<option value=\"60014\">60014</option>" +
				"<option value=\"60033\">60033</option>" +
				"<option value=\"60135\">60135</option>" +
				"<option value=\"60138\">60138</option>" +
				"<option value=\"60145\">60145</option>" +
				"<option value=\"60146\">60146</option>" +
				"<option value=\"60152\">60152</option>" +
				"<option value=\"60180\">60180</option>" +
				"<option value=\"61008\">61008</option>" +
				"<option value=\"61011\">61011</option>" +
				"<option value=\"61012\">61012</option>" +
				"<option value=\"61016\">61016</option>" +
				"<option value=\"61038\">61038</option>" +
				"<option value=\"61046\">61046</option>" +
				"<option value=\"61065\">61065</option>" +
				"<option value=\"61073\">61073</option>" +
				"<option value=\"61080\">61080</option>" +
				"<option value=\"61101\">61101</option>" +
				"<option value=\"61103\">61103</option>" +
				"<option value=\"61107\">61107</option>" +
				"<option value=\"61108\">61108</option>" +
				"<option value=\"61109\">61109</option>" +
				"<option value=\"61111\">61111</option>" +
				"<option value=\"61114\">61114</option>" +
				"<option value=\"61356\">61356</option>" +
				"<option value=\"62038\">62038</option>" +
			"</select>";
		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"11\">RURAL IMPROVED W/BLDG - 11             </option>" +
				"<option value=\"21\">RURAL NOT IMPROVED - 21             </option>" +
				"<option value=\"30\">LOTS NOT IMPROVED - 30             </option>" +
				"<option value=\"32\">RES VACANT LAND - 32             </option>" +
				"<option value=\"40\">LOTS IMPROVED - 40             </option>" +
				"<option value=\"41\">MODEL HOMES - 41             </option>" +
				"<option value=\"50\">COMMERCIAL RESIDENTIAL 6 UNIT - 50             </option>" +
				"<option value=\"52\">COMMERCIAL RESIDENTIAL 6 UNIT 10/30 - 52             </option>" +
				"<option value=\"60\">COMMERCIAL BUSINESS - 60             </option>" +
				"<option value=\"62\">COMMERCIAL VACANT LAND - 62             </option>" +
				"<option value=\"70\">COMMERCIAL OFFICE - 70             </option>" +
				"<option value=\"72\">DEVELOPER COMMERCIAL VACANT LAND 10/30 - 72             </option>" +
				"<option value=\"80\">INDUSTRIAL PROPERTY - 80             </option>" +
				"<option value=\"82\">DEVELOPER INDUSTRIAL VACANT LAND 10/30 - 82             </option>" +
				"<option value=\"90\">TAX EXEMPT - 90             </option>" +
			"</select>";
		TOWNSHIP_SELECT =
			"<select tabindex=\"200\" id=\"ddlTownship\" name=\"ddlTownship\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"BE\">BELVIDERE</option>" +
				"<option value=\"BN\">BONUS</option>" +
				"<option value=\"BO\">BOONE</option>" +
				"<option value=\"CA\">CALEDONIA</option>" +
				"<option value=\"FL\">FLORA</option>" +
				"<option value=\"LE\">LEROY</option>" +
				"<option value=\"MA\">MANCHESTER</option>" +
				"<option value=\"PG\">POPLAR GROVE</option>" +
				"<option value=\"SP\">SPRING</option>" +
			"</select>";

	}
}
