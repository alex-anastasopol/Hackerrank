package ro.cst.tsearch.titledocument.abstracts;

import java.util.ArrayList;

import com.stewart.ats.base.name.Name;

/**
 * This class is used to store the names of the chapters for TSD index. 
 * @author mihaid
 *
 */
public class ChapterNames {
	public ArrayList<Name> grantors ;
	public ArrayList<Name> grantees ;
	public ArrayList<Name> granteesLander ;
	public String originalGrantor;
	
	public ChapterNames() {
		super();
		this.grantors = new ArrayList<Name>();
		this.grantees = new ArrayList<Name>();
		this.granteesLander = new ArrayList<Name>();
		this.originalGrantor = "";
	}

	
	public ChapterNames(ArrayList<Name> grantors, ArrayList<Name> grantees,ArrayList<Name> granteesLander) {
		super();
		this.grantors = grantors;
		this.grantees = grantees;
		this.granteesLander = granteesLander;
	}

	public ArrayList<Name> getGrantees() {
		return grantees;
	}

	public void setGrantees(ArrayList<Name> grantees) {
		this.grantees = grantees;
	}
	
	public ArrayList<Name> getGranteesLander() {
		return granteesLander;
	}

	public void setGranteesLander(ArrayList<Name> granteesLander) {
		this.granteesLander = granteesLander;
	}

	public ArrayList<Name> getGrantors() {
		return grantors;
	}

	public void setGrantors(ArrayList<Name> grantors) {
		this.grantors = grantors;
	}
	
	
	public String getOriginalGrantor() {
		return originalGrantor;
	}


	public void setOriginalGrantor(String originalGrantor) {
		this.originalGrantor = originalGrantor;
	}


	public void addGrantor(Name n) {
		grantors.add(n);
	}
	
	public void addGrantee(Name n) {
		grantees.add(n);
	}
	
	public void addGranteeLander(Name n) {
		granteesLander.add(n);
	}
	
	public void addGrantor(String first, String middle, String last) {
		Name n = new Name();
		n.setFirstName(first);
		n.setMiddleName(middle);
		n.setLastName(last);
		grantors.add(n);
	}
	
	public void addGrantee(String first, String middle, String last) {
		Name n = new Name();
		n.setFirstName(first);
		n.setMiddleName(middle);
		n.setLastName(last);
		grantees.add(n);
	}
	
	public void addGranteeLander(String first, String middle, String last) {
		Name n = new Name();
		n.setFirstName(first);
		n.setMiddleName(middle);
		n.setLastName(last);
		granteesLander.add(n);
	}
	
	public void clearGrantors() {
		grantors.clear();
	}
	
	public void clearGrantees() {
		grantors.clear();
	}
	
	public void clearGranteesLander() {
		grantors.clear();
	}
	
}
