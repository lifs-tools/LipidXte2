/***********************************************************************
   This file is part of CalcSF of the LipidXplorer package.

    CalsSF is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Foobar is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

**********************************************************************/

#ifndef LIPIDXEXCEPTION_H
#define LIPIDXEXCEPTION_H

#include <boost/python/object.hpp>

// for the LipidXException
extern PyObject* myExceptionTypeObj;

PyObject* createExceptionClass(const char* name);//, PyObject* baseTypeObj = PyExc_Exception)

#endif
