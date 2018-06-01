package ro.cst.tsearch.search.bean;

import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.legal.SubdivisionI;

public class LegalSKLDIteratorEntry implements Cloneable{
	private String lot;
	private String block;
	private String lotHigh;
	private String blockHigh;
	private String mapIdBook;
	private String mapIdPage;
	public String getLot() {
		return lot;
	}
	public void setLot(String lot) {
		this.lot = lot;
	}
	public String getBlock() {
		return block;
	}
	public void setBlock(String block) {
		this.block = block;
	}
	public String getMapIdBook() {
		return mapIdBook;
	}
	public void setMapIdBook(String mapIdBook) {
		this.mapIdBook = mapIdBook;
	}
	public String getMapIdPage() {
		return mapIdPage;
	}
	public void setMapIdPage(String mapIdPage) {
		this.mapIdPage = mapIdPage;
	}
	public String getLotHigh() {
		return lotHigh;
	}
	public void setLotHigh(String lotHigh) {
		this.lotHigh = lotHigh;
	}
	public String getBlockHigh() {
		return blockHigh;
	}
	public void setBlockHigh(String blockHigh) {
		this.blockHigh = blockHigh;
	}
	public void setSubdivision(SubdivisionI subdivisionI) {
		if(subdivisionI != null) {
			mapIdBook = subdivisionI.getPlatBook();
			mapIdPage = subdivisionI.getPlatPage();
			lot = subdivisionI.getLot();
			lotHigh = subdivisionI.getLotThrough();
			block = subdivisionI.getBlock();
			
		}
	}
	public boolean isSubdivisionComplete() {
		return StringUtils.isNotEmpty(mapIdBook) && 
			StringUtils.isNotEmpty(mapIdPage) && 
			(StringUtils.isNotEmpty(lot) ||
			StringUtils.isNotEmpty(block));
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((block == null) ? 0 : block.hashCode());
		result = prime * result + ((lot == null) ? 0 : lot.hashCode());
		result = prime * result + ((lotHigh == null) ? 0 : lotHigh.hashCode());
		result = prime * result
				+ ((mapIdBook == null) ? 0 : mapIdBook.hashCode());
		result = prime * result
				+ ((mapIdPage == null) ? 0 : mapIdPage.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LegalSKLDIteratorEntry other = (LegalSKLDIteratorEntry) obj;
		if (block == null) {
			if (other.block != null)
				return false;
		} else if (!block.equals(other.block))
			return false;
		if (lot == null) {
			if (other.lot != null)
				return false;
		} else if (!lot.equals(other.lot))
			return false;
		if (lotHigh == null) {
			if (other.lotHigh != null)
				return false;
		} else if (!lotHigh.equals(other.lotHigh))
			return false;
		if (mapIdBook == null) {
			if (other.mapIdBook != null)
				return false;
		} else if (!mapIdBook.equals(other.mapIdBook))
			return false;
		if (mapIdPage == null) {
			if (other.mapIdPage != null)
				return false;
		} else if (!mapIdPage.equals(other.mapIdPage))
			return false;
		return true;
	}
	
	@Override
	public LegalSKLDIteratorEntry clone() {
		try{
			return ( (LegalSKLDIteratorEntry)super.clone() );
		}
		catch(CloneNotSupportedException e){
			e.printStackTrace();
		}
		return null;
	}

}
