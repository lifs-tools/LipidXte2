"""
Created on 29.03.2017
peakStrainer_util contains functions that are useful but not used
@author: mirandaa
"""
from base64 import b64decode, b64encode
from array import array
import sys
import os
import logging
import random
import xml.etree.ElementTree as ET
import re
import copy
from collections import OrderedDict, namedtuple

log = logging.getLogger(os.path.basename(__file__))


def encodePeaks(masses, intens):
    peak_list = []
    for mass, intens in sorted(zip(masses, intens)):
        peak_list.append(mass)
        peak_list.append(intens)

    peaks = array("f", peak_list)
    if sys.byteorder != "big":
        peaks.byteswap()

    encoded = b64encode(peaks).decode()
    return encoded


def decode_mzXML_Peaks(encodedPeaks):
    """
        Note zmass and intensity are together
       
        """

    decoded = b64decode(encodedPeaks)
    peaks = array("f", decoded)

    if sys.byteorder != "big":
        peaks.byteswap()

    mass = peaks[::2]
    intens = peaks[1::2]
    return mass, intens


def ThermoRawfile2Scans_sample(file_path):
    # NOTE: for testing use ThermoRawfile2Scans_sample instead
    # https://github.com/ethz-institute-of-microbiology/fisher_py/blob/main/examples/raw_file_reader_example.py
    log.info("raw file: %s", file_path)
    from fisher_py.raw_file_reader import RawFileReaderAdapter, RawFileAccess
    from fisher_py.data.business import (
        GenericDataTypes,
        ChromatogramTraceSettings,
        TraceType,
        ChromatogramSignal,
        SpectrumPacketType,
        Scan,
    )
    from fisher_py.data.filter_enums import MsOrderType
    from fisher_py.data import Device, ToleranceUnits
    from fisher_py.mass_precision_estimator import PrecisionEstimate

    rawfile = RawFileReaderAdapter.file_factory(file_path)

    rawfile.select_instrument(Device.MS, 1)

    # Get the first and last scan from the RAW file
    first_scan_number = rawfile.run_header_ex.first_spectrum
    last_scan_number = rawfile.run_header_ex.last_spectrum

    # get the start and end time from the RAW file
    start_time = rawfile.run_header_ex.start_time
    end_time = rawfile.run_header_ex.end_time

    Labels = namedtuple(
        "Labels", "mass intensity resolution baseline noise charge"
    )

    def get_out(raw, scan):
        data = raw.get_centroid_stream(scan, False)

        return Labels(
            tuple(data.masses),
            tuple(data.intensities),
            tuple(data.resolutions),
            tuple(data.baselines),
            tuple(data.noises),
            tuple(data.charges),
        )

    MSrawscans = []
    for scanNum in range(first_scan_number, last_scan_number + 1):
        if scanNum % 10 == 0:
            continue  # just take a sample

        data = rawfile.get_centroid_stream(scanNum, False)
        if data.length == 0:
            log.debug(
                f"scan {scanNum} {rawfile.get_scan_event_string_for_scan_number(scanNum)} has no masses"
            )
            continue
        peak_datas = get_out(rawfile, scanNum)
        filterLine = rawfile.get_scan_event_string_for_scan_number(scanNum)
        retTime = rawfile.retention_time_from_scan_number(scanNum) * 60
        row = (scanNum, filterLine, peak_datas, retTime, file_path)
        MSrawscans.append(row)

    log.info("Scan Count is %d", len(MSrawscans))
    rawfile.dispose()

    return MSrawscans


def mergePeaksOnFilterline_withRandom(scans):
    log.info("Merging %d scans", len(scans))

    filterLines = set(
        [filterLine for scanNo, filterLine, peakData, retTime in scans]
    )
    log.info("found %d uniqe filterlines", len(filterLines))

    # create dict
    mergedPeaks = {}
    for filterLine in filterLines:
        mergedPeaks[filterLine] = ([], [], [])

    #     populate dict
    for scanNo, filterLine, peakData, retTime in scans:
        (zmass, abunds, resols, baseline, noise, charge) = peakData

        logging.warn("Altering content for testing:")
        logging.warn("masses with an odd M/Z will be have random resolution")

        for idx, val in enumerate(zmass):
            massInt = int(val)
            if massInt % 2 == 1:
                zmass[idx] = massInt + random.random()

        mergedPeaks[filterLine][0].extend(zmass)
        mergedPeaks[filterLine][1].extend(abunds)
        mergedPeaks[filterLine][2].extend(resols)

    return mergedPeaks


def setLogger(LOG_FILENAME="log.log"):

    # log to file
    logging.basicConfig(
        format="%(message)s",
        filename=LOG_FILENAME,
        filemode="w",
        level=logging.DEBUG,
    )
    # log to console
    logging.getLogger().addHandler(logging.StreamHandler())


def namespace(element):
    m = re.match("\{.*\}", element.tag)
    return m.group(0) if m else ""


def getMZXMLEncondedScans(filePath):
    #     TODO:handle different namespaces of mzxml
    namespaces = {
        "xmlns": "http://sashimi.sourceforge.net/schema_revision/mzXML_3.0"
    }
    ET.register_namespace(
        "", "http://sashimi.sourceforge.net/schema_revision/mzXML_3.0"
    )
    tree = ET.parse(filePath)

    scanElems = tree.findall(".//xmlns:scan", namespaces)

    rawscans = []
    for scan in scanElems:
        encodedPeaks = scan.find(".//xmlns:peaks", namespaces).text
        scanNo = int(scan.attrib["num"])
        filterLine = scan.attrib["filterLine"]
        retTime = scan.attrib["retentionTime"]
        object = (scanNo, filterLine, encodedPeaks, retTime)
        rawscans.append(object)

    return zip(*rawscans)


def write2templateMzXML(newfilename, scanPeaks):
    namespaces = {
        "xmlns": "http://sashimi.sourceforge.net/schema_revision/mzXML_3.0"
    }
    ET.register_namespace(
        "", "http://sashimi.sourceforge.net/schema_revision/mzXML_3.0"
    )
    scriptPath = os.path.dirname(os.path.realpath(__file__))
    tree = ET.parse(scriptPath + "//template.mzXML")

    msRunElement = tree.find(".//xmlns:msRun", namespaces)
    scanTemplete = msRunElement.find(".//xmlns:scan", namespaces)

    for idx, scan in enumerate(scanPeaks):
        (masses, intens) = scanPeaks[scan][:2]
        newScan = copy.deepcopy(scanTemplete)
        newScan.attrib["filterLine"] = scan
        newScan.attrib["peaksCount"] = str(len(masses))
        newScan.attrib["num"] = str(idx + 1)
        newScan.attrib["scanType"] = scan.split()[4]

        msLevel = 1 if " ms " in scan else 2
        if msLevel == 1:
            newScan.remove(newScan.find(".//xmlns:precursorMz", namespaces))
        else:
            precursorMz = newScan.find(".//xmlns:precursorMz", namespaces)
            match = re.match(r".* (.*)@(...)", scan, re.M | re.I)
            precursorMz.attrib["activationMethod"] = match.group(2)
            precursorMz.text = match.group(1)

        newScan.attrib["msLevel"] = str(msLevel)
        newScan.attrib["polarity"] = "-" if " - " in scan else "+"
        newScan.attrib["retentionTime"] = "PT{}S".format(0.0 + idx)

        encodedPeaks = encodePeaks(masses, intens)
        newScan.find(".//xmlns:peaks", namespaces).text = encodedPeaks
        msRunElement.append(newScan)

    msRunElement.remove(scanTemplete)

    tree.write(newfilename, encoding="ISO-8859-1", xml_declaration=True)


def reorderScans(filtered_bins, order=None):
    if order == None:
        return filtered_bins

    newfiltered_bins = OrderedDict()
    for idxOrder, selectionText in enumerate(order):
        for scan in filtered_bins:
            if selectionText in scan:
                newfiltered_bins[scan + ", " + str(idxOrder)] = filtered_bins[
                    scan
                ]

    return newfiltered_bins
