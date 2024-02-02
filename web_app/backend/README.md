# LipidXte Web Installation guide

## Install external dependencies
1. node.js
1. python 2.7
1. wxPython2.8-win64
1. (for windows) VCForPython27.msi

## Install dependencies for Python 2.7
```bash
pip install lxml
pip install pandas
pip install comptypes
```

## Pipeline requirements
1. (Optional) mzXML file conversion: PeakStrainer/peakStrainer.py
1. Reorder: PeakStrainer/reorder.py
1. Merge: lipidXplorer/lipidXplorer2Lipidx.py
   * It needs LipidXplorer/lximport.py and lxrun.py
   
## Install dependencies for LipidXte
* Copy ```masterXML``` database in User folder
* Copy ```.massSpec``` folder in User folder

## Run LipidXteServer

```bash
npm run start
```


# Parsing rawfile
https://sjcockell.me/2010/06/11/parsing-thermo-finnigan-raw-files/



# For Windows in Linux

## Install wine32 in Linux
https://www.systutorials.com/239913/install-32-bit-wine-1-8-centos-7/

## Install wintricks
https://askubuntu.com/questions/881435/problem-to-install-net-4-6-using-wine

wine python peakStrainer.py 20171128_FTMS-DIA_interlab_QE-MPI_PC_S1.RAW

# In linux
sudo pip install https://extras.wxpython.org/wxPython4/extras/linux/gtk3/centos-7/wxPython-4.0.4-cp27-cp27mu-linux_x86_64.whl
sudo yum install SDL


## Import FA_Anions in Sqlite database
```bash
$ sqlite3 LipidXteSqlite.db
sqlite> DROP TABLE IF EXISTS FA;
sqlite> CREATE TABLE IF NOT EXISTS FA (id INTEGER PRIMARY KEY NOT NULL, mz REAL NOT NULL, fa_c INTEGER NOT NULL, fa_db INTEGER NOT NULL, fa_iso INTEGER NOT NULL);
sqlite> .mode csv
sqlite> .import faanions.csv FA
sqlite> .exit
```

```bash
sqlite3 LipidXteSqlite.db < add_fa.sql
```
