package cbrContactSheet;

import generalpurpose.gpUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;




public class cbrContactSheetZipper {
 
  
  gpUtils xU = null;
  private boolean isOK = true;
  private int loglevel=9;
  
  //------------------------------------------------------------
  private void do_log(int level , String sIn)
  //------------------------------------------------------------
  {
    if( level > loglevel ) return;
	System.out.println(sIn);
  }
  //------------------------------------------------------------
  private void do_error(String sIn)
  //------------------------------------------------------------
  {
	  do_log(0,sIn);
	  
  }
  //------------------------------------------------------------
  public cbrContactSheetZipper(int ilog , gpUtils iu)
  //------------------------------------------------------------
  {
	  loglevel=ilog;
	  xU = iu;
  }
  
  public boolean UnzippedCorrectly()
  {
	  return isOK;
  }
  
  //------------------------------------------------------------
  public ArrayList<String> getFilesInZip(String zipFile)
  //------------------------------------------------------------
  {
	  ArrayList<String> list = new ArrayList<String>();
	  ZipInputStream zis = null;
	  try
		{
	    	//get the zip file content
	    	zis = new ZipInputStream(new FileInputStream(zipFile));
	    	//get the zipped file list entry
	    	ZipEntry ze = zis.getNextEntry();
	    	while(ze!=null){
	    	    String fileName = ze.getName();
	    	    if( ze.isDirectory() == false ) {
	    	    	if( (xU.isGrafisch(fileName)) && (fileName.toUpperCase().indexOf("MACOSX") < 0) ) {
	    	    	 list.add(fileName);
 	 	             //do_log(9, zipFile + "-> " +fileName);
	    	    	}
	    	    }
	            ze = zis.getNextEntry();
	    	}
	    }
		catch(Exception e){
		   do_error("Cannot list [" + zipFile + "] " + e.getMessage());
	       return null;
	    }
	    finally {
	    	try {
	    	zis.closeEntry();
		    zis.close();
	    	}
	    	catch(Exception e){
	 		   do_error("Cannot close [" + zipFile + "] " + e.getMessage());
	 	       return null;
	 	    }
	    }
	     
	  
	   return list;
  }
  
  //------------------------------------------------------------ 
  public boolean extractFilesFromCBZ( String zipFile , String sDir , ArrayList<String> list)
  //------------------------------------------------------------
  {
	    byte[] buffer = new byte[1024];
		File folder = new File(sDir);
		if ( folder.exists() == false ) {
		     do_error("Cannot locate directory [" + sDir + "]");
		     isOK=false;
		     return false;
		}
		if ( folder.isDirectory() == false ) {
			do_error("[" + sDir + "] is not a directory");
			isOK=false;
		    return false;	
		}
	  
		int teller=0;
		ZipInputStream zis = null;
		try
		{
	    	//get the zip file content
	    	zis = new ZipInputStream(new FileInputStream(zipFile));
	    	//get the zipped file list entry
	    	ZipEntry ze = zis.getNextEntry();
	    	while(ze!=null){
	    	    String fileName = ze.getName();
	    	  
	            //new File(newFile.getParent()).mkdirs();
	    	    
	    	    int found = -1;  // Found is the number to be given to the image - the sequenceon sorted list
	    	    if( ze.isDirectory() == false) {
	    	      for(int i=0;i<list.size();i++)
	    	      {
	    	    	if( list.get(i).compareTo(fileName) == 0 ) {
	    	    		found=i;
	    	    		break;
	    	    	}
	    	      }
	    	    }
	            if( found < 0 ) {
	    	  		ze = zis.getNextEntry();
	    	  		continue;
	    	    }
	            teller++;
	            String suf =xU.GetSuffix( fileName );
	            String imgTempFileName = "ContactImage-" + String.format("%03d",found) + "." + suf.toLowerCase();
	            File newFile = new File(sDir + xU.ctSlash + imgTempFileName);
	            do_log(9,"Extracting : " + fileName + " -> " + imgTempFileName);
	            //
	            FileOutputStream fos = new FileOutputStream(newFile);             
	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	       		   fos.write(buffer, 0, len);
	            }
	            fos.close();   
	            ze = zis.getNextEntry();
	    	}
	        
	    }
		catch(Exception e){
		   do_error("Cannot unzip " + e.getMessage());
	       isOK=false;
	    }
		finally {
		    	try {
		    	zis.closeEntry();
			    zis.close();
		   	}
		    	catch(Exception e){
		 		   do_error("Cannot close [" + zipFile + "] " + e.getMessage());
		 	       return false;
		 	    }
		}
	 	
	    return true;
  }
  
  

}











