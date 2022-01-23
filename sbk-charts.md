<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# SBK Charts 
The sbk-charts is the python application to create the xlsx file for given single or multiple CSV file containing the SBK performance 
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

# Single CSV file processing

Example command with single CSV file
```
kmg@kmgs-MBP SBK % ./sbk-charts -i ./samples/charts/sbk-file-read.csv -o ./samples/charts/sbk-file-read.xlsx 

   _____   ____    _  __            _____   _    _              _____    _______    _____
  / ____| |  _ \  | |/ /           / ____| | |  | |     /\     |  __ \  |__   __|  / ____|
 | (___   | |_) | | ' /   ______  | |      | |__| |    /  \    | |__) |    | |    | (___
  \___ \  |  _ <  |  <   |______| | |      |  __  |   / /\ \   |  _  /     | |     \___ \
  ____) | | |_) | | . \           | |____  | |  | |  / ____ \  | | \ \     | |     ____) |
 |_____/  |____/  |_|\_\           \_____| |_|  |_| /_/    \_\ |_|  \_\    |_|    |_____/

Sbk Charts Version : 0.96
Input Files :  ./samples/charts/sbk-file-read.csv
Output File :  ./samples/charts/sbk-file-read.xlsx
xlsx file : ./samples/charts/sbk-file-read.xlsx created
Time Unit : NANOSECONDS
Reading : FILE

```
you can see the sample [fil read in csv](./samples/charts/sbk-file-read.csv) as input file and the generated output file is [file read graphs](./samples/charts/sbk-file-read.xlsx)


## Multiple CSV files processing

Example command with multiple CSV files
```
kmg@kmgs-MBP SBK % ./sbk-charts -i ./samples/charts/sbk-file-read.csv,./samples/charts/sbk-rocksdb-read.csv -o ./samples/charts/sbk-file-rocksdb-read.xlsx

   _____   ____    _  __            _____   _    _              _____    _______    _____
  / ____| |  _ \  | |/ /           / ____| | |  | |     /\     |  __ \  |__   __|  / ____|
 | (___   | |_) | | ' /   ______  | |      | |__| |    /  \    | |__) |    | |    | (___
  \___ \  |  _ <  |  <   |______| | |      |  __  |   / /\ \   |  _  /     | |     \___ \
  ____) | | |_) | | . \           | |____  | |  | |  / ____ \  | | \ \     | |     ____) |
 |_____/  |____/  |_|\_\           \_____| |_|  |_| /_/    \_\ |_|  \_\    |_|    |_____/

Sbk Charts Version : 0.96
Input Files :  ./samples/charts/sbk-file-read.csv,./samples/charts/sbk-rocksdb-read.csv
Output File :  ./samples/charts/sbk-file-rocksdb-read.xlsx
xlsx file : ./samples/charts/sbk-file-rocksdb-read.xlsx created
Time Unit : NANOSECONDS
Reading : FILE, ROCKSDB

```
you can see the sample [fil read in csv](./samples/charts/sbk-file-read.csv) and the [rocksdb red in csv](./samples/charts/sbk-rocksdb-read.csv) as input files and the generated output file is [file and rocksdb read comparesion](./samples/charts/sbk-file-rocksdb-read.xlsx)


