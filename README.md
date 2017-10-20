# cbrContactSheet
This application creates a contactsheet from CBR file.  A contactsheet provides a single overview of the first 12 images comprised in a Comic Book archive.  

Usage: cbrContactSheet {File,Directory} --T(hreads) nn --S(cale) nn --R(ows) nn --W(orkdir) FOLDER --filter --grayscale --expires nn --L(loglevel) nn --C(ompress)

(File,Directory) : defines the folder which contains the CBR files of which the contactsheet are to be created.
T(hreads) nn : Specifies the number of threads to be used. A single thread processes a single CBR
S(cale) nn   : Resizing ratio, e.g. 0.5 reduces the images on the contactsheet to half their original size
R(ows) nn    : Defines the number of rows on the contactsheet
W(orkdir)    : Defines the name of the working folder. The folder must exist prior to starting cbrContactSheet. The folder will comprise the contactsheets created.
F(ilter)     : This is a switch. When set comic pages without images will ne detected and removed from the contactsheet
G(rayscale)  : Currently not used.
E(pires) nn  : Defines the number seconds after which the application will be stopped.
L(og)        : {0..9} : defines the detail level of the logging information. 0 is minimal logging.
C(ompress)   : This is a switch. When set all contactsheets created will be compiled into an singel resulting CBR. Currently not used.

