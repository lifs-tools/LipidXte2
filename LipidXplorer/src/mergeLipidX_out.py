import sys
import os
import glob
import pandas as pd


def readCSV(out_file):
    print("processing {} to dataframe".format(out_file))
    return pd.read_csv(out_file)


def main(dirpath):
    print("using dirpath " + dirpath)
    searchpath = os.path.join(dirpath, "*-out.csv")

    print("searchpath " + searchpath)
    out_files = glob.glob(searchpath)
    print("found {} out_files".format(len(out_files)))

    df_list = []
    new_df = None
    for out_file in sorted(out_files):
        print("out_file " + out_file)
        filename = os.path.basename(out_file)
        filename = filename.replace("-out.csv", "")
        nce = "" + filename[-2:]

        df = readCSV(out_file)
        if new_df is None:
            new_df = pd.DataFrame(columns=df.columns)

        nce_row = {"PRM": "#NCE", "EC": nce}
        new_df = new_df._append(nce_row, ignore_index=True)

        new_df = new_df._append(df, ignore_index=True)

    if new_df is not None:
        outpath = os.path.join(dirpath, "merged.csv")
        print("writing to " + outpath)
        new_df.to_csv(outpath, index=False)
        print("Done")


if __name__ == "__main__":
    print("select the Folder with *-out.csv files from lipidxplorer")
    dirpath = " ".join(sys.argv[1:])
    main(dirpath)
