#!/usr/local/bin/python3
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

# SBK-Charts :  Storage Benchmark Kit - Charts

import argparse
import pandas
from openpyxl import Workbook


class SbkCharts:
    def __init__(self, iFile, oFile):
        self.iFile = iFile
        self.oFile = oFile

    def create_sheets(self):
        df = pandas.read_csv(self.iFile)
        header = df.columns.tolist()
        wb = Workbook()
        ws1 = wb.active
        ws1.title = "Regular"
        ws2 = wb.create_sheet()
        ws2.title = "Total"
        ws1.append(header)
        ws2.append(header)
        for row in df.iterrows():
            lt = list()
            for h in header:
                lt.append(row[1][h])
            if row[1]['Type'] == 'Total':
                ws2.append(lt)
            else:
                ws1.append(lt)
        wb.save(self.oFile)
        print("xlsx file %s created" % self.oFile)


def main():
    parser = argparse.ArgumentParser(description='sbk charts')
    parser.add_argument('-i', '--ifile', help='Input CSV file', required=True)
    parser.add_argument('-o', '--ofile', help='Output xlsx file', default="out.xlsx")
    args = parser.parse_args()
    charts = SbkCharts(args.ifile, args.ofile)
    print('Input file is ', charts.iFile)
    print('Output file is ', charts.oFile)
    charts.create_sheets()


if __name__ == "__main__":
    main()
