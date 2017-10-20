package cbrContactSheet;



import generalpurpose.gpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;










import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class cbrContactSheetUnRAR {

	 gpUtils xU = null;

	 private static Log logger = LogFactory.getLog(cbrContactSheetUnRAR.class.getName());
	 private int loglevel=9;
	 
	 
	//---------------------------------------------------------------------------------
	private void do_error(String s)
	//---------------------------------------------------------------------------------
	{
				System.err.println(s);
	}

	//---------------------------------------------------------------------------------
	private void do_log(int level ,String s)
	//---------------------------------------------------------------------------------
	{
			   if( level > loglevel ) return;
				System.out.println(s);
	}
	  	
	 
	//---------------------------------------------------------------------------------
	cbrContactSheetUnRAR( gpUtils iu , int il)
	//---------------------------------------------------------------------------------
	{
		xU = iu;
		loglevel=il;
	}
	
	//---------------------------------------------------------------------------------
	public ArrayList<String> listFilesInCBR(String CBRFileName)
	//---------------------------------------------------------------------------------
	{
	    ArrayList<String> slist = null;
		File Farch = new File(CBRFileName);
		Archive arch = null;
		try {
		    arch = new Archive(Farch);
		    if (arch.isEncrypted()) {
				logger.warn("archive is encrypted cannot extreact");
				return null;
			}
		    slist = new ArrayList<String>();
			List<FileHeader> flist = arch.getFileHeaders();
			for(int i=0;i<flist.size();i++)
			{
				if( flist.get(i).isDirectory() ) continue;
				String FF = flist.get(i).getFileNameString();
				slist.add(FF);
			}
		} 
		catch (RarException e) {
			do_error("A==>" + CBRFileName);
		    logger.error(e);
		    return null;
		} 
		catch (IOException e1) {
			do_error("B==>" + CBRFileName);
		    logger.error(e1);
		    return null;
		}
		catch (Exception e) {
			do_error("C==>" + CBRFileName);
		    logger.error(e);
		    return null;
		} 
		finally {
			try {
			 arch.close();
		     arch=null;	 
			}
			catch(Exception e ) {
			  do_error("D==>" + CBRFileName);
			  return null;
			}
		}
		//
		return slist;
	}
	
	//---------------------------------------------------------------------------------
	public boolean extractFilesFromCBR(String CBRFileName, String DestinationFolder , ArrayList<String> list)
	//---------------------------------------------------------------------------------
	{
		  
		    File Farch = new File(CBRFileName);
		    File dest = new File(DestinationFolder);
			if (!dest.exists() || !dest.isDirectory()) {
		       do_error("the destination must exist and point to a directory: "  + DestinationFolder);
		       return false;
			}
		    //
		    Archive arch = null;
			try {
				//System.err.println("==>" + CBRFileName);
			    arch = new Archive(Farch);
			    
			    if (arch.isEncrypted()) {
					logger.warn("archive is encrypted cannot extreact");
					return false;
				}
				
			    FileHeader fh = null;
			    while (true) {
				  fh = arch.nextFileHeader();
				  if (fh == null) {
				    break;
				  }
				  if (fh.isEncrypted()) {
				    logger.warn("file is encrypted cannot extract: " + fh.getFileNameString());
				    continue;
				  }
				  if (fh.isDirectory()) continue;
				  String FName = fh.getFileNameString();
				  String suf = xU.GetSuffix(FName);
				  if( suf == null ) continue;
				  if( xU.isGrafisch( FName ) == false ) continue;
				  //
				  int found = -1;  // the sequence is the list is the number to be given to the image
				  for(int k=0;k<list.size();k++)
				  {
					  if( list.get(k).compareTo( FName ) == 0 ) {
						  found=k;
						  break;
					  }
				  }
				  if( found < 0 ) continue;
				  //
				  String ImageFileName = "ContactImage-" + String.format("%03d",found) + "." + suf.toLowerCase();
			      //  
				  try {
				    File f = createContactImageFile( ImageFileName , dest );
					OutputStream stream = new FileOutputStream(f);
					arch.extractFile(fh, stream);
					stream.close();
				    
				  } catch (IOException e) {
				    logger.error("error extracting the file", e);
				  } catch (RarException e) {
				    logger.error("error extraction the file", e);
				  }
				  
			    }
			    
			} 
			catch (RarException e) {
				do_error("A==>" + CBRFileName);
			    logger.error(e);
			    return false;
			} 
			catch (IOException e1) {
				do_error("B==>" + CBRFileName);
			    logger.error(e1);
			    return false;
			}
			catch (Exception e) {
				do_error("C==>" + CBRFileName);
			    logger.error(e);
			    return false;
			} 
			finally {
				try {
				 arch.close();
			     arch=null;	 
				}
				catch(Exception e ) {
				  do_error("D==>" + CBRFileName);
				  return false;
				}
			}
			//
			return true;
	}
	
	
	//---------------------------------------------------------------------------------
	private static File createContactImageFile(String DestName, File destination) 
	//---------------------------------------------------------------------------------
	{
			File f = null;
			String name = null;
			name = DestName;
			f = new File(destination, name);
			if (!f.exists()) {
			    try {
				f = makeFile(destination, name);
			    } catch (IOException e) {
				logger.error("error creating the new file: " + f.getName(), e);
			    }
			}
			return f;
	 }
	
	
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	
	 
	 
	 
	 
	/*
	 public static void extractArchive(String archive, String destination) {
			if (archive == null || destination == null) {
			    throw new RuntimeException("archive and destination must me set");
			}
			File arch = new File(archive);
			if (!arch.exists()) {
			    throw new RuntimeException("the archive does not exit: " + archive);
			}
			File dest = new File(destination);
			if (!dest.exists() || !dest.isDirectory()) {
			    throw new RuntimeException(
				    "the destination must exist and point to a directory: "
					    + destination);
			}
			extractArchive(arch, dest);
		    }

		   

	 public static void extractArchive(File archive, File destination) {
			Archive arch = null;
			try {
			    arch = new Archive(archive);
			} catch (RarException e) {
			    logger.error(e);
			} catch (IOException e1) {
			    logger.error(e1);
			}
			if (arch != null) {
			    if (arch.isEncrypted()) {
				logger.warn("archive is encrypted cannot extreact");
				return;
			    }
			    FileHeader fh = null;
			    while (true) {
				fh = arch.nextFileHeader();
				if (fh == null) {
				    break;
				}
				if (fh.isEncrypted()) {
				    logger.warn("file is encrypted cannot extract: "
					    + fh.getFileNameString());
				    continue;
				}
				logger.info("extracting: " + fh.getFileNameString());
				try {
				    if (fh.isDirectory()) {
					createDirectory(fh, destination);
				    } else {
					File f = createFile(fh, destination);
					OutputStream stream = new FileOutputStream(f);
					arch.extractFile(fh, stream);
					stream.close();
				    }
				} catch (IOException e) {
				    logger.error("error extracting the file", e);
				} catch (RarException e) {
				    logger.error("error extraction the file", e);
				}
			    }
			}
		    }

		    private static File createFile(FileHeader fh, File destination) {
			File f = null;
			String name = null;
			if (fh.isFileHeader() && fh.isUnicode()) {
			    name = fh.getFileNameW();
			} else {
			    name = fh.getFileNameString();
			}
			f = new File(destination, name);
			if (!f.exists()) {
			    try {
				f = makeFile(destination, name);
			    } catch (IOException e) {
				logger.error("error creating the new file: " + f.getName(), e);
			    }
			}
			return f;
		    }
*/
		    private static File makeFile(File destination, String name)
			    throws IOException {
			String[] dirs = name.split("\\\\");
			if (dirs == null) {
			    return null;
			}
			String path = "";
			int size = dirs.length;
			if (size == 1) {
			    return new File(destination, name);
			} else if (size > 1) {
			    for (int i = 0; i < dirs.length - 1; i++) {
				path = path + File.separator + dirs[i];
				new File(destination, path).mkdir();
			    }
			    path = path + File.separator + dirs[dirs.length - 1];
			    File f = new File(destination, path);
			    f.createNewFile();
			    return f;
			} else {
			    return null;
			}
		    }

		    /*
		    private static void createDirectory(FileHeader fh, File destination) {
			File f = null;
			if (fh.isDirectory() && fh.isUnicode()) {
			    f = new File(destination, fh.getFileNameW());
			    if (!f.exists()) {
				makeDirectory(destination, fh.getFileNameW());
			    }
			} else if (fh.isDirectory() && !fh.isUnicode()) {
			    f = new File(destination, fh.getFileNameString());
			    if (!f.exists()) {
				makeDirectory(destination, fh.getFileNameString());
			    }
			}
		    }

		    private static void makeDirectory(File destination, String fileName) {
			String[] dirs = fileName.split("\\\\");
			if (dirs == null) {
			    return;
			}
			String path = "";
			for (String dir : dirs) {
			    path = path + File.separator + dir;
			    new File(destination, path).mkdir();
			}

		    }
		    */
}
