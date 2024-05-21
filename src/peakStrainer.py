# encoding: utf-8
"""
Created on 29.03.2017
PeakStrainer is a tool to reduce the size of an MS spectra,
usage: peakStrainer spectra_file.raw
@author: mirandaa
"""
import sys
import os
import time
import logging
import csv
from utils.peakStrainer_util import write2templateMzXML
import itertools
import math
import collections

log = logging.getLogger(os.path.basename(__file__))


def main(file):
    log.setLevel(logging.INFO)
    if log.level == logging.DEBUG:
        log.addHandler(logging.StreamHandler())  # log to console
    if log.level == logging.DEBUG:
        logging.basicConfig(filename="peakStrainer.log", filemode="w")

    start = time.perf_counter()

    # log.debug('Start %f', start)
    #     if len(sys.argv) == 1:
    #         print("A filename must be provided")
    #         raise SystemExit

    filename = file  # " ".join(sys.argv[1:])
    scans = ThermoRawfile2Scans(filename, dropElbowIIT=True)
    if log.level == logging.DEBUG:
        ThermoRawfile2Scans_csv(scans)

    """ filtering by scan
    scans = filterScanBy_retentionTime(scans)
    scans = filterScanBy_filterline(scans)
    scans = filterScanBy_samples(scans, step_size = 3)
    """
    scans = removeLockFromHeader(scans)
    # Note: mergePeaksOnFilterline_withRandom to generate testing data
    filterLines = mergePeaksOnFilterline(scans)
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(filterLines, "mergePeaksOnFilterline.csv")

    preFiltered_filterlines = {
        k: preliminaryFilter(v) for k, v in list(filterLines.items())
    }
    # preFiltered_filterlines = {k: preliminaryReductionFilter(v) for k, v in filterLines.iteritems()}
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(preFiltered_filterlines, "preliminaryFilter.csv")

    """ create bins for peaks
    filterlines_withBins = {k: generateBins_decimalPlaces(v) for k, v in filterLines.iteritems()}
    filterlines_withBins = {k: generateBins_resolution(v) for k, v in filterLines.iteritems()}
    filterlines_withBins = {k: generateBins_theoreticalResolution(v) for k, v in preFiltered_filterlines.iteritems()}
    filterlines_withBins = {k: generateBins_resolutionPowerFunc(v,a,b) for k, v in preFiltered_filterlines.iteritems()} # 5408000.0, 2096000.0
    """
    filterlines_withBins = {
        k: generateBins_resolutionPowerFunc(v, 5408000.0, 0.5)
        for k, v in list(filterLines.items())
    }
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(filterlines_withBins, "generateBins.csv")

    #     filterlines_withBins_ms = {k: generateBins_resolutionPowerFunc(v, 5408000.0, 0.5 ) for k, v in filterLines.iteritems() if ' ms ' in k }
    #     filterlines_withBins_msms = {k: generateBins_resolutionPowerFunc(v, 5408000.0, 0.5 ) for k, v in filterLines.iteritems() if ' ms ' not in k}
    #     filterlines_withBins = filterlines_withBins_ms.update(filterlines_withBins_msms)
    #     for different function for ms and for msms

    filterlines_withBins = {
        k: alterBins_mergeOverlap(v)
        for k, v in list(filterlines_withBins.items())
    }
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(filterlines_withBins, "mergeBins.csv")

    """ put each mass in a bin
    filterlines_inBins = {k: sortMassIn_sortWindow(v) for k, v in filterlines_withBins.iteritems()}
    filterlines_inBins = {k: sortMassIn_FirstBin(v) for k, v in filterlines_withBins.iteritems()}
    filterlines_inBins = {k: sortMassIn_NarrowestBin(v) for k, v in filterlines_withBins.iteritems()}
    """
    filterlines_inBins = {
        k: sortMassIn_FirstBin(v)
        for k, v in list(filterlines_withBins.items())
    }
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(filterlines_inBins, "sortMassIn.csv")

    filterlines_binData = {
        k: aggregateBinData(v) for k, v in list(filterlines_inBins.items())
    }
    if log.level == logging.DEBUG:
        filterlineBins_csv(filterlines_binData, "aggregateBinData.csv")

    filterlines_filtered = {
        k: filterBins(v) for k, v in list(filterlines_binData.items())
    }
    if log.level == logging.DEBUG:
        filterlineBins_csv(filterlines_filtered, "filteredBins.csv")

    filtered_peaks = {
        k: bins2Peaks(v, filterlines_inBins[k])
        for k, v in list(filterlines_filtered.items())
    }
    if log.level == logging.DEBUG:
        filterlinePeaks_csv(filtered_peaks, "mzXMLdata.csv")

    # can't use filtered bins in mzxml because it is not readable by seems
    filtered_bins = {
        k: formatPeaks(v) for k, v in list(filterlines_filtered.items())
    }

    filtered_bins = reorder4lipidxplorer(filtered_bins)

    newfilename = filename[:-4] + ".mzXML"
    log.info("Writing results to %s", newfilename)
    write2templateMzXML(newfilename, filtered_bins)

    log.debug("finish %f", time.perf_counter() - start)


def isElbowIIT(rawfile, end, scanNum, filterLine):
    nextScan = min(scanNum + 1, end)
    nextNextScan = min(scanNum + 2, end)
    next_filterLine = rawfile.GetFilterForScanNum(nextScan)
    nextNext_filterLine = rawfile.GetFilterForScanNum(nextNextScan)
    if filterLine != next_filterLine or nextScan == end:  # there is no elbow
        return False

    ion_injection_time = rawfile.GetTrailerExtraForScanNum(scanNum)[
        "Ion Injection Time (ms)"
    ]
    next_ion_injection_time = rawfile.GetTrailerExtraForScanNum(nextScan)[
        "Ion Injection Time (ms)"
    ]

    if (
        next_filterLine != nextNext_filterLine
    ):  # can't average so use estimation
        return ion_injection_time > next_ion_injection_time + (
            math.sqrt(next_ion_injection_time) * 3
        )

    nextNext_ion_injection_time = rawfile.GetTrailerExtraForScanNum(
        nextNextScan
    )["Ion Injection Time (ms)"]
    average = (next_ion_injection_time + nextNext_ion_injection_time) / 2
    stdev = math.sqrt(
        (next_ion_injection_time - average) ** 2
        + (nextNext_ion_injection_time - average) ** 2
    )

    # TODO use pandas dataframe and getdo groupby and average to get these numbers
    return ion_injection_time > average + max(
        (stdev * 3), (math.sqrt(next_ion_injection_time) * 3)
    )  # max used because sometimes the deviation is too small


def ThermoRawfile2Scans(file_path, dropElbowIIT=True):
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

    log.info("General File Information:")
    log.info(f"   Number of instruments: {rawfile.instrument_count}")
    log.info(f"   Instrument model: {rawfile.get_instrument_data().model}")
    log.info(f"   Instrument name: {rawfile.get_instrument_data().name}")
    log.info(
        f"   Serial number: {rawfile.get_instrument_data().serial_number}"
    )
    log.info(
        f"   Software version: {rawfile.get_instrument_data().software_version}"
    )
    log.info(
        f"   Firmware version: {rawfile.get_instrument_data().hardware_version}"
    )
    log.info(f"   Units: {rawfile.get_instrument_data().units}")
    log.info(f"   Mass resolution: {rawfile.run_header_ex.mass_resolution}")
    log.info(f"   Number of scans: {rawfile.run_header_ex.spectra_count}")
    log.info(f"   Scan range: {first_scan_number} - {last_scan_number}")
    log.info(f"   Time range: {start_time} - {end_time}")
    log.info(
        f"   Mass range: {rawfile.run_header_ex.low_mass} - {rawfile.run_header_ex.high_mass}"
    )

    # Get the number of filters present in the RAW file
    number_filters = len(rawfile.get_filters())

    # Get the scan filter for the first and last spectrum in the RAW file
    first_filter = rawfile.get_filter_for_scan_number(first_scan_number)
    last_filter = rawfile.get_filter_for_scan_number(last_scan_number)

    log.info("Filter Information:")
    log.info(f"   Scan filter (first scan): {str(first_filter)}")
    log.info(f"   Scan filter (last scan): {str(last_filter)}")
    log.info(f"   Total number of filters:{number_filters}")
    log.info("")

    Labels = collections.namedtuple(
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
    # Note: to reshape into lists use
    # scanNum, filterLine, peak_datas, retTime = list(zip(*MSrawscans)
    # retTime, file_name   #Note: to get list of mass use
    # peak_data = peak_datas[0]
    # peak_data.mass

    return MSrawscans


def removeLockFromHeader(scans):
    log.info("removeLockFromHeader")
    newScans = []
    for scan in scans:
        scanList = list(scan)
        header = scanList[1]
        scanList[1] = header.replace("lock ", "")
        newScans.append(scanList)
    return newScans


def ThermoRawfile2Scans_csv(scans, filename="ThermoRawfile2Scans.csv"):
    with open(filename, "wb") as csv_file:
        writer = csv.writer(csv_file, delimiter=",")
        writer.writerow(("scanNum", "filterLine", "retTime", "row"))
        for scan in scans:
            (scanNum, filterLine, peak_datas, retTime) = scan
            for row in zip(*peak_datas):
                line = (scanNum, filterLine, retTime) + row
                writer.writerow(line)


"""
Filter on scans
"""


def filterScanBy_retentionTime(
    scans, lowSeconds=0.5, highSeconds=float("inf")
):
    log.info("filterScanBy_retentionTime %f to %f", lowSeconds, highSeconds)
    result = []
    for row in scans:
        scanNum, filterLine, peak_datas, retTime = row
        if retTime > +lowSeconds and retTime <= highSeconds:
            result.append(row)
    return result


def filterScanBy_filterline(scans, subtext=" ms ", keep=False):
    log.info("filterScanBy_filterline %s filter keep: %s", subtext, keep)
    result = []
    for row in scans:
        scanNum, filterLine, peak_datas, retTime = row
        if keep:
            if subtext in filterLine:
                result.append(row)
        else:  # not keep
            if subtext not in filterLine:
                result.append(row)
    return result


def filterScanBy_samples(scans, step_size=10):
    log.info("filterScanBy_samples step_size %d", step_size)
    result = []
    for idx, row in enumerate(scans):
        if idx % step_size == 0:
            result.append(row)
    return result


"""
merge peaks and coarse grained filtering 
"""


def mergePeaksOnFilterline(scans):
    log.info("Merging %d scans", len(scans))

    filterLines = set(
        [
            filterLine
            for scanNo, filterLine, peakData, retTime, file_name in scans
        ]
    )
    log.info("found %d uniqe filterlines", len(filterLines))

    # create dict
    mergedPeaks = {}
    for filterLine in filterLines:
        mergedPeaks[filterLine] = ([], [], [], [], [], [], [])

    #     populate dict
    for scanNo, filterLine, peakData, retTime, file_name in scans:
        (zmass, abunds, resols, baseline, noise, charge) = peakData
        mergedPeaks[filterLine][0].extend(zmass)
        mergedPeaks[filterLine][1].extend(abunds)
        mergedPeaks[filterLine][2].extend(resols)
        mergedPeaks[filterLine][3].extend(baseline)
        mergedPeaks[filterLine][4].extend(noise)
        mergedPeaks[filterLine][5].extend(charge)
        mergedPeaks[filterLine][6].extend([file_name] * len(zmass))

    return mergedPeaks


def filterlinePeaks_csv(mergedPeaks, filename="filterlinePeaks.csv"):
    with open(filename, "wb") as csv_file:
        writer = csv.writer(csv_file, delimiter=",")
        writer.writerow(("Scan", "Row data"))
        for scan in mergedPeaks:
            peak_data = mergedPeaks[scan]
            for row in zip(*peak_data):
                line = (scan,) + row
                writer.writerow(line)


def preliminaryFilter(
    peakData, decimal_places=2, minCount=2, minRepetitionRate=0.0
):
    log.debug(
        "preliminaryFilter rounding to decimal_places %d, minCount = %d, minRepetitionRate %f",
        decimal_places,
        minCount,
        minRepetitionRate,
    )
    masses = peakData[0]
    log.debug("initial peak count %d", len(masses))

    halfdec = (10 ** -decimal_places) / 2

    # bins plus round up and round down to catch the edge cases of close to top or close to bottom of bin
    # eg  123.104 round = 123.10 up=123.11 down= 123.10
    #     123.106 round = 123.11 up=123.11 down= 123.10
    #     by counting the bins from round up and down we get a bin count
    #     123.10 : 3 and 123.11 : 3
    # for peaks with no adjacent bins the peak count is 2

    rounddown = [round(mass - halfdec, decimal_places) for mass in masses]
    roundeds = [round(mass, decimal_places) for mass in masses]
    roundup = [round(mass + halfdec, decimal_places) for mass in masses]

    bins = dict()
    for rounded in roundeds:
        bins[rounded] = bins.get(rounded, 0) + 1

    for rounded in roundup:
        bins[rounded] = bins.get(rounded, 0) + 1

    for rounded in rounddown:
        bins[rounded] = bins.get(rounded, 0) + 1

    log.debug("Prelimianry groups count %d", len(bins))
    minCount_bin = {
        bin: (count / 3) + 1
        for bin, count in list(bins.items())
        if (count / 3) + 1 >= minCount
    }
    log.debug("groups with minCount %d: %d", minCount, len(minCount_bin))
    minCount_bin_max = (
        float(max(minCount_bin.values())) if len(minCount_bin) != 0 else 0
    )
    minRepetitionRate_bin = {
        bin: count
        for bin, count in list(minCount_bin.items())
        if (count / minCount_bin_max) > minRepetitionRate
    }
    log.debug(
        "groups with minRepetitionRate %f: %d",
        minRepetitionRate,
        len(minRepetitionRate_bin),
    )

    preFilterPass = [
        rounded in list(minRepetitionRate_bin.keys()) for rounded in roundeds
    ]
    compressedPeakData = list(
        itertools.compress(list(zip(*peakData)), preFilterPass)
    )
    compressedPeakData = list(zip(*compressedPeakData))
    log.debug("final peak count %d", preFilterPass.count(True))
    return compressedPeakData


def preliminaryReductionFilter(peakData, reduction=0.10, minPeaks=10):
    peak_count = len(peakData[0])
    goal = int(peak_count * reduction)
    log.debug(
        "preliminaryReductionFilter will reduce peak count to max %d and min %d",
        goal,
        minPeaks,
    )

    decimal_places = 2
    minCount = 2
    minRepetitionRate = 0.5
    toggle = False
    while peak_count > goal:
        if toggle:
            decimal_places += 1
        else:
            minRepetitionRate += 0.1
        peakData = preliminaryFilter(
            peakData, decimal_places, minCount, minRepetitionRate
        )
        peak_count = len(peakData[0])

    return peakData


"""
generate bins
"""


def generateBins_decimalPlaces(peakData, decimal_places=4):
    log.debug("generateBins_decimalPlaces, decimal_places %d", decimal_places)
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    log.debug("generateBins_decimalPlaces, peak count %d", len(masses))

    halfWidth = (10 ** -decimal_places) / 2
    bins_low = [round(mass - halfWidth, decimal_places) for mass in masses]
    bins_high = [round(mass + halfWidth, decimal_places) for mass in masses]

    peakData = (peakData[0], peakData[1], peakData[2], bins_low, bins_high)
    return peakData


def generateBins_resolution(peakData, decimal_places=4):
    log.debug("generateBins_resolution, decimal_places %d", decimal_places)
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    log.debug("generateBins_resolution, peak count %d", len(masses))

    bins_high = [
        mass + ((mass / resol) / 2) for mass, resol in zip(masses, resols)
    ]
    bins_low = [
        mass - ((mass / resol) / 2) for mass, resol in zip(masses, resols)
    ]
    # rounding to make bins larger
    halfWidth = (10 ** -decimal_places) / 2
    bins_low = [round(mass - halfWidth, decimal_places) for mass in bins_low]
    bins_high = [round(mass + halfWidth, decimal_places) for mass in bins_high]

    peakData = (peakData[0], peakData[1], peakData[2], bins_low, bins_high)
    return peakData


def peakWidth_at_hight(abunds, highth=0.95):
    """ Note:
    for future implementation
    assuming that the peak has a gausian curve,
    and that the resols is full width at half maximum, fwhm
    and given the equation in wikipedia for fwhm
    
    the formula for the full width at an abritary height between [0,1]
    
    is width = 2 * sqrt(abund**2 * log(hight) / (-4*log(2))) 
    
    """

    def widthAtHight(fwhm, normHight):
        return 2 * math.sqrt(
            fwhm ** 2 * math.log(normHight) / (-4 * math.log(2))
        )

    return [widthAtHight(abund, highth) for abund in abunds]


def generateBins_theoreticalResolution(peakData, decimal_places=4):
    log.debug(
        "generateBins_theoreticalResolution, decimal_places %d", decimal_places
    )
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    log.debug("generateBins_theoreticalResolution, peak count %d", len(masses))

    log.info("curve fitting to generate theoreticalResolution")
    from scipy.optimize import curve_fit

    def func(x, a, b):
        return a * (x ** -b)

    sort_mass = sorted(zip(abunds, masses, resols))  # filter on intensity
    # 5408000.0, 2096000.0
    select_abunds, select_masses, select_resols = zip(*sort_mass)
    log.debug("Selecting only 90% highest intensity, as per Kai S.")
    side_len = len(select_masses) / 10
    popt, pconv = curve_fit(
        func, select_masses[side_len:], select_resols[side_len:]
    )

    log.info("function a*(x**-b) uses values: mass, %f, %f ", popt[0], popt[1])

    theoResols = [func(mass, *popt) for mass in masses]

    bins_high = [
        mass + ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    bins_low = [
        mass - ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    # rounding to make bins larger
    halfWidth = (10 ** -decimal_places) / 2
    bins_low = [round(mass - halfWidth, decimal_places) for mass in bins_low]
    bins_high = [round(mass + halfWidth, decimal_places) for mass in bins_high]

    peakData = (peakData[0], peakData[1], peakData[2], bins_low, bins_high)
    return peakData


def generateBins_resolutionPowerFunc(
    peakData, a=1.0, minus_b=1.0, decimal_places=4
):
    log.debug(
        "generateBins_resolutionPowerFunc, mass +- res_func, res_func = a*(mass**-b), a= %f, b = %f",
        a,
        minus_b,
    )
    masses = peakData[0]
    log.debug("generateBins_resolutionPowerFunc, peak count %d", len(masses))

    def func(x, a, b):
        return a * (x ** -b)

    popt = (a, minus_b)

    log.info("function a*(x**-b) uses values: mass, %f, %f ", popt[0], popt[1])

    theoResols = [func(mass, *popt) for mass in masses]

    bins_high = [
        mass + ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    bins_low = [
        mass - ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    # rounding to make bins larger
    halfWidth = (10 ** -decimal_places) / 2
    bins_low = [round(mass - halfWidth, decimal_places) for mass in bins_low]
    bins_high = [round(mass + halfWidth, decimal_places) for mass in bins_high]

    peakData = (peakData[0], peakData[1], peakData[2], bins_low, bins_high)
    return peakData


def generateBins_theoreticalIntensity(peakData, decimal_places=4):
    log.debug(
        "generateBins_theoreticalIntensity, decimal_places %d", decimal_places
    )
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    log.debug("generateBins_theoreticalResolution, peak count %d", len(masses))

    log.info("curve fitting to generate theoreticalResolution")
    from scipy.optimize import curve_fit

    def func(x, a, b):
        return a * (x ** -b)

    sort_mass = sorted(zip(masses, abunds))

    select_masses, select_resols = zip(*sort_mass)
    log.debug("Selecting only middle of distribution, as per Kai S.")
    side_len = len(select_masses) / 15
    popt, pconv = curve_fit(
        func,
        select_masses[side_len:-side_len],
        select_resols[side_len:-side_len],
    )

    log.info("function a*(x**-b) uses values: mass, %f, %f ", popt[0], popt[1])

    theoResols = [func(mass, *popt) for mass in masses]

    bins_high = [
        mass + ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    bins_low = [
        mass - ((mass / resol) / 2) for mass, resol in zip(masses, theoResols)
    ]
    # rounding to make bins larger
    halfWidth = (10 ** -decimal_places) / 2
    bins_low = [round(mass - halfWidth, decimal_places) for mass in bins_low]
    bins_high = [round(mass + halfWidth, decimal_places) for mass in bins_high]

    peakData = (peakData[0], peakData[1], peakData[2], bins_low, bins_high)
    return peakData


def alterBins_mergeOverlap(peakData):
    log.debug("alterBins_mergeOverlap")

    sortedPeakData = list(zip(*sorted(zip(*peakData))))
    masses = list(sortedPeakData[0])
    abunds = list(sortedPeakData[1])
    resols = list(sortedPeakData[2])
    bins_low = list(sortedPeakData[3])
    bins_high = list(sortedPeakData[4])

    while True:  # bubble check
        changeCount = 0
        for idx, _ in enumerate(bins_high):
            if idx >= len(bins_high) - 1:
                continue
            low = bins_low[idx]
            high = bins_high[idx]
            nextLow = bins_low[idx + 1]
            nexthigh = bins_high[idx + 1]

            if low == nextLow and high == nexthigh:
                continue  # already updated

            if high > nextLow:
                newHigh = max(nexthigh, high)
                newLow = min(nextLow, low)

                bins_high[idx] = newHigh
                bins_high[idx + 1] = newHigh
                bins_low[idx] = newLow
                bins_low[idx + 1] = newLow

                changeCount += 1
        log.debug("merged %d pairs of bins", changeCount)
        if changeCount == 0:
            break

    peakData = (masses, abunds, resols, bins_low, bins_high)
    return peakData


"""
sort each mass in a bin
"""


def sortMassIn_FirstBin(peakData):
    log.debug(
        "sortMassIn_FirstBin, no sorting , just the first bins that matches"
    )
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    bins_low = peakData[3]
    bins_high = peakData[4]

    uniqBins = set(zip(bins_low, bins_high))
    log.debug("Mass count %d, available bins %d", len(masses), len(uniqBins))
    if len(masses) * len(uniqBins) > 1000000:
        log.debug(
            "this operation may take a long time, use sortMassIn_sortWindow instead"
        )
        return sortMassIn_sortWindow(peakData, int(len(uniqBins) / 100))
    bins = []
    for mass in masses:
        inbin = False
        for bin_low, bin_high in uniqBins:
            if mass >= bin_low and mass <= bin_high:
                bins.append((bin_low, bin_high))
                inbin = True
                break
        if not inbin:
            print("error")

    if len(bins) != len(masses):
        log.error(
            "There was a problem setting masses in bins, please verify bins where created correctly"
        )
        raise RuntimeError(
            "There was a problem setting masses in bins, please verify bins where created correctly"
        )

    peakData += (bins,)
    return peakData


def sortPeaksByBinWidth(peakData):
    bin_width = [
        bin_high - bin_low
        for bin_low, bin_high in zip(peakData[3], peakData[4])
    ]
    sorted_peakdata_plus = zip(*sorted(zip(bin_width, *peakData)))
    sorted_peakdata = sorted_peakdata_plus[1:]
    return sorted_peakdata


def sortMassIn_NarrowestBin(peakData):
    log.debug("sortMassIn_NarrowestBin")
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    bins_low = peakData[3]
    bins_high = peakData[4]

    uniqBins = set(zip(bins_low, bins_high))
    log.debug("Mass count %d, available bins %d", len(masses), len(uniqBins))
    if len(masses) * len(uniqBins) > 1000000:
        log.debug(
            "this operation may take a long time, use sortMassIn_sortNarrowWindow instead"
        )
        return sortMassIn_sortNarrowWindow(peakData, len(uniqBins) / 100)

    sorted_peakdata = sortPeaksByBinWidth(peakData)

    masses = sorted_peakdata[0]
    abunds = sorted_peakdata[1]
    resols = sorted_peakdata[2]
    bins_low = sorted_peakdata[3]
    bins_high = sorted_peakdata[4]

    bins = []
    for mass in masses:
        for bin_low, bin_high in uniqBins:
            if mass >= bin_low and mass <= bin_high:
                bins.append((bin_low, bin_high))
                break

    if len(bins) != len(masses):
        log.error(
            "There was a problem setting masses in bins, please verify bins where created correctly"
        )

    peakData = (
        peakData[0],
        peakData[1],
        peakData[2],
        peakData[3],
        peakData[4],
        bins,
    )
    return peakData


def sortMassIn_sortWindow(peakData, window=200):
    if window > 200:
        window = 200
    log.debug("sortMassIn_sortWindow, window size %d", window)

    peakData_sorted = list(zip(*sorted(zip(*peakData))))

    masses = peakData_sorted[0]
    abunds = peakData_sorted[1]
    resols = peakData_sorted[2]
    bins_low = peakData_sorted[-2]
    bins_high = peakData_sorted[-1]

    tryAgain = True

    while tryAgain:
        tryAgain = False
        bins = []
        for idx, mass in enumerate(masses):
            found = False
            lowidx = 0 if idx < (window / 2) else idx - (window / 2)
            lowidx = int(lowidx)
            highidx = window + lowidx
            if idx % window == 0:
                log.debug("at %d of %d", idx, len(masses))
            for bin_low, bin_high in zip(
                bins_low[lowidx:highidx], bins_high[lowidx:highidx]
            ):
                if mass >= bin_low and mass <= bin_high:
                    found = True
                    bins.append((bin_low, bin_high))
                    break
            if not found:
                log.error(
                    "the mass at index %d did not find a bin, window size %d",
                    idx,
                    window,
                )
                window = window * 2
                log.error("change window size to %d", window)
                tryAgain = True
                break

    if len(bins) != len(masses):
        log.error(
            "There was a problem setting masses in bins, please verify bins where created correctly, and window size"
        )

    peakData += (bins,)
    return peakData


def sortMassIn_sortNarrowWindow(peakData, window=200):
    log.debug("sortMassIn_sortNarrowWindow, window size %d", window)

    peakData_sorted = zip(*sorted(zip(*peakData)))

    masses = peakData_sorted[0]
    abunds = peakData_sorted[1]
    resols = peakData_sorted[2]
    bins_low = peakData_sorted[3]
    bins_high = peakData_sorted[4]

    bins = []
    for idx, mass in enumerate(masses):
        found = False
        lowidx = 0 if idx < (window / 2) else idx - (window / 2)
        highidx = window + lowidx

        # --
        bin_width = [
            bin_high - bin_low
            for bin_low, bin_high in zip(
                peakData_sorted[3][lowidx:highidx],
                peakData_sorted[4][lowidx:highidx],
            )
        ]
        sorted_peakdata_plus = zip(
            *sorted(
                zip(
                    bin_width,
                    peakData_sorted[0][lowidx:highidx],
                    peakData_sorted[1][lowidx:highidx],
                    peakData_sorted[2][lowidx:highidx],
                    peakData_sorted[3][lowidx:highidx],
                    peakData_sorted[4][lowidx:highidx],
                )
            )
        )
        sorted_peakdata = sorted_peakdata_plus[1:]
        # --

        if idx % window == 0:
            log.debug("at %d of %d", idx, len(masses))
        for bin_low, bin_high in zip(sorted_peakdata[3], sorted_peakdata[4]):
            if mass >= bin_low and mass <= bin_high:
                found = True
                bins.append((bin_low, bin_high))
                break
        if not found:
            log.error("the mass at index %d did not find a bin", idx)

    if len(bins) != len(masses):
        log.error(
            "There was a problem setting masses in bins, please verify bins where created correctly"
        )

    peakData = (
        peakData_sorted[0],
        peakData_sorted[1],
        peakData_sorted[2],
        peakData_sorted[3],
        peakData[4],
        bins,
    )
    return peakData


"""
aggregate bins
"""


def aggregateBinData(peakData):
    log.debug(
        "aggregateBinData count %d, count, sumZmass, sumAbund", len(peakData)
    )
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    bins_low = peakData[3]
    bins_high = peakData[4]
    bins = peakData[5]
    binsData = dict()

    newRow = 0, 0.0, 0.0  #  count, sumZmass, sumAbund
    for mass, abund, resol, bin_low, bin_high, bin in zip(*peakData):
        count, sumZmass, sumAbund = binsData.get(bin, newRow)
        count += 1
        sumZmass += mass
        sumAbund += abund
        binsData[bin] = count, sumZmass, sumAbund

    log.debug("bin count %d ", len(binsData))
    return binsData


def filterlineBins_csv(filterlineBins, filename="filterlineBins.csv"):
    with open(filename, "wb") as csv_file:
        writer = csv.writer(csv_file, delimiter=",")
        writer.writerow(("filterline", "binLow", "binH", "row data"))
        for filterline in filterlineBins:
            binsdata = filterlineBins[filterline]
            for bin in binsdata:
                row = binsdata[bin]
                line = (filterline,) + bin + row
                writer.writerow(line)


def getMaxCount(binData, disregardBottom=0.10):
    maxCount = 0
    maxAbunds = 0
    for bin in binData:
        count, sumZmass, sumAbund = binData[bin]
        if maxAbunds < (sumAbund / count):
            maxAbunds = sumAbund / count

    abundsCutoff = maxAbunds * disregardBottom
    log.debug(
        "bins with average intensity lower than %f are not evaluated to establish min repetition rate",
        abundsCutoff,
    )
    for bin in binData:
        count, sumZmass, sumAbund = binData[bin]
        if (sumAbund / count) < abundsCutoff:
            continue  # dont consider the
        if maxCount < count:
            maxCount = count

    log.debug(
        "Max peak count is %d, for bins with average abundance above %f ",
        maxCount,
        abundsCutoff,
    )
    return maxCount


def filterBins(
    binData, minRepetitionRate=0.70, decimal_places=4, disregardBottom=0.10
):
    decimal = 10 ** -decimal_places
    log.debug(
        "filterBins count %d ,minRepetitionRate %f, decimal places %d, %f, desrigarding bottom %f percent",
        len(binData),
        minRepetitionRate,
        decimal_places,
        decimal,
        disregardBottom,
    )
    if disregardBottom >= 1:
        raise RuntimeError("disregardBottom can not be more than 1")
    maxCount = getMaxCount(binData, disregardBottom)

    bintable = [(k + binData[k]) for k in binData]
    sortedBintable = sorted(bintable)
    # TODO this code is fishy , refactor
    binData_filtered = {}
    #     for bin in binData:
    for idx, row in enumerate(sortedBintable):
        bin = row[0:2]
        (count, sumZmass, sumAbund) = row[2:]
        if count >= (maxCount * minRepetitionRate):
            binData_filtered[bin] = (count, sumZmass, sumAbund)

        if idx + 1 >= len(sortedBintable):
            continue

        nextrow = sortedBintable[idx + 1]
        # test edge cases
        if (
            abs(nextrow[0] - row[1]) < decimal
        ):  # row highbin is adjecent nextrow lowbin
            if row[2] + nextrow[2] >= (maxCount * minRepetitionRate):
                binData_filtered[bin] = (count, sumZmass, sumAbund)
                binData_filtered[nextrow[0:2]] = nextrow[2:]

    log.debug("Filter rate %f", len(binData_filtered) / len(binData))

    return binData_filtered


def formatPeaks(binData):
    log.debug("formatPeaks into simple list pair, mass and intentisty")
    masses = []
    intens = []
    for bin in binData:
        (count, sumZmass, sumAbund) = binData[bin]
        masses.append(sumZmass / count)
        intens.append(sumAbund / count)

    return (masses, intens)


def bins2Peaks(binsData, peakData):
    log.debug(
        "bins2Peaks get all the peaks with given bin, into simple list pair, mass and intentisty"
    )
    bins_filtered = binsData.keys()
    masses = peakData[0]
    abunds = peakData[1]
    resols = peakData[2]
    bins_low = peakData[3]
    bins_high = peakData[4]
    bins = peakData[5]

    filteredRowdata = [
        (row[0], row[1]) for row in zip(*peakData) if row[5] in bins_filtered
    ]
    result = zip(*filteredRowdata)
    return result


def reorder4lipidxplorer(filtered_bins):

    log.debug("reorder4lipidxplorer so it goes ms+, msms+,ms-, msms-")
    filtered_bins1 = collections.OrderedDict(
        sorted(filtered_bins.items(), key=lambda t: t[0])
    )
    return filtered_bins1


if __name__ == "__main__":
    if len(sys.argv) == 1:
        print("A filename must be provided")
        raise SystemExit

    filename = " ".join(sys.argv[1:])

    for file in sys.argv[1:]:
        main(file)
