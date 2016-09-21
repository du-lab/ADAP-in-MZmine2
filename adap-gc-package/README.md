# ADAP Peak Detection. R-package

### Contains a single function _getPeaks_

* To install the package, type in the terminal: `R CMD INSTALL adap.peak.detection_0.0.1.tar.gz`.

* (Optional) To build the package, type in the terminal: `R CMD BUILD adap.peak.detection`.
The file __adap.peak.detection_0.0.1.tar.gz__ will be created.

### How to use (example)

```
library(adap.peak.detection)

params <- list()
params$nNode <- 1
params$Peak_span <- 11
params$Valley_span <- 9

PeakList <- getPeaks(intVec, params, mz, mzVec)     # for EIC peak detection
PeakList <- getPeaks(intVec, params)                # for TIC peak detection
```

### Requirements
* splus2R (>= 1.2-1), ifultools (>= 2.0-4), wmtsa (>= 2.0-2)
