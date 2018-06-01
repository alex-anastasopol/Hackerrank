package ro.cst.tsearch.parser;

import java.util.LinkedList;

public class HTMLService {
public static synchronized  void deleteComplexTag(LinkedList<TagStruct> list,String tagName){
	 
}
public static synchronized  LinkedList<IndexTag> indexTag(LinkedList<TagStruct> list,String tagName,int startIndex, int endIndex){
	LinkedList<IndexTag> index=new LinkedList<IndexTag>();
	int stare=0;
	int start=0;
	IndexTag iTemp=new IndexTag();
	for(int i=startIndex;i<endIndex;i++){
		if((list.get(i).tagType==TagStruct.BEGIN_TAG)&&(list.get(i).tagName.equalsIgnoreCase(tagName))){
			++stare;
			if(stare==1){
				start=i;
			}
		}
		if((stare>0)&&(list.get(i).tagType==TagStruct.END_TAG)&&(list.get(i).tagName.equalsIgnoreCase(tagName))){
			//iTemp=
			if(stare==1){
			iTemp=new IndexTag();
			iTemp.endTag=i;
			iTemp.startTag=start;
			index.add(iTemp);
			}
			--stare;
		
		}
		
	}
	return index;
}
public static synchronized  String  toString(LinkedList<TagStruct> list,int start,int end){
	String tmp="";
		for(int i=start;i<end;i++){
			tmp+=list.get(i).toString();
		}
	return tmp;
}
public static synchronized  String[][] getMatrix(LinkedList<TagStruct> list){
	LinkedList<IndexTag> indexTR=indexTag(list,"TR",0, list.size());
	LinkedList<IndexTag> indexTD=null;
	int i=0;
	int j=0;
	String[][] tmp=new String[200][200];
	for(i=0;i<indexTR.size();i++){
		indexTD=indexTag(list,"TD",indexTR.get(i).startTag,indexTR.get(i).endTag );
		for(j=0;j<indexTD.size();j++){
			tmp[i][j]=toString(list,indexTD.get(j).startTag+1,indexTD.get(j).endTag);
		}
	}

	return tmp;
}
public static synchronized  IndexTag getIndexRelation(String[][] matrix,String relation){
	int temp1=0;
	int temp2=0;
	IndexTag ind = new IndexTag();
	while(matrix[temp1][0]!=null){
		temp2=0;
		while(matrix[temp1][temp2]!=null){
			if(!matrix[temp1][temp2].equalsIgnoreCase(matrix[temp1][temp2].replaceAll(relation, ""))){
				ind.col=temp1;
				ind.line=temp2;
				return ind;
			}
			++temp2;
		}
		++temp1;
	}
return null;
}
public static synchronized  String getRelation(String[][] matrix,String nameRelation, String relation){
	
	IndexTag ind = new IndexTag();
	ind=getIndexRelation(matrix,nameRelation);
	if(relation.equalsIgnoreCase("up")){
		return matrix[ind.col][ind.line-1];
	}

	if(relation.equalsIgnoreCase("this")){
		return matrix[ind.col][ind.line];
	}

	if(relation.equalsIgnoreCase("down")){
		return matrix[ind.col][ind.line+1];
		
	}

	if(relation.equalsIgnoreCase("left")){
		return matrix[ind.col-1][ind.line];
	}

	if(relation.equalsIgnoreCase("right")){
		return matrix[ind.col+1][ind.line];
	}

	return "";
}
public static synchronized  String getRelation(LinkedList<TagStruct> list, String nameRelation,String relation){
	
	return getRelation(getMatrix(list),nameRelation,relation);
	
	
}

}
