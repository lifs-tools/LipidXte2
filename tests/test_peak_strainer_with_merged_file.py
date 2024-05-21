import glob
import os
from os.path import basename

import pytest

import peakStrainer
import reorder

import folder2LXproject
import mergeLipidX_out

# Linux version
lipidXpath = r"python3 src/lximport.py"
lipidXRun = r"python3 src/lxrun.py"


@pytest.mark.dependency()
def test_convert_rawfiles():
    dirpath = r"tests/resources/PC/"
    rawfiles = glob.glob(dirpath + r"*.raw")
    for rawfile in rawfiles:
        # print(rawfile)
        peakStrainer.main(rawfile)
        xmlfilename = rawfile[:-4] + ".mzXML"

        assert list(open(xmlfilename)) == list(
            open(dirpath + r"expected/" + basename(xmlfilename))
        )


@pytest.mark.dependency(depends=["test_convert_rawfiles"])
def test_peak_strainer_reorder():
    xmlfilepath = r"tests/resources/PC/"
    reorder.main(xmlfilepath)

    for xmlfile in glob.glob(xmlfilepath + r"ordered/*.mzXML"):
        assert list(open(xmlfile)) == list(
            open(xmlfilepath + r"ordered/expected/" + basename(xmlfile))
        )


@pytest.mark.dependency(
    depends=["test_convert_rawfiles", "test_peak_strainer_reorder"]
)
def test_merged_file():
    dirpath = r"tests/resources/PC/"
    print("generate lxp project files")
    folder2LXproject.main("PC", dirpath)
    print("using dirpath " + dirpath)
    searchpath = os.path.join(dirpath, "*.lxp")
    print("search path " + searchpath)

    lxp_files = glob.glob(searchpath)
    print("found {} lxp_files".format(len(lxp_files)))

    for f in lxp_files:
        os.system(lipidXpath + " --prj " + f)

    for f in lxp_files:
        print(f[0:-4] + ".sc")
        os.system(
            lipidXRun
            + " --prj "
            + f
            + " "
            + f[0:-4]
            + ".sc "
            + f[0:-4]
            + "-out.csv"
        )

    mergeLipidX_out.main(dirpath)

    assert len(list(open(dirpath + "merged.csv"))) == len(list(
        open(dirpath + "merged-expected.csv"))
    )
