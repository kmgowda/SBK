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
import xlsxwriter


class SbkCharts:
    def __init__(self, iFile, oFile):
        self.iFile = iFile
        self.oFile = oFile

    def create_sheets(self):
        df = pandas.read_csv(self.iFile)
        header = df.columns.tolist()
        wb = xlsxwriter.Workbook(self.oFile)
        ws1 = wb.add_worksheet("Regular")
        ws2 = wb.add_worksheet("Total")
        for c, h in enumerate(header):
            ws1.write(0, c, h)
            ws2.write(0, c, h)

        r1 = 1
        r2 = 1
        for row in df.iterrows():
            if row[1]['Type'] == 'Total':
                for c, h in enumerate(header):
                    ws2.write(r2, c, row[1][h])
                r2 += 1
            else:
                for c, h in enumerate(header):
                    ws1.write(r1, c, row[1][h])
                r1 += 1
        wb.close()
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
