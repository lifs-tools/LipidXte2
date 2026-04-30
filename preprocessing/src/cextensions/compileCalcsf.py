# /usr/bin/python
import os
import unittest_calcsf

os.remove("calcsf26.pyd")
os.system(
    "mingw32-gcc -o calcsf26.pyd calcsf.c -IC:\Python26\include -shared -lpython26 -LC:\Python26\libs"
)
unittest_calcsf.test()

os.system("copy calcsf26.pyd ..")
