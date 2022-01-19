#!/usr/local/bin/python3
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

# sbk_charts :  Storage Benchmark Kit - Charts

from collections import OrderedDict
from openpyxl import load_workbook
from openpyxl.chart import LineChart, Reference, Series
from openpyxl.utils import get_column_letter
import sbkpy.constants as constants


class SbkCharts:
    def __init__(self, file):
        self.file = file
        self.wb = load_workbook(self.file)
        self.time_unit = self.get_time_unit(self.wb[constants.R_PREFIX + "1"])
        self.nu_lantency_charts = 4
        self.latency_groups = [
            ["Percentile_10", "Percentile_20", "Percentile_25", "Percentile_30", "Percentile_40", "Percentile_50"],
            ["Percentile_50", "AvgLatency"],
            ["Percentile_50", "Percentile_60", "Percentile_70", "Percentile_75", "Percentile_80", "Percentile_90"],
            ["Percentile_92.5", "Percentile_95", "Percentile_97.5", "Percentile_99",
             "Percentile_99.25", "Percentile_99.5", "Percentile_99.75", "Percentile_99.9",
             "Percentile_99.95", "Percentile_99.99"]]

    def get_columns_from_worksheet(self, ws):
        return {
            cell.value: {
                'letter': get_column_letter(cell.column),
                'number': cell.column
            } for cell in ws[1] if cell.value
        }

    def get_latency_columns(self, ws):
        column_names = self.get_columns_from_worksheet(ws)
        ret = OrderedDict()
        ret['AvgLatency'] = column_names['AvgLatency']
        ret['MaxLatency'] = column_names['MaxLatency']
        for key in column_names.keys():
            if key.startswith("Percentile_"):
                ret[key] = column_names[key]
        return ret

    def get_time_unit(self, ws):
        colnames = self.get_columns_from_worksheet(ws)
        return ws.cell(row=2, column=colnames['LatencyTimeUnit']['number']).value

    def create_line_chart(self, title, x_title, y_title, height, width):
        chart = LineChart()
        # set the title of the chart
        chart.title = title
        # set the title of the x-axis
        chart.x_axis.title = x_title
        # set the title of the y-axis
        chart.y_axis.title = y_title
        chart.height = height
        chart.width = width
        return chart

    def create_latency_line_graph(self, title):
        return self.create_line_chart(title, "Intervals", "Latency time in " + self.time_unit, 25, 50)

    def create_latency_compare_graphs(self, ws, prefix):
        charts = []
        sheets = []
        for i in range(self.nu_lantency_charts):
            charts.append(self.create_latency_line_graph("Latency Variations"))
            sheets.append(self.wb.create_sheet("Latencies-" + str(i + 1)))

        latencies = self.get_latency_columns(ws)
        for x in latencies:
            data_series = Series(Reference(ws, min_col=latencies[x]['number'], min_row=2,
                                           max_col=latencies[x]['number'], max_row=ws.max_row),
                                 title=prefix + "-" + x)
            for i, g in enumerate(self.latency_groups):
                if x in g:
                    charts[i].append(data_series)
        for i, ch in enumerate(charts):
            sheets[i].add_chart(ch)

    def create_latency_graphs(self, ws, prefix):
        latencies = self.get_latency_columns(ws)
        for x in latencies:
            data_series = Series(Reference(ws, min_col=latencies[x]['number'], min_row=2,
                                           max_col=latencies[x]['number'], max_row=ws.max_row),
                                 title=prefix + "-" + x)
            chart = self.create_latency_line_graph(x + " Variations")
            # adding data
            chart.append(data_series)
            # add chart to the sheet
            sheet = self.wb.create_sheet(x)
            sheet.add_chart(chart)

    def create_throughput_mb_graph(self, ws, prefix):
        chart = self.create_line_chart("Throughput Variations in Mega Bytes / Seconds",
                                       "Intervals", "Throughput in MB/Sec", 25, 50)

        cols = self.get_columns_from_worksheet(ws)
        data_series = Series(Reference(ws, min_col=cols["MB/Sec"]['number'], min_row=2,
                                       max_col=cols["MB/Sec"]['number'], max_row=ws.max_row),
                             title=prefix + "-MB/Sec")
        # adding data
        chart.append(data_series)

        # add chart to the sheet
        sheet = self.wb.create_sheet("MB_Sec")
        sheet.add_chart(chart)

    def create_throughput_records_graph(self, ws, prefix):
        chart = self.create_line_chart("Throughput Variations in Records / Seconds",
                                       "Intervals", "Throughput in Records/Sec", 25, 50)
        cols = self.get_columns_from_worksheet(ws)
        data_series = Series(Reference(ws, min_col=cols["Records/Sec"]['number'], min_row=2,
                                       max_col=cols["Records/Sec"]['number'], max_row=ws.max_row),
                             title=prefix + "-Records/Sec")
        # adding data
        chart.append(data_series)
        # add chart to the sheet
        sheet = self.wb.create_sheet("Records_Sec")
        sheet.add_chart(chart)

    def create_graphs(self):
        r_name = constants.R_PREFIX + "1"
        t_name = constants.T_PREFIX + "1"
        ws1 = self.wb[r_name]
        self.create_throughput_mb_graph(ws1, r_name)
        self.create_throughput_records_graph(ws1, r_name)
        self.create_latency_compare_graphs(ws1, r_name)
        self.create_latency_graphs(ws1, r_name)
        self.wb.save(self.file)
