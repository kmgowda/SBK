<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK-Charts 
The sbk-chars is the python application to create the xlsx file for given CSV file containing the SBK performance 
results. The generated xlsx file contains the graphs of latency percentile variations and throughput variations.

Running SBK Charts:

```
<SBK directory>./sbk-charts
...
kmg@kmgs-MBP SBK % ./sbk-charts -h
usage: sbk-charts [-h] -i IFILES [-o OFILE]

sbk charts

optional arguments:
  -h, --help            show this help message and exit
  -i IFILES, --ifiles IFILES
                        Input CSV files, seperated by ','
  -o OFILE, --ofile OFILE
                        Output xlsx file

Please report issues at https://github.com/kmgowda/SBK

```