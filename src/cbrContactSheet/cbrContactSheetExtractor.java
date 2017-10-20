package cbrContactSheet;


import java.util.ArrayList;

import generalpurpose.gpUtils;
import logger.logLiason;
import generalImagePurpose.cmcImageRoutines;
import generalImagePurpose.gpFetchIIOMetadata;
import generalImagePurpose.gpLoadImageInBuffer;

public class cbrContactSheetExtractor {
	
	gpUtils xU = null;
	logLiason logger = null;
	gpLoadImageInBuffer iloader = null;
	cmcImageRoutines irout = null;
	gpFetchIIOMetadata iiom = null;
	

	private boolean isOK=false;
	private int TOLERANCE = 5;
	int HOR_MARGE = 20;
	int VER_MARGE = 35;
	int contactBreedte=-1;
	int contactHoogte=-1;
	int[] contactbuffer=null;
	
	private int GRIJS = 0x00c0c0c0;
	
    // params	
	private double RequestedScale = (double)0.5;
	private String TempDir = null;
	private int Layers = 3;
	private int loglevel;
	private boolean doFilter=false;
	//
	
	
	//private ArrayList<String> ArchiveList = null;    // list of all archives to be processed
	private ArrayList<String> ImageList = null;
	
	private int maxWidth=-1;
	private int maxHeigth=-1;
	private int minWidth=-1;
	private int minHeigth=-1;
	
	
	private int iiomWidth=-1;
	private int iiomHeigth=-1;
	
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
	public boolean isOK()
	//---------------------------------------------------------------------------------
	{
			return isOK;
	}

	//---------------------------------------------------------------------------------
	private int getNumberOfFilesNeeded()
	//---------------------------------------------------------------------------------
	{
			return (Layers * 5) - 3;
	}
		
	
	//---------------------------------------------------------------------------------
	cbrContactSheetExtractor(String iTmpDir , int iLay , double isca , int ilog, boolean dof)
	//---------------------------------------------------------------------------------
	{
		xU = new gpUtils();
		logger = new logLiason(123L);
		iloader = new gpLoadImageInBuffer(xU,logger);
		irout = new cmcImageRoutines(logger);
		iiom = new gpFetchIIOMetadata(xU, loglevel , false);
		
		//
		TempDir = iTmpDir;
		Layers = iLay;
		RequestedScale = isca;
		loglevel=ilog;
		doFilter=dof;
	}

	//---------------------------------------------------------------------------------
	public boolean makeSingleContactSheet(String CBRFileName)
	//---------------------------------------------------------------------------------
	{
		 if( PurgeTempDir() == false ) return false;
		 boolean ib = makeContactSheetFromFile(CBRFileName);
		 if( PurgeTempDir() == false ) return false;
		 return ib;
	}

	
	//---------------------------------------------------------------------------------
	private boolean fetchMetadata(String ImageFileName)
	//---------------------------------------------------------------------------------
	{
		iiomWidth=-1;
		iiomHeigth=-1;
		boolean ib = iiom.parseMetadataTree( ImageFileName );
    	if (ib ) {
    		int width = iiom.getWidth();
    		int heigth = iiom.getHeigth();
    		//int physicalWidthDPI = iiom.getPhysicalWidthDPI();
    	    //int physicalHeigthDPI = iiom.getPhysicalHeigthDPI();
    	    //
    		if( width < 0 ) ib=false;
    		if( heigth < 0 ) ib=false;
    		//if( physicalWidthDPI < 0 ) ib=false;
    		//if( physicalHeigthDPI < 0 ) ib=false;
    		//do_log(1,"[W=" + width + "] [H=" + heigth + "] [DPIx" + physicalWidthDPI + "] [DPIy" + physicalHeigthDPI + "]");
    		if( ib )  {
    			//do_log(iiom.getReport() );
        		iiomWidth=width;
        		iiomHeigth=heigth;
    		}
        }
    	return ib;
	}
	
	//---------------------------------------------------------------------------------
	private ArrayList<String> ImagesFilesInTempDir()
	//---------------------------------------------------------------------------------
	{
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> list = xU.GetFilesInDir( TempDir , null);
		for(int i=0;i<list.size();i++)
		{
			String FName = list.get(i);
			if( xU.isGrafisch( FName ) == false ) continue;
			boolean remove=false;
			if( FName.toUpperCase().startsWith("CONTACTIMAGE") == true ) remove=true;
			if( FName.toUpperCase().startsWith("SMALL-CONTACTIMAGE") == true ) remove=true;
			if( remove == false ) continue;
			FName = TempDir + xU.ctSlash + FName;
			ret.add(FName);
		}
		return ret;
	}
	
	//---------------------------------------------------------------------------------
	private boolean PurgeTempDir()
	//---------------------------------------------------------------------------------
	{
		ArrayList<String> list = ImagesFilesInTempDir();
		for(int i=0;i<list.size();i++)
		{
			if( xU.IsBestand( list.get(i) ) == false ) continue;
			do_log(9,"Removing [" + list.get(i) + "]" );
			if( xU.VerwijderBestand( list.get(i) ) == false ) return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean makeContactSheetFromFile(String FName)
	//---------------------------------------------------------------------------------
	{
		ImageList=null;
		int tipe=-1;
		if( xU.IsBestand(FName) == false ) {
			do_error("Cannot acces [" + FName + "]");
			return false;
		}
		//
		if( PurgeTempDir() == false ) return false;
		// get the files in archive
		String suf = xU.GetSuffix(FName);
		if( suf == null ) return false;
		if( (suf.compareToIgnoreCase("ZIP")==0) || (suf.compareToIgnoreCase("CBZ")==0) ) {
			tipe=0;
			ImageList = makeListOfFilesToExtract( FName , tipe);
			if( ImageList == null ) return false;
		}
		else 		
		if( (suf.compareToIgnoreCase("RAR")==0) || (suf.compareToIgnoreCase("CBR")==0) ) {
			tipe=1;
			ImageList = makeListOfFilesToExtract( FName , tipe);
			if( ImageList == null ) return false;
		}
		else {
			do_error("Unsupported suffix on [" + FName );
			return false;
		}
		// Sort list and thus assign sequence numbers
		ImageList = sortList( ImageList );
		if( ImageList == null ) return false;
		do_log(5,"Found in total [" + ImageList.size() + "] files to extract from [" + FName + "]");
		//
		ImageList = limitList(ImageList);
		if( ImageList == null ) return false;
		do_log(5,"Selection of images comprises [" + ImageList.size() + "] items");
		//
		if( extractImages(FName , ImageList , tipe) == false ) return false;
		// there should now be a number of images in the TEMPDIR
		ArrayList<String> imglist = ImagesFilesInTempDir();
		if( imglist.size() < 1 ) {
			do_error("There appear to be no images extracted in " + TempDir );
			return false;
		}
		
		// graphical part
		if( makeThumbNails() == false ) return false;
		//
		if( makeSheet(FName) == false ) return false;
		//
		return true;
	}

	//---------------------------------------------------------------------------------
	private ArrayList<String> makeListOfFilesToExtract(String FName , int tipe)
	//---------------------------------------------------------------------------------
	{
		boolean extractOK=false;
		ArrayList<String> list = null;
		if( tipe == 0 ) {
			cbrContactSheetZipper zipper = new cbrContactSheetZipper(loglevel,xU);
			list = zipper.getFilesInZip(FName);
			if( list == null ) {
				do_error("Could not list the files in [" + FName + "]");
			}
			else
			if( list.size() < 1 ) {
				do_error("No files found to extract in [" + FName + "]");
			}
			else {
				extractOK=true;
			}
			if( extractOK == false ) {
				 zipper=null;
				 return null;
			}
		}
		//
		else
		if( tipe == 1 ) {
			
			cbrContactSheetUnRAR extractor = new cbrContactSheetUnRAR(xU,loglevel);
			list = extractor.listFilesInCBR(FName);
			if( list == null ) {
				do_error("Could not list the files in [" + FName + "]");
			}
			else
			if( list.size() < 1 ) {
				do_error("No files found to extract in [" + FName + "]");
			}
			else {
				extractOK=true;
			}
			if( extractOK == false ) {
			 extractor=null;
			 return null;
			}
		}
		//
		else {
			do_error("Unsupported tipe [" + tipe + "]");
			return null;
		}
		return list;
	}
	
	
	//---------------------------------------------------------------------------------
	private ArrayList<String> limitList(ArrayList<String>list)
	//---------------------------------------------------------------------------------
	{
		// which ones - just take the first requested
		ArrayList<String> selection = new ArrayList<String>();
		for(int i=0;i<list.size();i++)
		{
					if( i>= (getNumberOfFilesNeeded() + TOLERANCE) ) break;   // +3 to add an extra 3 files 
					selection.add( list.get(i) );
		}
		list=null;
		return selection;
	}
	
	//---------------------------------------------------------------------------------
	private String RemoveSuffix(String s)
	//---------------------------------------------------------------------------------
	{
		String Ret = s;
		int idx = s.lastIndexOf(".");
		if( idx < 0 ) return Ret;
		try {
			Ret = Ret.substring(0,idx);
		}
		catch(Exception e ) { Ret = s; }
		return Ret;
	}
	
	// needs to be sorted and the SUFFIX also needs to be ignored (JPG,BMP;GIF can be combined in a CBR)
	//---------------------------------------------------------------------------------
	private ArrayList<String> sortList(ArrayList<String>list)
	//---------------------------------------------------------------------------------
	{
		ArrayList<String>Ret = new ArrayList<String>();
		for(int i=0;i<list.size();i++) {
			String s = list.get(i);
			Ret.add(s);
		}
		for(int x=0;x<Ret.size();x++)
		{
			boolean swap=false;
			for(int i=0;i<Ret.size()-1;i++)
			{
				String One = RemoveSuffix(Ret.get(i));
				String Two = RemoveSuffix(Ret.get(i+1));
				if( One.compareTo(Two) > 0 ) {
					swap=true;
					String s = Ret.get(i);
					Ret.set(i, Ret.get(i+1));
					Ret.set(i+1, s);
				}
			}
			if( swap == false ) break;
		}
		/*
		for(int i=0;i<Ret.size();i++)
		{
			if( Ret.get(i).compareTo(list.get(i))==0) continue;
			do_log("" + Ret.get(i) + " " + list.get(i));
		}
		*/
		list=null;
		return Ret;
		//return null;
	}
	
	//---------------------------------------------------------------------------------
	private boolean extractImages(String FName , ArrayList<String> list , int tipe)
	//---------------------------------------------------------------------------------
	{
		if( tipe == 0 ) {
			cbrContactSheetZipper zipper = new cbrContactSheetZipper(loglevel,xU);
			boolean ib = zipper.extractFilesFromCBZ( FName , TempDir , list);
			zipper = null;
			return ib;
		}
		else
		if( tipe == 1 ) {
			cbrContactSheetUnRAR extractor = new cbrContactSheetUnRAR(xU,loglevel);
			boolean ib = extractor.extractFilesFromCBR( FName , TempDir , list );
			extractor=null;
			if( ib == false ) return false;
		}
		else {
			do_error("unsupported tipe [" + tipe + "]");
			return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean makeThumbNails()
	//---------------------------------------------------------------------------------
	{
		maxWidth=-1;
		maxHeigth=-1;
		minWidth=-1;
		minHeigth=-1;
		//
		ArrayList<String> list = ImagesFilesInTempDir();
		if( list == null ) {
			do_error("Cannot read files from [" + TempDir + "]");
			return false;
		}
		int teller=0;
		int removed=0;
		for(int i=0;i<list.size();i++)
		{
			String FName = list.get(i);
			if( xU.isGrafisch(FName) == false ) continue;
			String ShortName = xU.getFolderOrFileName(FName);
			if( ShortName.toUpperCase().startsWith("SMALL-") ) continue;
		    //	
			do_log(9,"Assessing metadata [" + ShortName + "]");
			boolean ib = this.fetchMetadata(FName);
			if (ib == false ) {  // just read the image if iiom does not work
				do_log(5,"IIOM does not work - switch to loading image");
				int[] imgin = iloader.loadBestandInBuffer(FName);
				if( imgin == null ) {
					do_error("Could not load [" + FName + "]");
					continue;
				}
				//
				iiomWidth = iloader.getBreedte();
				iiomHeigth = iloader.getHoogte();
				imgin=null;
			}
			do_log(5,"[Width=" + iiomWidth + "] [Height=" + iiomHeigth + "] " );
			// landscape
			double verhor = (double)iiomWidth / (double)iiomHeigth;
			if( verhor > (double)0.9 ) {
				if( removed < TOLERANCE ) {
				  removed++;
				  do_log(9,"Removing [" + FName + "] image is too wide");
				  if( xU.VerwijderBestand( FName ) == false ) return false;
				  continue;
				}
			}
			//
			if( maxWidth < 0 ) {
				   maxWidth=iiomWidth;
				   maxHeigth=iiomHeigth;
				   minWidth=maxWidth;
				   minHeigth=maxHeigth;
			}
			if( maxWidth  < iiomWidth ) maxWidth = iiomWidth;
			if( minWidth  > iiomWidth ) minWidth = iiomWidth;
			if( maxHeigth < iiomHeigth) maxHeigth = iiomHeigth;
			if( minHeigth > iiomHeigth) minHeigth = iiomHeigth;
			
			teller++;
		}
		if( teller == 0 ) {
			do_error("There are no image files in " + TempDir);
			return false;
		}
		
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean makeSheet(String CBRName)
	//---------------------------------------------------------------------------------
	{
		String DEST=null;
		try {
		 String ShortName = xU.GetFileName(CBRName);
		 int idx = ShortName.lastIndexOf(".");
		 if( (idx < 0) || (idx>=ShortName.length()) ) return false;
		 ShortName = ShortName.substring(0,idx);
		 ShortName = xU.keepLettersAndNumbers(ShortName);
		 String ParentFolder = xU.getParentFolderName( TempDir );
		 DEST = ParentFolder + xU.ctSlash + "ContactSheet_" + ShortName + ".png";
		}
		catch(Exception e) {
			do_error("Something went wriong while creating DEST name");
			return false;
		}
		//    
	    double dbreedte = (((double)maxWidth * 5) * RequestedScale) + (double)(HOR_MARGE*6);
	    contactBreedte = (int)dbreedte;
	    double dhoogte  =   (((double)maxHeigth * Layers) * RequestedScale) + (double)(VER_MARGE*(Layers+1));
	    contactHoogte = (int)dhoogte;
	    //
	    do_log(5,"[Max=" + maxWidth + "x" + maxHeigth + "] [Min=" + minWidth + "x" + minHeigth  + "] [SHEET=" + contactBreedte + "x" + contactHoogte + "]");
	    //
	    contactbuffer = new int[ contactBreedte * contactHoogte ];
	    for(int i=0;i<contactbuffer.length;i++) contactbuffer[i]=0xffffffff;
	    //
	    int horstep = (int)((double)maxWidth * RequestedScale);
	    int verstep = (int)((double)maxHeigth * RequestedScale);
	    
	    
	    ArrayList<String> list = ImagesFilesInTempDir();
		if( list == null ) {
			do_error("Cannot read files from [" + TempDir + "]");
			return false;
		}
		int teller=0;
		int x=HOR_MARGE;
		int y=VER_MARGE;
		int oldx=x;
		int oldy=y;
		int rejectcounter=0;
		for(int i=0;i<list.size();i++)
		{
		  oldx=x;
		  oldy=y;
		  teller++;  // caution : starts at 1
		  //
		  if( teller > getNumberOfFilesNeeded() )  break;
		  //
		  if( teller == 1 ) {
			 x = HOR_MARGE;
			 y = VER_MARGE;
	      }
		  else
		  if( teller == 2 ) {
			  x = (horstep *2) + (HOR_MARGE * 3);
		  }
		  else
		  if( teller == 5 ) {
			  x = (horstep *2) + (HOR_MARGE * 3);
			  y = verstep + (VER_MARGE * 2);
		  }
		  else
		  if( ((teller - 8) % 5 == 0) && (teller >= 8) ) {
			  x = HOR_MARGE;
			  y += verstep + VER_MARGE;
		  }
		  else {
			  x += horstep + HOR_MARGE;
		  }
		  //
		  if( teller == 1 )	  do_rectangle( x , y , horstep*2 , verstep*2 , GRIJS );
		                else  do_rectangle( x , y , horstep , verstep , GRIJS );
		  //
		  String FName = list.get(i);
		  if( xU.isGrafisch(FName) == false ) continue;
		  String ShortName = xU.getFolderOrFileName(FName);
		  if( ShortName.toUpperCase().startsWith("SMALL-") ) continue;
		  //
		  do_log(9,"Loading [" + FName + "]");
		  int imgin[] = iloader.loadBestandInBuffer(FName);
		  if( imgin == null ) {
				do_error("Could not load [" + FName + "]");
				continue;
		  }
		  //
		  if( (doFilter) && (teller!=1) && (rejectcounter<TOLERANCE) ) { // see whether the image appears to have mainly 1 color
			  if( mightBeMonochrome(imgin) ) {
				  do_log(1,"[" + xU.getFolderOrFileName(CBRName) + "." + xU.getFolderOrFileName(FName) + "] might be monochrome - skipping");
				  //
				  rejectcounter++;
				  teller--;
				  x=oldx;
				  y=oldy;
				  continue;
			  }
		  }
		  //
		  double targetscale = ( teller == 1 ) ? RequestedScale * 2.1 : RequestedScale;
		  double scale = getOptimalScale( iloader.getBreedte() , iloader.getHoogte() , maxWidth , maxHeigth , targetscale );
		  do_log(5,"Resizing [" + FName + "] [Target=" + targetscale + "] [EffectiveScale=" + scale + "]");
		  //
		  int resizeWidth = (int)((double)scale * (double)iloader.getBreedte());
		  int resizeHeigth =  (resizeWidth * iloader.getHoogte()) / iloader.getBreedte();
		  int imgout[] = irout.resizeImage(imgin, iloader.getBreedte() , iloader.getHoogte() , scale , cmcImageRoutines.ImageType.RGB );
		  if( imgout == null ) {
				do_error("Could not resize [" + FName + "]");
				continue;
		  }
		  //
		  do_log(9,"Merging [" + FName + "]");
		  
		  int multiplier = teller == 1 ? 2 : 1;
		  do_merge( imgout , resizeWidth , resizeHeigth , x , y , horstep*multiplier , verstep*multiplier);
		  
		  /*
		  int reqBreedte = teller == 1 ? horstep*2 : horstep;
		  int reqHoogte = teller == 1 ? (verstep + VER_MARGE)*2 : verstep;
		  do_merge( imgout , resizeWidth , resizeHeigth , x , y , reqBreedte , reqHoogte );
		  */
		}
	    
	    //
		irout.writePixelsToFile(contactbuffer , contactBreedte , contactHoogte , DEST , cmcImageRoutines.ImageType.RGB);
		do_log(1,"Created contactsheet [" + DEST + "]");
		//
	    return true;
	}
	
	//---------------------------------------------------------------------------------
	private double getOptimalScale( int breedte , int hoogte , int maxbreedte , int maxhoogte , double scale)
	//---------------------------------------------------------------------------------
	{
		int gewensteBreedte = (int)((double)maxbreedte * scale);
		int gewensteHoogte  = (int)((double)maxhoogte * scale);
		int gewensteOppervl = (int)((double)(breedte * hoogte) * scale);
		//
		double horizontalScale =  (double)gewensteBreedte / (double)breedte;
		double verticalScale   =  (double)gewensteHoogte / (double)hoogte;
		
		// wat is de overschot indien horizontalScale genomen wordt
		int testBreedte = (int)((double)breedte * horizontalScale);
		int testHoogte  = (int)((double)hoogte * horizontalScale);
		int horizontalOverschot =  (testBreedte * testHoogte) - gewensteOppervl;
		
		// wat is de overschot indien verticalScale genomen wordt
		testBreedte = (int)((double)breedte * verticalScale);
		testHoogte  = (int)((double)hoogte * verticalScale);
		int verticalOverschot =  (testBreedte * testHoogte) - gewensteOppervl;
        //
		double calcScale = scale;
		if( Math.abs(horizontalOverschot) < Math.abs(verticalOverschot) ) calcScale = horizontalScale; else calcScale = verticalScale;
		//
		//do_log("==> Scale=" + scale + " Horscale=" + horizontalScale + "verScale=" + verticalScale + " Calc=" + calcScale);
		return calcScale;
	}
	
	//---------------------------------------------------------------------------------
	private void do_rectangle(int x , int y , int breedte , int hoogte , int kleur)
	//---------------------------------------------------------------------------------
	{
		for(int i=0;i<hoogte;i++)
		{
			contactbuffer[ x + ((y+i) * contactBreedte) ] = kleur;
			contactbuffer[ x + breedte + ((y+i) * contactBreedte) ] = kleur;
		}
		for(int i=0;i<breedte;i++)
		{
			contactbuffer[(y * contactBreedte) + x + i] = kleur;
			contactbuffer[((y + hoogte) * contactBreedte) + x + i] = kleur;
		}
	}
	
	//---------------------------------------------------------------------------------
	private void do_merge( int[] img , int breedte , int hoogte , int xoff , int yoff , int capbreedte , int caphoogte)
	//---------------------------------------------------------------------------------
	{
		int toegestanebreedte = (breedte > capbreedte) ? capbreedte : breedte;
		int toegestanehoogte  = (hoogte > caphoogte) ? caphoogte : hoogte;
		// 
		int centreerx = ( breedte - toegestanebreedte ) / 2;
		int centreery = ( hoogte - toegestanehoogte ) / 2;
		//
		int k = (centreery * breedte);
		for(int i=0;i<toegestanehoogte;i++)
		{
			int x = ((yoff + i ) * contactBreedte) + xoff;
			for(int j=0;j<toegestanebreedte;j++)
			{
				contactbuffer[ x + j ] = img [k + centreerx + j];
			}
			k += breedte;
		}
	}
	
	//---------------------------------------------------------------------------------
	private boolean mightBeMonochrome(int[] img)
	//---------------------------------------------------------------------------------
	{
		int[] hist= new int[256];
		for(int i=0;i<hist.length;i++) hist[i]=0;
		//
		for(int i=0;i<img.length;i++)
		{
			 int r = 0xff & ( img[i] >> 16); 
	         int g = 0xff & ( img[i] >> 8); 
	         int b = 0xff & img[i]; 
	         int gray = (int)Math.round(0.2126*r + 0.7152*g + 0.0722*b);
	         hist[gray] += 1;
		}
		//
		int threshold = (int)((double)img.length * 0.8);
		for(int i=0;i<hist.length;i++)
		{
			if( hist[i] > threshold ) return true;
		}
		return false;
	}
}
