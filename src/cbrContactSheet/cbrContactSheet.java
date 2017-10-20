package cbrContactSheet;




public class cbrContactSheet {

	
	
	public static void main(String[] args) {
		

		cbrContactSheetController ctrl = new cbrContactSheetController(args);
		if( ctrl.isOK() == false ) System.exit(1);
		if( ctrl.makeContactSheet() == false ) System.exit(1);
		
		
		
	}

	
	
	
}
