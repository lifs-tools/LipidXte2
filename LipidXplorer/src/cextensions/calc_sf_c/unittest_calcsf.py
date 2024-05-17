import sys

sys.path.append("../..")
# from lx.mfql import chemsc
from lx.mfql import chemParser

e = chemParser.parseElemSeq("C35 H67 O8 N1 P1 chg(1)")
print(e, e.getWeight())

m = 660.461
e = chemParser.parseElemSeq(
    "C[10..50] H[20..80] O[4..10] N[0..1] P[0..1] db(0,8) chg(1)"
)
for i in e.solveWithCalcSF(m, 10000):
    print(i, i.getWeight(), abs(m - i.getWeight()))
