rem calcsf26.pyd
mingw32-gcc -o calcsf26.pyd calcsf.c -IC:\Python26\include -shared -lpython26 -LC:\Python26\libs
unittest_calcsf.py

copy calcsf26.pyd ..
