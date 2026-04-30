# /usr/bin/python
import os

# import unittest_calcsf

# os.remove("calcsf26.pyd")
os.system(
    "gcc -fPIC -o calcsf.pyd calcsf.c -I/usr/local/include/python2.7 -shared -lpython2.7 -L/usr/local/lib/python2.7"
)
# unittest_calcsf.test()

os.system("copy calcsf26.pyd ..")
