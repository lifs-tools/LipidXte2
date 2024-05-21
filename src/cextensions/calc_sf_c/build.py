import os, shutil

print("Building for the underlying Python version.\n")
os.system("SET VS90COMNTOOLS=%VS100COMNTOOLS%")
os.system("setup.py install")
print("\nCopying calcsf.pyd to ../../lx/mfql/calcsf27_64\n")
shutil.copy(
    r"build/lib.win-amd64-2.7/calcsf.pyd", r"../../lx/mfql/calcsf27_64"
)
