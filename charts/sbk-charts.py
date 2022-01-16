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
from sbkpy.sbk_charts import SbkSheets, SbkCharts


def main():
    parser = argparse.ArgumentParser(description='sbk charts')
    parser.add_argument('-i', '--ifile', help='Input CSV file', required=True)
    parser.add_argument('-o', '--ofile', help='Output xlsx file', default="out.xlsx")
    args = parser.parse_args()
    print('Input file is ', args.ifile)
    print('Output file is ', args.ofile)
    sh = SbkSheets(args.ifile, args.ofile)
    sh.create_sheets()
    ch = SbkCharts(args.ofile)
    ch.create_graphs()


if __name__ == "__main__":
    main()
