import glob
import os

import folder2LXproject
import mergeLipidX_out

# Linux version
lipidXpath = r"python3 ../src/lximport.py"
lipidXRun = r"python3 ../src/lxrun.py"


def test_merged_file():
    dirpath = r"resources/PC/"
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

    assert list(open(dirpath + "merged.csv")) == list(
        open(dirpath + "merged-expected.csv")
    )
