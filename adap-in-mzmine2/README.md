# MZmine (Du-Lab modification)

To run MZmine:

* Install _Java SE Development Kit 8_ ([link](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))

* Install _Apache Maven_ ([link](https://maven.apache.org/download.cgi) or `brew install maven`)

* Install _R_ ([link](https://cran.r-project.org)) and _R_-packages: xcms ([link](http://bioconductor.org/packages/release/bioc/html/xcms.html)), ifultools, splus2R, wmtsa

* Install _adap.peak.detection_ package (see folder __adap_package__ for instructions)

* Install _MZmine_:

    * In the terminal, enter the directory __owen-mod-mzmine2-master__

    * Type `mvn clean install`. This will create __target__ folder containing the archive file __MZmine-2.21.zip__

    * Unzip the file __MZmine-2.21.zip__.

    * In the created folder __MZmine-2.21__, run the file __startMZmine_MacOSX.sh__

### Enjoy!
