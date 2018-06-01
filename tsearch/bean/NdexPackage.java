package ro.cst.tsearch.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.utils.PDFUtils;

import com.lowagie.text.PageSize;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.connection.sureclose.SureCloseConn;
import com.stewart.ats.tsrindex.server.UploadImage;

public class NdexPackage {
	
	private static final String RUN_SHEET_SUFFIX = "ATR";
	private static final String TITLE_SEARCH_SUFFIX = "TS";
	private static final String TITLE_SEARCH_COPIES_SUFFIX = "TSC";
	private static final String ASSIGNMENT_SUFFIX = "ASSG";
	private static final String FEDERAL_TAX_LIEN_SUFFIX = "FTL";
	
	
	private Search search;
	private ImageI.IType distributionType;
	
	
	private File runSheet;
	private File titleSearch;
	private List<DocumentI> titleSearchCopies = new ArrayList<DocumentI>();
	private List<DocumentI> assigments = new ArrayList<DocumentI>();
	private List<DocumentI> federalTaxLiens = new ArrayList<DocumentI>();
	
	private List<String> generatedFiles = new ArrayList<String>();  
			
	public Search getSearch() {
		return search;
	}
	public void setSearch(Search search) {
		this.search = search;
	}
	public ImageI.IType getDistributionType() {
		return distributionType;
	}
	public void setDistributionType(ImageI.IType distributionType) {
		this.distributionType = distributionType;
	}
	
	public File getRunSheet() {
		return runSheet;
	}
	public void setRunSheet(File runSheet) {
		this.runSheet = runSheet;
	}
	public String getRunSheetRemoteName() {
		return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + RUN_SHEET_SUFFIX + "." + distributionType;
	}
	public String getRunSheetPath(){
		if(runSheet.exists() && runSheet.isFile()) {
			if(IType.PDF.equals(distributionType)) {
				String currentPath = runSheet.getPath();
				try {
					String newPath = UploadImage.createTempPDF(currentPath, getSearch().getImagesTempDir(), null);
					if(!newPath.equals(currentPath)) {
						generatedFiles.add(newPath);
					}
					return newPath;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	
	public File getTitleSearch() {
		return titleSearch;
	}
	public void setTitleSearch(File titleSearch) {
		this.titleSearch = titleSearch;
	}
	public String getTitleSearchRemoteName() {
		Date date = search.getSa().getCertificationDate().getDate();
		if(date != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy");
			return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + TITLE_SEARCH_SUFFIX + sdf.format(date) + "." + distributionType;
		}
		//this should never happen
		return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + TITLE_SEARCH_SUFFIX + "." + distributionType;
	}
	public String getTitleSearchPath(){
		if(titleSearch.exists() && titleSearch.isFile()) {
			if(IType.PDF.equals(distributionType)) {
				String currentPath = titleSearch.getPath();
				try {
					String newPath = UploadImage.createTempPDF(currentPath, getSearch().getImagesTempDir(), null);
					if(!newPath.equals(currentPath)) {
						generatedFiles.add(newPath);
					}
					return newPath;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public List<DocumentI> getTitleSearchCopies() {
		return titleSearchCopies;
	}
	public void setTitleSearchCopies(List<DocumentI> titleSearchCopies) {
		this.titleSearchCopies = titleSearchCopies;
	}
	public List<DocumentI> getAssigments() {
		return assigments;
	}
	public void setAssigments(List<DocumentI> assigments) {
		this.assigments = assigments;
	}
	public List<DocumentI> getFederalTaxLiens() {
		return federalTaxLiens;
	}
	public void setFederalTaxLiens(List<DocumentI> federalTaxLiens) {
		this.federalTaxLiens = federalTaxLiens;
	}
	
	public String getTitleSearchCopiesRemoteName() {
		return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + TITLE_SEARCH_COPIES_SUFFIX + "." + distributionType;
	}
	public String getAssigmentsRemoteName() {
		return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + ASSIGNMENT_SUFFIX + "." + distributionType;
	}
	public String getFederalTaxLiensRemoteName() {
		return search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + FEDERAL_TAX_LIEN_SUFFIX + "." + distributionType;
	}
	
	
	/**
	 * Keep it for reference
	 * @param document
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getImageRemoteName(DocumentI document) {
		if(titleSearchCopies.contains(document)) {
			return document.prettyPrint() + TITLE_SEARCH_COPIES_SUFFIX + "." + distributionType;
		} else if(assigments.contains(document)) {
			return document.prettyPrint() + ASSIGNMENT_SUFFIX + "." + distributionType;
		} else if(federalTaxLiens.contains(document)) {
			return document.prettyPrint() + FEDERAL_TAX_LIEN_SUFFIX + "." + distributionType;
		} else {
			return null;
		}
	}
	
	/**
	 * Keep it for reference
	 * @param document
	 * @return
	 */
	@SuppressWarnings("unused")
	private InputStream getImageInputStream(DocumentI document) {
		ImageI image = document.getImage();
		if(image != null) {
			File imageFile = new File(image.getPath());
			if(imageFile.exists() && imageFile.isFile()) {
				if(IType.PDF.equals(distributionType)) {
					String currentPath = imageFile.getPath();
					try {
						String newPath = UploadImage.createTempPDF(currentPath, getSearch().getImagesTempDir(), null);
						if(!newPath.equals(currentPath)) {
							generatedFiles.add(newPath);
						}
						return new FileInputStream(newPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	public String getTitleSearchCopiesPath() throws FileNotFoundException {
		List<String> files = new ArrayList<String>();
		for (DocumentI document : getTitleSearchCopies()) {
			
			if("AO".equals(document.getDataSource())) {
				String htmlIndex = DBManager.getDocumentIndex(document.getIndexId());
				String tempPDF = PDFUtils.convertToPDFFile(htmlIndex.getBytes());
				generatedFiles.add(tempPDF);
				files.add(0, tempPDF);	//keep the AO file first
			} else {
			
				ImageI image = document.getImage();
				if(image != null) {
					File imageFile = new File(image.getPath());
					if(imageFile.exists() && imageFile.isFile()) {
						if(IType.PDF.equals(distributionType)) {
							String currentPath = imageFile.getPath();
							if(!currentPath.toLowerCase().endsWith(".pdf")) {
								String tempPDF = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
								try {
									byte[] asPdf = SureCloseConn.convertToPDF(FileUtils.readFileToByteArray(imageFile));
									FileUtils.writeByteArrayToFile(new File(tempPDF), asPdf);
									generatedFiles.add(tempPDF);
									files.add(tempPDF);
									
								} catch (IOException e) {
									e.printStackTrace();
								}
							} else {
								files.add(currentPath);
							}						
						}
					}
				}
			}
		}
		
		if(files.isEmpty()) {
			return null;
		}
		
		String finalName = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
		generatedFiles.add(finalName);
		Util.concatenatePdfs(files.toArray(new String[0]), finalName);
		
		if(ServerConfig.isNdexResizePDFPackage()) {
			String resizedPDF = PDFUtils.resizePdf(finalName, 0, PageSize.LETTER, true);
			generatedFiles.add(resizedPDF);
			
			return resizedPDF;
		} else {
			return finalName;
		}
		
	}
	
	public String getAssigmentsPath() throws FileNotFoundException {
		List<String> files = new ArrayList<String>();
		for (DocumentI document : getAssigments()) {
			ImageI image = document.getImage();
			if(image != null) {
				File imageFile = new File(image.getPath());
				if(imageFile.exists() && imageFile.isFile()) {
					if(IType.PDF.equals(distributionType)) {
						String currentPath = imageFile.getPath();
						if(!currentPath.toLowerCase().endsWith(".pdf")) {
							String tempPDF = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
							try {
								byte[] asPdf = SureCloseConn.convertToPDF(FileUtils.readFileToByteArray(imageFile));
								FileUtils.writeByteArrayToFile(new File(tempPDF), asPdf);
								generatedFiles.add(tempPDF);
								files.add(tempPDF);
								
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							files.add(currentPath);
						}						
					}
				}
			}
		}
		
		if(files.isEmpty()) {
			return null;
		}
		
		String finalName = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
		Util.concatenatePdfs(files.toArray(new String[0]), finalName);
		generatedFiles.add(finalName);
		
		if(ServerConfig.isNdexResizePDFPackage()) {
			String resizedPDF = PDFUtils.resizePdf(finalName, 0, PageSize.LETTER, true);
			generatedFiles.add(resizedPDF);
			
			return resizedPDF;
		} else {
			return finalName;
		}
		
	}
	
	public String getFederalTaxLiensPath() throws FileNotFoundException {
		List<String> files = new ArrayList<String>();
		for (DocumentI document : getFederalTaxLiens()) {
			ImageI image = document.getImage();
			if(image != null) {
				File imageFile = new File(image.getPath());
				if(imageFile.exists() && imageFile.isFile()) {
					if(IType.PDF.equals(distributionType)) {
						String currentPath = imageFile.getPath();
						if(!currentPath.toLowerCase().endsWith(".pdf")) {
							String tempPDF = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
							try {
								byte[] asPdf = SureCloseConn.convertToPDF(FileUtils.readFileToByteArray(imageFile));
								FileUtils.writeByteArrayToFile(new File(tempPDF), asPdf);
								generatedFiles.add(tempPDF);
								files.add(tempPDF);
								
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							files.add(currentPath);
						}						
					}
				}
			}
		}
		
		if(files.isEmpty()) {
			return null;
		}
		
		String finalName = Util.tempFileName(getSearch().getImagesTempDir(), "pdf");
		Util.concatenatePdfs(files.toArray(new String[0]), finalName);
		generatedFiles.add(finalName);
		
		if(ServerConfig.isNdexResizePDFPackage()) {
			String resizedPDF = PDFUtils.resizePdf(finalName, 0, PageSize.LETTER, true);
			generatedFiles.add(resizedPDF);
			
			return resizedPDF;
		} else {
			return finalName;
		}
		
	}
	
	public void cleanGeneratedFiles() {
		for (String fileName : generatedFiles) {
			FileUtils.deleteQuietly(new File(fileName));
		}
	}
	
}
