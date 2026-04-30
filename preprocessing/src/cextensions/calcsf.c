#include <Python.h>
#include <stdio.h>

/* input of lipidsfcalc: 
 * <lwBndC, upBndC, lwBndH, upBndH, wgtOPN, mass>
 * 	call: lipidsfcalc(0, 100, 5, 50, 23.8979, 849.987)
 */
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

static PyObject *
calcsf_totalcalcsf(PyObject *self, PyObject *args) {

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
	int newlwBndH;
	int charge;

	int count = 0;

	float mass, m, tolerance;
	float dbLowerBound, dbUpperBound;
	float summand_charge;
	float massSum;

	float CBuf, HBuf, OBuf, PBuf, NBuf, SBuf, NaBuf, DBuf, CiBuf, ClBuf, LiBuf, NiBuf, cRDB;
	float Htimes2;
	
	long jC, jH, jO, jN, jP, jS, jNa, jD, jCi, jCl, jLi, jNi;

	int lengthOutput = 32;
	int i;

	PyObject* seqBuffer;
	PyObject* listOutSeq;
	listOutSeq = PyList_New(0);

	if (!PyArg_ParseTuple(args, "iiiiiiiiiiiiiiiiiiiiiiiiffffi", 
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
				&mass, &tolerance, &dbLowerBound, &dbUpperBound, &charge))
		return NULL;

	//printf("TESTTEST\n\n");

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
		//cRDB = 2 * jC + jP - jH + jP + 2 + jN;
		//{ "C",  12.000000000,   +2.0, 'C', 0, 30, 0 },
		//{ "13C", 13.0033548378, +2.0, '1', 0, 0, 0 },
		//{ "H",   1.0078250321,  -1.0, 'H', 0, 50, 0 },
		//{ "D",   2.0141017780,  -1.0, 'D', 0, 0, 0 },
		//{ "N",  14.0030740052,  +1.0, 'N', 0, 5, 0 },
		//{ "15N", 15.0001088984,   +1.0, 'M', 0, 0, 0 },
		//{ "O",  15.9949146221,   0.0, 'O', 0, 10, 0 },
		//{ "F",  18.99840320,    -1.0, 'F', 0, 0, 0 },
		//{ "Na", 22.98976967,    -1.0, 'A', 0, 0, 0 },
		//{ "Si", 27.9769265327,  +2.0, 'I', 0, 0, 0 },
		//{ "P",  30.97376151,    +1.0, 'P', 0, 0, 0 },
		//{ "S",  31.97207069,     0.0, 'S', 0, 0, 0 },
		//{ "Cl", 34.96885271,    -1.0, 'L', 0, 0, 0 },
		//{ "Br", 78.9183376,     -1.0, 'B', 0, 0, 0 },
		///************************************************************************
		//* CALC_RDB:	Calculates rings & double bond equivalents.    		*
		//* Input: 	nothing (uses global variables)			   	*
		//* Returns. 	RDB.				       			*
		//*************************************************************************/
		//float calc_rdb(void)
		//{
		//int i;
		//float sum = 2.0;
		//
		//for (i=0; i < nr_el; i++)
		//	sum += el[i].val * el[i].cnt;
		//
		//return (sum/2.0);	
		//}
		cRDB = 2.0 + (float)((jC * 2) + (jH * -1) + (jCl * -1) + jN + (jNa * -1) + (jP) + (jD * -1) + (jCi * 2) + (jNi));
		cRDB = cRDB / 2.0;

		if ((dbLowerBound <= cRDB) && (cRDB <= dbUpperBound)){

			// check if it is the right mass
			massSum = (CBuf + HBuf + OBuf + NBuf + PBuf + SBuf + NaBuf + DBuf + CiBuf + ClBuf + LiBuf + NiBuf + summand_charge);
			if (charge != 0){
				m = ((float)abs(charge)) * mass;
				//tolerance = ((float)abs(charge)) * ((float)tolerance);
			} else {
				m = mass;
			}

//			fprintf(file, "%4.4f\n", cRDB);
//			fprintf(file, "jC: %d, jH: %d, jO: %d, jN: %d, jP: %d \n", jC, jH, jO, jN, jP);
//			fprintf(file, "massSum: %4.4f m: %4.4f tolerance: %4.4f\n", massSum, m, tolerance);

			if (massSum >= (m - tolerance)) {
				if (massSum <= (m + tolerance)){

					// is only valid for charge == [-1, 0, 1]
					//printf("massSum: %d, abs(charge) % 2: %d, jN % 2: %d\n", ((int)(abs(massSum)) % 2), (abs(charge) % 2), (jN % 2));
					if ((((((int)(abs(massSum) - abs(jD + jCi + jNi)) % 2) == ((abs(charge) % 2) + (jN % 2)) % 2)) && (jH < 128)) ||
							(jH > 127)){// || (jN == 0)){

						//printf("\nreturn: %d count:%d", i, count);

					//if (((jN % 2 != (int)(abs(massSum)) % 2) && abs(charge) % 2 == 1) ||
						//((jN % 2 != (int)(abs(massSum)) % 2) && abs(charge) % 2 == 0)){
				//		(abs(charge) % 2 == 0)){
				//fprintf(file, "HIT\n");

						PyList_Append(listOutSeq, PyList_New(12));
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

	//fclose(file);

	//for (i = count; i++; i < lengthOutput){
	//}

	return listOutSeq;
}

//******************************************************************************
//*                                                                            *
//*  Here is the SevenGoldenRules Algorithm. Watch out for 'floating point'.   *
//*                                                                            *
//******************************************************************************

static PyObject *
calcsf_sevenRules(PyObject *self, PyObject *args) {

	//!!! Prerequisit: subtract ion adduct from mass. 
	//	This is nessecary for using chemical constraints 
	//	on the sum compositions


	// floating point error variable
	int lwBndC, upBndC, lwBndH, upBndH;
	int lwBndO, upBndO, lwBndP, upBndP;
	int lwBndNa, upBndNa, lwBndS, upBndS;
	int lwBndN, upBndN, newlwBndH;
	int charge;

	int count = 0;
	int boolFitRules;

	float mass, tolerance;
	float dbLowerBound, dbUpperBound;
	float summand_charge;
	float massSum;

	float CBuf, HBuf, OBuf, PBuf, NBuf, SBuf, NaBuf, cRDB;
	float Htimes2;
	
	long jC, jH, jO, jN, jP, jS, jNa;

	int lengthOutput = 32;

	// for seven rules
	int intEMinus, intSum, intLewisSum, intSeniorMax, intSumOfValences;
	int jBr = 0;
	int jSi = 0;
	int jF = 0;
	int jCl = 0;

	PyObject* seqBuffer[10];
	PyObject* listOutSeq;
	listOutSeq = PyList_New(lengthOutput);

	// floating point error variable initialisation
	if (!PyArg_ParseTuple(args, "iiiiiiiiiiiiiiffffi", 
				&lwBndC, &upBndC, 
				&lwBndH, &upBndH, 
				&lwBndO, &upBndO, 
				&lwBndN, &upBndN, 
				&lwBndP, &upBndP, 
				&lwBndS, &upBndS,
				&lwBndNa, &upBndNa,
				&mass, &tolerance, 
				&dbLowerBound, &dbUpperBound, 
				&charge))
		return NULL;

	// charge can only be between 0 and 1
	summand_charge = (double)-0.00055 * (double)charge;
	
	//printf("charge: %d summand_charge: %f\n", charge, summand_charge);
	//
	// First Rule: restriction for element numbers
	//
	if ((mass / C) > upBndC) {
		upBndC = (int) mass / C;
	}

	// floating point error variable may be changed (but casted right)
	if ((mass / H) < upBndH) {
		upBndH =  (int)(mass / H);
	}

	if ((mass / O) < upBndO) {
		upBndO = (int) mass / O;
	}

	if ((mass / N) < upBndN) {
		upBndN = (int) mass / N;
	}

	if ((mass / NA) < upBndNa) {
		upBndNa = (int) mass / NA;
	}

	if ((mass / S) < upBndS) {
		upBndS = (int) mass / S;
	}

	if ((mass / P) < upBndP) {
		upBndP = (int) mass / P;
	}

	upBndC += 1;
	// floating point error variable is changed
	upBndH += 1;
	upBndO += 1;
	upBndP += 1;
	upBndN += 1;
	upBndS += 1;
	upBndNa += 1;

	Htimes2 = 2 * H;

	jP = lwBndP;
	PBuf = jP * P;

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

	for (jH; jH <= (int)upBndH; jH += 1){

		// double bound equivalence
		//cRDB = 2 * jC + jP - jH + jP + 2 + jN;
		//{ "C",  12.000000000,   +2.0, 'C', 0, 30, 0 }, valence: 4
		//{ "13C", 13.0033548378, +2.0, '1', 0, 0, 0 }, valence: 4
		//{ "H",   1.0078250321,  -1.0, 'H', 0, 50, 0 }, valence: 1
		//{ "D",   2.0141017780,  -1.0, 'D', 0, 0, 0 },
		//{ "N",  14.0030740052,  +1.0, 'N', 0, 5, 0 }, valence: 3
		//{ "15N", 15.0001088984,   +1.0, 'M', 0, 0, 0 }, valence: 3
		//{ "O",  15.9949146221,   0.0, 'O', 0, 10, 0 }, valence: 4
		//{ "F",  18.99840320,    -1.0, 'F', 0, 0, 0 }, 
		//{ "Na", 22.98976967,    -1.0, 'A', 0, 0, 0 }, valence: 1
		//{ "Si", 27.9769265327,  +2.0, 'I', 0, 0, 0 },
		//{ "P",  30.97376151,    +1.0, 'P', 0, 0, 0 }, valence: 3
		//{ "S",  31.97207069,     0.0, 'S', 0, 0, 0 }, valence: 3
		//{ "Cl", 34.96885271,    -1.0, 'L', 0, 0, 0 },
		//{ "Br", 78.9183376,     -1.0, 'B', 0, 0, 0 },
		///************************************************************************
		//* CALC_RDB:	Calculates rings & double bond equivalents.    		*
		//* Input: 	nothing (uses global variables)			   	*
		//* Returns. 	RDB.				       			*
		//*************************************************************************/
		//float calc_rdb(void)
		//{
		//int i;
		//float sum = 2.0;
		//
		//for (i=0; i < nr_el; i++)
		//	sum += el[i].val * el[i].cnt;
		//
		//return (sum/2.0);	
		//}
		//
		// Second Rule: 
		// check input
		//
		// SENIOR's theorem:
		// 	i) the sum of valences or the total number of atoms
		// 	having odd valences is even;
		// 	ii) the sum of valences is greater than or equal 
		// 	to twice the maximum valence;
		// 	iii) the sum of valences is greater than or equal
		// 	to twice the number of atoms minus 1
		
		boolFitRules = 0;
		// my version
		//intSumOfValences = jC * 4 + jH * 1 + jN * 3 + jO * 4 + jNa * 1 + jP * 3 + jS * 3;
		//intNumberOfAtoms = jC + jH + jN + jO + jNa + jP + jS;
		//
		//	in the orginal script Na is missing.....
		intEMinus = 4 * jC + jH + 7 * jBr + 7 * jCl + 7 * jF + 5 * jN + 6 * jO + 5 * jP + 6 * jS + 4 * jSi;
		intSum = jC + jH + jBr + jCl + jF + jN + jO + jP + jS + jSi;
		intLewisSum = 4 * jC + jH + jBr + jCl + jF + 3 * jN + 2 * jO + 3 * jP + 2 * jS + 4 * jSi;
		intSeniorMax = 4 * jC + jH + jBr + jCl + jF + 5 * jN + 2 * jO + 5 * jP + 6 * jS + 4 * jSi;

		// Senior check
		if (intSeniorMax < (2 * (intSum - 1))){
			boolFitRules = -1;
		}

		if (intSumOfValences <= 7) {
			boolFitRules = -1;
		}

		// Lewis Check i)
		if (intLewisSum % 2 == 1) {
			boolFitRules = -1;
		}

		// Lewis Check ii)
		if (intEMinus <= 2 * 4) {
			boolFitRules = -1;
		}

		// Lewis Check iii)
		if (intLewisSum < 2 * intSum) {
			boolFitRules = -1;
		}

		// Forth Rule: check C/H ratio
		//
		if (jC > 0) {
			if ((jH / jC < 0.2) | (jH / jC > 3.1))	{
				boolFitRules = -1;
			}
		}

		// Fifth Rule: check C/x (x != H) ratio
		//
		if (jC > 0) {
			if ((jN / jC < 0) | (jN / jC > 1.3)) {
				boolFitRules = -1;
			}
		}

		if (jC > 0 && jO > 0) {
			if (((jO / jC) < 0) | ((jN / jO) > 1.2)) {
				boolFitRules = -1;
			}
		}

		if (jC > 0) {
			if ((jP / jC < 0) | (jP / jC > 0.3)) {
				boolFitRules = -1;
			}
		}

		if (jC > 0) {
			if ((jS / jC < 0) | (jS / jC > 0.8)) {
				boolFitRules = -1;
			}
		}

		// Sixth Rule: element probability check through NOPS
		//
		if ((jN > 1) & (jO > 1) & (jP > 1) & (jS > 1)) {
			if ((jN >= 10) | (jO >= 20) | (jP >= 4) | (jS >= 3)) {
				boolFitRules = -1;
			}
		}

		if ((jN > 3) & (jO > 3) & (jP > 3)) {
			if ((jN >= 11) | (jO >= 22) | (jP >= 6)) {
				boolFitRules = -1;
			}
		}

		if ((jS > 1) & (jO > 1) & (jP > 1)) {
			if ((jS >= 3) | (jO >= 14) | (jP >= 3)) {
				boolFitRules = -1;
			}
		}

		if ((jS > 1) & (jN > 1) & (jP > 1)) {
			if ((jS >= 3) | (jN >= 4) | (jP >= 3)) {
				boolFitRules = -1;
			}
		}

		if ((jS > 6) & (jN > 6) & (jO > 6)) {
			if ((jS >= 8) | (jN >= 19) | (jO >= 14)) {
				boolFitRules = -1;
			}
		}

		cRDB = 2.0;
		cRDB += ((float)((jC * 2) + (jH * -1) + jN + (jNa * -1) + (jP))) / 2.0;

		if (((dbLowerBound <= cRDB) && (cRDB <= dbUpperBound)) & boolFitRules){

			// check if it is the right mass
			massSum = (CBuf + HBuf + OBuf + NBuf + PBuf + SBuf + NaBuf + summand_charge);
			if (massSum >= (mass - tolerance)) {
				if (massSum <= (mass + tolerance)){

					// is only valid for charge == [-1, 0, 1]
					//printf("massSum: %d, abs(charge) % 2: %d, jN % 2: %d\n", ((int)(abs(massSum)) % 2), (abs(charge) % 2), (jN % 2));
					//if (((((int)(abs(massSum)) % 2) == ((abs(charge) % 2) + (jN % 2)) % 2))){// || (jN == 0)){
					//
				//printf("Mass ok: %4.4f == %4.4f with tolerance: %4.4f\n", mass, (CBuf + HBuf + OBuf + NBuf + PBuf + summand_charge), tolerance);
				//printf("jC: %d, jH: %d, jO: %d, jN: %d, jP: %d\n", jC, jH, jO, jN, jP);
				//printf("jC * C: %4.4f, jH * H: %4.4f, jO * O: %4.4f, jN * N: %4.4f, jP * P: %4.4f\n", CBuf, HBuf, OBuf, NBuf, PBuf);
				//printf("massSum: %d, chargeN: %d\n", (((int)(abs(massSum)) % 2), ((abs(charge) % 2) + (jN % 2)) % 2));
				//printf("HIT\n");

						seqBuffer[count] = PyList_New(7);
						PyList_SetItem(seqBuffer[count], 0, PyInt_FromLong(jC));
						PyList_SetItem(seqBuffer[count], 1, PyInt_FromLong(jH));
						PyList_SetItem(seqBuffer[count], 2, PyInt_FromLong(jO));
						PyList_SetItem(seqBuffer[count], 3, PyInt_FromLong(jN));
						PyList_SetItem(seqBuffer[count], 4, PyInt_FromLong(jP));
						PyList_SetItem(seqBuffer[count], 5, PyInt_FromLong(jS));
						PyList_SetItem(seqBuffer[count], 6, PyInt_FromLong(jNa));

						//PyList_SetItem(listOutSeq, count, seqBuffer[count]);
						PyList_Append(listOutSeq, seqBuffer[count]);
						count += 1;
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
	return listOutSeq;
}


static PyMethodDef CalcMethods[] = {
	{ "totalcalcsf", calcsf_totalcalcsf, METH_VARARGS,
		"Calculate chemical sum forms."},

	{ "calcsfSevenRules", calcsf_sevenRules, METH_VARARGS,
		"Calculate chemical sum composition filtered due to the seven rules"},

		{NULL, NULL}
};

PyMODINIT_FUNC
initcalcsf(void){
	(void) Py_InitModule("calcsf", CalcMethods);
}



