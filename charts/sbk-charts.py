#!/usr/local/bin/python3
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

import argparse
from sbkpy.sheets import SbkMultiSheets
from sbkpy.charts import SbkMultiCharts


def main():
    parser = argparse.ArgumentParser(description='sbk charts')
    parser.add_argument('-i', '--ifiles', help="Input CSV files, seperated by ','", required=True)
    parser.add_argument('-o', '--ofile', help='Output xlsx file', default="out.xlsx")
    args = parser.parse_args()
    print('Input files are ', args.ifiles)
    print('Output file is ', args.ofile)
    sh = SbkMultiSheets(args.ifiles, args.ofile)
    sh.create_sheets()
    ch = SbkMultiCharts(args.ofile)
    ch.create_graphs()


if __name__ == "__main__":
    main()
