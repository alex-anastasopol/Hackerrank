package ro.cst.tsearch.servers.types;


public class ILMcHenryIS extends GenericISI {
	
	public static final long serialVersionUID = 10000000L;
	
	public ILMcHenryIS(long searchId) {
		super(searchId);
	}

	public ILMcHenryIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void loadSpecialFields() {
		pinNote = "(eg: 0133351009)";
		
		SCHOOL_SELECT = 
			"<select tabindex=\"110\" id=\"ddlSchool\" multiple=\"multiple\" size=\"4\" name=\"ddlSchool\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"D13            \">D13</option>" +
				"<option value=\"D15            \">D15</option>" +
				"<option value=\"D165           \">D165</option>" +
				"<option value=\"D18            \">D18</option>" +
				"<option value=\"D2             \">D2</option>" +
				"<option value=\"D211           \">D211</option>" +
				"<option value=\"D26            \">D26</option>" +
				"<option value=\"D3             \">D3</option>" +
				"<option value=\"D36            \">D36</option>" +
				"<option value=\"D46            \">D46</option>" +
				"<option value=\"D47            \">D47</option>" +
				"<option value=\"U100           \">U100</option>" +
				"<option value=\"U118           \">U118</option>" +
				"<option value=\"U12            \">U12</option>" +
				"<option value=\"U158           \">U158</option>" +
				"<option value=\"U19            \">U19</option>" +
				"<option value=\"U200           \">U200</option>" +
				"<option value=\"U211           \">U211</option>" +
				"<option value=\"U220           \">U220</option>" +
				"<option value=\"U300           \">U300</option>" +
				"<option value=\"U50            \">U50</option>" +
			"</select>";
		
		ZIP_SELECT = 
			"<select tabindex=\"27\" id=\"ddlZip\" multiple=\"multiple\" size=\"4\" name=\"ddlZip\">" +
				"<option value=\" No Selection \"> No Selection </option>" +
				"<option value=\"00000\">00000</option>" +
				"<option value=\"00060\">00060</option>" +
				"<option value=\"06001\">06001</option>" +
				"<option value=\"06002\">06002</option>" +
				"<option value=\"38671\">38671</option>" +
				"<option value=\"60001\">60001</option>" +
				"<option value=\"60002\">60002</option>" +
				"<option value=\"60004\">60004</option>" +
				"<option value=\"60005\">60005</option>" +
				"<option value=\"60007\">60007</option>" +
				"<option value=\"60008\">60008</option>" +
				"<option value=\"60010\">60010</option>" +
				"<option value=\"60011\">60011</option>" +
				"<option value=\"60012\">60012</option>" +
				"<option value=\"60013\">60013</option>" +
				"<option value=\"60014\">60014</option>" +
				"<option value=\"60015\">60015</option>" +
				"<option value=\"60016\">60016</option>" +
				"<option value=\"60018\">60018</option>" +
				"<option value=\"60020\">60020</option>" +
				"<option value=\"60021\">60021</option>" +
				"<option value=\"60022\">60022</option>" +
				"<option value=\"60025\">60025</option>" +
				"<option value=\"60026\">60026</option>" +
				"<option value=\"60030\">60030</option>" +
				"<option value=\"60031\">60031</option>" +
				"<option value=\"60033\">60033</option>" +
				"<option value=\"60034\">60034</option>" +
				"<option value=\"60035\">60035</option>" +
				"<option value=\"60039\">60039</option>" +
				"<option value=\"60041\">60041</option>" +
				"<option value=\"60042\">60042</option>" +
				"<option value=\"60043\">60043</option>" +
				"<option value=\"60044\">60044</option>" +
				"<option value=\"60045\">60045</option>" +
				"<option value=\"60046\">60046</option>" +
				"<option value=\"60047\">60047</option>" +
				"<option value=\"60048\">60048</option>" +
				"<option value=\"60050\">60050</option>" +
				"<option value=\"60051\">60051</option>" +
				"<option value=\"60053\">60053</option>" +
				"<option value=\"60056\">60056</option>" +
				"<option value=\"60057\">60057</option>" +
				"<option value=\"60060\">60060</option>" +
				"<option value=\"60061\">60061</option>" +
				"<option value=\"60062\">60062</option>" +
				"<option value=\"60064\">60064</option>" +
				"<option value=\"60067\">60067</option>" +
				"<option value=\"60068\">60068</option>" +
				"<option value=\"60069\">60069</option>" +
				"<option value=\"60070\">60070</option>" +
				"<option value=\"60071\">60071</option>" +
				"<option value=\"60072\">60072</option>" +
				"<option value=\"60073\">60073</option>" +
				"<option value=\"60074\">60074</option>" +
				"<option value=\"60076\">60076</option>" +
				"<option value=\"60077\">60077</option>" +
				"<option value=\"60078\">60078</option>" +
				"<option value=\"60079\">60079</option>" +
				"<option value=\"60080\">60080</option>" +
				"<option value=\"60081\">60081</option>" +
				"<option value=\"60083\">60083</option>" +
				"<option value=\"60084\">60084</option>" +
				"<option value=\"60085\">60085</option>" +
				"<option value=\"60087\">60087</option>" +
				"<option value=\"60088\">60088</option>" +
				"<option value=\"60089\">60089</option>" +
				"<option value=\"60090\">60090</option>" +
				"<option value=\"60091\">60091</option>" +
				"<option value=\"60093\">60093</option>" +
				"<option value=\"60094\">60094</option>" +
				"<option value=\"60096\">60096</option>" +
				"<option value=\"60097\">60097</option>" +
				"<option value=\"60098\">60098</option>" +
				"<option value=\"60099\">60099</option>" +
				"<option value=\"60101\">60101</option>" +
				"<option value=\"60102\">60102</option>" +
				"<option value=\"60103\">60103</option>" +
				"<option value=\"60106\">60106</option>" +
				"<option value=\"60107\">60107</option>" +
				"<option value=\"60108\">60108</option>" +
				"<option value=\"60110\">60110</option>" +
				"<option value=\"60114\">60114</option>" +
				"<option value=\"60115\">60115</option>" +
				"<option value=\"60118\">60118</option>" +
				"<option value=\"60120\">60120</option>" +
				"<option value=\"60121\">60121</option>" +
				"<option value=\"60123\">60123</option>" +
				"<option value=\"60124\">60124</option>" +
				"<option value=\"60126\">60126</option>" +
				"<option value=\"60130\">60130</option>" +
				"<option value=\"60131\">60131</option>" +
				"<option value=\"60133\">60133</option>" +
				"<option value=\"60134\">60134</option>" +
				"<option value=\"60135\">60135</option>" +
				"<option value=\"60136\">60136</option>" +
				"<option value=\"60137\">60137</option>" +
				"<option value=\"60139\">60139</option>" +
				"<option value=\"60140\">60140</option>" +
				"<option value=\"60142\">60142</option>" +
				"<option value=\"60143\">60143</option>" +
				"<option value=\"60145\">60145</option>" +
				"<option value=\"60146\">60146</option>" +
				"<option value=\"60148\">60148</option>" +
				"<option value=\"60152\">60152</option>" +
				"<option value=\"60154\">60154</option>" +
				"<option value=\"60156\">60156</option>" +
				"<option value=\"60157\">60157</option>" +
				"<option value=\"60160\">60160</option>" +
				"<option value=\"60162\">60162</option>" +
				"<option value=\"60163\">60163</option>" +
				"<option value=\"60164\">60164</option>" +
				"<option value=\"60169\">60169</option>" +
				"<option value=\"60172\">60172</option>" +
				"<option value=\"60173\">60173</option>" +
				"<option value=\"60174\">60174</option>" +
				"<option value=\"60175\">60175</option>" +
				"<option value=\"60177\">60177</option>" +
				"<option value=\"60178\">60178</option>" +
				"<option value=\"60180\">60180</option>" +
				"<option value=\"60181\">60181</option>" +
				"<option value=\"60184\">60184</option>" +
				"<option value=\"60185\">60185</option>" +
				"<option value=\"60187\">60187</option>" +
				"<option value=\"60188\">60188</option>" +
				"<option value=\"60189\">60189</option>" +
				"<option value=\"60191\">60191</option>" +
				"<option value=\"60193\">60193</option>" +
				"<option value=\"60194\">60194</option>" +
				"<option value=\"60195\">60195</option>" +
				"<option value=\"60196\">60196</option>" +
				"<option value=\"60201\">60201</option>" +
				"<option value=\"60404\">60404</option>" +
				"<option value=\"60433\">60433</option>" +
				"<option value=\"60439\">60439</option>" +
				"<option value=\"60441\">60441</option>" +
				"<option value=\"60446\">60446</option>" +
				"<option value=\"60448\">60448</option>" +
				"<option value=\"60462\">60462</option>" +
				"<option value=\"60466\">60466</option>" +
				"<option value=\"60490\">60490</option>" +
				"<option value=\"60502\">60502</option>" +
				"<option value=\"60506\">60506</option>" +
				"<option value=\"60523\">60523</option>" +
				"<option value=\"60527\">60527</option>" +
				"<option value=\"60532\">60532</option>" +
				"<option value=\"60538\">60538</option>" +
				"<option value=\"60540\">60540</option>" +
				"<option value=\"60550\">60550</option>" +
				"<option value=\"60585\">60585</option>" +
				"<option value=\"60586\">60586</option>" +
				"<option value=\"60607\">60607</option>" +
				"<option value=\"60609\">60609</option>" +
				"<option value=\"60610\">60610</option>" +
				"<option value=\"60613\">60613</option>" +
				"<option value=\"60614\">60614</option>" +
				"<option value=\"60630\">60630</option>" +
				"<option value=\"60642\">60642</option>" +
				"<option value=\"60646\">60646</option>" +
				"<option value=\"60647\">60647</option>" +
				"<option value=\"60651\">60651</option>" +
				"<option value=\"60655\">60655</option>" +
				"<option value=\"61008\">61008</option>" +
				"<option value=\"61012\">61012</option>" +
				"<option value=\"61032\">61032</option>" +
				"<option value=\"61038\">61038</option>" +
				"<option value=\"61052\">61052</option>" +
				"<option value=\"61065\">61065</option>" +
				"<option value=\"61081\">61081</option>" +
				"<option value=\"61101\">61101</option>" +
				"<option value=\"61104\">61104</option>" +
				"<option value=\"61111\">61111</option>" +
				"<option value=\"61241\">61241</option>" +
				"<option value=\"61254\">61254</option>" +
				"<option value=\"61443\">61443</option>" +
				"<option value=\"62321\">62321</option>" +
				"<option value=\"62438\">62438</option>" +
				"<option value=\"72019\">72019</option>" +
				"<option value=\"90098\">90098</option>" +
			"</select>";
		LAND_USE_SELECT = 
			"<select tabindex=\"92\" id=\"ddlLandUseCodes\" multiple=\"multiple\" size=\"4\" name=\"ddlLandUseCodes\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"0000\">NOT ASSIGNED - 0000           </option>" +
				"<option value=\"0011\">FARM W/BUILDINGS - 0011           </option>" +
				"<option value=\"0021\">FARM UNIMPROVED - 0021           </option>" +
				"<option value=\"0030\">RESIDENTIAL UNIMPROVED - 0030           </option>" +
				"<option value=\"0032\">SPEC VAC SUBD LAND - 0032           </option>" +
				"<option value=\"0040\">RESIDENTIAL W/BUILDING - 0040           </option>" +
				"<option value=\"0041\">MODEL HOME - 0041           </option>" +
				"<option value=\"0050\">COMMERCIAL MULTI-FAMILY - 0050           </option>" +
				"<option value=\"0052\">SPEC VAC SUB LAND - 0052           </option>" +
				"<option value=\"0060\">COMMERCIAL - 0060           </option>" +
				"<option value=\"0062\">SPEC VAC COM SUB LAND - 0062           </option>" +
				"<option value=\"0070\">OFFICE - 0070           </option>" +
				"<option value=\"0072\">SPEC VAC SUB LAND - 0072           </option>" +
				"<option value=\"0080\">INDUSTRIAL - 0080           </option>" +
			"</select>";
	}
	
	
}
