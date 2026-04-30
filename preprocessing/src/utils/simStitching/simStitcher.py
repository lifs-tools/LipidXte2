'''
Created on 24.05.2017

@author: mirandaa
'''
import sys, os
import logging
import re
from base64 import b64decode
from array import array
from collections import namedtuple
log = logging.getLogger(os.path.basename(__file__))
from utils.peakStrainer_util import getMZXMLEncondedScans, decode_mzXML_Peaks, write2templateMzXML
from itertools import compress

class Scan(object):
    
    class FilterLine(object):
        MODE_POS = '+'
        MODE_NEG = '-'
        
        def __init__(self, filterline):
            self.filterline = filterline
            self.match = re.match(r'(.*)\[(\d+\.\d*)-(\d+\.\d*)\]', filterline)
        
        def __str__(self):
            return self.filterline
        
        def __repr__(self):
            return self.filterline
        
        def _head(self):
            return self.match.group(1)
        def _low(self):
            return float(self.match.group(2))
        def _high(self):
            return float(self.match.group(3))
        def asTuple(self):
            return (self._head(),self._low(),self._high())
        def mode(self):
            if ' + ' in self._head(): return  self.MODE_POS
            elif ' - '  in self._head(): return  self.MODE_NEG
            else: return None
        
        
    def __init__(self, scanNo, filterLine, encodedPeaks, retTime = None):
        self.scanNo = scanNo
        self.filterLine = self.FilterLine(filterLine)
        self.encodedPeaks = encodedPeaks
        self.retTime = retTime
        self.masses, self.intens = decode_mzXML_Peaks(self.encodedPeaks)
        self.previous = None
        self.next = None
        
        
    def __str__(self):
        return 'Scan Num {}, {}, peak Count = {}, low ={}, high = {}'.format(self.scanNo,self.filterLine, len(self.masses), self.overlapLow(), self.overlapHigh())
    
    def __repr__(self):
        return '{}; {}; {}; {};'.format(self.scanNo,self.filterLine, self.retTime, self.encodedPeaks)
    
    def decode_mzXML_Peaks(self, encodedPeaks):
        decoded = b64decode(encodedPeaks)
        peaks = array('f',decoded)
    
        if sys.byteorder != 'big':
            peaks.byteswap()
        
        masses = peaks[::2]
        intens = peaks[1::2]
        return masses, intens
    
    def allPeaks(self):
        return self.decode_mzXML_Peaks(self.encodedPeaks)
    
    def nonOverlapPeaks(self):
        masses, intens = self.allPeaks()
        isNonOverlap = [mass >= self.overlapLow() and mass < self.overlapHigh() for mass in masses]
        
        return list(compress(masses, isNonOverlap)), list(compress(intens, isNonOverlap)) 
    

    def _getHighOverlap(self): # 3 cases either last one, or there is an overlap or there isn't
        if self.next is None or self.filterLine._head() != self.next.filterLine._head(): return 0
        
        if self.filterLine._high() > self.next.filterLine._low():
            return (self.filterLine._high() - self.next.filterLine._low())/2
        else:
            return 0
    
    def _getLowOverlap(self): # 3 cases either first one, or there is an overlap or there isn't
        if self.previous is None or self.filterLine._head() != self.previous.filterLine._head(): return 0
        
        if self.previous.filterLine._high() > self.filterLine._low():
            return (self.previous.filterLine._high() - self.filterLine._low())/2
        else:
            return 0
    
    
    def overlapLow(self):
        if self.previous is None or self.filterLine._head() != self.previous.filterLine._head(): # use the difference in high
            return self.filterLine._low()+ self._getHighOverlap()
        else:
            return self.filterLine._low() + self._getLowOverlap()
        
    def overlapHigh(self):
        if self.next is None  or self.filterLine._head() != self.next.filterLine._head(): # use the difference in low
            return self.filterLine._high()-self._getLowOverlap()
        else:
            return self.filterLine._high()-self._getHighOverlap()
  


def getSampleSimPeak(scans, relintens= 0.05, mzGap = 0.5):
    
    def hasGap(mass, masses, mzGap ):
        idx = masses.index(mass)
        prev =  masses[idx -1] if idx>0 else mass - mzGap
        next =  masses[idx +1] if idx< len(masses)-1 else mass + mzGap
        
        if mass - prev >= mzGap and next - mass >= mzGap:
            return True
        else:
            log.debug('no gap of {} for mass {}'.format(mzGap, mass))
            return False
        
    
    SimSample =  namedtuple('SimSample', 'mass intens filterLine')
    results = []
    for scan in scans:
        target = (scan.filterLine._high()+scan.filterLine._low())/2
        masses, intens = scan.nonOverlapPeaks()
        intensLimit = max(intens) * relintens
        
        hasIntens = [inten >= intensLimit for inten in intens ]
        
        validMasses = list(compress(masses,hasIntens ))
        validIntens = list(compress(intens,hasIntens ))
        
        hasMassesGap = [hasGap(mass, validMasses, mzGap) for mass in validMasses]
        
        gapMasses = list(compress(validMasses,hasMassesGap ))
        gapIntens = list(compress(validIntens,hasMassesGap ))

        sortedMasses = sorted(gapMasses, key= lambda mass: abs(mass-target))
        
        if not sortedMasses:
            log.error('no sample found for {}'.format(scan.filterLine))
        else:
            for mass in sortedMasses:
                idx = gapMasses.index(mass)
                simSample = SimSample(gapMasses[idx],gapIntens[idx],scan.filterLine )
                results.append(simSample)
        
    return results


def getMatchingMS(sampleSimPeaks, scans_ms):
    results = []
    for sample in sampleSimPeaks:
        probalemMSfilterline = sample.filterLine._head().replace('SIM', 'Full') #dirty hack
        scan_ms = [scan for scan in scans_ms if scan.filterLine._head() == probalemMSfilterline]
        if not scan_ms:
            log.error('no MS found for sample : {}'.format(sample))
            continue
        
        masses, intenses = scan_ms[0].allPeaks()
        target = sample.mass
        
        sortedMasses = sorted(masses, key=lambda mass: abs(mass-target))
        if abs(sortedMasses[0]-target) < 0.01:
            idx = masses.index(sortedMasses[0])
            results.append(sample+(masses[idx], intenses[idx], probalemMSfilterline))
    return results
        
        
def toCSVFile(fileBaseName, dictOrList):
    with file(fileBaseName+'.csv', 'wb') as outfile:
        if type(dictOrList) is list:
            string = '\n'.join(map(str,dictOrList))
            string = string.replace('(','') 
            string = string.replace(')','')
            
        elif type(dictOrList) is dict:
            string = '\n'.join(map(str,dictOrList.items()))
            string = string.replace(':',',')
            string = string.replace('(','') 
            string = string.replace(')','') 
            
        outfile.write(string)
            

def func(x, a, b):
    return  a*(x**-b)


def getFuncVariables(matchingSim):
    log.info('curve fitting to generate projection')
    result = {}
    uniqMSHeads = set([line[5] for line in matchingSim])
    from scipy.optimize import curve_fit
    for uniqMShead in uniqMSHeads:
        matchingSimSelection = [line for line in matchingSim if line[5] == uniqMShead]
#         minMSInten = max([line[4] for line in matchingSimSelection]) * 0.01 # only sample peaks with a minimum intensity for function, performs worse!!!!, dont do it
#         validSimSelection = [line for line in matchingSimSelection if line[4] > minMSInten]
        simMasses, simIntens, simHeads, msMasses, msIntens, msHeads = zip(*matchingSimSelection)
        
        sumSimIntens = sum(simIntens) # to normalize
        simMsIntens = sum(msIntens)
        simIntens_norm = [simInten / sumSimIntens for simInten in simIntens]
        msIntens_norm = [msInten / simMsIntens for msInten in msIntens]
        deviationFactors = [msIntens_norm[idx] / simIntens_norm[idx] for idx, _ in enumerate(msIntens_norm)]
        try:
            popt, pconv = curve_fit(func, msMasses, deviationFactors)
        except:
            log.error('ERROR: Values do not fit a curve')
            popt =  (0,0)
        log.info('function a*(x**-b) uses values: projection, %f, %f for %s', popt[0], popt[1], uniqMShead)
        result[uniqMShead] = popt
    return result

def project_ms2sim(matchingSims, popts):
    
#     simMasses, simIntens, simHeads, msMasses, msIntens, msHeads = zip(*matchingSim)
    
    correctionFactors = [func(simMass, *popts[msHead]) for simMass, simInten, simHead, msMass, msInten, msHead in matchingSims]
    
    simIntens_Corrected = [matchingSim[1] * correctionFactors[idx] for idx, matchingSim in enumerate(matchingSims)]
    return [matchingSim + (simIntens_Corrected[idx],) for idx, matchingSim in enumerate(matchingSims)]

def simStitcher(filePath = None, doCompare = True):
    log.setLevel(logging.DEBUG)
    log.addHandler(logging.StreamHandler()) # log to console
    if log.level == logging.DEBUG: logging.basicConfig(filename='simSitcher.log',filemode='w')
    
    log.info('getMZXMLEncondedScans from'+ filePath)
    scans_mzxml = getMZXMLEncondedScanRows(filePath)
    log.debug('\n'.join(map(str,scans_mzxml)))
    
    log.info('drop first scan because invalid')
    scans_mzxml = scans_mzxml[1:]
        
    log.info('get Sim scans')
    scans_mzxml_sim= [scan for scan in scans_mzxml if ' sim ' in scan[1].lower()]
    log.debug('\n'.join(map(str,scans_mzxml_sim)))
    toCSVFile('scans_mzxml_sim',scans_mzxml_sim )
    
    scans =[Scan(*scan_xml) for scan_xml in scans_mzxml_sim]
    
    log.info('get sorted filterlines') # where low is a number instead of a text
    scans.sort(key=lambda scan: scan.filterLine.asTuple())
    
    log.debug('\n'.join(map(str,scans)))
    toCSVFile('scans', scans)
    
    scans[0].next = scans[1] #previous is None
    for idx, scan in enumerate(scans):
        if idx == 0 or idx >= len(scans)-1: continue
        scans[idx].previous = scans[idx-1]
        scans[idx].next = scans[idx+1]
    scans[-1].previous = scans[-2] #next is None
    
    #-- build stitched scans
    scans_stitched = {}
    for scan in scans:
        head = scan.filterLine._head()
        masses, intens = scan.nonOverlapPeaks()
        scans_stitched.setdefault(head, ([],[]))[0].extend(masses)
        scans_stitched.setdefault(head, ([],[]))[1].extend(intens)
    
    log.info('stitched scans:')
    log.info('\n'.join(scans_stitched.keys()))
    log.debug('\n'.join(map(str,scans_stitched.items())))
    toCSVFile('scans_stitched', scans_stitched)
    names = scans_stitched.keys()
    # -- rename
    for stitched_Head in names:
        min_head = min([scan.filterLine._low() for scan in scans if scan.filterLine._head() == stitched_Head])
        max_head = max([scan.filterLine._high() for scan in scans if scan.filterLine._head() == stitched_Head])
        rename = stitched_Head +'['+str(min_head)+'-'+ str(max_head)+']'
        scans_stitched[rename] = scans_stitched[stitched_Head]
        del scans_stitched[stitched_Head]

    log.info('renamed stitched scans:')
    log.info('\n'.join(scans_stitched.keys()))
    
    
    log.info('writing to :'+outputStitchedFile(filePath))
    write2templateMzXML(outputStitchedFile(filePath), scans_stitched)      
    
    log.info('finish stitching')
    
    if not doCompare: raise SystemExit 
    
    log.info('start validation')
    
    scans_mzxml_ms= [scan for scan in scans_mzxml if ' ms ' in scan[1].lower() and ' full ' in scan[1].lower()]
    log.debug('\n'.join(map(str,scans_mzxml_ms)))
    toCSVFile('scans_mzxml_ms', scans_mzxml_ms)
    
    scans_ms =[Scan(*scan_xml) for scan_xml in scans_mzxml_ms]
    
    log.info('get Valid sim samples')
    sampleSimPeaks = getSampleSimPeak(scans)
    log.debug('\n'.join(map(str,sampleSimPeaks)))
    toCSVFile('sampleSimPeaks', sampleSimPeaks)
    
    log.info('get matching ms peak')
    matchingSim = getMatchingMS(sampleSimPeaks, scans_ms)
    
    popts = getFuncVariables(matchingSim)
    
    projected = project_ms2sim(matchingSim, popts)
    log.debug('\n'.join(map(str,projected)))
    toCSVFile('matchingSim', projected)
    
    log.info('finished comparing')
    
    log.info('start sim intens adjustment')
    scans_adjusted={}
    for idx, scan in enumerate(scans_stitched):
        (masses, intens) = scans_stitched[scan]
        probalemMSfilterline = scan.replace('SIM', 'Full') #dirty hack, second time!
        upto=probalemMSfilterline.index('[')
        maybeMSfilterline = probalemMSfilterline[:upto] 
        correctionFactors = [func(mass, *popts[maybeMSfilterline]) for mass in masses]
    
        intensAdjusted = [intens[idx] * correctionFactors[idx] for idx, _ in enumerate(intens)]
        scans_adjusted[scan] = (masses, intensAdjusted)
        
    log.debug('\n'.join(map(str,scans_adjusted.items())))
    toCSVFile('scans_adjusted', scans_adjusted)
    
    log.info('writing to :'+outputAdjustedFile(filePath))
    write2templateMzXML(outputAdjustedFile(filePath), scans_adjusted)
    
    log.info('finish adjustment')
    
    
    
def outputStitchedFile(fileName):
    dir,file = os.path.split(fileName)
    if not os.path.exists(dir+'\\stitched'):
        os.makedirs(dir+'\\stitched')
    newfilename = dir+'\\stitched\\'+file[:-6]+'-s'+file[-6:]
    return newfilename

def outputAdjustedFile(fileName):
    dir,file = os.path.split(fileName)
    if not os.path.exists(dir+'\\adjusted'):
        os.makedirs(dir+'\\adjusted')
    newfilename = dir+'\\adjusted\\'+file[:-6]+'-as'+file[-6:]
    return newfilename

    
def getMZXMLEncondedScanRows(filePath):
    return zip(*getMZXMLEncondedScans(filePath)) 


if __name__ == '__main__':
    if len(sys.argv) > 0:
        simStitcher(sys.argv[1])
    else:
        simStitcher()
    
    