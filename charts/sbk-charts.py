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

import sys, getopt

def main(argv):
   inputFile = ''
   outputFile = ''
   try:
      opts, args = getopt.getopt(argv,"i:o:",["ifile=","ofile="])
   except getopt.GetoptError:
      print('sbk-charts.py -i <input CSV file> -o <output file, default: out.xlsx>')
      sys.exit(2)
   for opt, arg in opts:
      if opt == '-h':
         print('sbk-charts.py -i <input CSV file> -o <output file, default: out.xlsx>')
         sys.exit()
      elif opt in ("-i", "--ifiles"):
         inputFile = arg
      elif opt in ("-o", "--ofile"):
         outputFile = arg
   if (len(outputFile) == 0):
        outputFile = "out.xlsx"
   print('Input file is ', inputFile)
   print('Output file is ', outputFile)

if __name__ == "__main__":
   main(sys.argv[1:])