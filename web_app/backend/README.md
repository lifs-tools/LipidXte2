# LipidXte Web Installation guide

## 📝 Table of Contents

- [Installation](#-installation)
- [Getting Started](#-getting-started)
- [Usage of handling RAW file](#-usage)
- [Deployment](#-deployment)
- [Built Using](#-built-using)
- [Authors](#-authors)
- [Acknowledgments](#-acknowledgements)

## 🧐 Installation

### Install external dependencies
1. node.js

### Pipeline requirements
1. (Optional) mzXML file conversion: PeakStrainer/peakStrainer.py
1. Reorder: PeakStrainer/reorder.py
1. Merge: lipidXplorer/lipidXplorer2Lipidx.py
   * It needs LipidXplorer/lximport.py and lxrun.py
   
### Install dependencies for LipidXte
* Copy ```masterXML``` database in User folder
* Copy ```.massSpec``` folder in User folder


### PM2
PM2 is a daemon process manager that will help us manage and keep our application online.

#### Install PM2

```bash
npm install pm2 -g
```

#### Add PM2 to the startup script
```bash
sudo pm2 startup systemd
```

## 🏁 Getting Started

### Run LipidXteServer locally or with PM2

Start `LipidXteServer` locally.
```bash
npm run start
```

Start `LipidXteServer` in PM2 and it will restart on file change.
```bash
cd /local/moon/drive_d/projects/LipidXteServer
sudo pm2 start index.js --watch
```

### Install certificates
We use SSL certificates for https connection. Please update the certificate lines in `index.js` when the certificate is renewed.

```js
814:   } else {
815:     // Run the server in production mode
816:     const fs = require('fs')
817:
818:     let options = {
819:       key: fs.readFileSync('/etc/pki/tls/private/lipidxte.key.2023'),  // Private keyfile
820:       cert: fs.readFileSync( '/etc/pki/tls/certs/lipidxte.pem.2024' )  // Certificate
821:     }
```

After updating `index.js`, if the app is not restarted, please use the following commands:

```bash
cd /local/moon/drive_d/projects/LipidXteServer

sudo pm2 status                  # Check the list of apps
sudo pm2 stop index.js           # Stop the server
sudo pm2 start index.js --watch  # Start the server with watch option
```


## 🎈 Usage of handlig RAW file

### Parsing rawfile
https://sjcockell.me/2010/06/11/parsing-thermo-finnigan-raw-files/



### For Windows in Linux

#### Install wine32 in Linux
https://www.systutorials.com/239913/install-32-bit-wine-1-8-centos-7/

#### Install wintricks
https://askubuntu.com/questions/881435/problem-to-install-net-4-6-using-wine

wine python peakStrainer.py 20171128_FTMS-DIA_interlab_QE-MPI_PC_S1.RAW

### In linux
sudo pip install https://extras.wxpython.org/wxPython4/extras/linux/gtk3/centos-7/wxPython-4.0.4-cp27-cp27mu-linux_x86_64.whl
sudo yum install SDL


## 🚀 Deployment

#### Import FA_Anions in Sqlite database
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

#### Release all the sources to https://github.com/lifs-tools/LipidXte2

##### git subtree

```bash
# Add remote URLs point to the separate projects that we're interested in
git remote add -f lipidxte git@git.mpi-cbg.de:scicomp/scidev_team/MassSpec.git
git remote add -f backend  git@git.mpi-cbg.de:scicomp/scidev_team/LipidXteServer.git
git remote add -f frontend git@git.mpi-cbg.de:scicomp/scidev_team/LipidXteWeb.git
git remote add -f LipidXplorer git@git.mpi-cbg.de:scicomp/scidev_team/lipidxplorer_legacy.git

# Merge the branches into the local Git project
git merge -s ours --no-commit --allow-unrelated-histories lipidxte/master
git merge -s ours --no-commit --allow-unrelated-histories backend/master
git merge -s ours --no-commit --allow-unrelated-histories frontend/master
git merge -s ours --no-commit --allow-unrelated-histories LipidXplorer/main

# Create a new directories and copy the git history of the dependent projects into it
git read-tree --prefix=core_app -u lipidxte/master
git read-tree --prefix=web_app/frontend -u frontend/master
git read-tree --prefix=web_app/backend -u backend/master
git read-tree --prefix=LipidXplorer -u LipidXplorer/main

# Commit the changes to keep them safe
git commit -m "chore: subtree merged for core_app, frontend, backend and LipidXplorer "

# Synchronize with updates and changes
git pull -s subtree lipidxte master
git pull -s subtree backend master
git pull -s subtree frontend master
git pull -s subtree LipidXplorer main
```

## ⛏ Built Using
- [VueJs](https://vuejs.org/) - Web Framework
- [Vite](https://vitejs.dev/) - Web Building and Testing Framework
- [Express](https://expressjs.com/) - Server Framework
- [NodeJs](https://nodejs.org/en/) - Server Environment
- [SQLite](https://sqlite.org/) - Database


## ✍ Authors
- [Kai Schuhmann](https://www.mpi-cbg.de/research/researchgroups/currentgroups/andrej-shevchenko/group-members) - initial ideas and work
- [@moon](https://git.mpi-cbg.de/moon) - programming work

## 🎉 Acknowledgements
- Tools from [lifs-tools](https://github.com/lifs-tools)
