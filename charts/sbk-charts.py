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
from collections import OrderedDict

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

    def get_percentile_columns(self, ws):
        colnames = self.get_columns_from_worksheet(ws)
        ret = OrderedDict()
        for key in colnames.keys():
            if key.startswith("Percentile_"):
                ret[key] = colnames[key]
        return ret

    def get_time_unit(self, ws):
        colnames = self.get_columns_from_worksheet(ws)
        return ws.cell(row=2, column=colnames['LatencyTimeUnit']['number']).value

    def create_percentile_graphs(self, wb, ws, time_unit):
        perc = self.get_percentile_columns(ws)
        for p in perc:
            data_series = Reference(ws, min_col=perc[p]['number'], min_row=1,
                               max_col=perc[p]['number'], max_row=ws.max_row)

            data_series.name = p

            chart = LineChart()
            # adding data to the Bar chart object
            chart.add_data(data_series, titles_from_data=True)

            # set the title of the chart
            chart.title = p + " variations"

            # set the title of the x-axis
            chart.x_axis.title = " Intervals "

            # set the title of the y-axis
            chart.y_axis.title = " Latency Time in "+time_unit

            chart.height = 40
            chart.width = 60

            # add chart to the sheet
            # the top-left corner of a chart
            # is anchored to cell E2 .
            newws = wb.create_sheet(p)
            newws.add_chart(chart)

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
        # print(self.get_columns_from_worksheet(ws1))
        self.create_percentile_graphs(wb, ws1, self.get_time_unit(ws1))
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
