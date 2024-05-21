# encoding: utf-8
"""
Created on 23.01.2018
reorder hcd for LipidXte
usage: reorder.py [the folder containing mzXML files]
@author: hongkee
"""

import sys
import os
import time
from utils.reorderScans.reorderScans import saveFiles


def main(dirpath):
    print(dirpath)
    targetOrder = [
        " ms , - ",
        "hcd25.00, - ",
        " ms , - ",
        "hcd30.00, - ",
        " ms , - ",
        "hcd35.00, - ",
    ]
    saveFiles(dirpath, targetOrder)


if __name__ == "__main__":
    if len(sys.argv) == 1:
        print("The folder name must be provided")
        raise SystemExit

    dirpath = " ".join(sys.argv[1:])
    main(dirpath)
