# location of the Python header files
 
PYTHON_VERSION = 3.12
PYTHON_LIBRARY_VERSION = py312
PYTHON_LIBRARY_PATH = /Library/Frameworks/Python.framework/Versions/3.12/lib
PYTHON_INCLUDE = /Library/Frameworks/Python.framework/Versions/3.12/include/python3.12
 
# location of the Boost Python include files and library
 
BOOST_INC = /usr/local/opt/boost/include
BOOST_LIB = /usr/local/opt/boost/lib
BOOST_PYTHON_LIB = /usr/local/opt/boost-python3/lib
 
# compile mesh classes
TARGET = CalcSF
MODULE = lipidxexception


install: $(TARGET).so
	cp $(TARGET).so ../../lx/mfql/calcsf37_64_arm/calcsf.so
 
$(TARGET).so: $(MODULE).o $(TARGET).o
	g++ -shared -Wl, -arch arm64 $(MODULE).o $(TARGET).o -L$(BOOST_LIB) -L$(BOOST_PYTHON_LIB) -lboost_python312 -L$(PYTHON_LIBRARY_PATH) -lpython$(PYTHON_VERSION) -o $(TARGET).so

$(TARGET).o: $(TARGET).cpp
	g++ -std=c++20 -arch arm64 -I$(PYTHON_INCLUDE) -I$(BOOST_INC) -fPIC -c $(TARGET).cpp
 
$(MODULE).o: $(MODULE).cpp
	g++ -std=c++20 -arch arm64 -I$(PYTHON_INCLUDE) -I$(BOOST_INC) -fPIC -c $(MODULE).cpp

