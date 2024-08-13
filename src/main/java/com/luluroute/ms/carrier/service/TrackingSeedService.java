package com.luluroute.ms.carrier.service;

import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.service.helper.TrackingSeedHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingSeedService {

    private static long[] weights = {1, 3, 7};

    @Autowired
    private TrackingSeedHelper trackingSeedHelper;


    public Long getActiveTrackingseed(String meterNo) throws ShipmentMessageException {
        String msg = "TrackingSeedService.getActiveTrackingseed()";
        try {
            log.debug(String.format(Constants.STANDARD_FIELD_INFO, "meterNo", meterNo));
            return trackingSeedHelper.getNextTrackingNumber(meterNo);

        } catch (Exception e) {
            log.error(Constants.STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    public String getNextTrackingNumber(String meterNo) throws ShipmentMessageException {
        Long nextTrackingNumber = getActiveTrackingseed(meterNo);
        trackingSeedHelper.asyncUpdateTrackingSeed(meterNo,nextTrackingNumber);
        log.debug(String.format(Constants.STANDARD_FIELD_INFO, "Tracking Number", nextTrackingNumber));
        return fedExEmtPsPlusTrackIdAndCheckDigit(String.valueOf(nextTrackingNumber));

    }

    private static String fedExEmtPsPlusTrackIdAndCheckDigit(String trackingNumber) {

        // get a list of all digits in reverse order
        int numArray[] = StringUtils.reverse(trackingNumber).chars().map(c -> c - '0').toArray();

        long checkDigitSum = 0;
        for (int index = 0; index < numArray.length; index++) {
            checkDigitSum += numArray[index] * weights[index % 3];
        }
        var remainder = checkDigitSum % 11;
        if (remainder == 10)
            remainder = 0;

        return String.format("%s%s", trackingNumber, remainder);
    }
}
