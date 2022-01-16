#!/usr/local/bin/python3
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

# SBK-sheets :  Storage Benchmark Kit - Sheets

import pandas
import xlsxwriter


class SbkSheets:
    def __init__(self, iFile, oFile):
        self.iFile = iFile
        self.oFile = oFile

    def wb_add_two_sheets(self, wb, r_name, t_name, df):
        header = df.columns.values
        r_ws = wb.add_worksheet(r_name)
        t_ws = wb.add_worksheet(t_name)
        for c, h in enumerate(header):
            r_ws.set_column(c, c, len(h))
            t_ws.set_column(c, c, len(h))
            r_ws.write(0, c, h)
            t_ws.write(0, c, h)
        r_row = 1
        t_row = 1
        for row in df.iterrows():
            if row[1]['Type'] == 'Total':
                for c, h in enumerate(header):
                    col_size = len(str(row[1][h])) + 1
                    if col_size > len(h):
                        t_ws.set_column(c, c, col_size)
                    t_ws.write(t_row, c, row[1][h])
                t_row += 1
            else:
                for c, h in enumerate(header):
                    col_size = len(str(row[1][h])) + 1
                    if col_size > len(h):
                        r_ws.set_column(c, c, col_size)
                    r_ws.write(r_row, c, row[1][h])
                r_row += 1

    def create_sheets(self):
        df = pandas.read_csv(self.iFile)
        wb = xlsxwriter.Workbook(self.oFile)
        self.wb_add_two_sheets(wb, "R1", "T1", df)
        wb.close()
        print("xlsx file %s created" % self.oFile)
