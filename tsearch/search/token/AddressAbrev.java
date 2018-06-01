/*
 * Created on May 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

/**
 * @author elmarie
 */
public class AddressAbrev {
	
	protected static final Category logger = Category.getInstance(AddressAbrev.class.getName());

	
	private static final Map all = new HashMap();
	private static final Map streetSufixes = new HashMap();
	private static final Map directions = new HashMap();
	private static final Map<String, String> directionAbbreviations = new HashMap<String, String>();
	static {
		 streetSufixes.put("ALLEE", "ALLEY");
		 streetSufixes.put("ALLEY", "ALLEY");
		 streetSufixes.put("ALLY", "ALLEY");
		 streetSufixes.put("ALY", "ALLEY");
		 streetSufixes.put("ANEX", "ANNEX");
		 streetSufixes.put("ANNEX", "ANNEX");
		 streetSufixes.put("ANNX", "ANNEX");
		 streetSufixes.put("ANX", "ANNEX");
		 streetSufixes.put("APT", "APARTMENT");
		 streetSufixes.put("ARC", "ARCADE");
		 streetSufixes.put("ARCADE", "ARCADE");
		 streetSufixes.put("AV", "AVENUE");
		 streetSufixes.put("AVE", "AVENUE");
		 streetSufixes.put("AVEN", "AVENUE");
		 streetSufixes.put("AVENU", "AVENUE");
		 streetSufixes.put("AVENUE", "AVENUE");
		 streetSufixes.put("AVN", "AVENUE");
		 streetSufixes.put("AVNUE", "AVENUE");
		 streetSufixes.put("BAYOO", "BAYOO");
		 streetSufixes.put("BAYOU", "BAYOO");
		 streetSufixes.put("BCH", "BEACH");
		 streetSufixes.put("BEACH", "BEACH");
		 streetSufixes.put("BEND", "BEND");
		 streetSufixes.put("BLDG", "BUILDING");
		 streetSufixes.put("BLF", "BLUFF");
		 streetSufixes.put("BLUF", "BLUFF");
		 streetSufixes.put("BLUFF", "BLUFF");
		 streetSufixes.put("BLUFFS", "BLUFFS");
		 streetSufixes.put("BLVD", "BOULEVARD");
		 streetSufixes.put("BND", "BEND");
		 streetSufixes.put("BOT", "BOTTOM");
		 streetSufixes.put("BOTTM", "BOTTOM");
		 streetSufixes.put("BOTTOM", "BOTTOM");
		 streetSufixes.put("BOUL", "BOULEVARD");
		 streetSufixes.put("BOULEVARD", "BOULEVARD");
		 streetSufixes.put("BOULV", "BOULEVARD");
		 streetSufixes.put("BR", "BRANCH");
		 streetSufixes.put("BRANCH", "BRANCH");
		 streetSufixes.put("BRDGE", "BRIDGE");
		 streetSufixes.put("BRG", "BRIDGE");
		 streetSufixes.put("BRIDGE", "BRIDGE");
		 streetSufixes.put("BRK", "BROOK");
		 streetSufixes.put("BRNCH", "BRANCH");
		 streetSufixes.put("BROOK", "BROOK");
		 streetSufixes.put("BROOKS", "BROOKS");
		 streetSufixes.put("BSMT", "BASEMENT");
		 streetSufixes.put("BTM", "BOTTOM");
		 streetSufixes.put("BURG", "BURG");
		 streetSufixes.put("BURGS", "BURGS");
		 streetSufixes.put("BYP", "BYPASS");
		 streetSufixes.put("BYPA", "BYPASS");
		 streetSufixes.put("BYPAS", "BYPASS");
		 streetSufixes.put("BYPASS", "BYPASS");
		 streetSufixes.put("BYPS", "BYPASS");
		 streetSufixes.put("CAMP", "CAMP");
		 streetSufixes.put("CANYN", "CANYON");
		 streetSufixes.put("CANYON", "CANYON");
		 streetSufixes.put("CAPE", "CAPE");
		 streetSufixes.put("CAUSEWAY", "CAUSEWAY");
		 streetSufixes.put("CAUSWAY", "CAUSEWAY");
		 streetSufixes.put("CEN", "CENTER");
		 streetSufixes.put("CENT", "CENTER");
		 streetSufixes.put("CENTER", "CENTER");
		 streetSufixes.put("CENTERS", "CENTERS");
		 streetSufixes.put("CENTR", "CENTER");
		 streetSufixes.put("CENTRE", "CENTER");
		 streetSufixes.put("CIR", "CIRCLE");
		 streetSufixes.put("CIRC", "CIRCLE");
		 streetSufixes.put("CIRCL", "CIRCLE");
		 streetSufixes.put("CIRCLE", "CIRCLE");
		 streetSufixes.put("CIRCLES", "CIRCLES");
		 streetSufixes.put("CK", "CREEK");
		 streetSufixes.put("CLB", "CLUB");
		 streetSufixes.put("CLF", "CLIFF");
		 streetSufixes.put("CLFS", "CLIFFS");
		 streetSufixes.put("CLIFF", "CLIFF");
		 streetSufixes.put("CLIFFS", "CLIFFS");
		 streetSufixes.put("CLUB", "CLUB");
		 streetSufixes.put("CMP", "CAMP");
		 streetSufixes.put("CNTER", "CENTER");
		 streetSufixes.put("CNTR", "CENTER");
		 streetSufixes.put("CNYN", "CANYON");
		 streetSufixes.put("COMMON", "COMMON");
		 streetSufixes.put("COR", "CORNER");
		 streetSufixes.put("CORNER", "CORNER");
		 streetSufixes.put("CORNERS", "CORNERS");
		 streetSufixes.put("CORS", "CORNERS");
		 streetSufixes.put("COURSE", "COURSE");
		 streetSufixes.put("COURT", "COURT");
		 streetSufixes.put("COURTS", "COURT");
		 streetSufixes.put("COVE", "COVE");
		 streetSufixes.put("COVES", "COVES");
		 streetSufixes.put("CP", "CAMP");
		 streetSufixes.put("CPE", "CAPE");
		 streetSufixes.put("CR", "CREEK");
		 streetSufixes.put("CRCL", "CIRCLE");
		 streetSufixes.put("CRCLE", "CIRCLE");
		 streetSufixes.put("CRECENT", "CRESCENT");
		 streetSufixes.put("CREEK", "CREEK");
		 streetSufixes.put("CRES", "CRESCENT");
		 streetSufixes.put("CRESCENT", "CRESCENT");
		 streetSufixes.put("CRESENT", "CRESCENT");
		 streetSufixes.put("CREST", "CREST");
		 streetSufixes.put("CRK", "CREEK");
 		streetSufixes.put("CROSSING", "CROSSING");
 		streetSufixes.put("CROSS", "CROSSING");
		 streetSufixes.put("CROSSROAD", "CROSSROAD");
		 streetSufixes.put("CRSCNT", "CRESCENT");
		 streetSufixes.put("CRSE", "COURSE");
		 streetSufixes.put("CRSENT", "CRESCENT");
		 streetSufixes.put("CRSNT", "CRESCENT");
		 streetSufixes.put("CRSSING", "CROSSING");
		 streetSufixes.put("CRSSNG", "CROSSING");
		 streetSufixes.put("CRT", "COURT");
		 streetSufixes.put("CSWY", "CAUSEWAY");
		 streetSufixes.put("CT", "COURT");
		 streetSufixes.put("CTR", "CENTER");
		 streetSufixes.put("CURVE", "CURVE");
		 streetSufixes.put("CV", "COVE");
		 streetSufixes.put("CYN", "CANYON");
		 streetSufixes.put("DALE", "DALE");
		 streetSufixes.put("DAM", "DAM");
		 streetSufixes.put("DEPT", "DEPARTMENT");
		 streetSufixes.put("DIV", "DIVIDE");
		 streetSufixes.put("DIVIDE", "DIVIDE");
		 streetSufixes.put("DL", "DALE");
		 streetSufixes.put("DM", "DAM");
		 streetSufixes.put("DR", "DRIVE");
		 streetSufixes.put("DRIV", "DRIVE");
		 streetSufixes.put("DRIVE", "DRIVE");
		 streetSufixes.put("DRIVES", "DRIVES");
		 streetSufixes.put("DRV", "DRIVE");
		 streetSufixes.put("DV", "DIVIDE");
		 streetSufixes.put("DVD", "DIVIDE");
		 streetSufixes.put("EST", "ESTATE");
		 streetSufixes.put("ESTATE", "ESTATE");
		 streetSufixes.put("ESTATES", "ESTATES");
		 streetSufixes.put("ESTS", "ESTATES");
		 streetSufixes.put("EXP", "EXPRESSWAY");
		 streetSufixes.put("EXPR", "EXPRESSWAY");
		 streetSufixes.put("EXPRESS", "EXPRESSWAY");
		 streetSufixes.put("EXPRESSWAY", "EXPRESSWAY");
		 streetSufixes.put("EXPW", "EXPRESSWAY");
		 streetSufixes.put("EXPY", "EXPRESSWAY");
		 streetSufixes.put("EXT", "EXTENSION");
		 streetSufixes.put("EXTENSION", "EXTENSION");
		 streetSufixes.put("EXTENSIONS", "EXTENSIONS");
		 streetSufixes.put("EXTN", "EXTENSION");
		 streetSufixes.put("EXTNSN", "EXTENSION");
		 streetSufixes.put("EXTS", "EXTENSIONS");
		 streetSufixes.put("FALL", "FALL");
		 streetSufixes.put("FALLS", "FALLS");
		 streetSufixes.put("FERRY", "FERRY");
		 streetSufixes.put("FIELD", "FIELD");
		 streetSufixes.put("FIELDS", "FIELDS");
		 streetSufixes.put("FL", "FLOOR");
		 streetSufixes.put("FLAT", "FLAT");
		 streetSufixes.put("FLATS", "FLATS");
		 streetSufixes.put("FLD", "FIELD");
		 streetSufixes.put("FLDS", "FIELDS");
		 streetSufixes.put("FLS", "FALLS");
		 streetSufixes.put("FLT", "FLAT");
		 streetSufixes.put("FLTS", "FLATS");
		 streetSufixes.put("FORD", "FORD");
		 streetSufixes.put("FORDS", "FORDS");
		 streetSufixes.put("FOREST", "FOREST");
		 streetSufixes.put("FORESTS", "FOREST");
		 streetSufixes.put("FORG", "FORGE");
		 streetSufixes.put("FORGE", "FORGE");
		 streetSufixes.put("FORGES", "FORGES");
		 streetSufixes.put("FORK", "FORK");
		 streetSufixes.put("FORKS", "FORKS");
		 streetSufixes.put("FORT", "FORT");
		 streetSufixes.put("FRD", "FORD");
		 streetSufixes.put("FREEWAY", "FREEWAY");
		 streetSufixes.put("FREEWY", "FREEWAY");
		 streetSufixes.put("FRG", "FORGE");
		 streetSufixes.put("FRK", "FORK");
		 streetSufixes.put("FRKS", "FORKS");
		 streetSufixes.put("FRNT", "FRONT");
		 streetSufixes.put("FRRY", "FERRY");
		 streetSufixes.put("FRST", "FOREST");
		 streetSufixes.put("FRT", "FORT");
		 streetSufixes.put("FRWAY", "FREEWAY");
		 streetSufixes.put("FRWY", "FREEWAY");
		 streetSufixes.put("FRY", "FERRY");
		 streetSufixes.put("FT", "FORT");
		 streetSufixes.put("FWY", "FREEWAY");
		 streetSufixes.put("GARDEN", "GARDEN");
		 streetSufixes.put("GARDENS", "GARDENS");
		 streetSufixes.put("GARDN", "GARDEN");
		 streetSufixes.put("GATEWAY", "GATEWAY");
		 streetSufixes.put("GATEWY", "GATEWAY");
		 streetSufixes.put("GATWAY", "GATEWAY");
		 streetSufixes.put("GDN", "GARDEN");
		 streetSufixes.put("GDNS", "GARDENS");
		 streetSufixes.put("GLEN", "GLEN");
		 streetSufixes.put("GLENS", "GLENS");
		 streetSufixes.put("GLN", "GLEN");
		 streetSufixes.put("GRDEN", "GARDEN");
		 streetSufixes.put("GRDN", "GARDEN");
		 streetSufixes.put("GRDNS", "GARDENS");
		 streetSufixes.put("GREEN", "GREEN");
		 streetSufixes.put("GREENS", "GREENS");
		 streetSufixes.put("GRN", "GREEN");
		 streetSufixes.put("GROV", "GROVE");
		 streetSufixes.put("GROVE", "GROVE");
		 streetSufixes.put("GROVES", "GROVES");
		 streetSufixes.put("GRV", "GROVE");
		 streetSufixes.put("GTWAY", "GATEWAY");
		 streetSufixes.put("GTWY", "GATEWAY");
		 streetSufixes.put("HARB", "HARBOR");
		 streetSufixes.put("HARBOR", "HARBOR");
		 streetSufixes.put("HARBORS", "HARBORS");
		 streetSufixes.put("HARBR", "HARBOR");
		 streetSufixes.put("HAVEN", "HAVEN");
		 streetSufixes.put("HAVN", "HAVEN");
		 streetSufixes.put("HBR", "HARBOR");
		 streetSufixes.put("HEIGHT", "HEIGHTS");
		 streetSufixes.put("HEIGHTS", "HEIGHTS");
		 streetSufixes.put("HGTS", "HEIGHTS");
		 streetSufixes.put("HIGHWAY", "HIGHWAY");
		 streetSufixes.put("HIGHWY", "HIGHWAY");
		 streetSufixes.put("HILL", "HILL");
		 streetSufixes.put("HILLS", "HILLS");
		 streetSufixes.put("HIWAY", "HIGHWAY");
		 streetSufixes.put("HIWY", "HIGHWAY");
		 streetSufixes.put("HL", "HILL");
		 streetSufixes.put("HLLW", "HOLLOW");
		 streetSufixes.put("HLS", "HILLS");
		 streetSufixes.put("HNGR", "HANGAR");
		 streetSufixes.put("HOLLOW", "HOLLOW");
		 streetSufixes.put("HOLLOWS", "HOLLOW");
		 streetSufixes.put("HOLW", "HOLLOW");
		 streetSufixes.put("HOLWS", "HOLLOW");
		 streetSufixes.put("HRBOR", "HARBOR");
		 streetSufixes.put("HT", "HEIGHTS");
		 streetSufixes.put("HTS", "HEIGHTS");
		 streetSufixes.put("HVN", "HAVEN");
		 streetSufixes.put("HWAY", "HIGHWAY");
		 streetSufixes.put("HWY", "HIGHWAY");
		 streetSufixes.put("INLET", "INLET");
		 streetSufixes.put("INLT", "INLET");
		 streetSufixes.put("IS", "ISLAND");
		 streetSufixes.put("ISLAND", "ISLAND");
		 streetSufixes.put("ISLANDS", "ISLANDS");
		 streetSufixes.put("ISLE", "ISLE");
		 streetSufixes.put("ISLES", "ISLE");
		 streetSufixes.put("ISLND", "ISLAND");
		 streetSufixes.put("ISLNDS", "ISLANDS");
		 streetSufixes.put("ISS", "ISLANDS");
		 streetSufixes.put("JCT", "JUNCTION");
		 streetSufixes.put("JCTION", "JUNCTION");
		 streetSufixes.put("JCTN", "JUNCTION");
		 streetSufixes.put("JCTNS", "JUNCTIONS");
		 streetSufixes.put("JCTS", "JUNCTIONS");
		 streetSufixes.put("JUNCTION", "JUNCTION");
		 streetSufixes.put("JUNCTIONS", "JUNCTIONS");
		 streetSufixes.put("JUNCTN", "JUNCTION");
		 streetSufixes.put("JUNCTON", "JUNCTION");
		 streetSufixes.put("KEY", "KEY");
		 streetSufixes.put("KEYS", "KEYS");
		 streetSufixes.put("KNL", "KNOLL");
		 streetSufixes.put("KNLS", "KNOLLS");
		 streetSufixes.put("KNOL", "KNOLL");
		 streetSufixes.put("KNOLL", "KNOLL");
		 streetSufixes.put("KNOLLS", "KNOLLS");
		 streetSufixes.put("KY", "KEY");
		 streetSufixes.put("KYS", "KEYS");
		 streetSufixes.put("LA", "LANE");
		 streetSufixes.put("LAKE", "LAKE");
		 streetSufixes.put("LAKES", "LAKES");
		 streetSufixes.put("LAND", "LAND");
		 streetSufixes.put("LANDING", "LANDING");
		 streetSufixes.put("LANE", "LANE");
		 streetSufixes.put("LANES", "LANE");
		 streetSufixes.put("LBBY", "LOBBY");
		 streetSufixes.put("LCK", "LOCK");
		 streetSufixes.put("LCKS", "LOCKS");
		 streetSufixes.put("LDG", "LODGE");
		 streetSufixes.put("LDGE", "LODGE");
		 streetSufixes.put("LF", "LOAF");
		 streetSufixes.put("LGT", "LIGHT");
		 streetSufixes.put("LIGHT", "LIGHT");
		 streetSufixes.put("LIGHTS", "LIGHTS");
		 streetSufixes.put("LK", "LAKE");
		 streetSufixes.put("LKS", "LAKES");
		 streetSufixes.put("LN", "LANE");
		 streetSufixes.put("LNDG", "LANDING");
		 streetSufixes.put("LNDNG", "LANDING");
		 streetSufixes.put("LOAF", "LOAF");
		 streetSufixes.put("LOCK", "LOCK");
		 streetSufixes.put("LOCKS", "LOCKS");
		 streetSufixes.put("LODG", "LODGE");
		 streetSufixes.put("LODGE", "LODGE");
		 streetSufixes.put("LOOP", "LOOP");
		 streetSufixes.put("LOOPS", "LOOP");
		 streetSufixes.put("LOT", "LOT");
		 streetSufixes.put("LOWR", "LOWER");
		 streetSufixes.put("MALL", "MALL");
		 streetSufixes.put("MANOR", "MANOR");
		 streetSufixes.put("MANORS", "MANORS");
		 streetSufixes.put("MDW", "MEADOW");
		 streetSufixes.put("MDWS", "MEADOWS");
		 streetSufixes.put("MEADOW", "MEADOW");
		 streetSufixes.put("MEADOWS", "MEADOWS");
		 streetSufixes.put("MEDOWS", "MEADOWS");
		 streetSufixes.put("MEWS", "MEWS");
		 streetSufixes.put("MILL", "MILL");
		 streetSufixes.put("MILLS", "MILLS");
		 streetSufixes.put("MISSION", "MISSION");
		 streetSufixes.put("MISSN", "MISSION");
		 streetSufixes.put("ML", "MILL");
		 streetSufixes.put("MLS", "MILLS");
		 streetSufixes.put("MNR", "MANOR");
		 streetSufixes.put("MNRS", "MANORS");
		 streetSufixes.put("MNT", "MOUNT");
		 streetSufixes.put("MNTAIN", "MOUNTAIN");
		 streetSufixes.put("MNTN", "MOUNTAIN");
		 streetSufixes.put("MNTNS", "MOUNTAINS");
		 streetSufixes.put("MOTORWAY", "MOTORWAY");
		 streetSufixes.put("MOUNT", "MOUNT");
		 streetSufixes.put("MOUNTAIN", "MOUNTAIN");
		 streetSufixes.put("MOUNTAINS", "MOUNTAINS");
		 streetSufixes.put("MOUNTIN", "MOUNTAIN");
		 streetSufixes.put("MSN", "MISSION");
		 streetSufixes.put("MSSN", "MISSION");
		 streetSufixes.put("MT", "MOUNT");
		 streetSufixes.put("MTIN", "MOUNTAIN");
		 streetSufixes.put("MTN", "MOUNTAIN");
		 streetSufixes.put("NCK", "NECK");
		 streetSufixes.put("NECK", "NECK");
		 streetSufixes.put("OFC", "OFFICE");
		 streetSufixes.put("ORCH", "ORCHARD");
		 streetSufixes.put("ORCHARD", "ORCHARD");
		 streetSufixes.put("ORCHRD", "ORCHARD");
		 streetSufixes.put("OVAL", "OVAL");
		 streetSufixes.put("OVERPASS", "OVERPASS");
		 streetSufixes.put("OVL", "OVAL");
		 streetSufixes.put("PARK", "PARK");
		 streetSufixes.put("PARKS", "PARKS");
		 streetSufixes.put("PARKWAY", "PARKWAY");
		 streetSufixes.put("PARKWAYS", "PARKWAYS");
		 streetSufixes.put("PARKWY", "PARKWAY");
		 streetSufixes.put("PASS", "PASS");
		 streetSufixes.put("PASSAGE", "PASSAGE");
		 streetSufixes.put("PATH", "PATH");
		 streetSufixes.put("PATHS", "PATH");
		 streetSufixes.put("PH", "PENTHOUSE");
		 streetSufixes.put("PIER", "PIER");
		 streetSufixes.put("PIKE", "PIKE");
		 streetSufixes.put("PIKES", "PIKE");
		 streetSufixes.put("PINE", "PINE");
		 streetSufixes.put("PINES", "PINES");
		 streetSufixes.put("PK", "PARK");
		 streetSufixes.put("PKWAY", "PARKWAY");
		 streetSufixes.put("PKWY", "PARKWAY");
		 streetSufixes.put("PRWY", "PARKWAY");
		 streetSufixes.put("PKWYS", "PARKWAYS");
		 streetSufixes.put("PKY", "PARKWAY");
		 streetSufixes.put("PL", "PLACE");
		 streetSufixes.put("PLACE", "PLACE");
		 streetSufixes.put("PLAIN", "PLAIN");
		 streetSufixes.put("PLAINES", "PLAINS");
		 streetSufixes.put("PLAINS", "PLAINS");
		 streetSufixes.put("PLAZA", "PLAZA");
		 streetSufixes.put("PLN", "PLAIN");
		 streetSufixes.put("PLNS", "PLAINS");
		 streetSufixes.put("PLZ", "PLAZA");
		 streetSufixes.put("PLZA", "PLAZA");
		 streetSufixes.put("PNES", "PINES");
		 streetSufixes.put("POINT", "POINT");
		 streetSufixes.put("POINTS", "POINTS");
		 streetSufixes.put("PORT", "PORT");
		 streetSufixes.put("PORTS", "PORTS");
		 streetSufixes.put("PR", "PRAIRIE");
		 streetSufixes.put("PRAIRIE", "PRAIRIE");
		 streetSufixes.put("PRARIE", "PRAIRIE");
		 streetSufixes.put("PRK", "PARK");
		 streetSufixes.put("PRR", "PRAIRIE");
		 streetSufixes.put("PRT", "PORT");
		 streetSufixes.put("PRTS", "PORTS");
		 streetSufixes.put("PT", "POINT");
		 streetSufixes.put("PTS", "POINTS");
		 streetSufixes.put("RAD", "RADIAL");
		 streetSufixes.put("RADIAL", "RADIAL");
		 streetSufixes.put("RADIEL", "RADIAL");
		 streetSufixes.put("RADL", "RADIAL");
		 streetSufixes.put("RAMP", "RAMP");
		 streetSufixes.put("RANCH", "RANCH");
		 streetSufixes.put("RANCHES", "RANCH");
		 streetSufixes.put("RAPID", "RAPID");
		 streetSufixes.put("RAPIDS", "RAPIDS");
		 streetSufixes.put("RD", "ROAD");
		 streetSufixes.put("RDG", "RIDGE");
		 streetSufixes.put("RDGE", "RIDGE");
		 streetSufixes.put("RDGS", "RIDGES");
		 streetSufixes.put("RDS", "ROADS");
		 streetSufixes.put("REAR", "REAR");
		 streetSufixes.put("REST", "REST");
		 streetSufixes.put("RIDGE", "RIDGE");
		 streetSufixes.put("RIDGES", "RIDGES");
		 streetSufixes.put("RIV", "RIVER");
		 streetSufixes.put("RIVER", "RIVER");
		 streetSufixes.put("RIVR", "RIVER");
		 streetSufixes.put("RM", "ROOM");
		 streetSufixes.put("RNCH", "RANCH");
		 streetSufixes.put("RNCHS", "RANCH");
		 streetSufixes.put("ROAD", "ROAD");
		 streetSufixes.put("ROADS", "ROADS");
		 streetSufixes.put("ROUTE", "ROUTE");
		 streetSufixes.put("ROW", "ROW");
		 streetSufixes.put("RPD", "RAPID");
		 streetSufixes.put("RPDS", "RAPIDS");
		 streetSufixes.put("RST", "REST");
		 streetSufixes.put("RUE", "RUE");
		 streetSufixes.put("RUN", "RUN");
		 streetSufixes.put("RVR", "RIVER");
		 streetSufixes.put("SHL", "SHOAL");
		 streetSufixes.put("SHLS", "SHOALS");
		 streetSufixes.put("SHOAL", "SHOAL");
		 streetSufixes.put("SHOALS", "SHOALS");
		 streetSufixes.put("SHOAR", "SHORE");
		 streetSufixes.put("SHOARS", "SHORES");
		 streetSufixes.put("SHORE", "SHORE");
		 streetSufixes.put("SHORES", "SHORES");
		 streetSufixes.put("SHR", "SHORE");
		 streetSufixes.put("SHRS", "SHORES");
		 streetSufixes.put("SIDE", "SIDE");
		 streetSufixes.put("SKYWAY", "SKYWAY");
		 streetSufixes.put("SLIP", "SLIP");
		 streetSufixes.put("SMT", "SUMMIT");
		 streetSufixes.put("SPC", "SPACE");
		 streetSufixes.put("SPG", "SPRING");
		 streetSufixes.put("SPGS", "SPRINGS");
		 streetSufixes.put("SPNG", "SPRING");
		 streetSufixes.put("SPNGS", "SPRINGS");
		 streetSufixes.put("SPRING", "SPRING");
		 streetSufixes.put("SPRINGS", "SPRINGS");
		 streetSufixes.put("SPRNG", "SPRING");
		 streetSufixes.put("SPRNGS", "SPRINGS");
		 streetSufixes.put("SPUR", "SPUR");
		 streetSufixes.put("SPURS", "SPURS");
		 streetSufixes.put("SQ", "SQUARE");
		 streetSufixes.put("SQR", "SQUARE");
		 streetSufixes.put("SQRE", "SQUARE");
		 streetSufixes.put("SQRS", "SQUARES");
		 streetSufixes.put("SQU", "SQUARE");
		 streetSufixes.put("SQUARE", "SQUARE");
		 streetSufixes.put("SQUARES", "SQUARES");
		 streetSufixes.put("ST", "STREET");
		 streetSufixes.put("STA", "STATION");
		 streetSufixes.put("STATION", "STATION");
		 streetSufixes.put("STATN", "STATION");
		 streetSufixes.put("STE", "SUITE");
		 streetSufixes.put("STN", "STATION");
		 streetSufixes.put("STOP", "STOP");
		 streetSufixes.put("STR", "STREET");
		 streetSufixes.put("STRA", "STRAVENUE");
		 streetSufixes.put("STRAV", "STRAVENUE");
		 streetSufixes.put("STRAVE", "STRAVENUE");
		 streetSufixes.put("STRAVEN", "STRAVENUE");
		 streetSufixes.put("STRAVENUE", "STRAVENUE");
		 streetSufixes.put("STRAVN", "STRAVENUE");
		 streetSufixes.put("STREAM", "STREAM");
		 streetSufixes.put("STREET", "STREET");
		 streetSufixes.put("STREETS", "STREETS");
		 streetSufixes.put("STREME", "STREAM");
		 streetSufixes.put("STRM", "STREAM");
		 streetSufixes.put("STRT", "STREET");
		 streetSufixes.put("STRVN", "STRAVENUE");
		 streetSufixes.put("STRVNUE", "STRAVENUE");
		 streetSufixes.put("SUMIT", "SUMMIT");
		 streetSufixes.put("SUMITT", "SUMMIT");
		 streetSufixes.put("SUMMIT", "SUMMIT");
		 streetSufixes.put("TER", "TERRACE");
		 streetSufixes.put("TERR", "TERRACE");
		 streetSufixes.put("TERRACE", "TERRACE");
		 streetSufixes.put("THROUGHWAY", "THROUGHWAY");
		 streetSufixes.put("TPK", "TURNPIKE");
		 streetSufixes.put("TPKE", "TURNPIKE");
		 streetSufixes.put("TR", "TRAIL");
		 streetSufixes.put("TRACE", "TRACE");
		 streetSufixes.put("TRACES", "TRACE");
		 streetSufixes.put("TRACK", "TRACK");
		 streetSufixes.put("TRACKS", "TRACK");
		 streetSufixes.put("TRAFFICWAY", "TRAFFICWAY");
//		 streetSufixes.put("TRA", "TRAIL");
		 streetSufixes.put("TRAIL", "TRAIL");
		 streetSufixes.put("TRAILS", "TRAIL");
		 streetSufixes.put("TRAK", "TRACK");
		 streetSufixes.put("TRCE", "TRACE");
		 streetSufixes.put("TRFY", "TRAFFICWAY");
		 streetSufixes.put("TRK", "TRACK");
		 streetSufixes.put("TRKS", "TRACK");
		 streetSufixes.put("TRL", "TRAIL");
		 streetSufixes.put("TRLR", "TRAILER");
		 streetSufixes.put("TRLS", "TRAIL");
		 streetSufixes.put("TRNPK", "TURNPIKE");
		 streetSufixes.put("TRPK", "TURNPIKE");
		 streetSufixes.put("TUNEL", "TUNNEL");
		 streetSufixes.put("TUNL", "TUNNEL");
		 streetSufixes.put("TUNLS", "TUNNEL");
		 streetSufixes.put("TUNNEL", "TUNNEL");
		 streetSufixes.put("TUNNELS", "TUNNEL");
		 streetSufixes.put("TUNNL", "TUNNEL");
		 streetSufixes.put("TURNPIKE", "TURNPIKE");
		 streetSufixes.put("TURNPK", "TURNPIKE");
		 streetSufixes.put("UN", "UNION");
		 streetSufixes.put("UNDERPASS", "UNDERPASS");
		 streetSufixes.put("UNION", "UNION");
		 streetSufixes.put("UNIONS", "UNIONS");
		 streetSufixes.put("UNIT", "UNIT");
		 streetSufixes.put("UPPR", "UPPER");
		 streetSufixes.put("VALLEY", "VALLEY");
		 streetSufixes.put("VALLEYS", "VALLEYS");
		 streetSufixes.put("VALLY", "VALLEY");
		 streetSufixes.put("VDCT", "VIADUCT");
		 streetSufixes.put("VIA", "VIADUCT");
		 streetSufixes.put("VIADCT", "VIADUCT");
		 streetSufixes.put("VIADUCT", "VIADUCT");
		 streetSufixes.put("VIEW", "VIEW");
		 streetSufixes.put("VIEWS", "VIEWS");
		 streetSufixes.put("VILL", "VILLAGE");
		 streetSufixes.put("VILLAG", "VILLAGE");
		 streetSufixes.put("VILLAGE", "VILLAGE");
		 streetSufixes.put("VILLAGES", "VILLAGES");
		 streetSufixes.put("VILLE", "VILLE");
		 streetSufixes.put("VILLG", "VILLAGE");
		 streetSufixes.put("VILLIAGE", "VILLAGE");
		 streetSufixes.put("VIS", "VISTA");
		 streetSufixes.put("VIST", "VISTA");
		 streetSufixes.put("VISTA", "VISTA");
		 streetSufixes.put("VL", "VILLE");
		 streetSufixes.put("VLG", "VILLAGE");
		 streetSufixes.put("VLGS", "VILLAGES");
		 streetSufixes.put("VLLY", "VALLEY");
		 streetSufixes.put("VLY", "VALLEY");
		 streetSufixes.put("VLYS", "VALLEYS");
		 streetSufixes.put("VST", "VISTA");
		 streetSufixes.put("VSTA", "VISTA");
		 streetSufixes.put("VW", "VIEW");
		 streetSufixes.put("VWS", "VIEWS");
		 streetSufixes.put("WALK", "WALK");
		 streetSufixes.put("WALKS", "WALKS");
		 streetSufixes.put("WALL", "WALL");
		 streetSufixes.put("WAY", "WAY");
		 streetSufixes.put("WA", "WAY");
		 streetSufixes.put("WAYS", "WAYS");
		 streetSufixes.put("WELL", "WELL");
		 streetSufixes.put("WELLS", "WELLS");
		 streetSufixes.put("WLS", "WELLS");
		 streetSufixes.put("WY", "WAY");
		 streetSufixes.put("XING", "CROSSING");

		directions.put("NORTH", "N");
		directions.put("N", "N");
		//directions.put("NO", "N");

		directions.put("SOUTH", "S");
		directions.put("S", "S");
		//directions.put("SO", "S");

		directions.put("EAST", "E");
		directions.put("E", "E");
		directions.put("WEST", "W");
		directions.put("W", "W");
		
		directions.put("NORTHEAST", "NE");
		directions.put("NE", "NE");
		directions.put("SOUTHEAST", "SE");
		directions.put("SE", "SE");
		directions.put("NORTHWEST", "NW");
		directions.put("NW", "NW");
		directions.put("SOUTHWEST", "SW");
		directions.put("SW", "SW");
		
		all.putAll(directions);
		all.putAll(streetSufixes);
		
		directionAbbreviations.put("N", "NORTH");
		directionAbbreviations.put("S", "SOUTH");
		directionAbbreviations.put("E", "EAST");
		directionAbbreviations.put("W", "WEST");
		
		directionAbbreviations.put("NE", "NORTHEAST");
		directionAbbreviations.put("SE", "SOUTHEAST");
		directionAbbreviations.put("NW", "NORTHWEST");
		directionAbbreviations.put("SW", "SOUTHWEST");
		
	}
	
	public static String getNormalForm (String s){
		s = normalForm(s);
		if (s.equals(""))
			return s;
		if (isAbbreviation(s)){
			return (String) all.get( s);
		}else{
			throw new IllegalArgumentException ("[" + s + "] is not a valid Street Abreviation");
		}
	}
	
	private static String normalForm (String s){
		s = s.toUpperCase();
		if (s.endsWith(".")){
			s = s.substring(0, s.length()-1);
		}
		return s;		
	}

    public static boolean containsAbbreviation(String s) {
        s=s.toUpperCase();
        Iterator it=streetSufixes.keySet().iterator();
        while (it.hasNext()) {
            String suf=(String)it.next();
            if (Pattern.compile("\\b"+suf+"\\b").matcher(s).find())
                return true;
        }
        return false;
    }
    
    public static String detectAbbreviation(String s) {
        s=s.toUpperCase();
        Iterator it=streetSufixes.keySet().iterator();
        while (it.hasNext()) {
            String suf=(String)it.next();
            if (Pattern.compile("\\b"+suf+"\\b").matcher(s).find())
                return suf;
        }
        return "";
    }
    
    public static String detectFirstEncounteredAbbreviation(String s) {
    	Map streetsufixes2 = streetSufixes;
        return detectFirstEncounteredAbbreviation(s, streetsufixes2);
    }

	/**
	 * @param s
	 * @param streetsufixes2
	 * @return
	 */
	public static String detectFirstEncounteredAbbreviation(String s, Map streetsufixes2) {
		s=s.toUpperCase();
        String[] split = s.split("\\s");
        for (String string : split) {
			String suf = (String) streetsufixes2.get(string);
			if (StringUtils.isNotEmpty(suf)){
				return string;
			}
		}
        return "";
	}
	
	public static String detectLastEncounteredAbbreviation(String s, Map streetsufixes2) {
		s=s.toUpperCase();
        String[] split = s.split("\\s");
        String lastSuffix = "";
        for (String string : split) {
			String suf = (String) streetsufixes2.get(string);
			if (StringUtils.isNotEmpty(suf)){
				lastSuffix = string;
			}
		}
        return lastSuffix;
	}
    
    public static String removeAbbreviation(String s) {
        s=s.toUpperCase();
        Iterator it=streetSufixes.keySet().iterator();
        boolean abrevNotFound = true;
        String regEX = "";
		while (it.hasNext() && abrevNotFound ) {
            String suf=(String)it.next();
            regEX = "\\b"+suf+"\\b";
			if (Pattern.compile(regEX).matcher(s).find())
                abrevNotFound = false;
			
        }
		if (!abrevNotFound){
			s = s.replaceAll(regEX, ""); 
		}
        return s;
    }
    
    public static String detectDirection(String s) {
        s=s.toUpperCase();
        Iterator it= directions.keySet().iterator();
        boolean abrevNotFound = true;
        String regEX = "";
        String sufRet = "";
		while (it.hasNext() && abrevNotFound ) {
            String suf=(String)it.next();
            regEX = "\\b"+suf+"\\b";
			if (Pattern.compile(regEX).matcher(s).find()){
				abrevNotFound = false;
				sufRet = suf;
			}
        }
        return sufRet;
    }
    
    public static String removeDirection(String s) {
        s=s.toUpperCase();
        Iterator it= directions.keySet().iterator();
        boolean abrevNotFound = true;
        String regEX = "";
		while (it.hasNext() && abrevNotFound ) {
            String suf=(String)it.next();
            regEX = "\\b"+suf+"\\b";
			if (Pattern.compile(regEX).matcher(s).find())
                abrevNotFound = false;
			
        }
		if (!abrevNotFound){
			s = s.replaceAll(regEX, ""); 
		}
        return s;
    }
	
	public static boolean isAbbreviation(String s){
		s = normalForm(s);
		boolean isAbrev = all.containsKey(s);
		//logger.debug (s + " is abrev: " + isAbrev); 
		return (isAbrev);
	}

	public static boolean isDirection(String s){
		s = normalForm(s);
		boolean isAbrev = directions.containsKey(s);
		//logger.debug (s + " is abrev: " + isAbrev); 
		return (isAbrev);
	}


	public static boolean isStreetSufix(String s){
		s = normalForm(s);
		boolean isAbrev = streetSufixes.containsKey(s);
		//logger.debug (s + " is abrev: " + isAbrev); 
		return (isAbrev);
	}


	public static List getAllAbbrevs(String s){
		return getAllAbbrevs(s, true);
	}
	
	public static List getAllDirections(){
		List values = new ArrayList(new HashSet(directions.values()));
		Collections.sort(values);
		return values;
 
	}
	
	public static List getAllDirectionsValues(){
		return getShortestKeys(directions);
	}
	
	public static List getAllStreetSuffixes(){
		List values = new ArrayList(new HashSet(streetSufixes.values()));
		Collections.sort(values);
		return values;
	}
	
	public static List getAllStreetSuffixesValues(){
		return getShortestKeys(streetSufixes);
	}

		
	public static List getShortestKeys(Map h){
		
		HashMap shortestNames = new HashMap();
		
		for (Iterator iter = h.keySet().iterator(); iter.hasNext();) {
			String variableForm = (String) iter.next();
			String standardForm = (String) h.get(variableForm);
		
			if (!shortestNames.containsKey( standardForm)){
				shortestNames.put( standardForm,variableForm);
			}else{
				String storedValue = (String) shortestNames.get(standardForm); 
				if (storedValue.length() > variableForm.length()){
					shortestNames.put( standardForm,variableForm);
				}
			}
		}

		List values = new ArrayList(shortestNames.values());
		Collections.sort(values);
		return values;
	}

	

	public static List getAllAbbrevs(String s, boolean shortestOnly){
		String normalized = normalForm(s);
		List l = new ArrayList();
		if (isAbbreviation(normalized)){
			String val = (String) all.get(normalized);  
			l = getAllAbrevForValue(val);
			if (shortestOnly){
				filterAbrev(l);
			}
			if (!l.contains(val)){
				l.add(0, val);
			}
		}else{
			l.add(s); 
		}
		//l.add("");
		return l;
	}


	/**
	 * @param l
	 */
	private static void filterAbrev(List l) {
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (isSupraString(element, l)){
				iter.remove();
			}
		}
	}

	/**
	 * @param element
	 * @param l
	 * @return
	 */
	private static boolean isSupraString(String supraString, List l) {
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (!supraString.equals(element) && (supraString.indexOf(element)!=-1)){
				return true;
			}
		}
		return false;
	}

	private static List getAllAbrevForValue(String val){
		List l = new ArrayList();
		for (Iterator iter = all.keySet().iterator(); iter.hasNext();) {
			String abrev = (String) iter.next();
			String value = (String) all.get(abrev);
			if (value.equals(val)){
				//logger.debug("value " + value); 
				l.add(abrev);
				//logger.debug("abr " + abrev); 
				//if (!abrev.equals(value) && !abrev.equals(value+"S")&& !abrev.equals(value+"ES")){
				//	l.add(abrev + ".");
					//logger.debug("abr " + abrev + "."); 
				//}
			}
		}
		Collections.sort(l);
		return l;
	}
	/**
	 * returns a copy iof the standard defined suffixes
	 * @return
	 */
	public static Map getStreetsufixes() {
		return new HashMap(streetSufixes);
	}
	
	public static String getFullSuffixFromAbbreviation(String abbreviation) {
		String result = (String)streetSufixes.get(abbreviation);
		if (result==null) {
			result = abbreviation;
		}
		return result;
	}
	
	public static String getFullDirectionFromAbbreviation(String abbreviation) {
		String result = directionAbbreviations.get(abbreviation);
		if (result==null) {
			result = abbreviation;
		}
		return result;
	}
	
	public static void main(String[] args) {
		logger.info (getAllAbbrevs("N"));
		logger.info (getAllAbbrevs("N", false));
		logger.info (getAllAbbrevs("N."));
		logger.info (getAllAbbrevs("North"));
		logger.info (getAllAbbrevs("Dr."));
		logger.info (getAllAbbrevs("Dr.", false));
		logger.info (getAllAbbrevs("av"));
		logger.info (getAllAbbrevs("av", false));
	}
}
