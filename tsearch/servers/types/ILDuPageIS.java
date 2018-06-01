package ro.cst.tsearch.servers.types;

  /**
   * @author mihaib
   *
  */

public class ILDuPageIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
	
	public ILDuPageIS(long searchId) {
		super(searchId);
	}

	public ILDuPageIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0524103012)";
	
	
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
					"<option value=\"00000\">00000</option>" +
					"<option value=\"06010\">06010</option>" +
					"<option value=\"33610\">33610</option>" +
					"<option value=\"37067\">37067</option>" +
					"<option value=\"60004\">60004</option>" +
					"<option value=\"60007\">60007</option>" +
					"<option value=\"60008\">60008</option>" +
					"<option value=\"60010\">60010</option>" +
					"<option value=\"60014\">60014</option>" +
					"<option value=\"60015\">60015</option>" +
					"<option value=\"60016\">60016</option>" +
					"<option value=\"60018\">60018</option>" +
					"<option value=\"60025\">60025</option>" +
					"<option value=\"60026\">60026</option>" +
					"<option value=\"60031\">60031</option>" +
					"<option value=\"60035\">60035</option>" +
					"<option value=\"60045\">60045</option>" +
					"<option value=\"60047\">60047</option>" +
					"<option value=\"60053\">60053</option>" +
					"<option value=\"60056\">60056</option>" +
					"<option value=\"60059\">60059</option>" +
					"<option value=\"60061\">60061</option>" +
					"<option value=\"60062\">60062</option>" +
					"<option value=\"60067\">60067</option>" +
					"<option value=\"60068\">60068</option>" +
					"<option value=\"60071\">60071</option>" +
					"<option value=\"60074\">60074</option>" +
					"<option value=\"60077\">60077</option>" +
					"<option value=\"60089\">60089</option>" +
					"<option value=\"60093\">60093</option>" +
					"<option value=\"60101\">60101</option>" +
					"<option value=\"60103\">60103</option>" +
					"<option value=\"60104\">60104</option>" +
					"<option value=\"60105\">60105</option>" +
					"<option value=\"60106\">60106</option>" +
					"<option value=\"60107\">60107</option>" +
					"<option value=\"60108\">60108</option>" +
					"<option value=\"60110\">60110</option>" +
					"<option value=\"60117\">60117</option>" +
					"<option value=\"60118\">60118</option>" +
					"<option value=\"60119\">60119</option>" +
					"<option value=\"60120\">60120</option>" +
					"<option value=\"60123\">60123</option>" +
					"<option value=\"60124\">60124</option>" +
					"<option value=\"60126\">60126</option>" +
					"<option value=\"60131\">60131</option>" +
					"<option value=\"60133\">60133</option>" +
					"<option value=\"60134\">60134</option>" +
					"<option value=\"60137\">60137</option>" +
					"<option value=\"60138\">60138</option>" +
					"<option value=\"60139\">60139</option>" +
					"<option value=\"60141\">60141</option>" +
					"<option value=\"60142\">60142</option>" +
					"<option value=\"60143\">60143</option>" +
					"<option value=\"60147\">60147</option>" +
					"<option value=\"60148\">60148</option>" +
					"<option value=\"60151\">60151</option>" +
					"<option value=\"60153\">60153</option>" +
					"<option value=\"60154\">60154</option>" +
					"<option value=\"60155\">60155</option>" +
					"<option value=\"60156\">60156</option>" +
					"<option value=\"60157\">60157</option>" +
					"<option value=\"60160\">60160</option>" +
					"<option value=\"60161\">60161</option>" +
					"<option value=\"60162\">60162</option>" +
					"<option value=\"60163\">60163</option>" +
					"<option value=\"60164\">60164</option>" +
					"<option value=\"60165\">60165</option>" +
					"<option value=\"60169\">60169</option>" +
					"<option value=\"60171\">60171</option>" +
					"<option value=\"60172\">60172</option>" +
					"<option value=\"60173\">60173</option>" +
					"<option value=\"60174\">60174</option>" +
					"<option value=\"60175\">60175</option>" +
					"<option value=\"60176\">60176</option>" +
					"<option value=\"60177\">60177</option>" +
					"<option value=\"60178\">60178</option>" +
					"<option value=\"60180\">60180</option>" +
					"<option value=\"60181\">60181</option>" +
					"<option value=\"60182\">60182</option>" +
					"<option value=\"60184\">60184</option>" +
					"<option value=\"60185\">60185</option>" +
					"<option value=\"60186\">60186</option>" +
					"<option value=\"60187\">60187</option>" +
					"<option value=\"60188\">60188</option>" +
					"<option value=\"60189\">60189</option>" +
					"<option value=\"60190\">60190</option>" +
					"<option value=\"60191\">60191</option>" +
					"<option value=\"60192\">60192</option>" +
					"<option value=\"60193\">60193</option>" +
					"<option value=\"60194\">60194</option>" +
					"<option value=\"60195\">60195</option>" +
					"<option value=\"60196\">60196</option>" +
					"<option value=\"60199\">60199</option>" +
					"<option value=\"60201\">60201</option>" +
					"<option value=\"60232\">60232</option>" +
					"<option value=\"60301\">60301</option>" +
					"<option value=\"60302\">60302</option>" +
					"<option value=\"60305\">60305</option>" +
					"<option value=\"60402\">60402</option>" +
					"<option value=\"60403\">60403</option>" +
					"<option value=\"60404\">60404</option>" +
					"<option value=\"60406\">60406</option>" +
					"<option value=\"60409\">60409</option>" +
					"<option value=\"60423\">60423</option>" +
					"<option value=\"60426\">60426</option>" +
					"<option value=\"60435\">60435</option>" +
					"<option value=\"60439\">60439</option>" +
					"<option value=\"60440\">60440</option>" +
					"<option value=\"60441\">60441</option>" +
					"<option value=\"60442\">60442</option>" +
					"<option value=\"60446\">60446</option>" +
					"<option value=\"60448\">60448</option>" +
					"<option value=\"60451\">60451</option>" +
					"<option value=\"60453\">60453</option>" +
					"<option value=\"60455\">60455</option>" +
					"<option value=\"60457\">60457</option>" +
					"<option value=\"60458\">60458</option>" +
					"<option value=\"60461\">60461</option>" +
					"<option value=\"60464\">60464</option>" +
					"<option value=\"60465\">60465</option>" +
					"<option value=\"60473\">60473</option>" +
					"<option value=\"60477\">60477</option>" +
					"<option value=\"60480\">60480</option>" +
					"<option value=\"60490\">60490</option>" +
					"<option value=\"60491\">60491</option>" +
					"<option value=\"60502\">60502</option>" +
					"<option value=\"60503\">60503</option>" +
					"<option value=\"60504\">60504</option>" +
					"<option value=\"60505\">60505</option>" +
					"<option value=\"60506\">60506</option>" +
					"<option value=\"60507\">60507</option>" +
					"<option value=\"60510\">60510</option>" +
					"<option value=\"60512\">60512</option>" +
					"<option value=\"60513\">60513</option>" +
					"<option value=\"60514\">60514</option>" +
					"<option value=\"60515\">60515</option>" +
					"<option value=\"60516\">60516</option>" +
					"<option value=\"60517\">60517</option>" +
					"<option value=\"60519\">60519</option>" +
					"<option value=\"60521\">60521</option>" +
					"<option value=\"60522\">60522</option>" +
					"<option value=\"60523\">60523</option>" +
					"<option value=\"60525\">60525</option>" +
					"<option value=\"60526\">60526</option>" +
					"<option value=\"60527\">60527</option>" +
					"<option value=\"60531\">60531</option>" +
					"<option value=\"60532\">60532</option>" +
					"<option value=\"60533\">60533</option>" +
					"<option value=\"60534\">60534</option>" +
					"<option value=\"60536\">60536</option>" +
					"<option value=\"60540\">60540</option>" +
					"<option value=\"60541\">60541</option>" +
					"<option value=\"60543\">60543</option>" +
					"<option value=\"60544\">60544</option>" +
					"<option value=\"60546\">60546</option>" +
					"<option value=\"60548\">60548</option>" +
					"<option value=\"60552\">60552</option>" +
					"<option value=\"60555\">60555</option>" +
					"<option value=\"60556\">60556</option>" +
					"<option value=\"60558\">60558</option>" +
					"<option value=\"60559\">60559</option>" +
					"<option value=\"60561\">60561</option>" +
					"<option value=\"60562\">60562</option>" +
					"<option value=\"60563\">60563</option>" +
					"<option value=\"60564\">60564</option>" +
					"<option value=\"60565\">60565</option>" +
					"<option value=\"60566\">60566</option>" +
					"<option value=\"60571\">60571</option>" +
					"<option value=\"60585\">60585</option>" +
					"<option value=\"60586\">60586</option>" +
					"<option value=\"60601\">60601</option>" +
					"<option value=\"60602\">60602</option>" +
					"<option value=\"60603\">60603</option>" +
					"<option value=\"60604\">60604</option>" +
					"<option value=\"60605\">60605</option>" +
					"<option value=\"60606\">60606</option>" +
					"<option value=\"60607\">60607</option>" +
					"<option value=\"60608\">60608</option>" +
					"<option value=\"60610\">60610</option>" +
					"<option value=\"60611\">60611</option>" +
					"<option value=\"60612\">60612</option>" +
					"<option value=\"60613\">60613</option>" +
					"<option value=\"60614\">60614</option>" +
					"<option value=\"60615\">60615</option>" +
					"<option value=\"60616\">60616</option>" +
					"<option value=\"60624\">60624</option>" +
					"<option value=\"60625\">60625</option>" +
					"<option value=\"60626\">60626</option>" +
					"<option value=\"60629\">60629</option>" +
					"<option value=\"60630\">60630</option>" +
					"<option value=\"60632\">60632</option>" +
					"<option value=\"60634\">60634</option>" +
					"<option value=\"60638\">60638</option>" +
					"<option value=\"60641\">60641</option>" +
					"<option value=\"60642\">60642</option>" +
					"<option value=\"60643\">60643</option>" +
					"<option value=\"60645\">60645</option>" +
					"<option value=\"60646\">60646</option>" +
					"<option value=\"60647\">60647</option>" +
					"<option value=\"60651\">60651</option>" +
					"<option value=\"60652\">60652</option>" +
					"<option value=\"60654\">60654</option>" +
					"<option value=\"60657\">60657</option>" +
					"<option value=\"60659\">60659</option>" +
					"<option value=\"60660\">60660</option>" +
					"<option value=\"60661\">60661</option>" +
					"<option value=\"60706\">60706</option>" +
					"<option value=\"60707\">60707</option>" +
					"<option value=\"60712\">60712</option>" +
					"<option value=\"60714\">60714</option>" +
					"<option value=\"60801\">60801</option>" +
					"<option value=\"60804\">60804</option>" +
					"<option value=\"60805\">60805</option>" +
					"<option value=\"60827\">60827</option>" +
					"<option value=\"60914\">60914</option>" +
					"<option value=\"60950\">60950</option>" +
					"<option value=\"61104\">61104</option>" +
					"<option value=\"61109\">61109</option>" +
					"<option value=\"61327\">61327</option>" +
					"<option value=\"61373\">61373</option>" +
					"<option value=\"61532\">61532</option>" +
					"<option value=\"61701\">61701</option>" +
					"<option value=\"62223\">62223</option>" +
					"<option value=\"62701\">62701</option>" +
					"<option value=\"90181\">90181</option>" +
			"</select>";
		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
					"<option value=\"\"> No Selection </option>" +
					"<option value=\"A\">APARTMENT - A              </option>" +
					"<option value=\"C\">COMMERCIAL - C              </option>" +
					"<option value=\"D\">PROP-HISTORICAL - D              </option>" +
					"<option value=\"E\">PROP-EXEMPT - E              </option>" +
					"<option value=\"F\">FARM - F              </option>" +
					"<option value=\"G\">PROP-GOLF-COURSE - G              </option>" +
					"<option value=\"H\">FARM-HOMESITE - H              </option>" +
					"<option value=\"I\">INDUSTRIAL - I              </option>" +
					"<option value=\"K\">PROP-MODEL - K              </option>" +
					"<option value=\"L\">PROP-LEASE - L              </option>" +
					"<option value=\"M\">PROP-MULTIPLE - M              </option>" +
					"<option value=\"N\">PROP-NON-RESIDENT - N              </option>" +
					"<option value=\"O\">PROP-OPEN-SPACE - O              </option>" +
					"<option value=\"P\">PROP-AIRPORT - P              </option>" +
					"<option value=\"R\">RESIDENTIAL - R              </option>" +
					"<option value=\"S\">PROP-SUBDIVISION - S              </option>" +
					"<option value=\"T\">PROP-LEASEHOLD - T              </option>" +
					"<option value=\"U\">PROP-UTILITY - U              </option>" +
					"<option value=\"V\">PROP-VACANT - V              </option>" +
			"</select>";
		TOWNSHIP_SELECT =
			"<select tabindex=\"200\" id=\"ddlTownship\" name=\"ddlTownship\">" +
					"<option value=\"\"> No Selection </option>" +
					"<option value=\"3\">ADDISON</option>" +
					"<option value=\"2\">BLOOMINGDALE</option>" +
					"<option value=\"9\">DOWNERS GROVE</option>" +
					"<option value=\"8\">LISLE</option>" +
					"<option value=\"5\">MILTON</option>" +
					"<option value=\"7\">NAPERVILLE</option>" +
					"<option value=\"1\">WAYNE</option>" +
					"<option value=\"4\">WINFIELD</option>" +
					"<option value=\"6\">YORK</option>" +
			"</select>";

	}
}
