package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class TXGenericFunctionsNTN {
	
	
	public static ResultMap parseLegal(ResultMap resultMap, Search search){
		
		if (search == null){
			return resultMap;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		String crtState = search.getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
		
		if ("TX".equals(crtState)){

			switch (countyId) {
			case CountyConstants.TX_Angelina:
				try {
					//0004 BARR & DAVP, TRACT 320.1, ACRES 12.49
					parseLegalAngelina(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Atascosa:
				try {
					//ABS A00324 J H GIBSON SV-79,11. ACRES
					//LOTS 1,2,3 BLK 113 CHARLOTTE
					parseLegalAtascosa(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Bandera:
				try {
					//ABST 133 E P I CO SVY 1263.5 TR 4 160 ACRES
					//CYPRESS FALLS LT 16 5.095
					parseLegalBandera(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
				
			case CountyConstants.TX_Bastrop:
				try {
					//AVALON A BLK 63 LT 4-10 14-20 0.796 ACRES
					parseLegalBastrop(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Bell:
				try {
					//MILL CREEK SECTION 15, BLOCK 003, LOT 0003
					parseLegalBell(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Bexar:
				try {
					//NCB 1167 BLK E 1/2 G LOT W 50 FT OF 8
					parseLegalBexar(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Bowie:
				try {
					//BEAVER LAKE ESTATES LOT 25 6034/065  05/04/11 BLK/TRACT 1 1.984 ACRES
					parseLegalBowie(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Brazoria:
				//OUTLOTS & TOWNSITE (BRAZORIA), BLOCK 171, LOT 5-6-7-8
				parseLegalBrazoria(resultMap);
				break;
				
			case CountyConstants.TX_Brazos:
				//PECAN RIDGE PH 1, BLOCK 4, LOT 4 (PT OF)#####
				parseLegalBrazos(resultMap);
				break;
				
			case CountyConstants.TX_Brooks:
				//12 1 MONROE & ADAMS
				//AB #97 J M & L CHAPA LT 2 SGIAN-DUBH S/D
				parseLegalBrooks(resultMap);
				break;
			
			case CountyConstants.TX_Burnet:
				try {
					//S8150 SUNSET OAKS LOT H-8 BLK 1
					parseLegalBurnet(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Cameron:
				try {
					//TREASURE HILLS SUBDIVISION 5 LOT 29 BLK 3
					//HARLINGEN- PARKSIDE ESTATES-3 LOT 51
					parseLegalCameron(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Chambers:
				try {
					//BK 19 LT 3 FOX WINNIE
					//1 - 2 10 ANAHUAC
					parseLegalChambers(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Collin:
				try {
					//BRIARMEADE #1 (CPL), BLK C, LOT 4
					parseLegalCollin(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Cooke:
				try {
					//CUNNINGHAM ADDN, BLOCK 3, LOT 10 & PT OF LT 9, 1501 E BROADWAY
					parseLegalCooke(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Coryell:
				try {
					//HOUSE CREEK NORTH PHASE 2, BLOCK 1, LOT 24
					//12    2 COLONIAL PARK 9
					parseLegalCoryell(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Dallas:
				try {
					//GANO & EAKINS BLK 1/857 PT LT 4 71.4X79X50X28 CHESTNUT VOL 91166/2806 EX082091 CO-DALLAS 0857 001   00400      1DA0857 001
					parseLegalDallas(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Denton:
				try {
					//Denton COLONY NO 21 BLK 150 LOT 38
					parseLegalDenton(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Ector:
				try {
					//LOT 2  BLK 5  FAIR OAKS
					//HERBERT WIGHT BLOCK 42A
					parseLegalEctor(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_El_Paso:
				try {
					//3 CANUTILLO HEIGHTS UNIT 1 LOT 6 (10794.00 SQ FT)
					//BLK 29 HORIZON MESA #6 LOT 10
					parseLegalElPaso(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Ellis:
				try {
					//7R 1 CARLTON EST #2 1.13 ACRES
					//15 C LA VISTA ESTS PH 3     0.759 ACRES
					parseLegalEllis(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Erath:
				try {
					//S5660 RIVER NORTH II ADDITION, BLOCK 21, LOT 4
					parseLegalErath(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Fort_Bend:
				try {
					//CANYON GATE AT THE BRAZOS SEC 3, BLOCK 3, LOT 28
					//0002 T ALLSBERRY, TRACT 29, ACRES 1.0045, OAK FOREST ACRES
					parseLegalFortBend(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Galveston:
				try {
					//ABST 20 PAGE 3 LOT 37 BLK 12 ANNALEA KINGSPARK SEC B & WHITEHALL SEC B
					parseLegalGalveston(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Grayson:
				try {
					//CLASSICS SUBDIVISION, BLOCK 1, LOT PT 3 & 4, ACRES .283
					//G-1075 STEWART HENRY A-G1075, ACRES 6.0
					parseLegalGrayson(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Guadalupe:
				try {
					//BENTWOOD RANCH UNIT #9, BLOCK 19, LOT 41
					//LOT: 15 BLK: 3 ADDN: ASHLEY PLACE UNIT 1 LN
					parseLegalGuadalupe(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Hardin:
				//AB 7 MM BRADLEY PARCEL 7-6-A 5.01 ACRES
				//861 /414 LOT 14 VINSON LOTS AB 14 E DUNCAN PARCEL 14-93-O 0.34 ACRES
				parseLegalHardin(resultMap);
				break;
				
				
			case CountyConstants.TX_Harris:
				//LT 10 BLK 18 GLEN IRIS SEC 3
				parseLegalHarris(resultMap);
				break;
				
			case CountyConstants.TX_Hays:
				//CHANDLER ADDN, BLOCK 1, LOT 9 & PT OF 8
				//CIMARRON ACRES LOT 1   0.34 AC
				parseLegalHays(resultMap);
				break;
				
			case CountyConstants.TX_Henderson:
				//AB 485 I V MICHELLI SUR  TR 118A (RE: PT TR 2)
				//AB 59 J P BROWN SUR  BONANZA BEACH  BLK 1 LTS 26-32
				parseLegalHenderson(resultMap);
				break;
				
			case CountyConstants.TX_Hidalgo:
				//PALM HEIGHTS LOT 5 BLK 2
				//SIESTA VILLAGE #1 LOT 27 BLOCK 4
				parseLegalHidalgo(resultMap);
				break;
				
			case CountyConstants.TX_Hood:
				//LOT 176 BLOCK 2 OAK TRAIL SHORES SEC F
				//LOT 2501  PECAN PLANTATION UN 17
				parseLegalHood(resultMap);
				break;
				
			case CountyConstants.TX_Hunt:
				try {
					//A1183 YOUNG HENRY,TRACT 27, ACRES 2.8837
					//BELLAVISTA DOS ADDITION, LOT 66
					parseLegalHunt(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Jefferson:
				try {
					//BEAUMONT WEST #5 L21 B3 1980 BRYANT WAY
					//LT 16 BLK 10 BELLAIRE 2 4211 CHARLOTTE DR
					parseLegalJefferson(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Jim_Hogg:
				try {
					//11,12 50 KOEHLER
					//8 124 KOEHLER SEC A
					parseLegalJimHogg(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Jim_Wells:
				try {
					//SAGEWOOD TWNHOUSE UN #1B LT 23 UNIT 1-B 0000.000
					parseLegalJimWells(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Johnson:
				try {
					//LOT 19 BLK 18 OAK VALLEY ESTATES PH 6,7
					//ABST 163 TR 6F W J CULVERHOUSE
					parseLegalJohnson(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Kaufman:
				try {
					//BLUFF VIEW EST #2 BLOCK C LOT 15
					//CLUB LAKE EST BLOCK 27 PT
					parseLegalKaufman(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Kerr:
				try {
					//ABS A0708 SCHELLHASE, SUR 651,ACRES .21
					//BUENA VISTA BLK 2 LOT 15 & PT 16
					parseLegalKerr(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_La_Salle:
				try {
					//LOT 15 BLOCK 10 COLONIA JIMENEZ
					//L 9 BLK 19 SPOHN ENC
					parseLegalLaSalle(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Liberty:
				try {
					//BIG THICKET LAKE ESTATES, LOT 215-218, SEC 18, ACRES .8152
					//000354 B TARKINGTON, TRACT 92, ACRES 7.7253
					parseLegalLiberty(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Lubbock:
				try {
					//ALFORD TERRACE L 122 & N25'OF 123
					//SUNNY SLOPE BLK 7 L 6
					parseLegalLubbock(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Maverick:
				try {
					//HOLLY PARK UNIT # 3, BLOCK 5, LOT 11
					parseLegalMaverick(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_McLennan:
				try {
					//BELLMEAD CT Block 14 Lot 9
					//ABSTRACT 0346.00S22 FRAZIER W W, ACRES 32.9
					parseLegalMcLennan(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Medina:
				try {
					//Medina GERONIMO FOREST, BLOCK 8, LOT 9
					parseLegalMedina(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Midland:
				try {
					//SEC:  7 SE/4  SURV: H P HILLIARD  BLK:  X
					//BLK:  008  LOT:  006  ADDN: BARBERDALE SEC 2
					parseLegalMidland(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Montgomery:
				try {
					//Montgomery A0062 AMELUNG LOUIS F NO 1, TRACT 4 LT 12, ACRES 2.570
					parseLegalMontgomery(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Navarro:
				try {
					//CORSICANA OT 452W 4
					//LOT: C-2 & N PT OF D BLOCK: 449 SUBD: CORSICANA OT
					//A10655 T J PALMER ABST 8
					parseLegalNavarro(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Nueces:
				try {
					//Nueces BAYSIDE ACRES 2 LTS 13 ,14 & 15 BK 5
					parseLegalNueces(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Orange:
				try {
					//ABST. 15  J. JETT (LOT PART OF 34 & 36 BLANDALE)
					//LOT 12  BLK 1 BON AIR COURTS
					//ABST. 384  W. C. SHARP  TR 085
					parseLegalOrange(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Palo_Pinto:
				try {
					//AB 431 TR 12-A  GW TREIGHENBLK 12G#10431-00-0012 -00A-00-0LII
					//CLIFFS PHASE VII LOT 20 (AKA LOTS 19&20) G#C0250-07-00000-020-00-0   CLIFFS PHASE VII
					//PK LAKE 8-1-93 AREA 2-1 LOT 76 (FEE SIMPLE .650 AC & FERC .110 AC)  G#P0500-00-02010-076-00-0  DOCK #7224 & #10910/N DLS0087032   PK LAKE
					parseLegalPaloPinto(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Panola:
				try {
					//WESTERN HILLS LT 3
					//JONES ADDITION, BLOCK 2, LOT 5, SECT 2
					//AB 133 J COATS LOT 14 ACRES 1.50
					//BLK 94-A LT 1,PT LT 2 P-1
					parseLegalPanola(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Parker:
				try {
					//LOT:  9, BLK:  1, ADDN: BIG TIMBERS   BIG TIMBERS
					//19 C BOLING RANCH EST
					parseLegalParker(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Potter:
				try {
					//HAMLET # 4 CORR LOT 003     BLOCK 0036
					//BEVERLY GARDENS LOT         BLOCK 0001 S 1/2 OF 2 LESS E 10FT ROW
					parseLegalPotter(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Randall:
				try {
					//SLEEPY HOLLOW #6 AMD LOT         BLOCK 0015 CREIGHTON PLACE 20 EXC S 4FT
					//SOUTH LAWN # 4 LOT 017     BLOCK 016DW
					//RIDGECREST ADDN # 16 LOT         BLOCK 0053 SE 12FT OF 12 & 13 LESS SE 8.8FT
					parseLegalRandall(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Rockwall:
				try {
					//HIDDEN VALLEY EST #2, LOT 17, ACRES 5.016
					//A0022 J H BAILEY,TRACT 1-2,.821 ACRES
					parseLegalRockwall(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_San_Patricio:
				try {
					//LTS 1,2 BLK 264 ARANSAS PASS, 0.321 ACRES
					parseLegalSanPatricio(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Smith:
				try {
					//BRIARWOOD BRIARWOOD   LT 5 BL 1005F
					//ABST A0003 M D L CARMONA TRACT 129B   TR 129B (PT OF 4AC TR SEE TR129B.1)
					parseLegalSmith(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Somervell:
				try {
					//REYNOLDS BEND ADDITION, LOT 10, ACRES .257   R0800
					//A136 MILAM CO SCH LD, TRACT E3-10, ACRES .24    E3-10 A136
					parseLegalSomervell(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Tarrant:
				try {
					//ANDERSON, FRANK M SUBDIVISION   BLK   1   LOT   A               96.25% UNDIVIDED INTEREST       *00038652*
					parseLegalTarrant(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Taylor:
				try {
					//ELMWOOD WEST SEC 1, BLOCK D, LOT E65 OF 6 & W20 OF 7
					//HAMILTON HEIGHTS SEC 1, BLOCK D, LOT 27
					parseLegalTaylor(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Tom_Green:
				try {
					//LOT 31 BLK 6 ARONS GLEN ADDITION
					//LOT 19 BLK 4-E CHAPMAN S/D
					parseLegalTomGreen(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Travis:
				try {
					//LOT 27 BLK C LOST CREEK AT GAINES RANCH SUBD REPLAT
					//50% OF LOT 29-31 BLK 2 BON AIR KNOLLS***UNDIVIDED INTEREST ACCOUNT***
					parseLegalTravis(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Van_Zandt:
				try {
					//ABST: 74, SUR:  J BAYES
					//BLK:  4, LOT:  8, ADDN: TOWN & COUNTRY
					//BLK:  4, LOT:, ADDN: LL5  CANTON
					parseLegalVanZandt(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Victoria:
				try {
					//MAYFAIR I LOT 14 BLOCK 2
					//ABSTRACT 01690 C DE LA GARZA ABST 169, ACRES 1.
					//00920 ALEJO PEREZ ABST 92, ACRES 1.806
					parseLegalVictoria(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Waller:
				try {
					//A047600 A-476 JOHN IVY, TRACT 1, ACRES 60.45
					parseLegalWaller(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Webb:
				try {
					//UNIT 2 BLDG A VILLAS PLAZA CONDOS & UND 7.14% INT OUT LOT 3 & 4 BLK 849 ED
					//LOS PRESIDENTES, BLOCK 3, LOT 3, UNIT 12
					parseLegalWebb(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Wichita:
				try {
					//LOT 24 BLK 2 STONE LAKE ESTATES PHASE 2
					//LOT 12 BLK 10 BRIDGE CREEK EST SEC 3
					parseLegalWichita(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Williamson:
				try {
					//DOAK ADDITION, BLOCK 53, LOT 4
					//AW0131 COURSEY, P. SUR., ACRES 1.0
					//GARDEN PARK SEC 1, BLOCK A, LOT 8, ACRES 0.750
					parseLegalWilliamson(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			
			case CountyConstants.TX_Wilson:
				try {
					//KOTHMANN SUB, LOT 11NB (E 1/2) & 12NA (W 1/2), ACRES 5.0
					//A0022 M XIMENEZ SUR, TRACT 13A, ACRES 15.0
					parseLegalWilson(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Wise:
				try {
					//7R1  GRAND OAKS ESTATES
					//7 1 FOREST HILL ADDITION  A-627 MEP & PRR
					parseLegalWise(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case CountyConstants.TX_Zapata:
				try {
					//LOPENO TOWNSITE (SANCHEZ), BLOCK 15, LOT 2
					//LOTS 15 & 16 LAKEVIEW SUBDIVISION BLOCK 164
					//ABST 151 PORC 202 A GARCIA CHARCO REDONDO GRANT
					parseLegalZapata(resultMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		
		}
		
		return resultMap;
	}

	public static void parseLegalBrazoria(ResultMap resultMap){
		//OUTLOTS & TOWNSITE (BRAZORIA), BLOCK 171, LOT 5-6-7-8
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll(
						"(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\bPT\\b", "");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		//String legalTemp = legal;

		Pattern p = Pattern.compile("(?is)\\b(Abst:?|AB\\s+)?\\s*(A?[\\d-]{3,})\\b");
		legal = extractAbst(resultMap, legal, p);
		legal = legal.replaceAll("null", "").replaceAll("\\s+"," ");

		p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([-&\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b((?:BLO?C?K?:?)|(?:BK:?))\\s+([[A-Z]|\\d]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+([[A-Z-]|\\d]+)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)(.*?)\\s+(S/D|SEC|,|PH|UNIT|BLO?C?K|LO?T|TR(?:ACT)?S?)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)([^,]+)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst(",", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
			
		}
	}
	
	public static void parseLegalHarris(ResultMap resultMap){
		//LT 10 BLK 18 GLEN IRIS SEC 3
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll(
						"(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\bPT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\.\\d]+\\s*INT\\b", "");

		//legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		//String legalTemp = legal;

		Pattern p = Pattern.compile("(?is)\\b(Abst:?|AB\\s+)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([-& \\d]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b((?:BLO?C?K?:?)|(?:BK:?))\\s+([[A-Z]|\\d]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+([[A-Z-]|\\d ]+(?:(?:\\s+[[A-Z-]|\\d]+)?\\s*&\\s*[[A-Z-]|\\d]+)?)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UNI?T?)\\s+(\\d+)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(UNI?T?|LTS?|BLK|BLDG|ABST|TRS)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)([^,]+)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?)BLK\\b", "");
			subdiv = subdiv.replaceFirst("(.*?)ABST\\b", "");
			subdiv = subdiv.replaceFirst("(.*?)BLDG\\b", "");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s*(\\d*)\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\s+AMEND", " ");
			subdiv = subdiv.replaceFirst(",", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("^\\.", " ");
						
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
	}
	
	public static void parseLegalBexar(ResultMap resultMap) throws Exception{
		//NCB 1167 BLK E 1/2 G LOT W 50 FT OF 8
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,\\s-]+|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		String[] plats = legal.split("\\bCB\\b");
		String pb = "";
		String pg = "";
		for (String eachBook : plats){
			if (StringUtils.isNotEmpty(eachBook)){
				eachBook = "CB " + eachBook;
				p = Pattern.compile("(?is)\\b(CB)\\s*(\\d+[A-Z]?)\\s*(P\\s*-?)\\s*(\\d+[A-Z]?)");
				Matcher ma = p.matcher(eachBook);
				if (StringUtils.isNotEmpty(pb.trim())){
					pb += ";";
				}
				if (ma.find()){
					pb = pb + " " + ma.group(2).trim();
				}
				if (StringUtils.isNotEmpty(pg.trim())){
					pg += ";";
				}
				p = Pattern.compile("(?is)\\b([P|A]\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				while (ma.find()){
					pg = pg + " " + ma.group(2).trim();
				}
			}
		}
		if (StringUtils.isNotEmpty(pb) && StringUtils.isNotEmpty(pg)){
			//resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
			//resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());
		} else {		
			// extract ncb from legal description
			p = Pattern.compile("(?is)\\b(N?CB)\\s*([\\d]+[A-Z]?)\\b");
			legal = extractNcb(resultMap, legal, p);
		}
		
		p = Pattern.compile("(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*-?\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
			
		// extract subdivision name from legal description
		String subdiv = "";
		boolean hasSub = false;
		p = Pattern.compile("(?is)\\(([A-Z]+[^\\)]+)\\)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
			hasSub = true;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LOTS?\\s*:?|BLDG|UNIT)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
				hasSub = true;
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\\"([A-Z]+[^\\\"]+)\\\"");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
					hasSub = true;
				}
			}
		} 
		if (!hasSub | subdiv.matches("\\w\\s+\\w\\s+\\w+.*")) {
			subdiv = "";
		}

		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?).*", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\sFLG.*", "$1")
					.replaceFirst("(.+)\\sREFER.*", "$1")
					.replaceFirst(",", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ")
					.replaceAll("\\\"+", "")
					.replaceAll("(?is)\\s*@\\s*", " at ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
		
	}

	public static void parseLegalDallas(ResultMap resultMap) throws Exception{
		//GANO & EAKINS BLK 1/857 PT LT 4 71.4X79X50X28 CHESTNUT VOL 91166/2806 EX082091 CO-DALLAS 0857 001   00400      1DA0857 001
		
		//Nueces BAYSIDE ACRES 2 LTS 13 ,14 & 15 BK 5
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		String originalLegal = legal;// i need the <br>

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			if (ma.group(1).trim().length()>=2) {		//at least one of O, T and S from LO?T?S? must be present
				lot = lot + " " + ma.group(2);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\s");
		legal = extractTract(resultMap, legal, p);

		// extract cross refs from legal description
		@SuppressWarnings("rawtypes")
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(REFER\\s*(?:TO)?\\s*:?)\\s*([\\d-\\s]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2).replaceAll("(?is)[\\s-]+", ""));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		ma.reset();
		p = Pattern.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:EX)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern.compile("(?is)\\bINT\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1).trim());
			line.add(ma.group(2).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber", "InstrumentDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			resultMap.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		legal = legal.replaceAll("", "");

		// extract subdivision name from legal description
		String subdiv = "";
		
		originalLegal = originalLegal.replaceFirst("(?is)\\A\\s*<\\s*/?\\s*br\\s*/?\\s*>\\s*", "");
		originalLegal = originalLegal.replaceAll("(?is)\\b([X\\d\\s,\\.'/]+)\\s+OUT\\sOF\\b", "");
		
		originalLegal = GenericFunctions.switchIdentifierWithNumber(originalLegal, "SEC(?:TION)?");
		String[] lines = originalLegal.split("(?is)<\\s*/?\\s*br\\s*/?\\s*>");
		
		if (lines.length > 0) {
			subdiv = lines[0];
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
			subdiv = subdiv.replaceFirst("\\sASSESSORS MAP.*", "");
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}

	}
	
	public static void parseLegalDenton(ResultMap resultMap) throws Exception{
		parseLegalTarrant(resultMap);
	}
	public static void parseLegalFortBend(ResultMap resultMap) throws Exception{
		//CANYON GATE AT THE BRAZOS SEC 3, BLOCK 3, LOT 28
		//0002 T ALLSBERRY, TRACT 29, ACRES 1.0045, OAK FOREST ACRES
		
		//Montgomery A0062 AMELUNG LOUIS F NO 1, TRACT 4 LT 12, ACRES 2.570
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\AA?(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\A\\s*-\\s*", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalGalveston(ResultMap resultMap) throws Exception{
		//ABST 20 PAGE 3 LOT 37 BLK 12 ANNALEA KINGSPARK SEC B & WHITEHALL SEC B
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		
		legal = legal.replaceAll("(?is)\\([\\d-]+\\)+", "");
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\b(OUT)(LOT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\bPT\\s+OF(\\s+OUT)?\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		legal = extractBlock(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\s");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST)\\s*(\\d+)(\\s+PAGE\\s+[\\d&]+)?\\b");
		legal = extractAbst(resultMap, legal, p);
		
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.+)\\s+ABST\\b.*");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)(LOTS?\\s*:?|BLK|BLDG|UNIT)\\s+(.*)(\\s+SUR|$)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\\"([A-Z]+[^\\\"]+)\\\"");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		} 
		
		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.trim().length() > 2) {

			subdiv = subdiv.replaceFirst("(?is)\\A\\s*[\\d-\\(\\)]+", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*LOTS?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*BLKS?\\b", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}

	public static void parseLegalJefferson(ResultMap resultMap){
		//BEAUMONT WEST #5 L21 B3 1980 BRYANT WAY
		//LT 16 BLK 10 BELLAIRE 2 4211 CHARLOTTE DR
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)([\\d-]+)([A-Z])", "$1 $2");
		
		legal = legal.replaceAll("(?is)\\b(L)\\s*(\\d)", "$1OT $2");
		legal = legal.replaceAll("(?is)\\b(B)\\s*(\\d)", "$1LOCK $2");
		
		legal = legal.replaceAll("(?is)\\([\\d-]+\\)+", "");
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\b(OUT)(LOT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		
		legal = legal.replaceAll("(?is)\\bALL\\s+OF\\b", "");
		//NW41X91 FT
		legal = legal.replaceAll("(?is)\\b([SWNE]+|FRONT)\\s*[\\d\\s,\\.'/]+(\\s*X\\s*[\\d\\s,\\.'/]+)?(?:\\s*F\\s*T)?(\\s*O\\s*F)?\\b", "");
		legal = legal.replaceAll("(?is)\\bPT\\s+OF(\\s+OUT)?\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers


		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BL?\\s*(?:OC)?KS?\\s*[:|\\.]?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b");
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		legal = extractUnit(resultMap, legal, p);

		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\s");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABST)\\s*(\\d+)(\\s+PAGE\\s+[\\d&]+)?\\b");
		legal = extractAbst(resultMap, legal, p);

		// extract subdivision name from legal description
		legal = legal.replaceAll("(?is)\\bLT\\s*&\\s*LT\\b", "LT");
		String subdiv = "";
		p = Pattern.compile("(?is)(.+)\\s+(PHASE|L(?:O?T)?S?(?:\\s+BLK)?|SEC|BLK|BLDG|UNIT|,|\\d{3,})");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)(LOTS?\\s*:?|BLK|BLDG|UNIT)\\s+(.*)(\\d{3,}|$)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\\"([A-Z]+[^\\\"]+)\\\"");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		} 
		
		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.trim().length() > 2) {

			subdiv = subdiv.replaceFirst("(?is)\\A\\s*[\\d-\\(\\)]+", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*LO?T?S?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*BL?K?S?\\b", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z#][\\d']+\\s*$", "$1");
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
		
	}
	
	public static void parseLegalLiberty(ResultMap resultMap){
		//BIG THICKET LAKE ESTATES, LOT 215-218, SEC 18, ACRES .8152
		//000354 B TARKINGTON, TRACT 92, ACRES 7.7253
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\b(MOBILE HOME|SEC|TRACT|BLOCK|A-)\\b", ", $1");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers


		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([^,]+|[^$]+)");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BL\\s*(?:OC)?KS?)\\s*([^,]*|[^$]*)\\b");
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*([^,]+|[^$]+)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N\\s*I?T\\s*-?)\\s*([^,]+|[^$]+)\\b");
		legal = extractUnit(resultMap, legal, p);

		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+([^,]+|[^$]+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([^,]+|[^$]+)\\b");
		legal = extractBuilding(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([^,]+|[^$]+)\\b");
		legal = extractTract(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		if (legal.contains("IMPROVEMENT")){
			p = Pattern.compile("(?is)(A-.*)");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\AA?-?(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
			
		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.trim().length() > 2) {

			subdiv = subdiv.replaceFirst("(?is)\\A\\s*[\\d-\\(\\)]+", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*LO?T?S?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*BL?K?S?\\b", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z][\\d']+\\s*$", "$1");
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
		
	}
	
	public static void parseLegalWaller(ResultMap resultMap){
		//A047600 A-476 JOHN IVY, TRACT 1, ACRES 60.45
		
		//Medina GERONIMO FOREST, BLOCK 8, LOT 9
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\b(MOBILE HOME|SEC|TRACT|BLOCK|BLK|LOT)\\b", ", $1");
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(N?CB\\s*:?)\\s+(\\d+[A-Z]?)");
		legal = extractNcb(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\A(?:ABS)?\\s*(?:AB?|S)(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
			subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
		} else {
			p = Pattern.compile("(?is)(A-?\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}

	public static void parseLegalChambers(ResultMap resultMap){
		//BK 19 LT 3 FOX WINNIE
		//1 - 2 10 ANAHUAC
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bPT\\s+(\\d+\\s+[A-Z]+)\\b", "BLK $1");//weird case 32417
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\b(MOBILE HOME|SEC|TRACT|BLOCK|BLK|LOT)\\b", ", $1");
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A([\\d&-]+)\\s+(\\d+)?");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = block.replaceAll("\\s*&\\s*", " ").trim();
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)(?:LO?TS?\\s*:?|BLK?S?|BLDG|UNIT|TR(?:ACT)?)\\s+(.*)$");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.*)$");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalGuadalupe(ResultMap resultMap){
		//BENTWOOD RANCH UNIT #9, BLOCK 19, LOT 41
		//LOT: 15 BLK: 3 ADDN: ASHLEY PLACE UNIT 1 LN
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replace(" &amp; ", " & ");
		legal = legal.replace("\\d+/\\d+\\s*&", "& ");
		legal = legal.replaceAll("(\\d+),\\s+?(\\d+)", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s+([\\d\\s&,-]+[A-Z]?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?\\s*:?)\\s+(\\d+|[A-Z]{1,2})\\s+\\b");
		legal = extractBlock(resultMap, legal, p);
		
		// extract ncb from legal description
		p = Pattern.compile("(?is)\\b(CB\\s*:?)\\s+(\\d+[A-Z]?)");
		legal = extractNcb(resultMap, legal, p);
				
		// extract abstract from legal description
		p = Pattern.compile("(?is)\\b(ABS\\s*:?)\\s+(\\d+)\\b");
		legal = extractAbst(resultMap, legal, p);

		
		p = Pattern.compile("(?is)\\b(SEC\\s*:?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UNIT|UT-)\\s*:?\\s+([#\\d]+)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*:?)\\s+([#\\d]+)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(?:PLAT)?\\s*:?\\s*(\\d{3,5})\\s*/\\s*(\\d+)\\b");
		Matcher mat = p.matcher(legal);
		if (mat.find()) {
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), mat.group(1).trim());
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), mat.group(2).trim());
		}
		
		String subdiv = "";
		p = Pattern.compile("(?is)\\bADDN\\s*:?\\s*(.+)(?:\\s+UNIT|PH|TEX|VA|[\\d\\.]*|$)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			subdiv = mat.group(1);
		} else {
			p = Pattern.compile("(?is)\\(([^\\)]+)");
			mat.usePattern(p);
			mat.reset();
			if (mat.find()) {
				subdiv = mat.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				mat.usePattern(p);
				mat.reset();
				if (mat.find()) {
					subdiv = mat.group(1);
				} 
			}
		}
		
		if (subdiv.length() != 0) {

			p = Pattern.compile("(?is)(\\bNO|#)\\s*(\\d+)\\b");
			legal = extractUnit(resultMap, legal, p);
			
			subdiv = subdiv.replaceAll("(NO|#)\\s*\\d+", "");
			subdiv = subdiv.replaceAll("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceAll("(.*)\\s+(PARCEL|PHASE|UNIT).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(ADD|LO?T|BLK).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNREC.*)", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(TRACT|LTS?|[\\d+\\.,]+\\s+ACS?).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+([A-Z]/[A-Z]).*", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceAll("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+TEX\\s*#.*", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+[\\d\\.]+\\s+AC.*", "");
			subdiv = subdiv.replaceAll("COM\\s+AT", "");
			subdiv = subdiv.replaceAll("COR\\s+OF", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
		}
	}

	public static void parseLegalComal(ResultMap resultMap){
		//BULVERDE ESTATES 1, LOT 159, ACRES 2.525
		//BAVARIAN HILLS, BLOCK 3, LOT 14 S 1/2
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal)){
			return;
		}
		
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)[^-]\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)GR PT", " ");
		legal = legal.replaceAll("(?is)\\(\\s*[\\d\\.,]+\\s*\\)", " ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d&,\\s-]+|[\\d\\s-[A-Z]]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-#]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(CB:?|CITY\\s*BLOCK)\\s*([\\d(?:[A-Z])?]+)\\b");
		legal = extractNcb(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS(?:TRACT)?|A)\\s*[:-]?\\s*(\\d+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+(?:[A-Z])?|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?-?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s*(\\d+)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]?-?\\d*?)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)([^$]+)");
			ma = p.matcher(legal);
			
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			subdiv = subdiv.replaceAll("(?is)\\bCITY\\s+BLOCK\\b", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
			}
		}
				
	}
	
	public static void parseLegalSanPatricio(ResultMap resultMap){
		//LTS 1,2 BLK 264 ARANSAS PASS, 0.321 ACRES
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal)){
			return;
		}
		
		legal = legal.replaceAll("\\s*&amp;\\s*", " & ");
		//legal = legal.replaceAll("(\\d+),\\s+?(\\d+)", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("\\b\\d+'\\s+OF\\b", "");
		legal = legal.replaceAll("\\AND\\s+ALL\\s+OF\\b", "");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d-,\\s&]+[A-Z]?|[\\d&A-Z]+|\\d+[A-Z]{1,2})\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?)\\.?\\s+(\\d+[A-Z-]?)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BL?O?C?KS?)\\s+([\\d-&\\s,]+[A-Z]?|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN|UNITS?)\\s+#?\\s*(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG)\\s+(\\d+|[A-Z]-\\d+|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC)\\s+([\\d,\\s]+|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH|PHASE)\\s+(\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		legal = legal.replaceAll("\\b[A-Z]\\d?/\\d+", "");
		
		String subdiv = "";
		p = Pattern.compile("(?is)BLO?C?KS?\\s+([^,]+),");
		Matcher mat = p.matcher(legal);
		if (mat.find()) {
			subdiv = mat.group(1);
		} else {
			p = Pattern.compile("(?:\\s*)?(.+)\\s+(UNIT|BLOCKS?|TR|TRACT|BLKS?|LO?TS?|TRACT)\\b*");
			mat = p.matcher(legal);
			if (mat.find()) {
				subdiv = mat.group(1);
			} else {
				p = Pattern.compile("\\b(?:\\s*)?(.+?)(?:LO?T|TR|BLOCK|TRACT|PH |UNIT |BLDG)");
				mat.reset();
				mat.usePattern(p);
				if (mat.find()) {
					subdiv = mat.group(1);
				} else {
					p = Pattern.compile("\\b(ABS?T?)\\s+(.+?)\\s+(UND|SUR(?:VEY)?).*");
					mat.reset();
					mat.usePattern(p);
					if (mat.find()) {
						subdiv = mat.group(2);
					} else {
						p = Pattern.compile("\\A([^,]+),");
						mat.reset();
						mat.usePattern(p);
						if (mat.find()) {
							subdiv = mat.group(1);
						}
					}
				}
			}
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("(\\bNO\\s+|#\\s*)\\d+", "");
			subdiv = subdiv.replaceAll("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceAll("(.*)\\s+(PARCEL|PHASE|UNIT).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(ADD|BLO?C?KS?|LO?T).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNREC.*)", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(BLO?C?KS?|TRACT|LTS?|[\\d+\\.,]+\\s+ACS).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNDIV).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+([A-Z]/[A-Z]).*", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) #\\s*ACRES.*", "$1");
			subdiv = subdiv.replaceAll("\\b[A-Z]\\d+/\\d+", "");
			subdiv = subdiv.replaceAll("\\A[\\d+\\s+|-](.*)", "$1");
			subdiv = subdiv.replaceAll("\\ALT\\s+(.*)", "$1");
			subdiv = subdiv.replaceAll("[#\\d\\.,]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\bLYING IN.*", "");
			subdiv = subdiv.replaceAll("COM\\s+AT", "");
			subdiv = subdiv.replaceAll("COR\\s+OF", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
		}
		
	}
	
	public static void parseLegalAtascosa(ResultMap resultMap){
		//ABS A00324 J H GIBSON SV-79,11. ACRES
		//LOTS 1,2,3 BLK 113 CHARLOTTE
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");


		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll(
						"(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("\\b(\\d+)\\s*(&|,)\\s*(\\d+)\\b", "$1$2$3");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(UNIT)\\s*([\\d]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?)\\s+([[A-Z-]|\\d]+)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b((?:BLO?C?K?)|(?:BK))\\s+([[A-Z]|\\d,]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([-&\\w\\d,]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)(.*?)\\s+(PH|UNIT|BLO?C?K|LO?TS?|TR)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)([^,]+)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
			
			p = Pattern.compile("(?is)\\A(?:ABS)?\\s*(?:AB?|S)(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
			}

			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(-?U\\s*N?\\s*I?T\\s*-?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s(BL?KS?)\\s*%", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst(",", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
		
	}
	
	public static void parseLegalKendall(ResultMap resultMap){
		//A10153 - SURVEY 727 J DEDEKER 1. ACRES
		//ESCORIAL SUBDIVISION UNIT 1 BLK 1 LOT 12 (DEEP HOLLOW), 10.224 ACRES UNIT 1
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\d+)\\s+THRU\\s+(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)[^-]\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)(\\s+[SWNE]{1,2})?\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)GR PT", " ");
		legal = legal.replaceAll("(?is)\\(\\s*[\\d\\.,]+\\s*\\)", " ");
		
		//remove N 1/2 OF from  HART, BLOCK AL, LOT N 1/2 OF 4
		legal = legal.replaceAll("\\b(N|S|W|E)\\b\\s\\d+/\\d+ OF", "");
		// remove  LESS 10 FT from  HIGHWOOD, BLOCK 11, LOT 8 LESS 10 FT & 9
		legal = legal.replaceAll("LESS \\d+ FT", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s&-]+[A-Z]?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(CB\\s*:?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractNcb(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?-?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s*(\\d+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN?I?T?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]?-?\\d*?)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.*)$");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}

		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			
			p = Pattern.compile("(?is)\\b(A(?:BS)?(?:TRACT)?\\s?)(\\d+)[-|\\s]+SUR(?:VEY)?\\s+\\d+\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
			}
		
			subdiv = subdiv.replaceFirst("\\A(?:ABS(?:TRACT)?)\\s+(.*)", "$1");
			subdiv = subdiv.replaceFirst("\\A(?:S\\d+-)\\s*(.*)", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+UNIT\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+BLOCK\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+LOT\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+PH(ASE)?\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sREFER.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+U\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+[\\d\\.]+\\s*ACRES.*", "$1");
			subdiv = subdiv.replaceFirst(",", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}

	public static void parseLegalTarrant(ResultMap resultMap){
		//ANDERSON, FRANK M SUBDIVISION   BLK   1   LOT   A               96.25% UNDIVIDED INTEREST       *00038652*
		
		//Denton COLONY NO 21 BLK 150 LOT 38
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\s[^\\s]+)<br>([^\\s]+)(BLO?C?KS?\\s)", "$1$2 $3");
		legal = legal.replaceAll("(?is)(B)(?:<br>)?(LO?C?KS?\\s)", " $1$2");
		legal = legal.replaceAll("(?is)(LO?TS?\\s+\\d+)", " $1");
		
		String originalLegal = legal;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", " ");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bLOTS\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "LOT $1 & LOT $2");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", " ");
		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(?is),", ", ");
		legal = legal.replaceAll("(?is)\\b(LOT\\s*[\\d-]+)([A-Z]\\d+)", "$1 $2");//95296DEN; O T RHOME, BLOCK 17, LOT1-3C41 G04
		legal = legal.replaceAll("(?is)\\b(LOT\\s*\\d{2}+)\\s*&\\s*(\\d)\\s+(\\d)([A-Z]\\d+)", "$1&$2$3 $4");//95289DEN; O T RHOME,  BLOCK 16,  LOT11 & 1 2C41 G04
		legal = legal.replaceAll("(?is)\\*\\d+\\*", "");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z][\\d&-]+|[\\d\\s,&]+[A-Z]?\\d?)(\\b|INT\\d+|VOL\\d+)");
		legal = extractLot(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+[A-Z]?|[A-Z]{1,2})\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?#?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(A(?:BS?T?)?)\\s*([\\d+&\\s]+[A-Z]?)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		legal = legal.replaceAll("\\b(TR(?:ACT)?)\\s+(\\d+[A-Z]),\\s*(\\d+[A-Z])", "$1 $2 $1 $3 ");
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-B]?|[A-B])\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		if (!(originalLegal.toLowerCase().contains("mineral") || originalLegal.toLowerCase().contains("energy")
				|| originalLegal.toLowerCase().contains("gas well") || originalLegal.toLowerCase().contains("personal property"))){
			String[] lines = originalLegal.split("<br>");
			if (lines.length > 0){
				String subdiv = lines[0];
				subdiv = subdiv.replaceAll("(?is)(\\bNO)?\\s+\\d+\\s*$|#\\s*\\d+\\s*$", "");
				subdiv = subdiv.replaceAll("(?is)\\b(A(?:BST?)?)\\s*([\\d+\\s&]+[A-Z]?)\\b", "");
				subdiv = subdiv.replaceAll(",", "");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+BLO?C?K.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+(UNIT|SUBDIVISION|ADDN|ADDITION|SEC|INST|TR\\b).*", "$1");
				subdiv = subdiv.replaceAll("(?is)([^\\(]+)\\(.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+PH(?:ASE)?.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+MHP.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+LOT.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+([\\d\\.]+\\s+ACRES)", "$1");
				subdiv = subdiv.trim();
				if (subdiv.length() != 0){
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				}
				if (legal.matches(".*\\bCOND.*")){
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
				}
			}
		}
	}

	public static void parseLegalCollin(ResultMap resultMap){
		//BRIARMEADE #1 (CPL), BLK C, LOT 4
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
			
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d/]+[A-Z]?|[A-Z])[ ,]");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = "";
		Matcher ma ;
		if (legal.contains("(")) {
			p = Pattern.compile("(?is)\\A([^\\(]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} 
		} else {
			p = Pattern.compile("(?is)\\bABST?\\s+([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				ma.usePattern(p);
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
		
	public static void parseLegalParker(ResultMap resultMap){
		//LOT:  9, BLK:  1, ADDN: BIG TIMBERS   BIG TIMBERS
		//19 C BOLING RANCH EST
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bMH ONLY\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		legal = legal.replaceAll("(?is)\\s{3,}", " | ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A([\\d&,-]+)\\s+(\\d+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\bADDN:\\s*(.+?)\\s+(\\1.*)$");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\bADDN:\\s*([^\\|]+).*");
			ma = p.matcher(legal);
				
			if (ma.find()) {
				subdiv = ma.group(1);
			}else {
				p = Pattern.compile("(?is)\\A(.*)$");
				ma = p.matcher(legal);
					
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		} 
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalKaufman(ResultMap resultMap){
		//BLUFF VIEW EST #2 BLOCK C LOT 15
		//CLUB LAKE EST BLOCK 27 PT
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
			
		Pattern p = Pattern.compile("(?is)\\b(L[O|0]TS?\\s*:?)\\s*([\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalLubbock(ResultMap resultMap){
		//ALFORD TERRACE L 122 & N25'OF 123
		//SUNNY SLOPE BLK 7 L 6
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		String untouchedLegal = legal;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
			
		Pattern p = Pattern.compile("(?is)\\b.*(LO?T?S?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+).*");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			legal = legal.replaceAll("(?is)\\b(LO?T?S?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+)", "$1 $2 $1 $3");
		}
		
		p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s*([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		if (untouchedLegal.trim().startsWith("BLK")){
			p = Pattern.compile("(?is)\\bTR\\s*&?\\s*(.+)");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalWebb(ResultMap resultMap){
		//UNIT 2 BLDG A VILLAS PLAZA CONDOS & UND 7.14% INT OUT LOT 3 & 4 BLK 849 ED
		//LOS PRESIDENTES, BLOCK 3, LOT 3, UNIT 12
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
			
		Pattern p = Pattern.compile("(?is)\\b.*(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+).*");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			legal = legal.replaceAll("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+)", "$1 $2 $1 $3");
		}
		
		p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\s+(?:(?:SUR(?:VEY)?|P)\\s+\\d+)?\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = "";
		if (legal.contains(",")){
			p = Pattern.compile("(?is)\\A([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		} else {
			p = Pattern.compile("(?is)\\b(?:BLK|BLDG|TR|ABST)\\s+(.+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalMidland(ResultMap resultMap){
		//SEC:  7 SE/4  SURV: H P HILLIARD  BLK:  X
		//BLK:  008  LOT:  006  ADDN: BARBERDALE SEC 2
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bMH ONLY\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:(\\d+)-[A-Z]\\d+[A-Z])|(?:\\d+))\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?:?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*IT\\s*:?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST(?:RACT)?)\\s*:?\\s+([\\d&]+)\\b");
		legal = extractAbst(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s*:?\\s*(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s*:\\s*([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s*:\\s*([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(ADDN|SURV):\\s*(.*)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(2);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("(?is)(.*?)\\s+(SEC|EXT).*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\bOF\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)(.*)\\s+([\\d\\s]+)\\s+\\1", "$1");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalMontgomery(ResultMap resultMap) throws Exception{
		parseLegalFortBend(resultMap);
	}
	
	public static void parseLegalWichita(ResultMap resultMap){
		//LOT 24 BLK 2 STONE LAKE ESTATES PHASE 2
		//LOT 12 BLK 10 BRIDGE CREEK EST SEC 3
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bMH ONLY\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((\\d+)[A-Z]?|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?:?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*IT\\s*:?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST(?:RACT)?)\\s*\\.?\\s+([\\d&]+)\\b");
		legal = extractAbst(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s*:?\\s*(\\d+(?:\\s*-\\s*[A-Z])?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(\\d+-?(?:\\d+|[A-Z])?)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\b(SEC)\\s+(.*)");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(2);
			}
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(?is)\\A\\s*SEC\\b", "");
			subdiv = subdiv.replaceFirst("(?is)(.*?)\\s+(SEC|EXT|ABST).*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\bOF\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)(.*)\\s+([\\d\\s]+)\\s+\\1", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalMaverick(ResultMap resultMap){
		//HOLLY PARK UNIT # 3, BLOCK 5, LOT 11
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABST?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = "";
		p = Pattern.compile("(?is)\\A([^\\.]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(A-)\\s*([\\d&]+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalJimWells(ResultMap resultMap){
		//SAGEWOOD TWNHOUSE UN #1B LT 23 UNIT 1-B 0000.000
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
//		p = Pattern.compile("(?is)\\A([^\\.]+)");
//		Matcher ma = p.matcher(legal);
//		if (ma.find()) {
//			subdiv = ma.group(1);
//		} else {
//			p = Pattern.compile("(?is)\\A([^,]+)");
//			ma = p.matcher(legal);
//			if (ma.find()) {
//				subdiv = ma.group(1);
//			}
//		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(A-)([\\d+\\s&]+)\\b");
			Matcher ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-)([\\d+\\s&]+)\\b", "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalAngelina(ResultMap resultMap){
		//0004 BARR & DAVP, TRACT 320.1, ACRES 12.49
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d/-]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\d&-\\.]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*#?\\s*((?:[A-Z]|\\d+)-[A-Z]|[\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^\\,]+),\\s*TRACT");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A([^,]+),\\s*BLO?C?KS?");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(?:ABS\\s+)?(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBandera(ResultMap resultMap){
		//AVALON A BLK 63 LT 4-10 14-20 0.796 ACRES
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A(.+)\\s+SVY");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+BLKS?\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+LTS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+TR\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(ABST)\\s+(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBastrop(ResultMap resultMap){
		//Bastrop West Estates, Lot 6, ACRES 2
		//MADISON, J. C., BLOCK 2, LOT 1
		//A427 Perry, C R, ACRES 11.517
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\b\\d+'", "");
		legal = legal.replaceAll("(?is)\\bDIV\\b", "");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:ion)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A(.+)\\s+SVY");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					} else {
						p = Pattern.compile("(?is)\\A(.+)\\s+SERIAL\\b");
						ma = p.matcher(legal);
						if (ma.find()) {
							subdiv = ma.group(1);
						}else {
							p = Pattern.compile("(?is)\\A(.+)\\s+(?:TR|TRACT|ACRES)\\b");
							ma = p.matcher(legal);
							if (ma.find()) {
								subdiv = ma.group(1);
							}
						}
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(A)(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			if (org.apache.commons.lang.StringUtils.countMatches(subdiv, ",") == 2){
				if (subdiv.contains("&")){
					subdiv = subdiv.replaceAll("(?is)([^,]+)\\s*,\\s*([^&]+)\\s*&\\s*(.*)", "$2 $1 & $3");
				} else {
					subdiv = subdiv.replaceAll("(?is)([^,]+)\\s*,\\s*(.*)", "$2 $1");
				}
			}
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b|REPLAT).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBell(ResultMap resultMap){
		//MILL CREEK SECTION 15, BLOCK 003, LOT 0003
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\,\\s*(\\d+)", "$1,$2");
		
		legal = legal.replaceAll("(?is)\\b\\d+'", "");
		legal = legal.replaceAll("(?is)\\bDIV\\b", "");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d,]+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "UN(?:IT)?S?");
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SECT?(?:ION)?");
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(A-?)(\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBowie(ResultMap resultMap){
		//BEAVER LAKE ESTATES LOT 25 6034/065  05/04/11 BLK/TRACT 1 1.984 ACRES
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\b(BLK/TRACT)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 BLK $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?|BLK/TRACT)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(\\d+)\\s*/\\s*(\\d+)\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			List<List> body = new ArrayList<List>();
			
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			body.add(line);
			
			legal = legal.replaceAll(ma.group(0), "");
			
			ResultTable rt = (ResultTable) resultMap.get("SaleDataSet");
			if (rt == null){
				rt = new ResultTable();
			} else {
				String[][] bodyRT = rt.getBody();
				if (bodyRT.length != 0) {
					for (int i = 0; i < bodyRT.length; i++) {
						line = new ArrayList<String>();
						for (int j = 0; j < rt.getHead().length; j++) {
							line.add(bodyRT[i][j]);
						}
						body.add(line);
					}
				}
			}
				
			if (body != null && body.size() > 0) {
				rt = new ResultTable();
				String[] header = {"InstrumentNumber", "Book", "Page", "InstrumentDate", "SalesPrice"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", rt);
			}
		}
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(A-?)(\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBrazos(ResultMap resultMap){
		//PECAN RIDGE PH 1, BLOCK 4, LOT 4 (PT OF)#####
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABST?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-\\.]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A\\s*(A)\\s*([\\d&]+)\\s*,");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		String subdiv = "";
		p = Pattern.compile("(?is)\\A([^\\.]+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
						
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBrooks(ResultMap resultMap){
		//12 1 MONROE & ADAMS
		//AB #97 J M & L CHAPA LT 2 SGIAN-DUBH S/D
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\b(MH|IMP) ONLY\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		legal = legal.replaceAll("(?is)\\b\\d+\\s+X\\s+\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)\\bPTS?\\s+AC\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]\\s*\\d+'-(\\d+)\\b", "$1");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");
		legal = legal.replaceAll("(?is)'", "");
		legal = legal.replaceAll("(?is)-\\s+(\\d+)", "-$1");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		legal = legal.replaceAll("(?is)\\s+-(\\d+)", "$1");
		
		legal = legal.replaceAll("(?is)\\bALL\\s+(\\d+),?\\s+(\\d+)", "$1-$2");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		p = Pattern.compile("(?is)\\A([\\d&,/-]+)\\s+([\\d-]+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll(",", " ");
			lot = lot.replaceAll("/", "-");
			lot = lot.replaceAll("\\A-", "");
			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(AB)\\s*\\s+#(\\d{1,3}[A-Z]?)\\b(\\s+S\\s+#\\d+)?");
		ma = p.matcher(legal);
		if (ma.find()) {
			String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\bADDN:\\s*([^\\|]+).*");
		ma = p.matcher(legal);
				
		if (ma.find()) {
			subdiv = ma.group(1);
		}else {
			p = Pattern.compile("(?is)\\A(.*)$");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		} 
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceAll("(?is)\\s+\\d+(ST|ND|RD|TH)\\s+ADD(ITIO)?N\\b.*", "");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bBLK\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+\\s+LANDS?\\b).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]?\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalBurnet(ResultMap resultMap){
		//S8150 SUNSET OAKS LOT H-8 BLK 1
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d,]+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PLAT)\\s+([A-Z]?\\d+)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			resultMap.put(PropertyIdentificationSetKey.PLAT_INSTR.getKeyName(), ma.group(2).trim());
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABS)\\s+(A\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalCameron(ResultMap resultMap){
		//TREASURE HILLS SUBDIVISION 5 LOT 29 BLK 3
		//HARLINGEN- PARKSIDE ESTATES-3 LOT 51
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d,]+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([A-Z]-\\d+)\\s*([,&]+)\\s*([A-Z]-\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d,&-]+[A-Z]?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s+(?:NO\\.\\s+)?([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?S?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		legal = legal.replaceAll("\\A\\s*LOT\\b", "");
		legal = legal.replaceAll("\\A\\s*(BLK|UNI?T)\\b", "");
		legal = legal.replaceAll("\\A\\s*SURVEY-\\b", "");
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABST)\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			if (subdiv.contains("- ")){
				subdiv = subdiv.replaceAll("(?is)\\A([^-]+)", "");
			}
			subdiv = subdiv.replaceAll("(?is)\\A\\s*RESUBD\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?|ACRES).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL|AMENDED).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s*[&|\\.|;|-]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\b[SWNE]{1,2}\\s*[\\d'\\.]+", ""); 
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalCooke(ResultMap resultMap){
		//CUNNINGHAM ADDN, BLOCK 3, LOT 10 & PT OF LT 9, 1501 E BROADWAY
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&)\\s*(\\d+)", "$1$2$3");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d/-]+|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\d&-\\.]+|[A-Z])\\s*-");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*#?\\s*((?:[A-Z]|\\d+)-[A-Z]|[\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^\\,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(?:ABSTRACT\\s+)?(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalCoryell(ResultMap resultMap){
		//HOUSE CREEK NORTH PHASE 2, BLOCK 1, LOT 24
		//12    2 COLONIAL PARK 9
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bPT\\s*(\\d+)\\b", " $1");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\b(MOBILE HOME|SEC|TRACT|BLOCK|BLK|LOT)\\b", ", $1");
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");
		legal = legal.replaceAll("(?is)(\\d+)[SWNE]{1,2}\\s*[\\d'/\\.]+", "$1&");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\A(\\d+)\\s+([A-Z])\\s+(\\d+)\\b", "$1$2 $3");//093130000, but not for 105986920
		legal = legal.replaceAll("(?is)\\A(\\d+)([A-Z]{3,})\\b", "$1 $2");
		legal = legal.replaceAll("(?is);", "&");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A([\\d&-]+[A-Z]?)\\s+(\\d+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = block.replaceAll("\\s*&\\s*", " ").trim();
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A([^,]+)");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.*)$");
			ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\bTR\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bIMPROVEMENT ONLY.*", "");
			subdiv = subdiv.replaceAll("(?is)\\bAKA\\b.*", "");
			subdiv = subdiv.replaceAll("(?is)\\s+[\\d/\\.&\\s]+\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)\\s+\\d{3,}\\s+.*", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalEctor(ResultMap resultMap){
		//LOT 2  BLK 5  FAIR OAKS
		//HERBERT WIGHT BLOCK 42A
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d,]+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([A-Z]-\\d+)\\s*([,&]+)\\s*([A-Z]-\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s+([A-Z]?[\\d,&-]+[A-Z]?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?S?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		Matcher ma = null;
		if (legal.contains("BLK ")){
			p = Pattern.compile("(?is)\\bBLK\\b(.+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} 
		} else if (legal.contains("BLOCK ")){
			p = Pattern.compile("(?is)\\A(.+)\\s+BLOCKS?\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		} else {
			p = Pattern.compile("(?is)\\bLOTS?\\b(.+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
				
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABST)\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			if (subdiv.contains("- ")){
				subdiv = subdiv.replaceAll("(?is)\\A([^-]+)", "");
			}
			subdiv = subdiv.replaceAll("(?is)\\A\\s*RESUBD\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL|AMENDED).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s*[&|\\.|;|-]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\b[SWNE]{1,2}\\s*[\\d'\\.]+", ""); 
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalElPaso(ResultMap resultMap){
		//3 CANUTILLO HEIGHTS UNIT 1 LOT 6 (10794.00 SQ FT)
		//BLK 29 HORIZON MESA #6 LOT 10
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bMH ONLY\\b", "");
		legal = legal.replaceAll("(?is)\\bMOBILE HOME( ONLY ON)?\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\s+([\\d,\\.]+\\s+SQ\\s+FT)\\b", " ($1)");
		if (!legal.toLowerCase().contains("lot")){
			legal = legal.replaceAll("(?is)\\s+([\\d&-]+\\s+\\()", " LOT $1");
		}
		
		legal = legal.replaceAll("(?is)\\b[\\d\\.]+\\s+FT\\s+OF\\b", ""); 
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		boolean blockExtracted = false;
		p = Pattern.compile("(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			blockExtracted = true;
		}
		legal = extractBlock(resultMap, legal, p);
		
		if (!blockExtracted){
			p = Pattern.compile("(?is)\\A(\\d{1,3}(?:-?[A-Z])?|[A-Z])\\b");
			ma = p.matcher(legal);
			
			if (ma.find()){
				String block = ma.group(1);
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
				legal = legal.replaceAll(ma.group(0), "");
			}
		}
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UNIT)\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.+)\\s+(REPLAT|LOT)\\b");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("#.*", "");
			subdiv = subdiv.replaceAll("\\bREPLAT.*", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalEllis(ResultMap resultMap){
		//7R 1 CARLTON EST #2 1.13 ACRES
		//15 C LA VISTA ESTS PH 3     0.759 ACRES
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bMH ONLY\\b", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = legal.replaceAll("[\\d,\\.]+\\s+(ACRES|AC)\\b", "");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\A\\s*([SWNE]{1,2}\\s+)?PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\s+OF", "");
		
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A([\\d&,-]+[A-Z]?)\\s+(\\d+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?|SECT)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.*)$");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		subdiv = subdiv.replaceAll("[\\d,\\.]\\s+(ACRES|AC)\\b", "");
		
		if (subdiv.length() != 0) {
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalErath(ResultMap resultMap) throws Exception{
		//S5660 RIVER NORTH II ADDITION, BLOCK 21, LOT 4
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d&,-;]+[A-Z]?(?:[\\d;]+[A-Z])?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BLOCKS?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UNI?T)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^;]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\A(?:A|S)(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll(";", "");	
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLOCK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					//.replaceFirst("(?is)(.+)\\s(?:\\d+(ST|ND|RD|TH)?\\s+)ADD.*", "$1")
					//.replaceFirst("(?is)(.+)\\sADD.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\A\\s*-\\s*", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalGrayson(ResultMap resultMap){
		//CLASSICS SUBDIVISION, BLOCK 1, LOT PT 3 & 4, ACRES .283
		//G-1075 STEWART HENRY A-G1075, ACRES 6.0
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bPT\\b", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\b(MOBILE HOME|SEC|TRACT|BLOCK|BLK|LOT)\\b", ", $1");
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d&,\\s-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL\\s*(?:OC)?KS?)\\s*(\\d+|[A-Z])\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\b(?:A-)\\s*(G\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*G-\\d+", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalHardin(ResultMap resultMap){
		//AB 7 MM BRADLEY PARCEL 7-6-A 5.01 ACRES
		//861 /414 LOT 14 VINSON LOTS AB 14 E DUNCAN PARCEL 14-93-O 0.34 ACRES
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\b\\d+'", "");
		legal = legal.replaceAll("(?is)\\bDIV\\b", "");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\s+", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:ion)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(AB)\\s+(\\d+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(\\d+)\\s*/\\s*(\\d+)\\b");
		Matcher ma = p.matcher(legal);
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		while (ma.find()){
			
			line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			body.add(line);
			
			legal = legal.replaceAll(ma.group(0), "");
		}
				
		if (body != null && body.size() > 0) {
			
			ResultTable rt = (ResultTable) resultMap.get("SaleDataSet");
			if (rt == null){
				rt = new ResultTable();
			} else {
				String[][] bodyRT = rt.getBody();
				if (bodyRT.length != 0) {
					for (int i = 0; i < bodyRT.length; i++) {
						line = new ArrayList<String>();
						for (int j = 0; j < rt.getHead().length; j++) {
							line.add(bodyRT[i][j]);
						}
						body.add(line);
					}
				}
			}
			
			rt = new ResultTable();
			String[] header = {"InstrumentNumber", "Book", "Page", "InstrumentDate", "SalesPrice"};
			rt = GenericFunctions2.createResultTable(body, header);
			resultMap.put("SaleDataSet", rt);
		}
		
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\b(?:LOT|BLO?C?K)\\b(.+)\\s+AB\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\b(?:AB|BLK|LOT)\\b(.+)\\s+PARCEL\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(BLK|OF)\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\bHOUSE ONLY LOCATED ON .*", "");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b|REPLAT|PARCEL).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalHays(ResultMap resultMap){
		//CHANDLER ADDN, BLOCK 1, LOT 9 & PT OF 8
		//CIMARRON ACRES LOT 1   0.34 AC
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		
		legal = legal.replaceAll("(?is)\\bPT\\s+\\d+", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s,&-]+[A-Z]?-?\\d?[A-Z]?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABST?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&-]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&-]+[A-Z]?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = "";
		if (legal.contains(",")){
			p = Pattern.compile("(?is)\\A([^\\,]+)");
			Matcher ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} 
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+LOT");
			Matcher ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\bTR\\s+(.+)\\s+GEO");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(A)\\s*([\\d&-]+)\\b");
			Matcher ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(BUILDING ONLY|TR)\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = subdiv.replaceAll("\\bGEO\\b", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalHenderson(ResultMap resultMap){
		//AB 485 I V MICHELLI SUR  TR 118A (RE: PT TR 2)
		//AB 59 J P BROWN SUR  BONANZA BEACH  BLK 1 LTS 26-32
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)([-]{2,})", " $1");
		legal = legal.replaceAll("(?is)[N|S|W|E]?[\\d/\\.]+(\\s+FT)?\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b\\d+\\.\\d+\\b", "");

		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\b\\d+'", "");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS)\\s+(\\d+)\\s+(\\d+)", "$1 $2&$3");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		legal = legal.replaceAll("\\([^\\)]+\\)", "");
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:ion)?)\\s+([\\d&]+(?:\\s*[A-Z])?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(AB)\\s+(\\d+)\\b");
		legal = extractAbst(resultMap, legal, p);		
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\b(?:SUR)\\b(.+)\\s+(BLK|LO?T|TR)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\b(?:AB)\\b(.+)\\s+SUR\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(BLK|OF)\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\bHOUSE ONLY LOCATED ON .*", "");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b|REPLAT|PARCEL).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalHidalgo(ResultMap resultMap){
		//PALM HEIGHTS LOT 5 BLK 2
		//SIESTA VILLAGE #1 LOT 27 BLOCK 4
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(BNG\\s+)?AN\\s+IRR(\\s+TR)?\\b", " ");
		legal = legal.replaceAll("(?is)\\bIRR\\s+TR\\b", " ");
		
		legal = legal.replaceAll("(?is)[S|W|N|E]{1,2}\\s*[\\d/'\\.]+(\\s+FT)?\\s+OF\\b", "");
		
		legal = legal.replaceAll("(?is)-?\\s*([S|W|N|E]{1,2})?\\s*[\\d/'\\.]+\\s*AC\\b", " ");
		legal = legal.replaceAll("(?is)-?\\s*[S|W|N|E]{1,2}\\s*[\\d/'\\.]+", " ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)-\\s*(UNIT|LOTS?|BLKS?)\\b", " $1");
			
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s*([\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*.?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:IO)?N?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|BLDG).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalHood(ResultMap resultMap){
		//LOT 176 BLOCK 2 OAK TRAIL SHORES SEC F
		//LOT 2501  PECAN PLANTATION UN 17
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		
		legal = legal.replaceAll("(?is)\\b\\d+/\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*;\\s*(\\d+)&\\s*(\\d+)", "$1&$2&$3");
		legal = legal.replaceAll("(?is);\\s+", ";");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(LOT\\s+)?PT\\s+\\d+(\\s+&)?(?:\\s+ALL\\s+\\d+)?", "");
		legal = legal.replaceAll("(?is)\\bLOT\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\.\\d]+\\s*INT\\b", "");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		//String legalTemp = legal;

		Pattern p = Pattern.compile("(?is)\\b(ABST\\s+)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);

		//LOT 5  3 & 4
		//LOT 172R & 173R
		//LOT 20A AND 21A
		legal = legal.replaceAll("(?is)\\b(LOT)\\s+(\\d+)\\s+([\\d&]+)\\b", "$1 $2&$3");
		legal = legal.replaceAll("(?is)\\b(LOT)\\s+(\\d+[A-Z])\\s*(?:&|AND)\\s*(\\d+[A-Z])\\b", "$1 $2&$3");
		
		p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([-&;\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal.replaceAll("(?is);&\\s+", ";"), p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z]|\\d&;]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+([[A-Z-]|\\d]+(?:(?:\\s+[[A-Z-]|\\d]+)?\\s*&\\s[[A-Z-]|\\d]+)?)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLO?C?K)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LO?TS?)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)(.+)\\s+ABST\\b");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					subdiv = legal;
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLK\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (ABST|SECT?(?:IO)?N?)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst(",", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d-]+\\s*$", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
	}
	
	public static void parseLegalHunt(ResultMap resultMap){
		//A1183 YOUNG HENRY,TRACT 27, ACRES 2.8837
		//BELLAVISTA DOS ADDITION, LOT 66
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\[[^\\]]+\\]", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+\\d+/\\d+\\b", "");
		
		//LOT 14B,16B,18B,20B
		int counter = 0;
		while (legal.matches("(?is).*\\b(LO?TS?)\\s+(\\d+)([A-Z]),.*") && counter < 7){
			legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)([A-Z]),(\\d+\\3)", "$1 $2$3 $1 $4");
			counter++;
		}
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([A-Z]?[\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?|[^,]+)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(A\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*S\\d+", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalJimHogg(ResultMap resultMap){
		//11,12 50 KOEHLER
		//8 124 KOEHLER SEC A
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		if (legal.contains("TANGIBLE")){
			return;
		}
		legal = legal.replaceAll("(?is)\\b(MH ONLY|MIDDLE)\\b", "");
		legal = legal.replaceAll("(?is)\\b(SENT)\\b.*", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = legal.replaceAll("(?is)\\b([SWNE]+|FRONT)\\s*\\d+[,/\\.]?(\\d?'?)?(\\s+[O|0]F)?", "");
		legal = legal.replaceAll("(?is)\\b\\d+'(\\s+[O|0]F)", "");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\A\\s*([SWNE]{1,2}\\s+)?PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\s+OF", "");
		
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");
		
		legal = legal.replaceAll("(?is)\\A\\d{4,}\\b", "");
		
		legal = legal.replaceAll("(?is)#\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s*\\d+'\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(\\d+)\\s*,\\s*(\\d+)\\s*$", "$1,$2");
		
		//011080006101000000000 -   10, 61 KOHLER &E/2 11
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+),(\\s+\\d+\\s+\\w+)\\s+&\\s*(\\d+)", "$1&$3 $2");
		
		//011055000400400000000#####E/2 3 4 GRUY ALL 4,5
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(\\s+\\d+\\s+\\w+)\\s+(?:&|ALL)\\s*([\\d,]+)\\s*$", "$1&$3 $2");
		
		//01102-50015-00600-000000#####S50' 15  CW HELLEN 4-6
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(\\s+[A-Z\\s]+)\\s+([\\d,-]+)\\s*$", "$3 $1 $2");
		
		//011080001900300000000 -  3 & 19 KOEHLER E 10' 0F2
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(?:&|-)(\\s*\\d+\\s+[A-Z\\s]+)\\s+([\\d,-]+)\\s*$", "$1&$3 $2");
		
		legal = legal.replaceAll("(?is)(.*?)(\\d+)\\s*([&|-|,])\\s*(\\d+)\\s*$", "$2$3$4 $1");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A([\\d&,-]+[A-Z]?)\\s+(\\d+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll("\\s*[&;,]+\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = block.replaceAll("\\s*[&;,]+\\s*", " ").trim();
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?|SECT)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([A-Z]?[\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.*)$");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceAll("\\b(SEC)\\b.*", "");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\b\\d+\\s*X\\s*\\d+\\b.*", "");
			subdiv = subdiv.replaceAll("(?is)\\b[\\d,]+\\s*SQ\\s+FT\\b", "");
			subdiv = subdiv.replaceAll("[\\d,\\.]\\s+(ACRES|AC)\\b", "");
			subdiv = subdiv.replaceAll("\\b(MH|DCD|PARCEL)\\b.*", "");
			
			subdiv = subdiv.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+AD.*", "");
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+['\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalJohnson(ResultMap resultMap){
		//LOT 19 BLK 18 OAK VALLEY ESTATES PH 6,7
		//ABST 163 TR 6F W J CULVERHOUSE
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*/\\s*\\d+\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*\\d+\\s*FT(\\s+OF)?\\s*", "");
		
		legal = prepareLegal(legal);

		legal = legal.replaceAll("(?is)\\b[A-Z]#.*", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		Pattern p = Pattern.compile("(?is)\\b(ABST\\s+)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([-,&\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z]|\\d&;]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+([-,&\\w\\d]+)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LO?TS?)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(.*)");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(ABST)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find()) {
						subdiv = ma.group(2);
					}
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceAll("\\([^\\)]+\\)", "");
			subdiv = subdiv.replaceAll("\\b\\d+\\.\\d+.*", "");
			subdiv = subdiv.replaceAll("\\bAKA\\b.*", "");
			subdiv = subdiv.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+AD.*", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLK\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (ABST|SECT?(?:IO)?N?)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst(",", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d-]+\\s*$", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
	}
	
	public static void parseLegalKerr(ResultMap resultMap){
		//ABS A0708 SCHELLHASE, SUR 651,ACRES .21
		//BUENA VISTA BLK 2 LOT 15 & PT 16
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+ACRES\\s+[\\d\\.]+", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d,]+)\\s*,\\s*(\\d+)", "$1 $2,$3");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+)\\s*([A-Z])\\s*[,&]+\\s*(\\d+)\\s+([A-Z])\\b", "$1 $2$3 LOT $4$5");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(\\d+[A-Z])\\s*([,&]+)\\s*(\\d+)", "$1 $2 LOT $4");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABS)\\s+(A\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			subdiv = subdiv.replaceAll("\\bAKA\\b.*", "");
			subdiv = subdiv.replaceAll("\\s*@\\s*", " AT ");
			
			subdiv = GenericFunctions1.switchNumbers(subdiv, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalLaSalle(ResultMap resultMap){
		//LOT 15 BLOCK 10 COLONIA JIMENEZ
		//L 9 BLK 19 SPOHN ENC
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*/\\s*\\d+\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*\\d+\\s*FT(\\s+OF)?\\s*", "");
		
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+AC(?:RES?)?\\b", "");
		legal = legal.replaceAll("(?is)\\s+ACRE(?:RES?)?\\s+[\\d\\.]+", "");
		legal = legal.replaceAll("(?is)\\d+\\.\\d+", "");
		legal = legal.replaceAll("(?is)\\b\\d+'?\\s*X\\s*\\d+'?(\\s+FT)?\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		legal = legal.replaceAll("(?is)[\\d/]+\\s+\\d+\\s*FT\\s*X\\s*\\d+\\s*FT\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = prepareLegal(legal);

		legal = legal.replaceAll("(?is)\\b[A-Z]#.*", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		Pattern p = Pattern.compile("(?is)\\b(AB(?:ST)?(?:RACT)?)\\s+([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);

		legal = legal.replaceAll("(?is)\\b,\\s+(\\d+)\\b", ",$1");
		
		p = Pattern.compile("(?is)\\b(LO?T?S?)\\s+([-,&\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z]|\\d&;]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		//TRACTS 70 71 AND 72 
		legal = legal.replaceAll("(?is)\\b(TRACTS\\s+\\d+)\\s+(\\d+&\\d+)\\b", "$1&$2");
		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+([A-Z]?[\\d&-]+[A-Z]?)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LO?TS?)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(SECTION)\\s+(.*)");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(TR(?:ACT)?S?)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find()) {
						subdiv = ma.group(2);
					} else {
						p = Pattern.compile("(?is)\\b(SUR\\s+\\d+)\\s+(.*)");
						ma.usePattern(p);
						if (ma.find()) {
							subdiv = ma.group(2);
						} else {
							p = Pattern.compile("(?is)\\b(.*(MH|MOBILE HOME))\\b");
							ma.usePattern(p);
							if (ma.find()) {
								subdiv = ma.group(1);
							} else {
								p = Pattern.compile("(?is)\\b([^-]+)\\s*-\\s*LABEL\\b");
								ma.usePattern(p);
								if (ma.find()) {
									subdiv = ma.group(1);
								}
							}
						}
					}
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv) && subdiv.length() > 3) {
		
			subdiv = subdiv.replaceAll("(?is)\\b(.*(MH|MOBILE HOME))\\b.*", "$1"); 
			subdiv = subdiv.replaceAll("\\([^\\)]+\\)", "");
			subdiv = subdiv.replaceAll("\\b\\d+\\.\\d+.*", "");
			subdiv = subdiv.replaceAll("\\bAKA\\b.*", "");
			subdiv = subdiv.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+AD.*", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLK\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (ABST|SECT?(?:IO)?N?)\\b", "$1");
			subdiv = subdiv.replaceFirst("\\A\\s*(SECT?(?:IO)?N?)\\b", "");
			subdiv = subdiv.replaceFirst("\\A\\s*(ABST?(RACT)?)\\b", "");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceAll("\\A\\s*SUR\\s+\\d+", "");
			subdiv = subdiv.replaceAll("\\s+[\\d-\\.]+\\s+AC(RES?)?\\s*$", "");
			subdiv = subdiv.replaceFirst(",", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+#?[\\d-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\bPFS\\d+.*", "");
			subdiv = subdiv.replaceAll("['|#]\\s*$", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
	}
	
	public static void parseLegalMcLennan(ResultMap resultMap){
		//BELLMEAD CT Block 14 Lot 9
		//ABSTRACT 0346.00S22 FRAZIER W W, ACRES 32.9
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
			
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		
		legal = legal.replaceAll("(?is)\\b\\d+'?\\s*X\\s*\\d+'?(\\s+FT)?\\b", "");
		
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+AC(?:RES?)?\\b", "");
		legal = legal.replaceAll("(?is)\\b(BNG\\s+)?AN\\s+IRR(\\s+TR)?\\b", " ");
		legal = legal.replaceAll("(?is)\\bIRR\\s+TR\\b", " ");
		legal = legal.replaceAll("(?is)\\s+\\d+\\.\\d+\\s+", "");
		legal = legal.replaceAll("(?is)[S|W|N|E]{1,2}\\s*[\\d/'\\.]+(\\s+FT)?\\s+OF\\b", "");
		
		legal = legal.replaceAll("(?is)\\b-?\\s*[S|W|N|E]{1,2}\\s*[\\d/'\\.]+", " ");
		
		legal = legal.replaceAll("(?is)\\b(TR)(\\d+)\\b", "$1 $2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)-\\s*(UNIT|LOTS?|BLKS?)\\b", " $1");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s*([\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+(\\d+\\.\\d+S\\d+)");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*.?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:IO)?N?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|BLDG).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+?) (TR\\b|TRACT).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (ACRES?).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)([^,]+).*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\bMOBILE HOME ONLY\\b", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("(?is)\\s+[A-Z]\\s*$", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalMedina(ResultMap resultMap){
		parseLegalWaller(resultMap);
	}
	
	public static void parseLegalNavarro(ResultMap resultMap){
		//CORSICANA OT 452W 4
		//LOT: C-2 & N PT OF D BLOCK: 449 SUBD: CORSICANA OT
		//A10655 T J PALMER ABST 8
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT(\\s+OF)?\\b", "");
		legal = legal.replaceAll("(?is)\\+", "");
		
		legal = legal.replaceAll("(?is)\\b(?:(?:[SWNE]{1,2}\\s+)?PT\\s+OF)\\s+(\\d+\\s+&)", "$1");
		legal = legal.replaceAll("\\b(?:[SWNE]{1,2}\\s+[\\d/]+\\s+OF|PT)\\s+(\\d+)\\s+&\\s+ALL\\s+(\\d+)\\b", "$1&$2");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\b,\\s+(\\d+)\\b", ",$1");
		legal = legal.replaceAll("(?is)\\s*&\\s*(\\d+)", "&$1");
		String legalOriginal = legal;
		legalOriginal = legalOriginal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([A-Z]|[A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?:?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		
		
		String subdiv = legalOriginal;
		p = Pattern.compile("(?is)\\bSUBD:\\s*(.+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)(.*)\\s+(BLO?C?KS?)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
						
			subdiv = subdiv.replaceAll("(?is)\\A\\s*A\\d+\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT|UNDIVIDED INTEREST).*", "$1");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			
			subdiv = subdiv.replaceAll("\\s+#\\d+", "");
			
			subdiv = subdiv.replaceAll("\\bPT\\s+(\\d+)\\s+&\\s+ALL\\s+(\\d+)\\b", "$1&$2");
			
			p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?|[A-Z]{1,2})\\b");
			subdiv = extractPhase(resultMap, subdiv, p);
			
			p = Pattern.compile("(?is)\\b(ABST)\\s+([\\d-]+[A-Z]?)\\b");
			subdiv = extractAbst(resultMap, subdiv, p);
			
			subdiv = subdiv.replaceAll("\\b([A-Z])\\s*,\\s*([A-Z]&[A-Z])", "$1,$2");
			subdiv = subdiv.replaceAll("\\s*,\\s*$", "");
			
			p = Pattern.compile("(?is)\\b([\\d&,/-]+[A-Z]?|[A-Z])(\\s+[A-Z]?[\\d-&]+(?:[A-Z]?(?:-\\d+)?)|\\s+[A-Z]|\\s+[A-Z,&]{5})?\\s*$");
			ma = p.matcher(subdiv);
			
			if (ma.find()) {
				if (ma.group(2) != null){
					String block = ma.group(1);
					block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
					block = LegalDescription.cleanValues(block, false, true);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
					
					String lot = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName()); 
					if (lot == null){
						lot = " ";
					} else {
						 lot += " ";
					}
							
					lot += ma.group(2);
					lot = lot.replaceAll(",", " ");
					lot = lot.replaceAll("/", "-");
					lot = lot.replaceAll("\\A-", "");
					lot = lot.replaceAll("\\s*&\\s*", " ").trim();
					lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
					lot = LegalDescription.cleanValues(lot, false, true);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
				} else {
					String lot = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName()); 
					if (lot == null){
						lot = " ";
					} else {
						 lot += " ";
					}
					lot += ma.group(1);
					lot = lot.replaceAll(",", " ");
					lot = lot.replaceAll("/", "-");
					lot = lot.replaceAll("\\A-", "");
					lot = lot.replaceAll("\\s*&\\s*", " ").trim();
					lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
					lot = LegalDescription.cleanValues(lot, false, true);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
				}
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)(.+) (ABST|BLO?C?KS?|SURVEY|SH\\b|PH\\b|PHASE\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	public static void parseLegalNueces(ResultMap resultMap) throws Exception{
		parseLegalDallas(resultMap);
	}
	
	public static void parseLegalOrange(ResultMap resultMap){
		//ABST. 15  J. JETT (LOT PART OF 34 & 36 BLANDALE)
		//LOT 12  BLK 1 BON AIR COURTS
		//ABST. 384  W. C. SHARP  TR 085
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+AC(RE)?S?\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*/\\s*\\d+\\b", "");
		legal = legal.replaceAll("(?is)\\s*[SWNE]{1,2}\\s*\\d+\\s*FT(\\s+OF)?\\s*", "");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s*X\\s*\\d+'?\\b", "");
		legal = legal.replaceAll("(?is)\\b(LOT|BLK) PART OF\\b", "$1 ");
		 
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");

		legal = legal.replaceAll("(?is)\\b[A-Z]#.*", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		Pattern p = Pattern.compile("(?is)\\b(ABST\\.?)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([-,&\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal.replaceAll("(\\d+)\\s*[,&]\\s*(\\d+)", "$1,$2"), p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z](?:[\\d-]+)?|\\d&;]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)\\s+#?([-,&\\w\\d]+)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(ABST\\.?)\\s+(.*?)\\s+(TR(?:ACT)?S?)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(.*)\\s+(UNIT)\\b");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {		
				ma.reset();
				p = Pattern.compile("(?is)\\b(BLKS?)\\s+(.*)");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(LOT)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find() && legal.startsWith("LOT ")) {
						subdiv = ma.group(2);
					} else {
						ma.reset();
						p = Pattern.compile("(?is)\\A([^,]+)");
						ma.usePattern(p);
						if (ma.find()) {
							subdiv = ma.group(1);
						} 
					}
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceAll("\\AABST\\.?", "");
			subdiv = subdiv.replaceAll("\\([^\\)]+\\)", "");
			subdiv = subdiv.replaceAll("\\b\\d+\\.\\d+.*", "");
			subdiv = subdiv.replaceAll("\\bAKA\\b.*", "");
			subdiv = subdiv.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+AD.*", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLK\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (ABST|SECT?(?:IO)?N?|TRACT|TR)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+SER(IAL)?\\s*\\.?\\s*#.*", "");
			
			subdiv = subdiv.replaceAll("(?is)\\A([^,]+).*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\A([^#]+).*", "$1");
			
			subdiv = subdiv.replaceAll("\\s*\\p{Punct}\\s*$", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
	}
	
	public static void parseLegalPaloPinto(ResultMap resultMap){
		//AB 431 TR 12-A  GW TREIGHENBLK 12G#10431-00-0012 -00A-00-0LII
		//CLIFFS PHASE VII LOT 20 (AKA LOTS 19&20) G#C0250-07-00000-020-00-0   CLIFFS PHASE VII
		//PK LAKE 8-1-93 AREA 2-1 LOT 76 (FEE SIMPLE .650 AC & FERC .110 AC)  G#P0500-00-02010-076-00-0  DOCK #7224 & #10910/N DLS0087032   PK LAKE
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(PT\\s+OF)\\b", " $1 ");
		legal = legal.replaceAll("(&\\s*ADJ)\\b", " $1 ");
		legal = legal.replaceAll("(?is)(#\\s*|&\\s*)?(\\d+)?\\.\\d+\\s*AC(RE)?S?\\b", " ");
		legal = legal.replaceAll("(?is)(\\s+PT\\s+OF)?\\s+\\d?\\.\\d+\\s+AC(RE)?S?\\b", "");
		legal = legal.replaceAll("(SER(?:IAL)?\\s*\\.?\\s*#.*)", " $1");
		legal = legal.replaceAll("(RAD\\d+.*)", " $1");
		legal = legal.replaceAll("(DOCK ONLY)", " $1 ");
		legal = legal.replaceAll("(?is)[A-Z]/[A-Z]#\\s*[A-Z\\d]{5,}.*", "");
		legal = legal.replaceAll("(?is)[A-Z]#[A-Z\\d]{5}-.*", "");
		
		legal = legal.replaceAll("(?is)(?:[SWNE]{1,2}\\s*/\\s*\\d)\\s*OF\\s*(\\d+)", " $1");
		legal = legal.replaceAll("(?is)(?:[SWNE]{1,2}\\s*/\\s*\\d)\\s*OF\\b", "");
		
		legal = legal.replaceAll("(?is)(BLO?C?KS?|LO?TS?|PHASE)\\b", " $1");
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+(ALL)(\\d+)\\b", "$1 $3");
		
		legal = prepareLegal(legal);
			
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([A-Z]?[\\d,&-]+[A-Z]?-?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(AB)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*#?\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?)\\s*([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		subdiv = subdiv.replaceAll("(?is)\\([^\\)]+\\)", "");
		subdiv = subdiv.replaceAll("(?is)([^\\(]+)", "$1");
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A\\s*AB\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(TR\\b|TRACT)\\s*", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?i)(.+?) (BLO?C?KS?|TR\\b|TRACT\\b).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("(?is)\\bABST\\b", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) [\\d-]+\\s+AREA\\s+(?:\\d+)?.*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) [\\d-]+\\s*$", "$1");
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalPanola(ResultMap resultMap){
		//WESTERN HILLS LT 3
		//JONES ADDITION, BLOCK 2, LOT 5, SECT 2
		//AB 133 J COATS LOT 14 ACRES 1.50
		//BLK 94-A LT 1,PT LT 2 P-1
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		String untouchedLegal = legal;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\bP-\\d+\\b", "");
			
		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([A-Z]?[\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\.?\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s*([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&]+|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		
		if (untouchedLegal.trim().startsWith("BLK")){
			p = Pattern.compile("(?is)\\bTR\\s*&?\\s*(.+)");
			Matcher ma = p.matcher(legal);
			
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(A-\\d+[A-Z]?)\\b");
			Matcher ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(AB|ABST\\.?|TR\\b|TRACT|BLO?C?KS?|LO?TS?)\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|PARCEL|SCAD#).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("\\p{Punct}\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalPotter(ResultMap resultMap){
		//Potter
		//HAMLET # 4 CORR LOT 003     BLOCK 0036
		//BEVERLY GARDENS LOT         BLOCK 0001 S 1/2 OF 2 LESS E 10FT ROW
		
		
		//Randall
		//SLEEPY HOLLOW #6 AMD LOT         BLOCK 0015 CREIGHTON PLACE 20 EXC S 4FT
		//SOUTH LAWN # 4 LOT 017     BLOCK 016DW
		//RIDGECREST ADDN # 16 LOT         BLOCK 0053 SE 12FT OF 12 & 13 LESS SE 8.8FT
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+AC(RES)?(\\s+OF)?\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		legal = legal.replaceAll("(?is)\\bELY\\s+PTN\\s+OF\\b", "");
		
		legal = legal.replaceAll("(?is)\\bIRREG\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		legal = legal.replaceAll("(?is)\\b\\d+\\s*FT\\s*X\\s*\\d\\s*FT\\b", "");
		legal = legal.replaceAll("(?is)\\b(LESS\\s+)?([SWNE]{1,2}\\s+)?[\\dO'\\.]+\\s*FT(\\s+[SWNE]{1,2})?\\s+(&|OF\\b)", "");
		legal = legal.replaceAll("(?is)\\b(EXC|LESS)\\s+([SWNE]{1,2}\\s+)?[SWNE]{1,2}\\s+[\\d'\\.]+\\s*FT\\b", "");
		legal = legal.replaceAll("(?is)(&\\s*|\\bBOTH\\s+|\\b)LESS\\s+[SWNE]{1,2}\\s+\\d+\\s*FT\\s+(ALLEY\\s+)?ROW\\b", "");
		legal = legal.replaceAll("(?is)\\b(LESS\\s+)?(ALLEY\\s+)?ROW\\b", "");
		legal = legal.replaceAll("(?is)\\s+ON\\s+(\\d+)$", "&$1");
		legal = legal.replaceAll("(?is)\\bIN\\s+(.*?)\\s+OF", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(\\d+)(FT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");		
		
		legal = legal.replaceAll("(?is)&\\s+ALL(\\s+OF)?\\b", "&");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*THRU\\s*(\\d+).*", "$1-$2");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(PLUS)\\s*(\\d+)", "$1&$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		legal = legal.replaceAll("(?is)\\b(BLOCK)\\s+(TR)\\b", "$1; $2");
		
		legal = legal.replaceAll("(?is)\\bPLACE\\s*\\d+", "");
		
		Pattern p = Pattern.compile("(?is)\\b(TRACTS?|TRS?)\\s+([\\d,&-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		legal = legal.replaceAll("(?is)\\b(LOT\\s+BLOCK.*?)([\\d&-]+)\\s*$", "$1 LOT $2");
		
		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d,&-]+[A-Z]?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?S?)\\s+([\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		legal = legal.replaceAll("\\A\\s*LOT\\b", "");
		legal = legal.replaceAll("\\A\\s*(BLK|UNI?T)\\b", "");
		legal = legal.replaceAll("\\A\\s*SURVEY-\\b", "");
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABST)\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*RESUBD\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll("\\b(REPL(AT)?|AMD)\\b", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = subdiv.replaceAll("(?is)\\bSECT\\b", "");
			String unit = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
			if (StringUtils.isEmpty(unit)) {
				Matcher matcher = Pattern.compile("(.+)#\\s*(\\d+)").matcher(subdiv);
				if (matcher.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), matcher.group(2));
				}
			}
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b|ADDN\\b|CORR\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?|ACRES).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL|AMENDED).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s*[&|\\.|;|-]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\p{Punct}\\s*$", "");
			subdiv = subdiv.replaceAll("\\b[SWNE]{1,2}\\s*[\\d'\\.]+", ""); 
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalRandall(ResultMap resultMap) throws Exception{
		parseLegalPotter(resultMap);
	}
	public static void parseLegalRockwall(ResultMap resultMap) throws Exception{
		//HIDDEN VALLEY EST #2, LOT 17, ACRES 5.016
		//A0022 J H BAILEY,TRACT 1-2,.821 ACRES
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNE]{1,2}|LESS)?\\s*[\\d,\\.'/]+(?:\\s*F\\s*T)?\\s*OF\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNE]{1,2}|LESS)?\\s*[\\d,\\.'/]+(?:\\s*FT)\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNE]{1,2})\\s+PT\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = legal.replaceAll("(?is)&\\s+ALL(\\s+OF)?\\b", "&");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+[A-Z]?|[A-Z]{1,2})(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?|U\\s+)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\AA?(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s(ACS|UNIT)\\b.*", "$1");
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)(\\s+U\\s*N?\\s*I?T\\s*-?|-UNRECORDED)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\A\\s*-\\s*", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ");
			subdiv = subdiv.replaceAll("\\p{Punct}\\s*$", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
			}
		}
	}
	
	public static void parseLegalSmith(ResultMap resultMap) throws Exception{
		//BRIARWOOD BRIARWOOD   LT 5 BL 1005F
		//ABST A0003 M D L CARMONA TRACT 129B   TR 129B (PT OF 4AC TR SEE TR129B.1)
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		legal = legal.replaceAll("(?is)\\bV\\s+([A-Z]+)", "@@@$1");	//V MOORE, account number 100000001502060001
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)@@@([A-Z]+)", "V $1");
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");
		legal = legal.replaceAll("(?is)&\\s+ALL\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?(?:/SPACE)?)\\s*([A-Z]?[\\d,&-]+[A-Z]?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?S?)\\s*([\\d,/-]+[A-Z]?)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACT)\\s+([\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?S?)\\s+([\\d&]+(?:\\s*[A-Z])?)\\b");
		legal = extractSection(resultMap, legal, p);
				
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A(.+)\\s+TRACT\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?K?S?\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(ABST)\\s+(A-?\\d+[A-Z]?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			if (subdiv.contains("- ")){
				subdiv = subdiv.replaceAll("(?is)\\A([^-]+)", "");
			}
			subdiv = subdiv.replaceAll("(?is)\\A\\s*RESUBD\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll(",", " ");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?|ACRES).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL|AMENDED).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s*[&|\\.|;|-]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\b[SWNE]{1,2}\\s*[\\d'\\.]+", ""); 
			
			subdiv = subdiv.replaceAll("(?is)\\b(\\w+[\\s-]+\\w+)\\s+\\1\\b", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(\\w+)\\s+\\1\\b", "$1");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
		
	}
	
	public static void parseLegalSomervell(ResultMap resultMap) throws Exception{
		//REYNOLDS BEND ADDITION, LOT 10, ACRES .257   R0800
		//A136 MILAM CO SCH LD, TRACT E3-10, ACRES .24    E3-10 A136
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		legal = legal.replaceAll("(?is)& OTHER SURVEYS\\b", " ");

		legal = legal.replaceAll("(?is)\\b[\\d\\.,]+'?\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&)\\s*(\\d+)", "$1$2$3");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d/-]+|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([A-Z]?[\\d&-\\.]+|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*#?\\s*((?:[A-Z]|\\d+)-[A-Z]|[\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^\\,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\b(?:A)([\\d-]+([A-Z]{1,2})?)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			} else {
				p = Pattern.compile("(?is)\\b(?:ABST(?:RACT)?\\s+)([\\d-]+([A-Z]{1,2})?)\\b");
				ma = p.matcher(subdiv);
				if (ma.find()) {
					String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
					absNo = absNo.replaceAll("(?is)[-]+", " ");
					resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
					subdiv = subdiv.replaceAll(ma.group(0) + ".*", "");
				}
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\b(ABST?(RACT)?|MOBILE HOME|MANUFACTURED HOUSE).*", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", " ");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;|#]\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*\\d+\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalTaylor(ResultMap resultMap) throws Exception{
		//ELMWOOD WEST SEC 1, BLOCK D, LOT E65 OF 6 & W20 OF 7
		//HAMILTON HEIGHTS SEC 1, BLOCK D, LOT 27
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = legal.replaceAll("(?is)\\b[SWNE]{1}[\\d\\.']+\\s+(OF\\b)?", " ");

		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\,\\s*(\\d+)", "$1,$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*(?:TO|THRU)\\s*(\\d+)", "$1-$2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)([^,]+)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		// extract abstract number from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\AA?(\\d+)\\b");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
			subdiv = subdiv.replaceAll(ma.group(0), "");
		}
		if (StringUtils.isNotEmpty(absNo)){
			absNo = absNo.replaceAll("(?is)[-]+", " ");
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
				
		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s(UNDIV|REP(?:LAT)? OF|OUTLOT).*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
					.replaceFirst(",", "")
					.replaceFirst("(?is)\\A\\s*-\\s*", "")
					.replaceFirst("(?is)\\b(NO\\s+)?\\d+\\s*$", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ");
			
			subdiv = subdiv.replaceAll("(?is)\\b[SWNE]{1}[\\d\\.]+\\s+(OF)?\\d?", " ");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
			}
		}
	}
	
	public static void parseLegalTomGreen(ResultMap resultMap) throws Exception{
		//LOT 31 BLK 6 ARONS GLEN ADDITION
		//LOT 19 BLK 4-E CHAPMAN S/D
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		
		legal = legal.replaceAll("(?is)\\b\\d+/\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*;\\s*(\\d+)&\\s*(\\d+)", "$1&$2&$3");
		legal = legal.replaceAll("(?is);\\s+", ";");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(LOT\\s+)?PT\\s+\\d+(\\s+&)?(?:\\s+ALL\\s+\\d+)?", "");
		legal = legal.replaceAll("(?is)\\bLOT\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\.\\d]+\\s*INT\\b", "");
		
		legal = legal.replaceAll("(?is)(NORTH|SOUTH|EAST|WEST)\\b", " $1");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		Pattern p = Pattern.compile("(?is)\\b(ABST\\s+|A-)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([-&\\w\\d]+)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\A\\s*([\\d&,/-]+)\\s+BLO?C?K\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll(",", " ");
			lot = lot.replaceAll("/", "-");
			lot = lot.replaceAll("\\A-", "");
			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
		}
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z]|\\d&;-]+[A-Z]?)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?T?(?:ION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)(?:\\s+TRS)?\\s+([[A-Z-]|\\d]+(?:(?:\\s+[[A-Z-]|\\d]+)?\\s*&\\s[[A-Z-]|\\d]+)?)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLO?C?K)\\s+(.*)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LO?TS?)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)(.+)\\s+ABST\\b");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					ma.reset();
					p = Pattern.compile("(?is)\\b(TRACT)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find()) {
						subdiv = ma.group(2);
					} else {
						subdiv = legal;
					}
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLK\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (ABST|SECT?(?:IO)?N?)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");
			subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
			subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
			subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
			subdiv = subdiv.replaceFirst(",", " ");
			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d-]+\\s*$", "");
			subdiv = subdiv.replaceAll("/SAISD\\b", "");
			
			subdiv = subdiv.replaceAll("(?is)\\bBEING\\s+\\d+(\\.\\d+)?\\s+AC(RE)?S?\\b", "");
			
			subdiv = subdiv.replaceAll("(?is)\\b(\\w+[\\s-]+\\w+)\\s+\\1\\b", "$1");
			subdiv = subdiv.replaceAll("(?is)\\A.*\\s+S-\\d+\\b", "");
			
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*(.*)\\s*TRACTS?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*BLO?C?KS?\\s+\\d+", "");
			
			subdiv = subdiv.replaceAll("(?is)-\\s*NO\\s*\\.", "");
			subdiv = subdiv.replaceAll("(?is)\\b\\d+\\.\\d+.*", "");
			
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
		
	}
	
	public static void parseLegalTravis(ResultMap resultMap) throws Exception{
		//LOT 27 BLK C LOST CREEK AT GAINES RANCH SUBD REPLAT
		//50% OF LOT 29-31 BLK 2 BON AIR KNOLLS***UNDIVIDED INTEREST ACCOUNT***
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");

		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s*\\d+\\.\\d+'\\s+(OF)\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s*[\\d+\\.]+'", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s*[\\d+\\.]+X[\\d+\\.]+\\s+TRI\\b", "");
		
		legal = legal.replaceAll("(?is)\\b\\d+/\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*&\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*;\\s*(\\d+)&\\s*(\\d+)", "$1&$2&$3");
		legal = legal.replaceAll("(?is);\\s+", ";");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(LOT\\s+)?PT\\s+\\d+(\\s+&)?(?:\\s+ALL\\s+\\d+)?", "");
		legal = legal.replaceAll("(?is)\\bLOT\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\.\\d]+\\s*INT\\b", "");
		legal = legal.replaceAll("(?is)\\*{3}[^\\*]+\\*{3}", "");
		
		legal = legal.replaceAll("(?is)(NORTH|SOUTH|EAST|WEST)\\b", " $1");
		
		legal = legal.replaceAll("(?is)\\b(VILLAGE)(SEC)\\b", "$1 $2");

		//legal = GenericFunctions.replaceNumbers(legal);

		Pattern p = Pattern.compile("(?is)\\b(ABST?)\\s*([\\d-]{1,})\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(LO?TS?:?)\\s+([-&\\\\w\\d,]+[A-Z]?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?K?|BK)\\s+([[A-Z]|\\d&;-]+[A-Z]?)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC?T?(?:ION:?)?)\\s+(\\d+-?[A-Z]?|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(TRA?C?T?S?)(?:\\s+TRS)?\\s+([[A-Z-]|\\d]+(?:(?:\\s+[[A-Z-]|\\d]+)?\\s*&\\s[[A-Z-]|\\d]+)?)\\b");
		legal = extractTract(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG?)\\s+([A-Z]{1,3}|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PHA?S?E?S?)\\s+([A-Z]{1,2}|\\d+)\\b");
		legal = extractPhase(resultMap, legal, p);

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLO?C?K|BLDG?)\\s+(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(LO?TS?|UNI?T)\\s+(.*)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\bABS\\s+(.*)");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					ma.reset();
					p = Pattern.compile("(?is)\\b(TRACT)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find()) {
						subdiv = ma.group(2);
					} else {
						subdiv = legal;
					}
				}
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(UNI?T?\\s*-?|PH(?:ASE)?S?)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLO?C?KS?\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (SECT?(?:IO)?N?)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) (&?\\s*ABST?)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?) BLDG\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*?)\\bLTS?\\b", "$1");

			subdiv = subdiv.replaceFirst("(?is)([^\\(]+)\\(.*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d-]+\\s*$", "");
			
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*(.*)\\s*TRACTS?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*BLO?C?KS?\\s+\\d+", "");
			subdiv = subdiv.replaceFirst("(?is)\\b(THE RESUB OF|AMENDED PLAT OF LOT|UNRECORDED)\\b", "");
			
			subdiv = subdiv.replaceFirst("(?is)\\*?\\s*(RESUB\\s+OF|&)\\s+LOTS?\\b", "");
			subdiv = subdiv.replaceFirst("(?is)\\bAMENDED.*", "");
			subdiv = subdiv.replaceFirst("(?is)\\b(PLUS|MH |REVISED).*", "");
			
			subdiv = subdiv.replaceAll("(?is)\\A.*SUR\\s+\\d+\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bACR\\s*\\d+(\\.\\d+)?\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bACR\\s*\\.\\d+\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bLOT\\b", "");
			subdiv = subdiv.replaceAll("[,\\(\\)]+", " ");
			subdiv = subdiv.replaceAll("\\*+", " ");
					
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}

		}
		
	}

	public static void parseLegalVanZandt(ResultMap resultMap){
		//ABST: 74, SUR:  J BAYES
		//BLK:  4, LOT:  8, ADDN: TOWN & COUNTRY
		//BLK:  4, LOT:, ADDN: LL5  CANTON
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\b(MH ONLY|PTS?|S/SIDE)", "");
		legal = legal.replaceAll("(?is),\\s{3,}", ", ");
		legal = legal.replaceAll("(?is),\\s+&", "& ");
		
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");
		legal = legal.replaceAll("(?is)(\\d+-)\\s+([A-Z]{2,})", "$1$2");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(;|,)\\s*(\\d+)", "$1$2$3");
		
		legal = GenericFunctions.replaceNumbers(legal);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([\\d&,;-]+(?:[A-Z]{1,2})?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*((?:(\\d+)-[A-Z]\\d+[A-Z])|(?:[\\d;]+)|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?:?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*IT\\s*:?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST(?:RACT)?)\\s*:?\\s+([\\d&]+)\\b");
		legal = extractAbst(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*:?\\s*(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s*:\\s*([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s*:\\s*([\\w-&/]+)\\b");
		legal = extractTract(resultMap, legal, p);
						
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(ADDN|SURV?):\\s*(.*)");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(2);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("(?is)([^,]+).*", "$1");
			subdiv = subdiv.replaceFirst("(?is)(.*?)\\s+(SEC|EXT|PFS|AKA|SER#?)\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\bOF\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)(.*)\\s+([\\d\\s]+)\\s+\\1", "$1");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalVictoria(ResultMap resultMap){

		//MAYFAIR I LOT 14 BLOCK 2
		//ABSTRACT 01690 C DE LA GARZA ABST 169, ACRES 1.
		//00920 ALEJO PEREZ ABST 92, ACRES 1.806
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.toUpperCase();
		legal = GenericFunctions.replaceNumbers(legal);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+[\\d\\.]+\\s+AC(\\s+OF)?\\b", "");
		legal = legal.replaceAll("(?is)\\bACRES\\s+[\\d\\.]+", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+(SERIAL)\\b", " $1");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");
		
		legal = legal.replaceAll("(?is)\\bIRREG\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[\\d']+\\s*X\\s*[\\d']+\\b", "");
		legal = legal.replaceAll("(?is)\\b\\d+\\s*FT\\s*X\\s*\\d\\s*FT\\b", "");
		legal = legal.replaceAll("(?is)\\b(LESS\\s+)?([SWNE]{1,2}\\s+)?[\\d'\\.]+\\s*FT\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(EXC|LESS)\\s+([SWNE]{1,2}\\s+)?[SWNE]{1,2}\\s+[\\d'\\.]+\\s*FT\\b", "");
		legal = legal.replaceAll("(?is)(&\\s*|\\bBOTH\\s+|\\b)LESS\\s+[SWNE]{1,2}\\s+\\d+\\s*FT\\s+(ALLEY\\s+)?ROW\\b", "");
		legal = legal.replaceAll("(?is)\\bLESS\\s+ALLEY\\s+ROW\\b", "");
		legal = legal.replaceAll("(?is)\\bIN\\s+(.*?)\\s+OF", "");
		
		legal = legal.replaceAll("(?is)\\b(EAST|WEST|SOUTH|NORTH) PT OF\\b", "");
		legal = legal.replaceAll("(?is)\\b@", "AT ");
		legal = legal.replaceAll("(?is)\\b\\d+\\s*'\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\d'\\.]+\\s+TRI OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(\\d+)(FT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\([^\\)]*\\)", "");
		legal = legal.replaceAll("(?is)\\(.*", "");
		legal = legal.replaceAll("(?is),&", "&");		
		
		legal = legal.replaceAll("(?is)&\\s+ALL(\\s+OF)?\\b", "&");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(PLUS)\\s*(\\d+)", "$1&$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|,)\\s*(\\d+)", "$1$2$3");
		legal = legal.replaceAll("(?is)\\b(BLOCK)\\s+(TR)\\b", "$1; $2");
		
		legal = legal.replaceAll("(?is)\\bPLACE\\s*\\d+", "");
		
		Pattern p = Pattern.compile("(?is)\\b(TRACT|TR)\\s+([A-Z]?[\\d,&-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TRACTS|TRS)\\s+(\\d+\\s+\\d+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		//5200000300100#####STUBBLEFIELD LOTS 1 2 3 & 9 & PTS OF LOTS 10 & LOT 11 BLOCK 3
		legal = legal.replaceAll("(?is)\\bLOTS\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*&\\s*(\\d+)\\b", "LOT $1,$2,$3,$4");
		
		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d,&-]+[A-Z]?\\d?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:I?T)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+-?[A-Z]?|[A-Z]{1,2})\\b");
		legal = extractPhase(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SECT?(?:ION)?S?)\\s+([\\d&]+(?:\\s*[A-Z])?|[A-Z]{1,2})\\b");
		legal = extractSection(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(ABST|A-)\\s*(\\d+[A-Z]?)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		legal = legal.replaceAll("\\A\\s*LOT\\b", "");
		legal = legal.replaceAll("\\A\\s*(BLK|UNI?T)\\b", "");
		legal = legal.replaceAll("\\A\\s*SURVEY-\\b", "");
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(.+)\\s+UNIT\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+BLO?C?KS?\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\A(.+)\\s+LO?TS?\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					} else {
						p = Pattern.compile("(?is)\\A(.+)\\s+ABST\\b");
						ma = p.matcher(legal);
						if (ma.find()) {
							subdiv = ma.group(1);
						}
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(ABSTRACT\\s*)?\\d+", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*RESUBD\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|TRACT).*", "$1");
			
			subdiv = subdiv.replaceAll("\\b(REPL(AT)?|AMD)\\b", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b|ADDN\\b|CORR\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?|ACRES).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL|AMENDED).*", "$1");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s*[&|\\.|;|-]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\p{Punct}\\s*$", "");
			subdiv = subdiv.replaceAll("\\b[SWNE]{1,2}\\s*[\\d'\\.]+", ""); 
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalWilliamson(ResultMap resultMap){
		//DOAK ADDITION, BLOCK 53, LOT 4
		//AW0131 COURSEY, P. SUR., ACRES 1.0
		//GARDEN PARK SEC 1, BLOCK A, LOT 8, ACRES 0.750
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+#?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABST?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*([\\d&-]+[A-Z]?)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.*\\bSUR\\.?),");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A([^\\.]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(AB?-?)\\s*([\\d&]+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
				subdiv = subdiv.replaceAll("(?is)\\b(A-?\\d+)\\b", "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\bA[A-Z]\\d+", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*-\\s*", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\.", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = subdiv.replaceAll("\\bBLDG\\s*$", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalWilson(ResultMap resultMap){
		//KOTHMANN SUB, LOT 11NB (E 1/2) & 12NA (W 1/2), ACRES 5.0
		//A0022 M XIMENEZ SUR, TRACT 13A, ACRES 15.0
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = GenericFunctions.replaceNumbers(legal);
//		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
//		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");
		legal = legal.replaceAll("(?is)\\bOUT OF ORG\\b", "");
		legal = legal.replaceAll("(?is)\\bMH(\\s+LOC)?(\\s+ON)?\\s+[\\d#-]+\\b", "");
		legal = legal.replaceAll("(?is)\\bMH\\s+LOC\\b", "");
		legal = legal.replaceAll("(?is)\\bF/B\\s+\\d+(-[A-Z])?\\b", "");

		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&)\\s*(\\d+)", "$1$2$3");
		
		legal = legal.replaceAll("(?is)\\b(LOT)\\s+(\\d+[A-Z]{1,2})\\s+&\\s+(\\d+[A-Z]{1,2})\\b", "$1 $2 $1 $3");
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d,&-]+(?:[A-Z]{1,2})?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d/-]+|[A-Z])\\b");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s+([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\d&\\.-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s+((?:[A-Z]|\\d+)-?[A-Z]|[\\d&]+)\\b");
		legal = extractSection(resultMap, legal, p);
		
		String subdiv = legal;
		p = Pattern.compile("(?is)\\A([^\\,]+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			
			p = Pattern.compile("(?is)\\A\\s*(?:A)(\\d+)\\b");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(1).replaceAll("\\s*&\\s*", " "), "0").trim();
				absNo = absNo.replaceAll("(?is)[-]+", " ");
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
				subdiv = subdiv.replaceAll(ma.group(0), "");
			}
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?).*", "$1");
			
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalWise(ResultMap resultMap){
		//7R1  GRAND OAKS ESTATES
		//7 1 FOREST HILL ADDITION  A-627 MEP & PRR
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\b(MH ONLY|MIDDLE)\\b", "");
		legal = legal.replaceAll("(?is)\\b(SENT)\\b.*", "");
		legal = legal.replaceAll("(?is),\\s{3,}", " ");
		
		legal = legal.replaceAll("(?is)\\b([SWNE]+|FRONT)\\s*\\d+[,/\\.]?(\\d?'?)?(\\s+[O|0]F)?", "");
		legal = legal.replaceAll("(?is)\\b\\d+'(\\s+[O|0]F)", "");
		
		legal = legal.replaceAll("(?is)\\b[A-Z][\\d-]{10,12}\\b", "");
		
		legal = prepareLegal(legal);
		
		legal = legal.replaceAll("(?is)\\A\\s*([SWNE]{1,2}\\s+)?PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\s+OF", "");
		
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\s*,\\s*,\\s*", " , ");
		
		legal = legal.replaceAll("(?is)\\A\\d{4,}\\b", "");
		
		legal = legal.replaceAll("(?is)#\\d+\\b", "");
		
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s*\\d+'\\b", "");
		
		legal = legal.replaceAll("(?is)\\b(\\d+)\\s*,\\s*(\\d+)\\s*$", "$1,$2");
		
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+),(\\s+\\d+\\s+\\w+)\\s+&\\s*(\\d+)", "$1&$3 $2");
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(\\s+\\d+\\s+\\w+)\\s+(?:&|ALL)\\s*([\\d,]+)\\s*$", "$1&$3 $2");
		

		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(\\s+[A-Z\\s]+)\\s+([\\d,-]+)\\s*$", "$3 $1 $2");
		legal = legal.replaceAll("(?is)\\A\\s*(\\d+)(?:&|-)(\\s*\\d+\\s+[A-Z\\s]+)\\s+([\\d,-]+)\\s*$", "$1&$3 $2");
		
		legal = legal.replaceAll("(?is)(.*?)(\\d+)\\s*([&|-|,])\\s*(\\d+)\\s*$", "$2$3$4 $1");

//		legal = GenericFunctions.replaceNumbers(legal);
//		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
//		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		legal = legal.replaceAll("(?is)\\s*&\\s*[SWNE]{1,2}\\s*\\d+\\s*OF\\s*(\\d)", "-$1");
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d&,-]+[A-Z]?|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		legal = extractLot(resultMap, legal, p);

		String blockRegEx = "";
		
		blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?)\\s*((?:\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?)\\b";
		p = Pattern.compile(blockRegEx);
		legal = extractBlock(resultMap, legal, p);
		
		legal = legal.replaceAll("(?is)(-\\d+)\\s+&\\s+(\\d)", "$1&$2");
		
		p = Pattern.compile("(?is)\\A([\\d&,-]+[A-Z]?|[A-Z])\\s+(\\d+|[A-Z])?\\b");
		Matcher ma = p.matcher(legal);
		
		if (ma.find()) {
			String lot = ma.group(1);
			lot = lot.replaceAll("\\s*[&;,]+\\s*", " ").trim();
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			
			if (ma.group(2) != null){
				String block = ma.group(2);
				block = block.replaceAll("\\s*[&;,]+\\s*", " ").trim();
				block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
			}
			legal = legal.replaceAll(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(UN\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?|SECT)\\s+(\\d+|[A-Z])");
		legal = extractSection(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([A-Z]?[\\d,-]+[A-Z]?|[A-Z])\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(A-)([\\d+\\s&]+)\\b");
		legal = extractAbst(resultMap, legal, p);
						
		legal = legal.replaceAll("(?is)\\bTRACT\\b", "");
		
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\A(.*)$");
		ma = p.matcher(legal);
		
		if (ma.find()) {
			subdiv = ma.group(1);
		}
				
		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("\\b(SEC)\\b.*", "");
			
			subdiv = cleanSubdivisionName(subdiv);
			
			subdiv = subdiv.replaceAll("(?is)\\b\\d+\\s*X\\s*\\d+\\b.*", "");
			subdiv = subdiv.replaceAll("(?is)\\b[\\d,]+\\s*SQ\\s+FT\\b", "");
			subdiv = subdiv.replaceAll("[\\d,\\.]\\s+(ACRES|AC)\\b", "");
			subdiv = subdiv.replaceAll("\\b(MH|DCD|PARCEL)\\b.*", "");
			
			subdiv = subdiv.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+AD.*", "");
			
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?|SURVEY|SH\\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND|UNI?T?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (UDI|UND|NO LABEL).*", "$1");
			subdiv = subdiv.replaceAll("\\s+['\\d/\\.]+\\s*$", "");
			subdiv = subdiv.replaceAll("\\s+[&|\\.|;]\\s*$", "");
			subdiv = subdiv.replaceAll("#\\s*[\\d'\\s-]+\\s*$", "");
			subdiv = subdiv.replaceAll("-{2,}", "");
			
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	public static void parseLegalZapata(ResultMap resultMap){
		//LOPENO TOWNSITE (SANCHEZ), BLOCK 15, LOT 2
		//LOTS 15 & 16 LAKEVIEW SUBDIVISION BLOCK 164
		//ABST 151 PORC 202 A GARCIA CHARCO REDONDO GRANT
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
//		legal = GenericFunctions.replaceNumbers(legal);
//		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
//		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bOF\\s+[SWNE]{1,2}\\s*[\\d/]+\\b", "");

		legal = legal.replaceAll("(?is)\\b\\d+'\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)&\\s+\\d+\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b\\d+/\\d+\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(\\d{4}\\s+\\d+x\\d+)\\s+\\1", "");
		legal = legal.replaceAll("(?is)\\bPORC?(ION)?\\s+[\\d/]+\\b", "");
		
		legal = legal.replaceAll("(?is)\\bS/D\\b", "SUBDIVISION");
		
		legal = legal.replaceAll("(?is)\\((.*?SUBD[^\\)]*)\\)", " $1");
		
		legal = legal.replaceAll("(?is)\\b\\d+\\s+FEET OF\\b", "SUBDIVISION");
		legal = prepareLegal(legal);
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", "");
		
		legal = legal.replaceAll("(?is)\\b(LO?TS?)\\s+([\\d\\s,&-/]+)\\s*&\\s*([\\d/]+)", "$1 $2 $1 $3");
		
		Pattern p = Pattern.compile("(?is)\\b.*(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+).*");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			legal = legal.replaceAll("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)(\\s*&\\s*\\d+)", "$1 $2 $1 $3");
		}
		
		p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z]?[\\d\\s,&-]+[A-Z]?\\d?)\\b");
		legal = extractLot(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*([\\d&]+|[A-Z])\\b");
		legal = extractSection(resultMap, legal, p);
		legal = legal.replaceAll("(?is)\\b(SEC(?:TION)?)\\b", "");
		
		legal = legal.replaceAll("(?is)\\bBLOCK\\s+TR\\b", "BLOCK ");
		
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([A-Z]?[\\d,/-]+[A-Z]?|[A-Z]{1,2}|[A-Z][\\d/]+)");
		legal = extractBlock(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(UN(?:IT)?S?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		legal = extractUnit(resultMap, legal, p);

		p = Pattern.compile("(?is)\\b(ABS?T?(?:RACT)?)\\s+([A-Z]?[\\d+\\s&]+)\\s+(?:(?:SUR(?:VEY)?|P)\\s+\\d+)?\\b");
		legal = extractAbst(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		legal = extractTract(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		legal = extractBuilding(resultMap, legal, p);
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
		legal = extractPhase(resultMap, legal, p);
		
		legal = legal.replaceAll("(?is)\\A\\s*LOTS?\\s+(BLOCKS?\\s+)", "$1");
		legal = legal.replaceAll("(?is)\\A\\s*(LOT\\s+)?/\\d+\\s+", "");
		legal = legal.replaceAll("(?is)\\s+/\\d+\\s+", " ");
		
		String subdiv = "";
		if (legal.contains(",")){
			p = Pattern.compile("(?is)\\A([^,]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		} else {
			p = Pattern.compile("(?i)(.+)\\b(?:HT|BLO?C?KS?|BLDG|TR|ABST)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?i)\\b(?:BLO?C?KS?)\\s+(.+)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)){
			subdiv = subdiv.replaceAll("(?is)\\A(\\s*LOTS?)\\s+\\1?", "");
			subdiv = subdiv.replaceAll("(?is)\\A(\\s*BLOCKS?)\\s+", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*\\)\\s*", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*TR(\\s+\\d+)?\\s+", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?(RACT)?", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BUILDING ONLY\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.+) HT\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) PH(ASE).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (L[O|0]TS?|SHARE\b).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (BLO?C?KS?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.+) (TR\\b|TRACT|&\\s*UND).*", "$1");
			subdiv = subdiv.replaceAll("#\\s*[\\d-]?\\s*$", "");
			subdiv = subdiv.replaceAll("&\\s*$", "");
			subdiv = subdiv.replaceAll(",", "");
			subdiv = subdiv.replaceAll("\\bAC:\\s*[\\d\\.]+", "");
			subdiv = subdiv.replaceAll("\\b[\\d\\.]+\\s+ACR?E?S.*", "");
			subdiv = cleanSubdivisionName(subdiv);
				
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
				
			if (legal.matches(".*\\bCOND.*")){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}
	
	/**
	 * extract lot from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractLot(ResultMap resultMap, String legal, Pattern p){
		
		String lot = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		if (lot == null){
			lot = "";
		}
		String legalTemp = legal;
		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0");
				legalTemp = legalTemp.replaceFirst(ma.group(0), " LOT ");

			}
			lot = lot.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
			if (lot.length() != 0) {
				if (org.apache.commons.lang.StringUtils.countMatches(lot, "-") > 1 && lot.matches("(?is)[^-]+-\\d+-.*")){
					lot = lot.replaceAll("-", " ");
				}
				lot = LegalDescription.cleanValues(lot, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return legal;
	}
	
	/**
	 * extract block from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractBlock(ResultMap resultMap, String legal, Pattern p){
		
		String block = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		if (block == null){
			block = "";
		}
		String legalTemp = legal;
		
		try {
			Matcher ma = p.matcher(legal);
			if (ma.find()) {
				block = org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0").trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
				
			block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\s*;\\s*", " ").trim();
			
			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			}
			
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return legal;
	}
	
	/**
	 * extract ncb from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractNcb(ResultMap resultMap, String legal, Pattern p){
		
		String ncb = (String) resultMap.get(PropertyIdentificationSetKey.NCB_NO.getKeyName());
		if (ncb == null){
			ncb = "";
		}
		String legalTemp = legal;
		
		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				ncb = ncb + " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
			ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
			if (ncb.length() != 0) {
				ncb = LegalDescription.cleanValues(ncb, false, true);
				resultMap.put(PropertyIdentificationSetKey.NCB_NO.getKeyName(), ncb);
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
					
		return legal;
	}
	
	/**
	 * extract abstract number from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractAbst(ResultMap resultMap, String legal, Pattern p){
		
		String absNo = (String) resultMap.get(PropertyIdentificationSetKey.ABS_NO.getKeyName());
		if (absNo == null){
			absNo = "";
		}
		String legalTemp = legal;
		
		try {
			Matcher ma = p.matcher(legal);
			if (ma.find()) {
				absNo = org.apache.commons.lang.StringUtils.stripStart(ma.group(2).replaceAll("\\s*&\\s*", " "), "0").trim();
				if (StringUtils.isNotEmpty(absNo)){
					absNo = absNo.replaceAll("\\s*&\\s*", " ").trim();
					absNo = LegalDescription.cleanValues(absNo, false, true);
					resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);
					
					legalTemp = legalTemp.replaceFirst(ma.group(0), " ABST ");
					legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
					legal = legalTemp;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return legal;
	}

	/**
	 * extract phase from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractPhase(ResultMap resultMap, String legal, Pattern p){
		
		String phase = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName());
		if (phase == null){
			phase = "";
		}
		String legalTemp = legal;
		
		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				phase += " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0").trim();
				phase = phase.trim();			
				legalTemp = legalTemp.replaceFirst(ma.group(), "").trim().replaceAll("\\s{2,}", " ");
			}
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		phase = phase.replaceAll("\\s*&\\s*", " ").trim();
		phase = LegalDescription.cleanValues(phase, false, true);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		
		return legal;
	}
	
	/**
	 * extract tract from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractTract(ResultMap resultMap, String legal, Pattern p){
		
		String tract = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName());
		if (tract == null){
			tract = "";
		}
		String legalTemp = legal;
		
		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				tract += " " +org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0").trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
			}
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		tract = tract.replaceAll("\\s*&\\s*", " ").trim();
		tract = LegalDescription.cleanValues(tract, false, true);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		
		return legal;
	}

	/**
	 * extract section from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractSection(ResultMap resultMap, String legal, Pattern p){
		
		String section = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName());
		if (section == null){
			section = "";
		}
		String legalTemp = legal;
		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				section += " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0");
				legalTemp = legalTemp.replaceFirst(ma.group(), "").trim().replaceAll("\\s{2,}", " ");
			}
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		section = section.replaceAll("\\s*&\\s*", " ").trim();
		section = LegalDescription.cleanValues(section, false, true);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
		
		return legal;
	}

	/**
	 * extract building from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractBuilding(ResultMap resultMap, String legal, Pattern p){
		
		String bldg = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName());
		if (bldg == null){
			bldg = "";
		}
		String legalTemp = legal;

		try {
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				bldg = bldg + " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0").trim();
				bldg = bldg.trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
			}
			legal = legalTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		bldg = bldg.replaceAll("\\s*&\\s*", " ").trim();
		bldg = LegalDescription.cleanValues(bldg, false, true);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
				
		return legal;
	}

	/**
	 * extract unit from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractUnit(ResultMap resultMap, String legal, Pattern p){
		
		String unit = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
		if (unit == null){
			unit = "";
		}
		
		String legalTemp = legal;
		
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0").replaceAll("\\-\\s*$", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "UNIT ");
			
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		}
		legal = legalTemp;
		unit = unit.replaceAll("\\s*&\\s*", " ").trim();
		unit = LegalDescription.cleanValues(unit, false, true);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
		
		return legal;
	}
	

	/**
	 * extract subdivision name from legal description
	 * @param resultMap
	 * @param legal - String legal description
	 * @param p - Pattern regex
	 * @return
	 */
	public static String extractSubdivisionName(ResultMap resultMap, String legal, Pattern p){
		
		
		return legal;
	}
	
	public static String prepareLegal(String legal){
		
		if (legal == null){
			return "";
		}
		legal = legal.replaceAll("(?is)\\b(ALL|TRI)\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\b[SWNE]{1,2}\\s+PT\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\bPTS?(\\s+OF)?\\b", "");
		legal = legal.replaceAll("(?is)\\bS/D\\b", "");
		legal = legal.replaceAll("(?is)\\bUN IT\\b", "UNIT");

		legal = legal.replaceAll("(?is)\\b([SWNEM]+|FRONT)\\s*[\\d\\s,\\.'/]+(\\s*X\\s*[\\d\\s,\\.'/]+)?(?:\\s*F\\s*T)?(\\s*[O|0]\\s*F)\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNEM]+|FRONT)\\s*[\\d\\s,\\.'/]+(\\s*X\\s*[\\d\\s,\\.'/]+)?(?:\\s*F\\s*T)(\\s*[O|0]\\s*F)?\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNEM]+|FRONT)\\s*[\\d,\\.'/]+(\\s*X\\s*[\\d\\s,\\.'/]+)\\b", "");
		legal = legal.replaceAll("(?is)\\b([SWNE]{1,2}|FRONT)\\s*[\\d,'/]{2,}\\s+", "");
		legal = legal.replaceAll("(?is)\\bPT\\s+OF(\\s+OUT)?\\b", "");
		legal = legal.replaceAll("(?is)\\b([X\\d\\s,\\.'/]+)\\s+OUT\\sOF\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.'/]+(?:\\s*F\\s*T)?\\s*(?:[O|0]\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1&$2");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*\\*\\s*(\\d+)", "$1&$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*(&|-)\\s*(\\d+)", "$1$2$3");
		legal = legal.replaceAll("(?is)\\b([A-Z])\\s*(&|-)\\s*([A-Z])\\b", "$1$2$3");
		
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)(\\d+-)([A-Z]{2,})", "$1 $2");
		
		legal = legal.replaceAll("(?is)\\b(L)\\s*(\\d)\\b", "$1OT $2");
		legal = legal.replaceAll("(?is)\\b(B)\\s*(\\d)\\b", "$1LOCK $2");
		
		legal = legal.replaceAll("(?is)\\((NCB?[^\\)]+)\\)", " $1");
		
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\b(OUT)(LOT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b(\\d+)\\s+WEST\\b", "$1");
		legal = legal.replaceAll("(?is)(\\d+)\\s+THRU\\s+(\\d+)", "$1-$2");
		
		return legal.trim();
	}
	
	public static String cleanSubdivisionName(String subdiv){
		
		subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
		subdiv = subdiv.replaceFirst("(.*)\\s+(SECT?(?:ION)?)\\b", "$1");
		subdiv = subdiv.replaceFirst("(.+?)\\s+ABST?.*", "$1");
		subdiv = subdiv.replaceFirst("(.+?)\\sFLG.*", "$1");
		subdiv = subdiv.replaceFirst("(.+?)\\sBLK.*", "$1");
		subdiv = subdiv.replaceFirst("(.+?)\\sLO?TS?.*", "$1");
		subdiv = subdiv.replaceFirst("(.+?)\\sUNIT.*", "$1");
		subdiv = subdiv.replaceFirst("(.+)\\sCONDO.*", "$1");
		subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
		subdiv = subdiv.replaceFirst("(.+)\\s(?:[\\d/]+\\s+)UND.*", "$1");
		subdiv = subdiv.replaceFirst("(.+)\\sACS\\b.*", "$1");
	

		subdiv = subdiv.replaceAll("&", " & ");
		
		subdiv = subdiv.replaceFirst("(.*)\\s+(?:RE)?SUBD?.*", "$1")
				.replaceAll("(.*)\\s+S/D.*", "$1")
				.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
				.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
				.replaceFirst("(.+)\\s(FLG|BLK).*", "$1")
				.replaceFirst(",", "")
				.replaceFirst("(?is)\\A\\s*-\\s*", "")
				.replaceFirst("(?is)(\\bNO\\s+|#)\\s*\\d+\\s*$", "")
				.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
				.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
				.replaceAll("\\s+", " ").trim();
		
		return subdiv;
	}
}
