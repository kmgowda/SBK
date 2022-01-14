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

import openpyxl
import pandas
import xlsxwriter
from openpyxl.chart import LineChart, Reference
from openpyxl.utils import get_column_letter


class SbkCharts:
    def __init__(self, iFile, oFile):
        self.iFile = iFile
        self.oFile = oFile

    def create_sheets(self):
        df = pandas.read_csv(self.iFile)
        header = df.columns.values
        wb = xlsxwriter.Workbook(self.oFile)
        ws1 = wb.add_worksheet("R-1")
        ws2 = wb.add_worksheet("T-1")
        for c, h in enumerate(header):
            ws1.set_column(c, c, len(h))
            ws2.set_column(c, c, len(h))
            ws1.write(0, c, h)
            ws2.write(0, c, h)

        r1 = 1
        r2 = 1
        for row in df.iterrows():
            if row[1]['Type'] == 'Total':
                for c, h in enumerate(header):
                    col_size = len(str(row[1][h])) + 1
                    if col_size > len(h):
                        ws2.set_column(c, c, col_size)
                    ws2.write(r2, c, row[1][h])
                r2 += 1
            else:
                for c, h in enumerate(header):
                    col_size = len(str(row[1][h])) + 1
                    if col_size > len(h):
                        ws1.set_column(c, c, col_size)
                    ws1.write(r1, c, row[1][h])
                r1 += 1
        wb.close()
        print("xlsx file %s created" % self.oFile)

    def get_columns_from_worksheet(self, ws):
        return {
            cell.value: {
                'letter': get_column_letter(cell.column),
                'number': cell.column
            } for cell in ws[1] if cell.value
        }

    def create_graphs(self):
        self.create_sheets()
        wb = openpyxl.load_workbook(self.oFile)
        ws1 = wb["R-1"]
        ws2 = wb["T-1"]
        """
        for row in ws1.iter_rows():
            lt = []
            for cell in row:
                lt.append(cell.value)
            print(lt)
        """
        #print(self.get_columns_from_worksheet(ws1))
        colnames = self.get_columns_from_worksheet(ws1)
        values = Reference(ws1, min_col=colnames['Percentile_10']['number'], min_row=2, max_col=colnames['Percentile_10']['number'], max_row=ws1.max_row)
        chart = LineChart()
        # adding data to the Bar chart object
        chart.add_data(values)

        # set the title of the chart
        chart.title = " Line chart "

        # set the title of the x-axis
        chart.x_axis.title = " X_AXIS "

        # set the title of the y-axis
        chart.y_axis.title = " Y_AXIS "

        # add chart to the sheet
        # the top-left corner of a chart
        # is anchored to cell E2 .
        ws3 = wb.create_sheet("Percentile_10")
        ws3.add_chart(chart)
        wb.save(self.oFile)



def main():
    parser = argparse.ArgumentParser(description='sbk charts')
    parser.add_argument('-i', '--ifile', help='Input CSV file', required=True)
    parser.add_argument('-o', '--ofile', help='Output xlsx file', default="out.xlsx")
    args = parser.parse_args()
    charts = SbkCharts(args.ifile, args.ofile)
    print('Input file is ', charts.iFile)
    print('Output file is ', charts.oFile)
    charts.create_graphs()


if __name__ == "__main__":
    main()
