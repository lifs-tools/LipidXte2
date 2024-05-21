"""
Created on 16.01.2018

@author: mirandaa
@author: HongKee Moon
"""
import sys
import os
import glob
from lxml import etree
import re


def main(class_name, dirpath):
    print("using dirpath " + dirpath)
    importdir = dirpath
    searchpath = os.path.join(dirpath + "/ordered", "*.mzXML")
    print("searchpath " + searchpath)

    mzXML_files = glob.glob(searchpath)
    print("found {} mzXML files".format(len(mzXML_files)))

    if len(mzXML_files) == 0:
        print("no files to process will exit")
        return -1

    print("from hcd and timerange from file {}".format(mzXML_files[0]))

    dom = etree.parse(mzXML_files[0])
    root = dom.getroot()
    scans = root.findall(
        ".//{http://sashimi.sourceforge.net/schema_revision/mzXML_3.0}scan"
    )

    hcdStarts = {}
    hcdEnds = {}
    for scan in scans:
        """populate lists"""
        print(scan.attrib["filterLine"])
        """get hcd"""
        m = re.search(r"@hcd(\d+).?", scan.attrib["filterLine"])
        if m:
            hcd = m.group(1)
        else:
            print(scan.attrib["filterLine"] + " not ms2")
            continue

        print(scan.attrib["retentionTime"])
        n = re.search(r"PT(.*?)S", scan.attrib["retentionTime"])
        if n:
            rt = int(float(n.group(1)))

        print(rt, hcd)

        if m and n:  # there is a hcd and a retention time
            if hcd not in hcdStarts.keys():
                hcdStarts[hcd] = rt - 1
            hcdEnds[hcd] = rt

    print("generate {} project files".format(len(hcdStarts)))

    # Class check based on the filename otherwise all the file considered as PC
    templateFile = r"src/resources/template_project_" + class_name + ".lxp"

    with open(templateFile, "r") as template:
        templateLines = template.read().splitlines()

    print(importdir)
    for hcd in hcdStarts:
        masterscanrun = os.path.join(importdir, "hcd" + hcd + ".sc")
        masterscanimport = masterscanrun
        resultfile = os.path.join(importdir, "hcd" + hcd + "-out.csv")
        timerange = "({},{})".format(hcdStarts[hcd], hcdEnds[hcd])
        print(timerange + " added")
        projectFile = os.path.join(importdir, "hcd" + hcd + ".lxp")

        """
        change the lines
        importdir = D:\ownCloud\kai_generate_out\tmp
        masterscanrun = D:\ownCloud\kai_generate_out\tmp\tmp_hcd.sc
        masterscanimport = D:\ownCloud\kai_generate_out\tmp\tmp_hcd.sc
        ini = D:\ownCloud\kai_generate_out\ImportSettings.ini
        timerange = (1,19)
        resultfile = D:\ownCloud\kai_generate_out\tmp\tmp_hcd-out.csv
        """
        newLines = []
        for line in templateLines:
            if line.startswith("importdir = "):
                line = "importdir = " + importdir
            elif line.startswith("masterscanrun = "):
                line = "masterscanrun = " + masterscanrun
            elif line.startswith("masterscanimport = "):
                line = "masterscanimport = " + masterscanimport
            elif line.startswith("timerange = "):
                line = "timerange = " + timerange
            elif line.startswith("resultfile = "):
                line = "resultfile = " + resultfile

            newLines.append(line)
        print("writing to {}".format(projectFile))
        with open(projectFile, "w") as f:
            f.write("\n".join(newLines))

        print(
            "*** are the mfql files where it says in the template project? ***"
        )


if __name__ == "__main__":
    print(
        "please provide class name and a folder with .mzXML files to generate lipidxplorere project files "
    )
    class_name = sys.argv[1]
    dirpath = " ".join(sys.argv[2:])
    main(class_name, dirpath)
