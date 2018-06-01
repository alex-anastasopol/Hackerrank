package ro.cst.tsearch.search.iterator.data;

import ro.cst.tsearch.utils.StringUtils;

public class PlatBookPage {
	private String book;
	private String page;
	public PlatBookPage(String book, String page) {
		super();
		this.book = book;
		this.page = page;
	}
	public String getBook() {
		return book;
	}
	public void setBook(String book) {
		this.book = book;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((book == null) ? 0 : book.hashCode());
		result = prime * result + ((page == null) ? 0 : page.hashCode());
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
		PlatBookPage other = (PlatBookPage) obj;
		if (book == null) {
			if (other.book != null)
				return false;
		} else if (!book.equals(other.book))
			return false;
		if (page == null) {
			if (other.page != null)
				return false;
		} else if (!page.equals(other.page))
			return false;
		return true;
	}
	/**
	 * Checks if the entry is valid. Valid means both book and page are not empty.<br>
	 * If book and/or page is empty, then the entry is not valid (empty)
	 * @return <code>true</code> if book and/or page is empty
	 */
	public boolean isEmpty(){
		return StringUtils.isEmpty(book) || StringUtils.isEmpty(page);
	}
	@Override
	public String toString() {
		return book + "-" + page;
	}
	
}
