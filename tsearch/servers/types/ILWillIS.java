package ro.cst.tsearch.servers.types;


public class ILWillIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
		
	public ILWillIS(long searchId) {
		super(searchId);
	}

	public ILWillIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0410094050270000)";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
				"<option value=\" No Selection \"> No Selection </option>" +
				"<option value=\"00000\">00000</option>" +
				"<option value=\"46303\">46303</option>" +
				"<option value=\"60025\">60025</option>" +
				"<option value=\"60030\">60030</option>" +
				"<option value=\"60041\">60041</option>" +
				"<option value=\"60042\">60042</option>" +
				"<option value=\"60045\">60045</option>" +
				"<option value=\"60046\">60046</option>" +
				"<option value=\"60047\">60047</option>" +
				"<option value=\"60050\">60050</option>" +
				"<option value=\"60060\">60060</option>" +
				"<option value=\"60061\">60061</option>" +
				"<option value=\"60062\">60062</option>" +
				"<option value=\"60068\">60068</option>" +
				"<option value=\"60073\">60073</option>" +
				"<option value=\"60076\">60076</option>" +
				"<option value=\"60077\">60077</option>" +
				"<option value=\"60081\">60081</option>" +
				"<option value=\"60084\">60084</option>" +
				"<option value=\"60089\">60089</option>" +
				"<option value=\"60098\">60098</option>" +
				"<option value=\"60101\">60101</option>" +
				"<option value=\"60102\">60102</option>" +
				"<option value=\"60103\">60103</option>" +
				"<option value=\"60106\">60106</option>" +
				"<option value=\"60107\">60107</option>" +
				"<option value=\"60108\">60108</option>" +
				"<option value=\"60110\">60110</option>" +
				"<option value=\"60115\">60115</option>" +
				"<option value=\"60119\">60119</option>" +
				"<option value=\"60123\">60123</option>" +
				"<option value=\"60124\">60124</option>" +
				"<option value=\"60126\">60126</option>" +
				"<option value=\"60131\">60131</option>" +
				"<option value=\"60133\">60133</option>" +
				"<option value=\"60134\">60134</option>" +
				"<option value=\"60136\">60136</option>" +
				"<option value=\"60137\">60137</option>" +
				"<option value=\"60139\">60139</option>" +
				"<option value=\"60142\">60142</option>" +
				"<option value=\"60148\">60148</option>" +
				"<option value=\"60154\">60154</option>" +
				"<option value=\"60156\">60156</option>" +
				"<option value=\"60157\">60157</option>" +
				"<option value=\"60164\">60164</option>" +
				"<option value=\"60172\">60172</option>" +
				"<option value=\"60174\">60174</option>" +
				"<option value=\"60175\">60175</option>" +
				"<option value=\"60178\">60178</option>" +
				"<option value=\"60185\">60185</option>" +
				"<option value=\"60187\">60187</option>" +
				"<option value=\"60188\">60188</option>" +
				"<option value=\"60189\">60189</option>" +
				"<option value=\"60190\">60190</option>" +
				"<option value=\"60191\">60191</option>" +
				"<option value=\"60194\">60194</option>" +
				"<option value=\"60301\">60301</option>" +
				"<option value=\"60304\">60304</option>" +
				"<option value=\"60400\">60400</option>" +
				"<option value=\"60401\">60401</option>" +
				"<option value=\"60402\">60402</option>" +
				"<option value=\"60403\">60403</option>" +
				"<option value=\"60404\">60404</option>" +
				"<option value=\"60406\">60406</option>" +
				"<option value=\"60407\">60407</option>" +
				"<option value=\"60408\">60408</option>" +
				"<option value=\"60409\">60409</option>" +
				"<option value=\"60410\">60410</option>" +
				"<option value=\"60411\">60411</option>" +
				"<option value=\"60415\">60415</option>" +
				"<option value=\"60416\">60416</option>" +
				"<option value=\"60417\">60417</option>" +
				"<option value=\"60418\">60418</option>" +
				"<option value=\"60421\">60421</option>" +
				"<option value=\"60423\">60423</option>" +
				"<option value=\"60430\">60430</option>" +
				"<option value=\"60431\">60431</option>" +
				"<option value=\"60432\">60432</option>" +
				"<option value=\"60433\">60433</option>" +
				"<option value=\"60434\">60434</option>" +
				"<option value=\"60435\">60435</option>" +
				"<option value=\"60436\">60436</option>" +
				"<option value=\"60437\">60437</option>" +
				"<option value=\"60438\">60438</option>" +
				"<option value=\"60439\">60439</option>" +
				"<option value=\"60440\">60440</option>" +
				"<option value=\"60441\">60441</option>" +
				"<option value=\"60442\">60442</option>" +
				"<option value=\"60443\">60443</option>" +
				"<option value=\"60444\">60444</option>" +
				"<option value=\"60445\">60445</option>" +
				"<option value=\"60446\">60446</option>" +
				"<option value=\"60447\">60447</option>" +
				"<option value=\"60448\">60448</option>" +
				"<option value=\"60449\">60449</option>" +
				"<option value=\"60450\">60450</option>" +
				"<option value=\"60451\">60451</option>" +
				"<option value=\"60452\">60452</option>" +
				"<option value=\"60453\">60453</option>" +
				"<option value=\"60455\">60455</option>" +
				"<option value=\"60457\">60457</option>" +
				"<option value=\"60458\">60458</option>" +
				"<option value=\"60459\">60459</option>" +
				"<option value=\"60460\">60460</option>" +
				"<option value=\"60461\">60461</option>" +
				"<option value=\"60462\">60462</option>" +
				"<option value=\"60463\">60463</option>" +
				"<option value=\"60464\">60464</option>" +
				"<option value=\"60465\">60465</option>" +
				"<option value=\"60466\">60466</option>" +
				"<option value=\"60467\">60467</option>" +
				"<option value=\"60468\">60468</option>" +
				"<option value=\"60471\">60471</option>" +
				"<option value=\"60474\">60474</option>" +
				"<option value=\"60475\">60475</option>" +
				"<option value=\"60477\">60477</option>" +
				"<option value=\"60480\">60480</option>" +
				"<option value=\"60481\">60481</option>" +
				"<option value=\"60482\">60482</option>" +
				"<option value=\"60484\">60484</option>" +
				"<option value=\"60485\">60485</option>" +
				"<option value=\"60487\">60487</option>" +
				"<option value=\"60490\">60490</option>" +
				"<option value=\"60491\">60491</option>" +
				"<option value=\"60495\">60495</option>" +
				"<option value=\"60501\">60501</option>" +
				"<option value=\"60502\">60502</option>" +
				"<option value=\"60503\">60503</option>" +
				"<option value=\"60504\">60504</option>" +
				"<option value=\"60505\">60505</option>" +
				"<option value=\"60506\">60506</option>" +
				"<option value=\"60507\">60507</option>" +
				"<option value=\"60510\">60510</option>" +
				"<option value=\"60513\">60513</option>" +
				"<option value=\"60515\">60515</option>" +
				"<option value=\"60516\">60516</option>" +
				"<option value=\"60517\">60517</option>" +
				"<option value=\"60521\">60521</option>" +
				"<option value=\"60523\">60523</option>" +
				"<option value=\"60525\">60525</option>" +
				"<option value=\"60532\">60532</option>" +
				"<option value=\"60534\">60534</option>" +
				"<option value=\"60537\">60537</option>" +
				"<option value=\"60538\">60538</option>" +
				"<option value=\"60540\">60540</option>" +
				"<option value=\"60542\">60542</option>" +
				"<option value=\"60543\">60543</option>" +
				"<option value=\"60544\">60544</option>" +
				"<option value=\"60545\">60545</option>" +
				"<option value=\"60548\">60548</option>" +
				"<option value=\"60554\">60554</option>" +
				"<option value=\"60555\">60555</option>" +
				"<option value=\"60558\">60558</option>" +
				"<option value=\"60559\">60559</option>" +
				"<option value=\"60560\">60560</option>" +
				"<option value=\"60561\">60561</option>" +
				"<option value=\"60563\">60563</option>" +
				"<option value=\"60564\">60564</option>" +
				"<option value=\"60565\">60565</option>" +
				"<option value=\"60566\">60566</option>" +
				"<option value=\"60567\">60567</option>" +
				"<option value=\"60575\">60575</option>" +
				"<option value=\"60584\">60584</option>" +
				"<option value=\"60585\">60585</option>" +
				"<option value=\"60586\">60586</option>" +
				"<option value=\"60591\">60591</option>" +
				"<option value=\"60601\">60601</option>" +
				"<option value=\"60606\">60606</option>" +
				"<option value=\"60607\">60607</option>" +
				"<option value=\"60612\">60612</option>" +
				"<option value=\"60613\">60613</option>" +
				"<option value=\"60617\">60617</option>" +
				"<option value=\"60618\">60618</option>" +
				"<option value=\"60619\">60619</option>" +
				"<option value=\"60621\">60621</option>" +
				"<option value=\"60624\">60624</option>" +
				"<option value=\"60625\">60625</option>" +
				"<option value=\"60626\">60626</option>" +
				"<option value=\"60629\">60629</option>" +
				"<option value=\"60630\">60630</option>" +
				"<option value=\"60632\">60632</option>" +
				"<option value=\"60634\">60634</option>" +
				"<option value=\"60636\">60636</option>" +
				"<option value=\"60638\">60638</option>" +
				"<option value=\"60639\">60639</option>" +
				"<option value=\"60640\">60640</option>" +
				"<option value=\"60641\">60641</option>" +
				"<option value=\"60642\">60642</option>" +
				"<option value=\"60643\">60643</option>" +
				"<option value=\"60644\">60644</option>" +
				"<option value=\"60645\">60645</option>" +
				"<option value=\"60647\">60647</option>" +
				"<option value=\"60649\">60649</option>" +
				"<option value=\"60651\">60651</option>" +
				"<option value=\"60652\">60652</option>" +
				"<option value=\"60659\">60659</option>" +
				"<option value=\"60803\">60803</option>" +
				"<option value=\"60901\">60901</option>" +
				"<option value=\"60914\">60914</option>" +
				"<option value=\"60915\">60915</option>" +
				"<option value=\"60935\">60935</option>" +
				"<option value=\"60940\">60940</option>" +
				"<option value=\"60950\">60950</option>" +
				"<option value=\"61764\">61764</option>" +
				"<option value=\"63101\">63101</option>" +
			"</select>";

		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"C\">COMMERCIAL - C              </option>" +
				"<option value=\"E\">EXEMPT PROPERTY - E              </option>" +
				"<option value=\"F\">FARM - F              </option>" +
				"<option value=\"G\">RESIDENTIAL DEVELOPER RELIEF - G              </option>" +
				"<option value=\"H\">COMMERCIAL DEVELOPERS RELIEF - H              </option>" +
				"<option value=\"I\">INDUSTRIAL - I              </option>" +
				"<option value=\"J\">INDUSTRIAL DEVELOPERS RELIEF - J              </option>" +
				"<option value=\"K\">INDUSTRIAL LAND-FARM LEASED - K              </option>" +
				"<option value=\"M\">INDUSTRIAL RECREATIONAL LAND - M              </option>" +
				"<option value=\"R\">RESIDENTIAL - R              </option>" +
				"<option value=\"U\">MINERAL RIGHTS - U              </option>" +
				"<option value=\"X\">COMMERCIAL RECREATIONAL LAND - X              </option>" +
				"<option value=\"Z\">COMMERCIAL LAND-FARM LEASED - Z              </option>" +
			"</select>"
			;
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"623            \">HS 111 MINOOKA</option>" +
				"<option value=\"635            \">HS 204 JOLIET</option>" +
				"<option value=\"636            \">HS 205 LOCKPORT</option>" +
				"<option value=\"640            \">HS 210 LINCOLN-WAY</option>" +
				"<option value=\"624            \">SD 114 MANHATTAN</option>" +
				"<option value=\"625            \">SD 122 NEW LENOX</option>" +
				"<option value=\"626            \">SD 157-C FRANKFORT</option>" +
				"<option value=\"627            \">SD 159 MOKENA</option>" +
				"<option value=\"628            \">SD 161 SUMMIT HILL</option>" +
				"<option value=\"604            \">SD 17 CHANNAHON</option>" +
				"<option value=\"629            \">SD 194 STEGER</option>" +
				"<option value=\"631            \">SD 201-C MINOOKA</option>" +
				"<option value=\"634            \">SD 203 ELWOOD</option>" +
				"<option value=\"605            \">SD 30-C TROY</option>" +
				"<option value=\"606            \">SD 33-C HOMER</option>" +
				"<option value=\"610            \">SD 70-C LARAWAY</option>" +
				"<option value=\"612            \">SD 81 UNION</option>" +
				"<option value=\"613            \">SD 84 ROCKDALE</option>" +
				"<option value=\"614            \">SD 86 JOLIET</option>" +
				"<option value=\"615            \">SD 88 CHANEY-MONGE</option>" +
				"<option value=\"616            \">SD 88-A RICHLAND</option>" +
				"<option value=\"617            \">SD 89 FAIRMONT</option>" +
				"<option value=\"618            \">SD 90 TAFT</option>" +
				"<option value=\"619            \">SD 91 MILNE-KELVIN GR</option>" +
				"<option value=\"620            \">SD 92 LUDWIG-REED-WALSH</option>" +
				"<option value=\"601            \">UD 1-U COAL CITY</option>" +
				"<option value=\"630            \">UD 200-U BEECHER</option>" +
				"<option value=\"632            \">UD 201-U CRETE-MONEE</option>" +
				"<option value=\"633            \">UD 202 PLAINNFIELD</option>" +
				"<option value=\"646            \">UD 203-U NAPERVILLE</option>" +
				"<option value=\"644            \">UD 204-U INDIAN PRAIRIE</option>" +
				"<option value=\"638            \">UD 207-U PEOTONE</option>" +
				"<option value=\"639            \">UD 209-U WILMINGTON</option>" +
				"<option value=\"647            \">UD 255-U REED-CUSTER</option>" +
				"<option value=\"641            \">UD 308-U OSWEGO</option>" +
				"<option value=\"645            \">UD 365-U VALLEY VIEW</option>" +
				"<option value=\"603            \">UD 5-U MANTENO</option>" +
		
			"</select>"
			;


		TOWNSHIP_SELECT = 
			"<select tabindex=\"200\" id=\"ddlTownship\" name=\"ddlTownship\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"04\">CHANNAHON</option>" +
				"<option value=\"23\">CRETE</option>" +
				"<option value=\"01\">CUSTER</option>" +
				"<option value=\"12\">DUPAGE</option>" +
				"<option value=\"09\">FLORENCE</option>" +
				"<option value=\"19\">FRANKFORT</option>" +
				"<option value=\"18\">GREEN GARDEN</option>" +
				"<option value=\"16\">HOMER</option>" +
				"<option value=\"10\">JACKSON</option>" +
				"<option value=\"30\">JOLIET</option>" +
				"<option value=\"11\">LOCKPORT</option>" +
				"<option value=\"14\">MANHATTAN</option>" +
				"<option value=\"21\">MONEE</option>" +
				"<option value=\"15\">NEW LENOX</option>" +
				"<option value=\"17\">PEOTONE</option>" +
				"<option value=\"06\">PLAINFIELD</option>" +
				"<option value=\"02\">REED</option>" +
				"<option value=\"05\">TROY</option>" +
				"<option value=\"22\">WASHINGTON</option>" +
				"<option value=\"08\">WESLEY</option>" +
				"<option value=\"07\">WHEATLAND</option>" +
				"<option value=\"20\">WILL</option>" +
				"<option value=\"03\">WILMINGTON</option>" +
				"<option value=\"13\">WILTON</option>" +
			"</select>";
	}
	
}
