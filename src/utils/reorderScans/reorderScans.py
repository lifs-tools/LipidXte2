'''
Created on 03.05.2017

@author: mirandaa
'''
import xml.etree.ElementTree as ET
import os
import re
import copy
from shutil import copyfile
    
def getScanHeaders(filePath):
        #     TODO:handle different namespaces of mzxml 
    namespaces = {'xmlns': 'http://sashimi.sourceforge.net/schema_revision/mzXML_3.0'}
    ET.register_namespace('', 'http://sashimi.sourceforge.net/schema_revision/mzXML_3.0')
    tree = ET.parse(filePath)
 
    scanElems = tree.findall('.//xmlns:scan', namespaces)
    
    scans = []
    for scan in scanElems:
#             scanNo = int(scan.attrib['num'])
#         scanNo = scan.attrib['num'].zfill(5)
#  scan number ges confused with other values so its not good
# maybe add zerowidth space to solve '\u200b'
        filterLine = scan.attrib['filterLine']
        scans.append(filterLine)

    return scans
    
def getMZXMLFiles(folderPath):
    result = []
    for file in os.listdir(folderPath):
        if file.lower().endswith(".mzxml"):
            result.append(os.path.join(folderPath, file))
    
    return result 


def getSourceList(folderPath):
    sourceList = []
    
    files = getMZXMLFiles(folderPath)
    first = files[0]
    scans = getScanHeaders(first)
    
    select = set()
    containsMS = False
    containsPos = False
    containsNeg = False
    for scan in scans:
        if not containsMS and ' ms ' in scan: containsMS = True 
        if not containsPos and ' + ' in scan: containsPos = True
        if not containsNeg and ' - ' in scan: containsNeg = True
        
        m = re.search(r'@(.*?) ', scan)
        if m:
            select.add(m.group(1))
    
    #----
    if containsMS:
        if containsNeg: sourceList.append(' ms , - ') # notice comma I split on comma
        if containsPos: sourceList.append(' ms , + ')
        
    for e in select:
        if containsNeg: sourceList.append(e+', - ')
        if containsPos: sourceList.append(e+', + ')
    
    
    return sourceList


def saveFile(filePath, targetOrder, includeSims=False):
       #     TODO:handle different namespaces of mzxml 
    namespaces = {'xmlns': 'http://sashimi.sourceforge.net/schema_revision/mzXML_3.0'}
    ET.register_namespace('', 'http://sashimi.sourceforge.net/schema_revision/mzXML_3.0')
    tree = ET.parse(filePath)
 
    msRunElement = tree.find('.//xmlns:msRun', namespaces)
    scanElems = msRunElement.findall('.//xmlns:scan', namespaces)
    filterlines = [scan.attrib['filterLine'] for scan in scanElems]
    
    _, sortedScanElems = zip(*sorted(zip(filterlines,scanElems )))
    
    for scan in scanElems:
        msRunElement.remove(scan)
    
    
    counter = 0
    for selectionLine in targetOrder:
        type = selectionLine.split(',')[0]
        mode = selectionLine.split(',')[1]
        for scan in sortedScanElems:
            scanLine = scan.attrib['filterLine']
            if includeSims == False and ' sim ' in scanLine.lower(): continue
            if type in scanLine and mode in scanLine:
                new_scan = copy.deepcopy(scan)
                counter = counter + 1 
                new_scan.attrib['retentionTime'] = 'PT'+str(counter)+'.0S'
                msRunElement.append(new_scan)

    dir,file = os.path.split(filePath)

    if not os.path.exists(dir+'/ordered'):
        os.makedirs(dir+'/ordered')
    newfilename = dir+'/ordered/'+file
    tree.write(newfilename, encoding='ISO-8859-1', xml_declaration=True)


def saveFiles(path, targetOrder, includeSims = False):
    print('|'.join(targetOrder))
    files =  getMZXMLFiles(path)
    
    for file in files:
        saveFile(file, targetOrder,includeSims)
        

if __name__ == '__main__':
    main()
    
    
    
    
