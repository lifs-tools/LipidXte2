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

//#include <boost/python/module.hpp>
//#include <boost/python/def.hpp>
#include <Python.h>

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
#define K 38.963708
#define CS 132.905433
#define BR 78.918336

static PyObject *
calcsf(PyObject *self, PyObject *args) {

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
	int lwBndK, upBndK, lwBndCs, upBndCs;
	int lwBndBr, upBndBr;
	int charge;

	int count = 0;

	double mass, m, tolerance;
	double dbLowerBound, dbUpperBound;
	double summand_charge;
	double massSum;

	double CBuf, HBuf, OBuf, PBuf, NBuf, SBuf; 
	double NaBuf, DBuf, CiBuf, ClBuf, LiBuf; 
	double NiBuf, FBuf, IBuf, AgBuf, AlBuf;
	double KBuf, CsBuf, BrBuf;
	double WBuf, TiBuf, cRDB;
	double Htimes2;
	
	long jC, jH, jO, jN, jP, jS, jNa, jD, jCi, 
		jCl, jLi, jNi, jF, jI, jAg, jAl, jW, jTi,
		jK, jCs, jBr;

	PyObject* listOutSeq;
	listOutSeq = PyList_New(0);

	if (!PyArg_ParseTuple(args, "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiddddi", 
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
				&lwBndK, &upBndK,
				&lwBndCs, &upBndCs,
				&lwBndBr, &upBndBr,
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
	upBndK += 1;
	upBndCs += 1;
	upBndBr += 1;

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
	jBr = lwBndBr;
	BrBuf = (double)jBr * BR;

	for (jBr; jBr < upBndBr; jBr += 1) {
		jCs = lwBndCs;
		CsBuf = (double)jCs * CS;

	for (jCs; jCs < upBndCs; jCs += 1) {
		jK = lwBndK;
		KBuf = (double)jK * K;

	for (jK; jK < upBndK; jK += 1) {
		jTi = lwBndTi;
		TiBuf = (double)jTi * TI;

	for (jTi; jTi < upBndI; jTi += 1) {
		jW = lwBndW;
		WBuf = (double)jW * W;

	for (jW; jW < upBndW; jW += 1) {
		jAl = lwBndAl;
		AlBuf = (double)jAl * AL;

	for (jAl; jAl < upBndAl; jAl += 1) {
		jAg = lwBndAg;
		AgBuf = (double)jAg * AG;

	for (jAg; jAg < upBndAg; jAg += 1) {
		jI = lwBndI;
		IBuf = (double)jI * I;

	for (jI; jI < upBndI; jI += 1) {
		jF = lwBndF;
		FBuf = (double)jF * FL;

	for (jF; jF < upBndF; jF += 1) {
		jCl = lwBndCl;
		ClBuf = (double)jCl * CL;

	for (jCl; jCl < upBndCl; jCl += 1) {
		jLi = lwBndLi;
		LiBuf = (double)jLi * LI;

	for (jLi; jLi < upBndLi; jLi += 1) {
		jNi = lwBndNi;
		NiBuf = (double)jNi * NI;

	for (jNi; jNi < upBndNi; jNi += 1) {
		jCi = lwBndCi;
		CiBuf = (double)jCi * CI;

	for (jCi; jCi < upBndCi; jCi += 1) {
		jD = lwBndD;
		DBuf = (double)jD * D;

	for (jD; jD < upBndD; jD += 1) {
		jP = lwBndP;
		PBuf = (double)jP * P;

	for (jP; jP < upBndP; jP += 1) {
		jS = lwBndS;
		SBuf = (double)jS * S;

	for (jS; jS < upBndS; jS += 1) {
		jNa = lwBndNa;
		NaBuf = (double)jNa * NA;

	for (jNa; jNa < upBndNa; jNa += 1) {
		jN = lwBndN;
		NBuf = (double)jN * N;

	for (jN; jN < upBndN; jN += 1) {
		jO = lwBndO;
		OBuf = (double)jO * O;

	for (jO; jO < upBndO; jO += 1) {
		jC = lwBndC;
		CBuf = (double)jC * C;

	for (jC; jC < upBndC; jC += 1) {
		jH = lwBndH;
		HBuf = (double)jH * H;

	for (jH; jH <= upBndH; jH += 1){

		// double bound equivalence
		cRDB = 2.0 + (double)((jC * 2) + (jH * -1) + (jCl * -1) + jN + (jNa * -1) + (jP) + (jD * -1) + (jCi * 2) + (jNi) + (jLi * -1) + (jS * 4) + (jI * 5) + (jF * 5));
		cRDB = cRDB / 2.0;

		if ((dbLowerBound <= cRDB) && (cRDB <= dbUpperBound)){

			// check if it is the right mass
			massSum = (CBuf + HBuf + OBuf + NBuf + PBuf + SBuf + NaBuf + DBuf + CiBuf + ClBuf + LiBuf + NiBuf + FBuf + IBuf + summand_charge);
			if (charge != 0){
				m = ((double)abs(charge)) * mass;
				//tolerance = ((double)abs(charge)) * ((double)tolerance);
			} else {
				m = mass;
			}

			//printf("%4.4f\n", cRDB);
			//printf("jC: %d, jH: %d, jO: %d, jN: %d, jP: %d \n", jC, jH, jO, jN, jP);
			//printf("massSum: %4.4f m: %4.4f tolerance: %4.4f\n", massSum, m, tolerance);

			if (massSum >= (m - tolerance)) {
				if (massSum <= (m + tolerance)){

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

						PyList_Append(listOutSeq, PyList_New(21));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 0, PyInt_FromLong(jC));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 1, PyInt_FromLong(jH));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 2, PyInt_FromLong(jO));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 3, PyInt_FromLong(jN));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 4, PyInt_FromLong(jP));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 5, PyInt_FromLong(jS));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 6, PyInt_FromLong(jNa));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 7, PyInt_FromLong(jD));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 8, PyInt_FromLong(jCi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 9, PyInt_FromLong(jCl));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 10, PyInt_FromLong(jLi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 11, PyInt_FromLong(jNi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 12, PyInt_FromLong(jF));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 13, PyInt_FromLong(jI));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 14, PyInt_FromLong(jAg));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 15, PyInt_FromLong(jAl));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 16, PyInt_FromLong(jW));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 17, PyInt_FromLong(jTi));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 18, PyInt_FromLong(jK));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 19, PyInt_FromLong(jCs));
						PyList_SetItem(PyList_GetItem(listOutSeq, count), 20, PyInt_FromLong(jBr));

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
	KBuf += TI;
	}
	CsBuf += TI;
	}
	BrBuf += TI;
	}

	return listOutSeq;
}

static PyMethodDef CalcSFMethods[] = {

	{"calcsf", (PyCFunction)calcsf, METH_VARARGS, "Calculate the sum composition from a m/z."},
	{NULL, NULL, 0, NULL}
};

void initcalcsf(void) {
/* PyMODINIT_FUNC initcalcsf(void) { */
	//Py_InitModule3("calcsf", CalcSFMethods, "Calculate the sum composition from a m/z.");
	(void) Py_InitModule("calcsf", CalcSFMethods);
}
