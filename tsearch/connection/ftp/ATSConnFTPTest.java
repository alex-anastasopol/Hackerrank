 /*
 * Created on May 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.connection.ftp;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import java.io.* ;


/**
 * @author alfred
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ATSConnFTPTest extends Thread {
	
	public final static String servername = "206.161.198.5";
	public final static String proxyhost = "216.37.78.231";
	public final static String user_input = "STELDI";
	public final static String passwd_input = "STELD1eqt";
	public final static String user_output = "STELDIOUTPUT";
	public final static String passwd_output = "STELD1eqt";
	/*public final static String servername = "127.0.0.1";
	public final static String proxyhost = "216.37.78.231";
	public final static String user_input = "admin";
	public final static String passwd_input = "admin";
	public final static String user_output = "admin";
	public final static String passwd_output = "admin";*/

	
	public final static String directory_queries_input = "c:/temp/query/";
	public final static String directory_result = "c:/temp/result/";
	public final static String NAME_CONNEXION = "STELDI";
	public final static String directory_queries_output = "/download/";
	public final static int TIMEMAX=6000000;
	public final static int TIMECHECK=20000;
	
	private String queryString;
	private String queryID;
	private String Result_file_name;

	
	public ATSConnFTPTest(String queryString, String queryID){
		this.queryString = queryString;
		this.queryID = queryID;
	}
	public ATSConnFTPTest(){
		
	}

	
	public synchronized boolean uploadDoc(String queryString,String filename,ATSConnFTP ftp,String prefix){
		try 
		{
			return ftp.uploadFile(queryString,prefix+"_"+filename+".txt");
		}
		catch(IOException e){System.out.println(e);return false;}

	}

//	public  boolean uploadImageRequest(String path_queries,String filename,ATSConnFTP ftp,String prefix){
//		try 
//		{
//			return ftp.uploadFile(path_queries,prefix+"_"+filename+".DOC");
//		}
//		catch(IOException e){System.out.println(e);return false;}
//
//	}
	
	public void run()
	{
		
		try {
		    
		    // upload the query file
			boolean test = uploadQueryFile(queryString, queryID);
			
			boolean testdownload = false;
			long timestop = 0;
			long timet = 0;
			
			if (test) {
			    
				System.out.println("Uploaded OK");
				//long timestart=System.currentTimeMillis();
				long time = System.currentTimeMillis()+(long)TIMEMAX;
				System.out.println("	Search in progress");
				long timestart=System.currentTimeMillis();
				sleep(TIMECHECK);	
				
				// pool for results
				while (System.currentTimeMillis()< time){
				    
					try{
						testdownload = downloadResultFile(queryID);				
					} catch(Exception e){System.out.println(e.toString());}
					
					if (testdownload==true){
						System.out.println("	Result downloaded");
						timestop=System.currentTimeMillis();
						Txtparser txtparser=new Txtparser();
						//txtparser.parse_file();
						if (Result_file_name!=null){
							txtparser.input2output(directory_result+queryID+"/"+Result_file_name,"C:/tamair.doc");						
						}
						timet=timestop-timestart;
						
						break;
					}
					else{
						System.out.println("	Search in progress");
					}
					sleep(TIMECHECK);
				}
				
				if (testdownload==false )System.out.println("	Sorry maybe the server is too busy");
				else System.out.println("Time to result : "+ timet);
				
			}
			else System.out.println("Uploaded failed");
		}
		catch(Exception e){System.out.println(e.toString());}
	}
	
//	public  void ImageRequest(int nbImage)
//	{
//		
//		try{
//			
//			boolean test=this.UploadImageFile(order_num);
//			
//			long timestop=0;
//			long timet=0;
//			int nbImagedownload=0;
//			if (test){
//				System.out.println("Uploaded OK");
//				//long timestart=System.currentTimeMillis();
//				long time = System.currentTimeMillis()+(long)TIMEMAX;
//				System.out.println("	Search in progress");
//				long timestart=System.currentTimeMillis();
//				sleep(TIMECHECK);				
//				while (System.currentTimeMillis()< time ){					
//					
//					try{
//						nbImage = DownloadImageDoc(order_num,nbImage);				
//					}
//					catch(Exception e){System.out.println(e.toString());}
//					
//					if (nbImage==0){
//						System.out.println("	Result downloaded");
//						timestop=System.currentTimeMillis();
//						timet=timestop-timestart;
//						deleteFolder(order_num);
//						break;
//					}
//					else{
//						System.out.println("	Search in progress");
//					}
//					if (nbImage>0)
//						sleep(TIMECHECK);
//				}
//				
//				if (nbImage>0 )System.out.println("	Sorry maybe the server is too busy");
//				else System.out.println("Time to result : "+ timet);
//				
//			}
//			else System.out.println("Uploaded failed");
//		}
//		catch(Exception e){System.out.println(e.toString());}
//	}
//	
	/*Upload all query files in the input folder*/
	
//	public  boolean UploadQueries(String path_queries) throws IOException
//    {
//		boolean UploadFinish=false;
//		try
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_input,passwd_input);
//			ftpclient.setbinary();		
//			File dir = new File(path_queries) ;
//			File fic ;
//			String[] fileList = dir.list();
//			try
//			{
//				for(int i=0 ; i<fileList.length; i++)
//				{
//					String Fichier = path_queries+fileList[i] ;
//					if(fileList[i]!="bak")
//					{
//						System.out.println("Upload of "+Fichier) ;
//						boolean result = uploadDoc(Fichier,fileList[i],ftpclient,NAME_CONNEXION);
//						// On efface le fichier upload
//						if (result){
//							try{
//								fic = new File(Fichier) ;
//								fic.delete() ;							
//							}
//							catch(Exception e){System.out.println("Pb de fichier") ;}
//						}                  
//					}
//					
//				}
//				
//			}
//			catch(Exception e){System.out.println(e.toString());}
//			
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){System.out.println(e.toString());}
//		return UploadFinish;
//    }
	
//	public  boolean  UploadQuery(String path_queries) throws IOException
//    {
//		synchronized(ATSConnFTPTest.class){
//		
//		boolean UploadFinish=false;
//		try
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_input,passwd_input);
//			ftpclient.setbinary();		
//			File dir = new File(path_queries) ;
//			File fic ;
//			String[] fileList = dir.list();
//			try
//			{
//				if (0<fileList.length)
//				{
//					String Fichier = path_queries+fileList[0] ;
//					System.out.println("Upload of "+Fichier) ;
//					boolean result = uploadDoc(Fichier,fileList[0],ftpclient,NAME_CONNEXION);
//					// On efface le fichier upload
//					if (result){
//						try{
//							fic = new File(Fichier) ;
//							fic.delete() ;
//							UploadFinish=true;
//						}
//						catch(Exception e){System.out.println("Pb de fichier") ;}
//
//						
//					}                  
//				}
//			
//			}
//			catch(Exception e){System.out.println(e.toString());}
//			
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){System.out.println(e.toString());}
//		return UploadFinish;
//		}
//    }	
	
	
//	public String getFileNametoParse(String ordernum){
//		File dir = new File(directory_result+ordernum);
//		File[] file =dir.listFiles();
//		for( int i=0; i<file.length; i++ )
//	    {
//	          if (file[ i ].getName().indexOf(".DOC")!=-1){
//	          	Result_file_name=file[ i ].getName();
//	          	
//	          }
//	          
//	    }
//		return Result_file_name;
//	
//	}
	
//	public void deleteFolder(String ordernum) throws IOException{
//		try    
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_output,passwd_output);
//			ftpclient.setbinary();
//			ftpclient.deleteDirectory(ordernum);
//			
//		}
//		catch(Exception e){System.out.println(e.toString());}
//
//	}
	
	public boolean downloadResultFile(String queriename) throws IOException
    {		
		
		synchronized(ATSConnFTPTest.class){
			boolean ok=false;
			try    
			{
				SetProxy sp = new SetProxy();
				sp.setProxyFTP(proxyhost);
				ATSConnFTP ftpclient=new ATSConnFTP();		
				ftpclient.setPassiveMode(true);		
				ftpclient.connectServer(servername,user_output,passwd_output);
				ftpclient.setbinary();
				int reply;
	
				reply = ftpclient.ChangeDirectory(queriename);
				if (!FTPReply.isPositiveCompletion(reply))
	            {
	                System.err.println("Directory "+queriename+" doesn't exist");
	            }
				else
				{
					FTPFile [] file = ftpclient.getListFiles();
					System.out.println( "Number of files in dir "+queriename+" : "+ file.length);
					File dir = new File(directory_result+queriename);
					dir.mkdir();
					for( int i=0; i<file.length; i++ )
				    {
				          if (file[ i ].getName().indexOf("DOC")!=-1){
				          	Result_file_name=file[ i ].getName();
				          	//System.out.println( "We ve got the power ranger : "+Result_file_name);
					        boolean result = ftpclient.downloadFile( file[ i ].getName(),directory_result+queriename+"/"+file[ i ].getName() );
					        if (result){
					        	ok=true;
					          	System.out.println( "We ve got the power ranger : "+Result_file_name);
					        	ftpclient.deleteFile(file[ i ].getName());
					        }
				          }
				          else{
				          		ftpclient.deleteFile(file[ i ].getName());
				          		System.out.println( file[ i ].getName()+ " deleted");
				          }
				          				          
				    }
					
					if (ok==true){
						
						reply = ftpclient.Cdup();
						if (!FTPReply.isPositiveCompletion(reply))
			            {
			                System.err.println("Change directory up failed");
			            }
						else
						{
							ftpclient.deleteDirectory(queriename);						
						}
					}
					
				}
				
				ftpclient.closesession();
				ftpclient.DisconnectFTP();
	
			}
			catch(Exception e){System.out.println(e.toString());}
			
			return ok;
		}
    }
//	public boolean DownloadImageFile(String queriename) throws IOException
//    {		
//		boolean ok=false;
//		try    
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_output,passwd_output);
//			ftpclient.setbinary();
//			int reply;
//
//			reply = ftpclient.ChangeDirectory(queriename);
//			if (!FTPReply.isPositiveCompletion(reply))
//            {
//                System.err.println("Directory "+queriename+" doesn't exist");
//            }
//			else
//			{
//				FTPFile [] file = ftpclient.getListFiles();
//				System.out.println( "Number of files in dir "+queriename+" : "+ file.length);
//				File dir = new File(directory_result+queriename);
//				dir.mkdir();
//				for( int i=0; i<file.length; i++ )
//			    {
//			          if (file[ i ].getName().indexOf("TIF")!=-1){
//			          	Result_file_name=file[ i ].getName();
//			          	//System.out.println( "We ve got the power ranger : "+Result_file_name);
//				        boolean result = ftpclient.downloadFile( file[ i ].getName(),directory_result+queriename+"/"+file[ i ].getName() );
//				        if (result){
//				        	ok=true;
//				          	System.out.println( "We ve got the power ranger : "+Result_file_name);
//				        	ftpclient.deleteFile(file[ i ].getName());
//				        }
//			          }
//			          else{
//			          		ftpclient.deleteFile(file[ i ].getName());
//			          		System.out.println( file[ i ].getName()+ " deleted");
//			          }
//			          				          
//			    }
//				
//				if (ok==true){
//					
//					reply = ftpclient.Cdup();
//					if (!FTPReply.isPositiveCompletion(reply))
//		            {
//		                System.err.println("Change directory up failed");
//		            }
//					else
//					{
//						//ftpclient.deleteDirectory(queriename);						
//					}
//				}
//				
//			}
//			
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){System.out.println(e.toString());}
//		
//		return ok;
//    }
	
//	public int DownloadImageDoc(String queriename,int nbImage) throws IOException
//    {		
//		
//		try    
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_output,passwd_output);
//			ftpclient.setbinary();
//			int reply;
//
//			reply = ftpclient.ChangeDirectory(queriename);
//			if (!FTPReply.isPositiveCompletion(reply))
//            {
//                System.err.println("Directory "+queriename+" doesn't exist");
//            }
//			else
//			{
//				FTPFile [] file = ftpclient.getListFiles();
//				System.out.println( "Number of files in dir "+queriename+" : "+ file.length);
//				File dir = new File(directory_result+queriename);
//				dir.mkdir();
//				for( int i=0; i<file.length; i++ )
//			    {
//			          if (file[ i ].getName().indexOf("TIF")!=-1){
//			          	Result_file_name=file[ i ].getName();
//			          	//System.out.println( "We ve got the power ranger : "+Result_file_name);
//				        boolean result = ftpclient.downloadFile( file[ i ].getName(),directory_result+queriename+"/"+file[ i ].getName() );
//				        if (result){				        	
//				          	System.out.println( "We ve got the power ranger : "+Result_file_name);
//				        	ftpclient.deleteFile(file[ i ].getName());
//				        	nbImage--;
//				        	System.out.println( file[ i ].getName()+ " deleted");
//				        }
//			          }	
//			          else
//			          {
//		          		ftpclient.deleteFile(file[ i ].getName());
//		          		System.out.println( file[ i ].getName()+ " deleted");
//			          				          	
//			          }
//			    }
//			
//				
//			}
//			
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){System.out.println(e.toString());return nbImage;}
//		
//		return nbImage;
//    }	
	
	
	
//	public  boolean DownloadAllFile() throws IOException
//    {		
//		boolean ok=false;
//		try    
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(proxyhost);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(servername,user_output,passwd_output);
//			ftpclient.setbinary();
//			FTPFile[] listFolder =ftpclient.getListFiles();
//			for( int i=0; i<listFolder.length; i++ ){
//				int reply;
//				reply = ftpclient.ChangeDirectory(listFolder[i].getName());
//				if (!FTPReply.isPositiveCompletion(reply))
//	            {
//	                System.err.println("Directory "+listFolder[i].getName()+" doesn't exist");
//	            }
//				else
//				{
//					FTPFile [] file = ftpclient.getListFiles();
//					System.out.println( "Number of files in dir "+listFolder[i].getName()+" : "+ file.length);
//					File dir2 = new File(directory_result+listFolder[i].getName());
//					dir2.mkdir();
//					for( int j=0; j<file.length; j++ )
//				    {
//				          boolean result = ftpclient.downloadFile( file[ j ].getName(),directory_result+listFolder[i].getName()+"/"+file[ j ].getName() );
//				          if (result) ftpclient.deleteFile(file[ j ].getName());
//				    }
//					reply = ftpclient.Cdup();
//					if (!FTPReply.isPositiveCompletion(reply))
//		            {
//		                System.err.println("Change directory up failed");
//		            }
//					else
//					{
//						ftpclient.deleteDirectory(listFolder[i].getName());
//						ok=true;
//					}
//					
//				}				
//			
//			
//			
//			}
//
//			
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){e.printStackTrace();}
//		
//		return ok;
//    }	
	
	
	public boolean uploadQueryFile(String queryString, String queryID) throws IOException
    {
		
		synchronized(ATSConnFTPTest.class) {
		    
			boolean UploadFinish=false;
			String FileToUpload=null;
			
			try
			{
				SetProxy sp = new SetProxy();
				sp.setProxyFTP(DataConstants.PROXYHOST);
				ATSConnFTP ftpclient=new ATSConnFTP();		
				ftpclient.setPassiveMode(true);		
				ftpclient.connectServer(DataConstants.SERVERNAME,DataConstants.USER_INPUT,DataConstants.PASSWD_INPUT);
				ftpclient.setbinary();		
				try
				{
					boolean result = uploadDoc(queryString,queryID,ftpclient,DataConstants.NAME_CONNEXION);
					UploadFinish=result;
				}
				catch(Exception e){System.out.println(e.toString());}
				ftpclient.closesession();
				ftpclient.DisconnectFTP();
	
			} catch(Exception e){System.out.println(e.toString());}
			
			return UploadFinish;
		}
    }	
	
//	public  boolean UploadImageFile(String query_number) throws IOException
//    {
//		boolean UploadFinish=false;
//		String FileToUpload=null;
//		try
//		{
//			SetProxy sp = new SetProxy();
//			sp.setProxyFTP(DataConstants.PROXYHOST);
//			ATSConnFTP ftpclient=new ATSConnFTP();		
//			ftpclient.setPassiveMode(true);		
//			ftpclient.connectServer(DataConstants.SERVERNAME,DataConstants.USER_INPUT,DataConstants.PASSWD_INPUT);
//			ftpclient.setbinary();		
//			try
//			{
//					File dir = new File(DataConstants.DIRECTORY_QUERIES_INPUT);					
//					String[] fileList = dir.list();
//					
//					for( int i=0; i<fileList.length; i++ )
//				    {
//				          if (fileList[ i ].indexOf(query_number)!=-1){
//				          	FileToUpload=fileList[ i ];
//				          	System.out.println( "File to upload found : "+FileToUpload);
//				          }				          
//				    }
//					if (FileToUpload!=null){
//						boolean result = uploadImageRequest(DataConstants.DIRECTORY_QUERIES_INPUT+FileToUpload,query_number,ftpclient,DataConstants.NAME_CONNEXION);
//						//Delete the upload file
//						if (result){
//							try{
//								File fic = new File(DataConstants.DIRECTORY_QUERIES_INPUT+FileToUpload) ;
//								fic.delete() ;
//								UploadFinish=true;
//							}
//							catch(Exception e){e.printStackTrace();}	
//						}              
//					}					
//					    
//			}
//			catch(Exception e){System.out.println(e.toString());}
//			ftpclient.closesession();
//			ftpclient.DisconnectFTP();
//
//		}
//		catch(Exception e){System.out.println(e.toString());}
//		return UploadFinish;
//    }		
	public static void main (String arg[]){
	
	    //ATSConnFTPTest test = new ATSConnFTPTest("ATSTEST137");
		//ATSConnFTPTest test2 = new ATSConnFTPTest("ATSTEST172");
		//ATSConnFTPTest test4 = new ATSConnFTPTest("ATSTEST173");/*
		//ATSConnFTPTest test5 = new ATSConnFTPTest("ATSTEST152");*/
		//test.start();
		/*test2.start();
		test4.start();
		/*test5.start();

		//ATSConnFTPTest test2 = new ATSConnFTPTest("ATSTEST130");
		/*ATSConnFTPTest test3 = new ATSConnFTPTest("ATSTEST131");
		ATSConnFTPTest test4 = new ATSConnFTPTest("ATSTEST132");
		ATSConnFTPTest test5 = new ATSConnFTPTest("ATSTEST133");
		ATSConnFTPTest test6 = new ATSConnFTPTest("ATSTEST134");
		ATSConnFTPTest test7= new ATSConnFTPTest("ATSTEST135");*/
		//ATSConnFTPTest test8 = new ATSConnFTPTest("ATSTEST136");
		/*test.start();
		test2.start();
		test4.start();*/
		/*test4.start();
		test5.start();
		test6.start();
		test7.start();*/
		//test8.start();
		//ATSConnFTPTest test = new ATSConnFTPTest();
		//System.out.println("Le fichier doc est :"+test.getFileNametoParse("ATSTEST42"));
		try{
			//test.ImageRequest(3);
			//test.DownloadAllFile();
			//test.UploadImageFile("ATSTEST108");
		}catch(Exception e){System.out.println(e.toString());}
		
	}
	 
	
	
}
