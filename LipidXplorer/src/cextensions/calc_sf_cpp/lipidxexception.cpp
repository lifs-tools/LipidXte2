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

#include <boost/python/class.hpp>
#include <boost/python/module.hpp>
#include <boost/python/def.hpp>
#include "lipidxexception.hpp"

namespace bp = boost::python;

PyObject* myExceptionTypeObj = 0;

PyObject* createExceptionClass(const char* name)//, PyObject* baseTypeObj = PyExc_Exception)
{
    using std::string;

    string scopeName = bp::extract<string>(bp::scope().attr("__name__"));
    string qualifiedName0 = scopeName + "." + name;
    char* qualifiedName1 = const_cast<char*>(qualifiedName0.c_str());

    //PyObject* typeObj = PyErr_NewException(qualifiedName1, baseTypeObj, 0);
    PyObject* typeObj = PyErr_NewException(qualifiedName1, PyExc_Exception, 0);
    if(!typeObj) bp::throw_error_already_set();
    bp::scope().attr(name) = bp::handle<>(bp::borrowed(typeObj));
    return typeObj;
}
