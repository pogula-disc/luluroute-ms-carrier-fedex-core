package com.luluroute.ms.carrier.fedex.controller;

import com.fedex.ursa.uvsdk.UVConstants;
import com.fedex.ursa.uvsdk.exceptions.UVRuntimeException;
import com.logistics.luluroute.carrier.fedex.TransitTimeDTO;
import com.luluroute.ms.carrier.config.SwaggerConfig;
import com.luluroute.ms.carrier.fedex.exception.MandatoryValuesMissingException;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.FuseLabelService;
import com.luluroute.ms.carrier.fedex.fuseapi.service.URSATransitTimeService;
import com.luluroute.ms.carrier.model.TransitTimeResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_ERROR;

/**
 * @author MANDALAKARTHIK1 The Class FedexTrackerController.
 */
@RestController
@RequestMapping("/v1/api/carrier/fedex")
@Api(value = "/v1/api/carrier/fedex", tags = {SwaggerConfig.ENTITY_SVC_TAG})
@Slf4j
public class FedexFuseController {

    @Autowired
    private URSATransitTimeService transitTimeServie;

    @Autowired
    FuseLabelService fuseLabelService;
    @PostMapping(value = "/transittime")
    public ResponseEntity<List<TransitTimeResponse>> getTransitTime(@RequestBody List<TransitTimeDTO> inputData) {
        String msg = "FedexController.getTransitTime()";
        try {
            List<TransitTimeResponse> estimatedDeliveryDates = transitTimeServie.getTransitTime(inputData);
            return new ResponseEntity<>(estimatedDeliveryDates, HttpStatus.OK);
        } catch (MandatoryValuesMissingException me) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(me));
            return new ResponseEntity<>(
                    Arrays.asList(TransitTimeResponse.builder()
                            .failureMessage("Mandatory data missing.. " + me.getMessage()).build()),
                    HttpStatus.BAD_REQUEST);
        } catch (ShipmentMessageException e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            return new ResponseEntity<>(
                    Arrays.asList(
                            TransitTimeResponse.builder().failureMessage(e.getCode() + " " + e.getDescription() + " " + e.getSource()).build()),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            return new ResponseEntity<>(
                    Arrays.asList(
                            TransitTimeResponse.builder().failureMessage(e.getMessage()).build()),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Diagnostic API to test Fuse API directly
     * @param namedFieldValuesInput
     * @return
     * @throws IOException
     * @throws UVRuntimeException
     */
    @PostMapping("/uvsdk")
    public Map<String, String> process( @RequestBody Map<String, String> namedFieldValuesInput) throws IOException, UVRuntimeException {
        LinkedHashMap<Long, String> in = new LinkedHashMap<>();
        namedFieldValuesInput.forEach(
                (key,value) -> {
                    long key1 = convertNameStringToURSAKey(key);
                    in.put(key1,value);
                }
        );
        log.info("Input transformed to URSA format : "+in);
        LinkedHashMap<Long, String> out;

        out = fuseLabelService.testURSASDK(in);

        log.info("Output from URSA in URSA format : "+in);
        Map<String,String> namedFieldValuesOutput = new HashMap<>();
        out.forEach(
                (key,value) -> namedFieldValuesOutput.put(getFieldName(key),value)
        );
        return namedFieldValuesOutput;
    }

    long convertNameStringToURSAKey(String strKey) {
        try {
            Integer value = (Integer) UVConstants.class.getField(strKey).get(null);
            long lv = Long.valueOf(value);
            return lv;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        }
    }

    String getFieldName(long longKey){
        return UVConstants.UVTaggedIOTagName.get(longKey);

    }
}
