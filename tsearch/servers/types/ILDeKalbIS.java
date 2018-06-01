package ro.cst.tsearch.servers.types;

  /**
   * @author mihaib
   *
  */

public class ILDeKalbIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
	
	public ILDeKalbIS(long searchId) {
		super(searchId);
	}

	public ILDeKalbIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0632276003)";
	
	
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
					"<option value=\"60002\">60002</option>" +
					"<option value=\"60111\">60111</option>" +
					"<option value=\"60112\">60112</option>" +
					"<option value=\"60113\">60113</option>" +
					"<option value=\"60115\">60115</option>" +
					"<option value=\"60119\">60119</option>" +
					"<option value=\"60123\">60123</option>" +
					"<option value=\"60129\">60129</option>" +
					"<option value=\"60130\">60130</option>" +
					"<option value=\"60135\">60135</option>" +
					"<option value=\"60140\">60140</option>" +
					"<option value=\"60145\">60145</option>" +
					"<option value=\"60146\">60146</option>" +
					"<option value=\"60150\">60150</option>" +
					"<option value=\"60151\">60151</option>" +
					"<option value=\"60152\">60152</option>" +
					"<option value=\"60175\">60175</option>" +
					"<option value=\"60178\">60178</option>" +
					"<option value=\"60421\">60421</option>" +
					"<option value=\"60505\">60505</option>" +
					"<option value=\"60510\">60510</option>" +
					"<option value=\"60511\">60511</option>" +
					"<option value=\"60518\">60518</option>" +
					"<option value=\"60520\">60520</option>" +
					"<option value=\"60530\">60530</option>" +
					"<option value=\"60531\">60531</option>" +
					"<option value=\"60540\">60540</option>" +
					"<option value=\"60545\">60545</option>" +
					"<option value=\"60548\">60548</option>" +
					"<option value=\"60550\">60550</option>" +
					"<option value=\"60552\">60552</option>" +
					"<option value=\"60556\">60556</option>" +
					"<option value=\"60563\">60563</option>" +
					"<option value=\"61045\">61045</option>" +
					"<option value=\"61052\">61052</option>" +
					"<option value=\"61068\">61068</option>" +
					"<option value=\"61353\">61353</option>" +
			"</select>";
		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"0011\">FARM WITH RESIDENCE AND/OR FAR - 0011           </option>" +
				"<option value=\"0021\">FARMLAND - 0021           </option>" +
				"<option value=\"0030\">VACANT RESIDENTIAL LOT - 0030           </option>" +
				"<option value=\"0032\">10-30 VACANT RESIDENTIAL LOT - 0032           </option>" +
				"<option value=\"0040\">IMPROVED RESIDENTIAL LOT - 0040           </option>" +
				"<option value=\"0041\">MODEL HOME 10-25 - 0041           </option>" +
				"<option value=\"0050\">VACANT COMMERCIAL LOTS - 0050           </option>" +
				"<option value=\"0052\">10-30 COMMERCIAL VACANT LAND - 0052           </option>" +
				"<option value=\"0060\">IMPROVED COMMERCIAL - 0060           </option>" +
				"<option value=\"0065\">COMMERCIAL WITH FARMLAND - 0065           </option>" +
				"<option value=\"0080\">INDUSTRIAL - 0080           </option>" +
				"<option value=\"0082\">10-30 INDUSTRIAL VACANT LAND - 0082           </option>" +
				"<option value=\"0085\">INDUSTRIAL WITH FARMLAND - 0085           </option>" +
				"<option value=\"0090\">TAX EXEMPT - 0090           </option>" +
				"<option value=\"4500\">STATE ASSESSED RAILROAD - 4500           </option>" +
				"<option value=\"4600\">POLLUTION CONTROL - 4600           </option>" +
				"<option value=\"5000\">LOCALLY ASSESSED RAILROAD - 5000           </option>" +
				"<option value=\"8000\">LEASEHOLD INTEREST - 8000           </option>" +
			"</select>";
		TOWNSHIP_SELECT =
			"<select tabindex=\"200\" id=\"ddlTownship\" name=\"ddlTownship\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"11\"> AFTON</option>" +
				"<option value=\"14\"> CLINTON</option>" +
				"<option value=\"09\"> CORTLAND</option>" +
				"<option value=\"08\"> DEKALB</option>" +
				"<option value=\"01\"> FRANKLIN</option>" +
				"<option value=\"03\"> GENOA</option>" +
				"<option value=\"02\"> KINGSTON</option>" +
				"<option value=\"07\"> MALTA</option>" +
				"<option value=\"05\"> MAYFIELD</option>" +
				"<option value=\"10\"> MILAN</option>" +
				"<option value=\"16\"> PAW PAW</option>" +
				"<option value=\"12\"> PIERCE</option>" +
				"<option value=\"19\"> SANDWICH</option>" +
				"<option value=\"13\"> SHABBONA</option>" +
				"<option value=\"18\"> SOMONAUK</option>" +
				"<option value=\"04\"> SOUTH GROVE</option>" +
				"<option value=\"15\"> SQUAW GROVE</option>" +
				"<option value=\"06\"> SYCAMORE</option>" +
				"<option value=\"17\"> VICTOR</option>" +
				"<option value=\"AU\">AURORA</option>" +
				"<option value=\"BA\">BATAVIA</option>" +
				"<option value=\"BR\">BIG ROCK</option>" +
				"<option value=\"BB\">BLACKBERRY</option>" +
				"<option value=\"BU\">BURLINGTON</option>" +
				"<option value=\"CA\">CAMPTON</option>" +
				"<option value=\"DU\">DUNDEE</option>" +
				"<option value=\"EL\">ELGIN</option>" +
				"<option value=\"GE\">GENEVA</option>" +
				"<option value=\"HA\">HAMPSHIRE</option>" +
				"<option value=\"KA\">KANEVILLE</option>" +
				"<option value=\"PL\">PLATO</option>" +
				"<option value=\"RU\">RUTLAND</option>" +
				"<option value=\"SC\">ST CHARLES</option>" +
				"<option value=\"SG\">SUGAR GROVE</option>" +
				"<option value=\"VI\">VIRGIL</option>" +
			"</select>";

	}
}
