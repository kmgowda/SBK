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

import sys
import argparse

def main():
   parser = argparse.ArgumentParser(description='sbk charts')
   parser.add_argument('-i','--ifile', help='Input CSV file', required=True)
   parser.add_argument('-o','--ofile', help='Output xlsx file',  default="out.xlsx")
   args = parser.parse_args()
   print('Input file is ', args.ifile)
   print('Output file is ', args.ofile)

if __name__ == "__main__":
   main()