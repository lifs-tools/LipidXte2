# ***********************************************************************
#   This file is part of CalcSF of the LipidXplorer package.
#
#    CalsSF is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Foobar is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
#
# **********************************************************************

import sys

sys.path.append("../..")
from lx.mfql.chemParser import parseElemSeq

e = parseElemSeq("C35 H67 O8 N1 P1 chg(1)")
print(e, e.getWeight())

m = 660.461
e = parseElemSeq("C[10..50] H[20..80] O[4..10] N[0..1] P[0..1] db(0,8) chg(1)")
for i in e.solveWithCalcSF(m, 10000):
    print(i, i.getWeight(), abs(m - i.getWeight()))
