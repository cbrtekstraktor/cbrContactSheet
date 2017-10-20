package cbrContactSheet;

import generalpurpose.gpPrintStream;
import generalpurpose.gpUtils;

public class cbrContactSheetExtractorThread extends Thread {
	
	gpUtils xU = null;
	
	private String CBRFileName = null;
	private String WorkDir = null;
	private int ThreadIdx=-1;
	private int Layers=-1;
    double Scale = (double)0.3;
    private boolean isOK=true;
    private int loglevel;
    private boolean doFilter=false;
	
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
  		if( level > loglevel ) return;
  		System.out.println(s);
  	}
   
  	//---------------------------------------------------------------------------------
  	cbrContactSheetExtractorThread(int idx , String iF , String iDir , int ilay , double isca , int ilev , boolean dof)
    //---------------------------------------------------------------------------------
    {
		xU = new gpUtils();
		CBRFileName = iF;
		WorkDir = iDir;
		ThreadIdx = idx;
		Layers=ilay;
		Scale = isca;
		loglevel=ilev;
		doFilter=dof;
		//
		do_log(1,"" +  ThreadIdx + " Starting [" + CBRFileName + "] in [" + WorkDir + "]");
	}

	private String getBusyFileName()
	{
		return WorkDir + xU.ctSlash + "BUSY.txt";
	}
	private String getCompleteFileName()
	{
		return WorkDir + xU.ctSlash + "COMPLETE.txt";
	}
	private void maakBusyFile()
	{
		gpPrintStream  po = new gpPrintStream( getBusyFileName() , "LATIN1");
		po.println( CBRFileName );
		po.close();
	}
	
	private void removeBusyFile()
	{
	    if( xU.IsBestand(getBusyFileName()) ) {
	    	xU.VerwijderBestand(getBusyFileName());
	    }
	}
	
	private void maakCompleteFile()
	{
		gpPrintStream  po = new gpPrintStream( getCompleteFileName() , "LATIN1");
		po.println( CBRFileName );
		po.close();
	}
	
	public boolean getOk()
	{
		return isOK;
	}
	
	public void run()
	{
		  maakBusyFile();
	      //  
		  try {
		   cbrContactSheetExtractor ext = new cbrContactSheetExtractor(WorkDir , Layers , Scale , loglevel , doFilter);
		   boolean ib = ext.makeSingleContactSheet(CBRFileName); if( ib == false ) isOK = false;
	       ext=null;
		  }
		  catch(Exception e) {
			  do_error("OOPS => major issue on [" + xU.getFolderOrFileName(CBRFileName) + "]");
			  isOK=false;
		  }
		  //
		  removeBusyFile();
	      maakCompleteFile();
	}
}
