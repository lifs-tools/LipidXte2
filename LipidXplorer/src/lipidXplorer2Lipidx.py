"""
Created on 17.01.2018

@author: mirandaa
"""
import sys
import glob
import os
import folder2LXproject
import mergeLipidX_out

# TODO rewrite these modules to run the method instead of calling from system
lipidXpath = r"/local/moon/LipidXplorer/lximport.py"
lipidXRun = r"/local/moon/LipidXplorer/lxrun.py"


def main(dirpath):
    print("genereate lxp project files")
    folder2LXproject.main(dirpath)
    print("using dirpath " + dirpath)
    searchpath = os.path.join(dirpath, "*.lxp")
    print("searchpath " + searchpath)

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


if __name__ == "__main__":
    print(
        "please provide a folder with .mzXML files to generate lipidX merged files "
    )
    dirpath = " ".join(sys.argv[1:])
    main(dirpath)
