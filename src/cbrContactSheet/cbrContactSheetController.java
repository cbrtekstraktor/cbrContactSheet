package cbrContactSheet;

import java.io.File;
import java.util.ArrayList;

import generalpurpose.gpUtils;
import logger.logLiason;

public class cbrContactSheetController {

	enum THREADSTATUS { WAIT , STARTED , BUSY , COMPLETED }
	enum SWITCHES { WORKDIR , SCALE , ROWS , ARCHIVE , THREADS , EXPIRES , LOGLEVEL }
	
	gpUtils xU = null;
	logLiason logger = null;
	
	private ArrayList<String> ArchiveList = null;  
	private boolean isOK=false;
	
	private long startedAt=0L;
	private long MAX_DURATION_IN_SECONDS = 300;
	private int MAX_THREADS = 5;
	private String TEMPDIR = "c:\\temp\\cbrContactSheet";
	private int LAYERS = 3;
	double SCALE = (double)0.20;
	private boolean doFilter=false;
	private boolean doGrayScale=false;
	private int LOGLEVEL = 5;
	private boolean doCompress=false;
	
	
	class ThreadMonitor {
		String ArchiveName = null;
		long starttime = -1L;
		long endtime = -1L;
		String TempDir = null;
		cbrContactSheetExtractorThread extr = null;
	    THREADSTATUS status = THREADSTATUS.WAIT;
	    boolean completionStatus=true;
	    ThreadMonitor(String s)
	    {
	    	ArchiveName = s;
	    }
	}
	ArrayList<ThreadMonitor> thlist = null;
	
	//---------------------------------------------------------------------------------
	private void do_error(String s)
	//---------------------------------------------------------------------------------
	{
		System.err.println(s);
	}

	//---------------------------------------------------------------------------------
	private void do_log(int level , String s)
	//---------------------------------------------------------------------------------
	{
		if( level > LOGLEVEL ) return;
		System.out.println(s);
	}

	//---------------------------------------------------------------------------------
	private void Usage()
	//---------------------------------------------------------------------------------
	{
		System.out.println("Usage: cbrContactSheet {File,Directory} --T(hreads) nn --S(cale) nn --R(ows) nn --W(orkdir) FOLDER --filter --grayscale --expires nn --L(loglevel) nn --C(ompress)");
		System.out.println("T(hreads) nn : Specifies the number of threads to be used. A single thread processes a single CBR");
		System.out.println("S(cale) nn   : Resizing ratio, e.g. 0.5 reduces the images on the contactsheet to half their original size");
		System.out.println("R(ows) nn    : Defines the number of rows on the contactsheet");
		System.out.println("W(orkdir)    : Defines the name of the working folder. The folder must exist prior to starting cbrContactSheet. The folder will comprise the contactsheets created.");
		System.out.println("F(ilter)     : This is a switch. When set comic pages without images will ne detected and removed from the contactsheet");
		System.out.println("G(rayscale)  : Currently not used.");
		System.out.println("E(pires) nn  : Defines the number seconds after which the application will be stopped.");
		System.out.println("L(og)        : {0..9} : defines the detail level of the logging information. 0 is minimal logging.");
		System.out.println("C(ompress)   : This is a switch. When set all contactsheets created will be compiled into an singel resulting CBR. Currently not used.");
	}
				
	//---------------------------------------------------------------------------------
	public boolean isOK()
	//---------------------------------------------------------------------------------
	{
		return isOK;
	}

	
	//---------------------------------------------------------------------------------
	cbrContactSheetController(String[] args) 
	//---------------------------------------------------------------------------------
	{
		startedAt = System.currentTimeMillis();
		xU = new gpUtils();
		logger = new logLiason(123L);
		if( checkArgs(args)) do_log(9,"Initialised OK");
	}
	
	//---------------------------------------------------------------------------------
	public boolean makeContactSheet()
	//---------------------------------------------------------------------------------
	{
			if( isOK == false ) {
				do_error("Initialisation did not conclude correctly - Abending");
				return false;
			}
			if( runThreads() == false ) isOK = false;
			/*
			// loop trough list
			isOK=true;
			for(int i=0;i<ArchiveList.size();i++)
			{
			 	cbrContactSheetExtractor ext = new cbrContactSheetExtractor( TEMPDIR , LAYERS , SCALE);
				boolean ib = ext.makeSingleContactSheet( ArchiveList.get(i) );
				ext=null;
				if( ib == false ) break;
			}
			do_log("Done");
			*/
			return isOK;
	}
		
	//---------------------------------------------------------------------------------
	private boolean checkArgs(String[] args)
	//---------------------------------------------------------------------------------
	{
		    SWITCHES expect = SWITCHES.ARCHIVE;
			if( args.length < 1) { Usage(); return false; }
			//
			for(int i=0;i<args.length;i++) 	
			{
			 //System.out.println(""+i+ " " +args[i]);
			 String sTemp = args[i].trim();
			 if( sTemp.startsWith("-") == false ) {
				switch (expect)
				{
				case ARCHIVE :  // file or folder
					{		
				    if( xU.IsBestand(sTemp) ) {
				        String suf = xU.GetSuffix(sTemp).toUpperCase().trim();
				        if( suf == null ) continue;
				        boolean add=false;
				        if( suf.compareToIgnoreCase("CBZ") == 0 ) add=true;
				        if( suf.compareToIgnoreCase("ZIP") == 0 ) add=true;
				        if( suf.compareToIgnoreCase("CBR") == 0 ) add=true;
				        if( suf.compareToIgnoreCase("TAR") == 0 ) add=true;
				        //
				        if( add ) {
				         ArchiveList = new ArrayList<String>();
				         ArchiveList.add(sTemp);
				        }
				    }
				    else
				    if( xU.IsDir(sTemp) ) {
				    	// are there files to be processed
				    	ArchiveList = new ArrayList<String>();
				    	ArrayList<String> list = xU.GetFilesInDirRecursive(sTemp,null);
				    	for(int j=0;j<list.size();j++)
				    	{
				    		String sArc = list.get(j);
				    		String suf = xU.GetSuffix(sArc);
				    		if( suf == null ) continue;
				    		boolean add=false;
				    		if( suf.compareToIgnoreCase("ZIP")==0) add=true;
				    		if( suf.compareToIgnoreCase("CBZ")==0) add=true;
				    		if( suf.compareToIgnoreCase("RAR")==0) add=true;
				    		if( suf.compareToIgnoreCase("CBR")==0) add=true;
				    		if( add == false ) continue;
				    		ArchiveList.add( sArc );
				    	}
				    	if( ArchiveList.size() < 1 ) {
				    		do_error("There are no CBR/CBZ/ZIP/RAR files in [" + sTemp + "]");
				    	}
				    }
				    else { do_error("Cannot access [" + sTemp + "]"); return false; }
				    do_log(5,"Will process [" + ArchiveList.size() + "] items");
				    break;
					}
				case ROWS : {
					int j = xU.NaarInt(sTemp);
					if( (j<2) || (j>5) ) {
						do_error("Unsupported number of layers [" + sTemp + "] default is [" + LAYERS + "]");
						Usage();
						return false;
					}
					else {
						LAYERS = j;
						expect = SWITCHES.ARCHIVE;
					}
					break;
				}
				case SCALE : {
					double dd = xU.NaarDouble(sTemp);
					if( (dd<0.1) || (dd>1) ) {
						do_error("Unsupported scale [" + dd + "] default is [" + SCALE + "] [MIN=0.1] [MAX=1]");
						Usage();
						return false;
					}
					else {
						SCALE = dd;
						expect = SWITCHES.ARCHIVE;
					}
					break;
				}
				case THREADS : {
					int j = xU.NaarInt(sTemp);
					if( (j<1) || (j>10) ) {
						do_error("Unsupported number of threads [" + sTemp + "] default is [" + MAX_THREADS + "] [MIN=1] [MAX=10]");
						Usage();
						return false;
					}
					else {
						MAX_THREADS = j;
						expect = SWITCHES.ARCHIVE;
					}
					break;
				}
				case WORKDIR : {
					if( xU.IsDir(sTemp) == false ) {
						do_error("Working folder [" + sTemp + "] cannot be accessed");
						Usage();
						return false;
					}
					else {
						if( sTemp.length() < 10 ) {
						  do_error("Workdir [" + sTemp + "] is too close to root - specify another");
						  Usage();
						  return false;
						}
						else {
						 TEMPDIR=sTemp;
						 expect = SWITCHES.ARCHIVE;
						}
					}
					break;
				}
				case EXPIRES : {
					int j = xU.NaarInt(sTemp);
					if( (j<30) || (j>3600) ) {
						do_error("Unsupported number of seconds before expiration [" + sTemp + "] default is [" + MAX_DURATION_IN_SECONDS + "] [MIN=30] [MAX=3600]");
						Usage();
						return false;
					}
					else {
						MAX_DURATION_IN_SECONDS = j;
						expect = SWITCHES.ARCHIVE;
					}
					break;
				}
				case LOGLEVEL : {
					int j = xU.NaarInt(sTemp);
					if( (j<0) || (j>9) ) {
						do_error("Unsupported loglevel [" + sTemp + "] default is [" + LOGLEVEL + "] [MIN=0] [MAX=9]");
						Usage();
						return false;
					}
					else {
						LOGLEVEL = j;
						expect = SWITCHES.ARCHIVE;
					}
					break;
				}
			    default : { do_error("Invalid state [" + expect + "] [Argument=" + sTemp + "]"); Usage(); return false;  }
				}
				expect = SWITCHES.ARCHIVE;	
				
			 }
			 // switches
			 else
			 if( sTemp.startsWith("-") )  // -x --x
			 {
				if( expect != SWITCHES.ARCHIVE ) {
					do_error("Expecting a value for [" + expect + "]");
					Usage();
					return false;
				}
				sTemp = xU.Remplaceer(sTemp,"-","").trim().toUpperCase();
				// --s(cale) --l(ayers) --t(empdir) --filter --grayscale
				if( (sTemp.compareToIgnoreCase("S")==0) || (sTemp.compareToIgnoreCase("SCALE")==0) ) {
					expect = SWITCHES.SCALE;
				}
				else
				if( (sTemp.compareToIgnoreCase("R")==0) || (sTemp.compareToIgnoreCase("ROWS")==0) ) {
					expect = SWITCHES.ROWS;
				}
				else
				if( (sTemp.compareToIgnoreCase("W")==0) || (sTemp.compareToIgnoreCase("WORKDIR")==0) ) {
					expect = SWITCHES.WORKDIR;
				}
				else
				if( (sTemp.compareToIgnoreCase("T")==0) || (sTemp.compareToIgnoreCase("THREADS")==0) ) {
					expect = SWITCHES.THREADS;
				}
				else
				if( (sTemp.compareToIgnoreCase("F")==0) || (sTemp.compareToIgnoreCase("FILTER")==0) ) {
					doFilter=true;
				}
				else
				if( (sTemp.compareToIgnoreCase("E")==0) || (sTemp.compareToIgnoreCase("EXPIRES")==0) ) {
					expect = SWITCHES.EXPIRES;
				}
				else
				if( (sTemp.compareToIgnoreCase("L")==0) || (sTemp.compareToIgnoreCase("LOG")==0) || (sTemp.compareToIgnoreCase("LOGLEVEL")==0) ) {
					expect = SWITCHES.LOGLEVEL;
				}
				else
				if( (sTemp.compareToIgnoreCase("G")==0) || (sTemp.compareToIgnoreCase("GRAY")==0) || (sTemp.compareToIgnoreCase("GRAYSCALE")==0)) {
					doGrayScale=true;
				}
				else
				if( (sTemp.compareToIgnoreCase("C")==0) || (sTemp.compareToIgnoreCase("COMPRESS")==0) ) {
					doCompress=true;
				}
				else {
					do_error("Unsuported switch [--" + sTemp + "]");
					Usage();
					return false;
				}
				//do_log(""+expect);
			 }
			 else {
					do_error("Invalid character found [" + sTemp + "]");
					Usage();
					return false;
			 }
			} // for
			
			// file folder
			if( ArchiveList == null ) { Usage(); return false; }
			if( ArchiveList.size() < 1 ) { Usage();  return false; }
			// TEMPDIR
			if( xU.IsDir( TEMPDIR ) == false ) {
				do_error("Temporay folder [" + TEMPDIR + "] is not accessible");
				return false;
			}
			// 
			reportParams();
			isOK=true;
			return isOK;
	}
	
	//---------------------------------------------------------------------------------
	private void reportParams()
	//---------------------------------------------------------------------------------
	{
		String sLog = "[Threads=" + MAX_THREADS + "] [Scale=" + SCALE + "] [Layers=" + LAYERS + "] [TEMPDIR=" + TEMPDIR +"] [FILTER=" + doFilter + "] [Grayscale=" + doGrayScale + "] [Expires=" + MAX_DURATION_IN_SECONDS + " sec] [LOGLEVEL=" + LOGLEVEL + "] [Compress=" + doCompress + "]";
		do_log(1,sLog);
	}
	
	//---------------------------------------------------------------------------------
	private String getWorkDirName(int i)
	//---------------------------------------------------------------------------------
	{
		String sDirName = TEMPDIR + xU.ctSlash + "WorkDir" + String.format("%03d",i);
		return sDirName;
	}
	
	//---------------------------------------------------------------------------------
	private boolean removeSignalFile(String sSignalFile)
	//---------------------------------------------------------------------------------
	{
		if( xU.IsBestand(sSignalFile) ) {
			do_log(9,"removing [" + sSignalFile + "");
			xU.VerwijderBestand(sSignalFile);
			if( xU.IsBestand(sSignalFile)) {
				do_error("Cannot remove [" + sSignalFile + "]");
				return false;
			}
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean cleanWorkFolders()
	//---------------------------------------------------------------------------------
	{
		// cleanDirs
		for(int i=0;i<MAX_THREADS;i++)
		{
			String sDir = getWorkDirName(i);
			String sSignalFile = sDir + xU.ctSlash + "BUSY.txt";
			if( removeSignalFile(sSignalFile) == false ) return false;
			sSignalFile = sDir + xU.ctSlash + "COMPLETE.txt";
			if( removeSignalFile(sSignalFile) == false ) return false;
			// graphical files
            ArrayList<String> list = xU.GetFilesInDir( sDir , null);
            for(int j=0;j<list.size();j++)
            {
            	String Fname = list.get(j);
            	if( xU.isGrafisch( Fname ) == false ) continue;
            	if( removeSignalFile(Fname) == false ) return false;
            }
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	public boolean VerwijderFolder( String sIn)
	//---------------------------------------------------------------------------------
	{
        File FObj = new File(sIn);
        if ( FObj.isDirectory() != true ) {
        	do_error("ERROR '" + sIn + ") -> folder not found");
        	return false;
        }
        if ( FObj.getAbsolutePath().length() < 10 ) {
        	do_error(sIn + "-> Length too small. Folder will not be deleted");
        	return false;  // blunt safety
        }
        FObj.delete();
        File XObj = new File(sIn);
        if ( XObj.isFile() == true ) {
        	do_error("ERROR" + sIn+ " -> could not be deleted");	
        }
        return true;
	}

	//---------------------------------------------------------------------------------
	private boolean doHouseKeeping()
	//---------------------------------------------------------------------------------
	{
		if( cleanWorkFolders() == false ) return false;
		for(int i=0;i<MAX_THREADS;i++)
		{
			String sDir = getWorkDirName(i);
			do_log(9,"Removing folder [" + sDir + "]");
			if( VerwijderFolder(sDir) == false ) return false;
		}	
		return true;
	}

	//---------------------------------------------------------------------------------
	private boolean prepareThreads()
	//---------------------------------------------------------------------------------
	{
		thlist = new ArrayList<ThreadMonitor>();
		for(int i=0;i<ArchiveList.size();i++)
		{
			ThreadMonitor x = new ThreadMonitor( ArchiveList.get(i));
			thlist.add(x);
		}
		// maak Dirs
		for(int i=0;i<MAX_THREADS;i++)
		{
			String sDirName = getWorkDirName(i);
			if( xU.IsDir(sDirName) ) continue;
			xU.CreateDirectory(sDirName);
			if( xU.IsDir(sDirName) == false) {
				do_error("Cannot create [" + sDirName + "]");
				return false;
			}
			do_log(5,"Created workdir [" + sDirName + "]");
		}
		if( cleanWorkFolders() == false ) return false;
		return true;
	}

	//---------------------------------------------------------------------------------
	private boolean isWorkDirInUse(String sTest)
	//---------------------------------------------------------------------------------
	{
		boolean inuse=false;
		for(int i=0;i<ArchiveList.size();i++)
		{
			if( (thlist.get(i).status == THREADSTATUS.STARTED) || (thlist.get(i).status == THREADSTATUS.BUSY) ) {
	           if( thlist.get(i).TempDir.compareToIgnoreCase(sTest) == 0 ) {
	        	   inuse=true;
	        	   break;
	           }
			}
		}
		return inuse;
	}

	//---------------------------------------------------------------------------------
	private int getNumberBusyStarted()
	//---------------------------------------------------------------------------------
	{
		int nbr=0;
		for(int i=0;i<ArchiveList.size();i++)
		{
			if( (thlist.get(i).status == THREADSTATUS.STARTED) || (thlist.get(i).status == THREADSTATUS.BUSY) ) nbr++;
		}
		return nbr;
	}

	//---------------------------------------------------------------------------------
	private void stopAllThreads()
	//---------------------------------------------------------------------------------
	{
		boolean inuse=false;
		for(int i=0;i<ArchiveList.size();i++)
		{
			if( (thlist.get(i).status == THREADSTATUS.STARTED) || (thlist.get(i).status == THREADSTATUS.BUSY) ) {
				do_log(1,"Stopping thread [" + thlist.get(i).ArchiveName );
                 thlist.get(i).extr.stop();	          
			}
		}
	}

	//---------------------------------------------------------------------------------
	private String getFreeWorkDir()
	//---------------------------------------------------------------------------------
	{
		String sFree = null;
		for(int i=0;i<MAX_THREADS;i++)
		{
		   String sDir = getWorkDirName(i);
		   if ( isWorkDirInUse(sDir) == false) {
			   sFree=sDir;
			   break;
		   }
		}
		if( sFree == null ) {
			do_error("there are no free workdirs");
			return null;
		}
		if( xU.IsDir(sFree) == false ) {
			do_error("workdir [" + sFree + "] does not exist");
			return null;
		}
		return sFree;
	}
	

	//---------------------------------------------------------------------------------
	private void report()
	//---------------------------------------------------------------------------------
	{
		do_log(1," ");
		String sLog = "[Started : " + xU.prntStandardDateTime(startedAt) + "] [Total duration : " + ((System.currentTimeMillis() - startedAt)/1000L) + " sec]";
		do_log(1,sLog);
		reportParams();
		for(int i=0;i<thlist.size();i++)
		{
		   String comm = thlist.get(i).completionStatus ? "OK" : "ERROR occured";
		   long ela = thlist.get(i).endtime > 1000L ? thlist.get(i).endtime - thlist.get(i).starttime : 0L;
		   String tt = thlist.get(i).TempDir == null ? "999" : xU.keepNumbers(xU.getFolderOrFileName(thlist.get(i).TempDir));
		   String slog = String.format("%-80s" , xU.getFolderOrFileName(thlist.get(i).ArchiveName)) + " " + xU.prntDateTime(thlist.get(i).starttime,"HH:mm:ss") + " " + String.format("%6d", (int)(ela/1000L)) + " sec [" + comm + "] [" + tt + "]";
		   do_log(1,slog);
		}
	}
	

	//---------------------------------------------------------------------------------
	private boolean runThreads()
	//---------------------------------------------------------------------------------
	{
		if( prepareThreads() == false ) return false;
		
		// run through the list until all complete
		long runStarted = System.currentTimeMillis();
		boolean isOK=true;
		long  period = System.currentTimeMillis();
		long  starttime = System.currentTimeMillis();
		while( true )
		{
			long elapsed = System.currentTimeMillis() - runStarted;
			if( (elapsed / 1000L) > MAX_DURATION_IN_SECONDS ) {
				do_error("Overall time out has been reached - too many files to process [Sec=" + MAX_DURATION_IN_SECONDS  + "]");
				break;
			}
			
			//System.out.print(".");
			
			// All done 
			int toGo=thlist.size();
			for(int i=0;i<thlist.size();i++)
			{
				if( thlist.get(i).status == THREADSTATUS.COMPLETED ) toGo--;
			}
			if( toGo == 0 ) {
				do_log(9,"All done");
				break;
			}
			
			// check the ones which are busy
			for(int i=0;i<thlist.size();i++)
			{
				if( thlist.get(i).status != THREADSTATUS.STARTED ) continue;
				// Look for "BUSY" or 'COMPLETE" file - if found then set to BUSY/COMPLETE and decrease nbr_started
				String sFile = thlist.get(i).TempDir + xU.ctSlash + "BUSY.txt";
				if( xU.IsBestand( sFile ) ) {
					if( removeSignalFile(sFile) == false ) { runStarted=0L; continue; } 
					thlist.get(i).status = THREADSTATUS.BUSY;
					do_log( 9,"" + i + " STARTED -> BUSY" );
				}
				else {
					sFile = thlist.get(i).TempDir + xU.ctSlash + "COMPLETE.txt";
					if( xU.IsBestand( sFile ) ) {
						if( removeSignalFile(sFile) == false ) { runStarted=0L; continue; } 
						thlist.get(i).status = THREADSTATUS.COMPLETED;
						thlist.get(i).completionStatus = thlist.get(i).extr.getOk();
						thlist.get(i).endtime = System.currentTimeMillis();
						thlist.get(i).extr = null;
						do_log( 9,"" + i + " STARTED -> COMPLETED" );
					}		
				}
			}
			
			// check the ones which are completed
			for(int i=0;i<thlist.size();i++)
			{
				if( thlist.get(i).status != THREADSTATUS.BUSY ) continue;
				// Look for "COMPLETED" file - set to COMPLETE and decrese nbr_busy
				String sFile = thlist.get(i).TempDir + xU.ctSlash + "COMPLETE.txt";
				if( xU.IsBestand( sFile ) ) {
					if( removeSignalFile(sFile) == false ) { runStarted=0L; continue; } 
					thlist.get(i).status = THREADSTATUS.COMPLETED;
					thlist.get(i).completionStatus = thlist.get(i).extr.getOk();
					thlist.get(i).endtime = System.currentTimeMillis();
					thlist.get(i).extr = null;
					do_log( 9,"" + i + " BUSY -> COMPLETED" );
				}
			}
			
			// if nbr_busy + nbr_started < MAX then you can start another thread
			if( getNumberBusyStarted() < MAX_THREADS ) {
				int idx = -1;
				for(int i=0;i<thlist.size();i++)
				{
					if( thlist.get(i).status == THREADSTATUS.WAIT ) { idx = i; break; }
				}
				if( idx >= 0 ) {
				  String sFreeWorkDir = getFreeWorkDir();
				  if( sFreeWorkDir == null ) {
					do_error("There are no free workdirs left - will abend");
					runStarted=0L;
					isOK=false;
					break;
				  }
				  // start it
				  thlist.get(idx).starttime = System.currentTimeMillis();
				  thlist.get(idx).status = THREADSTATUS.STARTED;
				  thlist.get(idx).TempDir = sFreeWorkDir;
				  //
				  thlist.get(idx).extr = new cbrContactSheetExtractorThread( idx , thlist.get(idx).ArchiveName ,  thlist.get(idx).TempDir , LAYERS , SCALE , LOGLEVEL , doFilter);
				  thlist.get(idx).extr.start();
				  //
				  //do_log("Starting " + thlist.get(idx).ArchiveName + " in " + sFreeWorkDir);
				}		
			}
			
			
			// sleep
			try {
				if( System.currentTimeMillis() - period > 5000L ) { 
					period=System.currentTimeMillis();
					long ela = (System.currentTimeMillis() - starttime)/1000L;
					do_log(1,"===> Active [" + this.getNumberBusyStarted() + "] [Remainder=" + toGo + "] [Elapsed=" + ela + " sec]"); 
				}
  			    Thread.sleep(500);
			}
			catch( Exception e ) {
				runStarted = 0L;
				break;
			}
		}
		
		// stop threads which are still running
		stopAllThreads();
		//
		if( doHouseKeeping() == false ) return false;
		//
		report();
		
		return isOK;
	}
	
	
}
