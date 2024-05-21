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

#include <boost/python/module.hpp>
#include <boost/python/def.hpp>
#include "lipidxexception.hpp"

#define C 12.0
#define H 1.0078250321
#define N 14.0030740052
#define P 30.97376151
#define O 15.9949146221
#define S 31.97207069
#define NA 22.98976967
#define D 2.0141017780
#define CI 13.0033548378
#define CL 34.968852
#define LI 7.016003
#define NI 15.0001088984
#define FL 18.9984032
#define I 126.904477
#define AG 106.905095
#define AL 26.981541
#define W 183.950953
#define TI 47.947947

static PyObject *
//calcsf_totalcalcsf(PyObject *self, PyObject *args) {
calcsf(PyObject *args) {

	// for IO
	//FILE *file;
	//file = fopen("log.txt", "w");
	//file = fopen("C:\\Users\\The Duke\\My_Projects\\lipidxplorer\\trunk\\lib\\cextensions\\log.txt", "w");
	
	int lwBndC, upBndC, lwBndH, upBndH;
	int lwBndO, upBndO, lwBndP, upBndP;
	int lwBndCi, upBndCi, lwBndNi, upBndNi;
	int lwBndS, upBndS, lwBndNa, upBndNa;
	int lwBndD, upBndD, lwBndCl, upBndCl;
	int lwBndN, upBndN, lwBndLi, upBndLi;
	int lwBndF, upBndF, lwBndI, upBndI;
	int lwBndAg, upBndAg, lwBndAl, upBndAl;
	int lwBndW, upBndW, lwBndTi, upBndTi;
	int newlwBndH;
	int charge;

	int count = 0;

	float mass, m, tolerance;
	float dbLowerBound, dbUpperBound;
	float summand_charge;
	float massSum;

	float CBuf, HBuf, OBuf, PBuf, NBuf, SBuf; 
	float NaBuf, DBuf, CiBuf, ClBuf, LiBuf; 
	float NiBuf, FBuf, IBuf, AgBuf, AlBuf;
	float WBuf, TiBuf, cRDB;
	float Htimes2;
	
	long jC, jH, jO, jN, jP, jS, jNa, jD, jCi, 
		jCl, jLi, jNi, jF, jI, jAg, jAl, jW, jTi;

	int i;

	PyObject* listOutSeq;
	listOutSeq = PyList_New(0);

	if (!PyArg_ParseTuple(args, "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiffffi", 
				&lwBndC, &upBndC, 
				&lwBndH, &upBndH, 
				&lwBndO, &upBndO, 
				&lwBndN, &upBndN, 
				&lwBndP, &upBndP, 
				&lwBndS, &upBndS,
				&lwBndNa, &upBndNa,
				&lwBndD, &upBndD, 
				&lwBndCi, &upBndCi,
				&lwBndCl, &upBndCl,
				&lwBndLi, &upBndLi,
				&lwBndNi, &upBndNi,
				&lwBndF, &upBndF,
				&lwBndI, &upBndI,
				&lwBndAg, &upBndAg,
				&lwBndAl, &upBndAl,
				&lwBndW, &upBndW,
				&lwBndTi, &upBndTi,
				&mass, &tolerance, &dbLowerBound, &dbUpperBound, &charge))
		return NULL;

	// charge can only be between 0 and 1
	summand_charge = (double)-0.00055 * (double)charge;
	//return (sum + self.charge * -0.00055) / abs(self.charge)
	//summand_charge = 0;
	//printf("charge: %d summand_charge: %f\n", charge, summand_charge);

	// check input
	upBndC += 1;
	upBndH += 1;
	upBndO += 1;
	upBndP += 1;
	upBndN += 1;
	upBndS += 1;
	upBndNa += 1;
	upBndD += 1;
	upBndCi += 1;
	upBndCl += 1;
	upBndLi += 1;
	upBndNi += 1;
	upBndF += 1;
	upBndI += 1;
	upBndAg += 1;
	upBndAl += 1;
	upBndW += 1;
	upBndTi += 1;

	Htimes2 = 2 * H;

	// N law: 	* if the number of N in the sum composition is odd then the mass is odd, too
	// 			* and the other way round
	// 			* things change, when the charge is odd, then it is:
	// 			* if the number of N in the sum composition is odd then the mass is even
	// 			* and the other way round
	// 			
	// Begin with algorithm 1
	
	//jP = lwBndP;
	//PBuf = jP * P;
	jTi = lwBndTi;
	TiBuf = (float)jTi * TI;

	for (jTi; jTi < upBndI; jTi += 1) {
		jW = lwBndW;
		WBuf = (float)jW * W;

	for (jW; jW < upBndW; jW += 1) {
		jAl = lwBndAl;
		AlBuf = (float)jAl * AL;

	for (jAl; jAl < upBndAl; jAl += 1) {
		jAg = lwBndAg;
		AgBuf = (float)jAg * AG;

	for (jAg; jAg < upBndAg; jAg += 1) {
		jI = lwBndI;
		IBuf = (float)jI * I;

	for (jI; jI < upBndI; jI += 1) {
		jF = lwBndF;
		FBuf = (float)jF * FL;

	for (jF; jF < upBndF; jF += 1) {
		jCl = lwBndCl;
		ClBuf = (float)jCl * CL;

	for (jCl; jCl < upBndCl; jCl += 1) {
		jLi = lwBndLi;
		LiBuf = (float)jLi * LI;

	for (jLi; jLi < upBndLi; jLi += 1) {
		jNi = lwBndNi;
		NiBuf = (float)jNi * NI;

	for (jNi; jNi < upBndNi; jNi += 1) {
		jCi = lwBndCi;
		CiBuf = (float)jCi * CI;

	for (jCi; jCi < upBndCi; jCi += 1) {
		jD = lwBndD;
		DBuf = (float)jD * D;

	for (jD; jD < upBndD; jD += 1) {
		jP = lwBndP;
		PBuf = (float)jP * P;

	for (jP; jP < upBndP; jP += 1) {
		jS = lwBndS;
		SBuf = (float)jS * S;

	for (jS; jS < upBndS; jS += 1) {
		jNa = lwBndNa;
		NaBuf = (float)jNa * NA;

	for (jNa; jNa < upBndNa; jNa += 1) {
		jN = lwBndN;
		NBuf = (float)jN * N;

	for (jN; jN < upBndN; jN += 1) {
		jO = lwBndO;
		OBuf = (float)jO * O;

	for (jO; jO < upBndO; jO += 1) {
		jC = lwBndC;
		CBuf = (float)jC * C;

	for (jC; jC < upBndC; jC += 1) {
		jH = lwBndH;
		HBuf = (float)jH * H;

	for (jH; jH <= upBndH; jH += 1){

		// double bound equivalence
		cRDB = 2.0 + (float)((jC * 2) + (jH * -1) + (jCl * -1) + jN + (jNa * -1) + (jP) + (jD * -1) + (jCi * 2) + (jNi) + (jLi * -1) + (jS * 4) + (jI * 5) + (jF * 5));
		cRDB = cRDB / 2.0;

		if ((dbLowerBound <= cRDB) && (cRDB <= dbUpperBound)){

			// check if it is the right mass
			massSum = (CBuf + HBuf + OBuf + NBuf + PBuf + SBuf + NaBuf + DBuf + CiBuf + ClBuf + LiBuf + NiBuf + FBuf + IBuf + summand_charge);
			if (charge != 0){
				m = ((float)abs(charge)) * mass;
				//tolerance = ((float)abs(charge)) * ((float)tolerance);
			} else {
				m = mass;
			}

			//printf("%4.4f\n", cRDB);
			//printf("jC: %d, jH: %d, jO: %d, jN: %d, jP: %d \n", jC, jH, jO, jN, jP);
			//printf("massSum: %4.4f m: %4.4f tolerance: %4.4f\n", massSum, m, tolerance);

			if (massSum >= (m - tolerance)) {
				if (massSum <= (m + tolerance)){

					printf(">>> m/z: %.4f theor.: %.4f\n", m, massSum);

					// is only valid for charge == [-1, 0, 1]
					//printf("massSum: %d, abs(charge) % 2: %d, jN % 2: %d\n", ((int)(abs(massSum)) % 2), (abs(charge) % 2), (jN % 2));
					if ((((((int)(abs(massSum)) % 2) == ((abs(charge) % 2) + (jN % 2)) % 2)) && ((jH + jD) < 128)) 
								|| ((jH + jD) > 127) 
								|| ((jD + jCi + jNi) > 0))/* || (jN == 0))*/{

						//printf("\nreturn: %d count:%d", i, count);

					//if (((jN % 2 != (int)(abs(massSum)) % 2) && abs(charge) % 2 == 1) ||
						//((jN % 2 != (int)(abs(massSum)) % 2) && abs(charge) % 2 == 0)){
				//		(abs(charge) % 2 == 0)){
				//fprintf(file, "HIT\n");
				//
						printf("HIT\n");

						PyList_Append(listOutSeq, PyList_New(18));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 0, PyLong_FromLong(jC));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 1, PyLong_FromLong(jH));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 2, PyLong_FromLong(jO));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 3, PyLong_FromLong(jN));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 4, PyLong_FromLong(jP));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 5, PyLong_FromLong(jS));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 6, PyLong_FromLong(jNa));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 7, PyLong_FromLong(jD));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 8, PyLong_FromLong(jCi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 9, PyLong_FromLong(jCl));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 10, PyLong_FromLong(jLi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 11, PyLong_FromLong(jNi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 12, PyLong_FromLong(jF));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 13, PyLong_FromLong(jI));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 14, PyLong_FromLong(jAg));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 15, PyLong_FromLong(jAl));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 16, PyLong_FromLong(jW));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 17, PyLong_FromLong(jTi));

						count += 1;

						//printf("\nmass: %4.4f, cRDB %.1f, lowDB: %.1f, upDB: %.1f\n", massSum, cRDB, dbLowerBound, dbUpperBound);
					}
				}
			}
		}
	HBuf += H;
	}
	CBuf += C;
	}
	OBuf += O;
	}
	NBuf += N;
	}
	NaBuf += NA;
	}
	SBuf += S;
	}
	PBuf += P;
	}
	DBuf += D;
	}
	CiBuf += CI;
	}
	NiBuf += NI;
	}
	LiBuf += LI;
	}
	ClBuf += CL;
	}
	FBuf += FL;
	}
	IBuf += I;
	}
	AgBuf += AG;
	}
	AlBuf += AL;
	}
	WBuf += W;
	}
	TiBuf += TI;
	}


	return listOutSeq;
}

namespace bp = boost::python;

BOOST_PYTHON_MODULE(calcsf)
{
	//bp::class_<CalcSF>("CalcSC", bp::init<std::string, bp::list, bp::list>())
	bp::def("calcsf", &calcsf);

	// the exception
	myExceptionTypeObj = createExceptionClass("LipidXException");
}
