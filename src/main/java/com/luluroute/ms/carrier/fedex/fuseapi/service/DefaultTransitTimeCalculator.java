package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.logistics.luluroute.carrier.fedex.TransitTimeDTO;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.carriermain.TransitModes;
import com.logistics.luluroute.rules.PlannedDeliveryDateRule;
import com.logistics.luluroute.util.DateUtil;
import com.luluroute.ms.carrier.fedex.exception.DefaultTransitTimeFailureException;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.model.EntityApplicableHoliday;
import com.luluroute.ms.carrier.model.EntityHoliday;
import com.luluroute.ms.carrier.repository.EntityAppHolidayRepository;
import com.luluroute.ms.carrier.repository.EntityHolidayRepository;
import com.luluroute.ms.carrier.service.RedisRehydrateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.luluroute.ms.carrier.fedex.util.Constants.FROM_DATE_FORMAT;
import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_FIELD_INFO;

/**
 * The Class DefaultTransitTimeCalculator.
 *
 * @author MANDALAKARTHIK1
 */

/** The Constant log. */
@Slf4j
@Service
public class DefaultTransitTimeCalculator {

	/** The app holiday repository. */
	@Autowired
	private EntityAppHolidayRepository appHolidayRepository;

	/** The holiday repository. */
	@Autowired
	private EntityHolidayRepository holidayRepository;

	/** The carrier code. */
	@Value("${config.carrier.code}")
	private String carrierCode;

	@Autowired
	private RedisRehydrateService redisRehydrateService;

	/**
	 * Calculate default transit days.
	 *
	 * @param inputData the input data
	 * @return the string
	 */
	public String calculateDefaultTransitDays(TransitTimeDTO inputData) {
		try {
			long dayMask = this.getDayCount(inputData.isSaturdayDelivery());
			long plannedDeliveryDate = PlannedDeliveryDateRule.calculatePlannedDateOfDelivery(
					inputData.getPlannedShipDate(), inputData.getTimeZone(), "0", "0", dayMask, dayMask, dayMask,
					inputData.getDefaultTransitDays(), inputData.getDefaultTransitDays(),
					this.getHolidaySet(inputData));
			return com.luluroute.ms.carrier.fedex.util.DateUtil.formatEpochToStringWithFormat(plannedDeliveryDate,
					inputData.getTimeZone(), Constants.PDD_DATE_FORMAT);
		} catch (Exception e) {
			throw new DefaultTransitTimeFailureException(
					"Failed to calculate default Transit time .. Root cause " + ExceptionUtils.getStackTrace(e), e);
		}
	}

	/**
	 * Gets the holiday set.
	 *
	 * @param inputData the input data
	 * @return the holiday set
	 */
	public List<String> getHolidaySet(TransitTimeDTO inputData) {
		CarrierMainPayload carrierProfile = redisRehydrateService.getCarrierByCode(carrierCode);
		SimpleDateFormat dateFormat = new SimpleDateFormat(FROM_DATE_FORMAT);
		List<EntityApplicableHoliday> applicableHolidayList = appHolidayRepository.findApplicableHolidays(
				carrierProfile.getCarrierId(), this.getCarrierModeCodeId(carrierProfile, inputData.getCarrierMode()),
				inputData.getDestinationCountry(), inputData.getDestinationState(), inputData.getDestinationCity());

		List<String> holidayIds = new ArrayList<>();
		for (EntityApplicableHoliday applicableHoliday : applicableHolidayList) {
			holidayIds.add(applicableHoliday.getHolidayId().toString());
		}
		log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime - holidayIds ", holidayIds.size()));
		LocalDate currentLocalDate = DateUtil.localDateForTimezone(inputData.getTimeZone());
		log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime - currentLocalDate", currentLocalDate));

		List<EntityHoliday> entityHolidays = holidayRepository.findHolidays(holidayIds, currentLocalDate,
				currentLocalDate.plusDays(15));

		List<String> entityHolidaySet = new ArrayList<>();
		if (!CollectionUtils.isEmpty(entityHolidays))
			entityHolidays.forEach(entityHoliday -> {
				entityHolidaySet.add(dateFormat.format(entityHoliday.getHolidayDate()));
			});
		return entityHolidaySet;
	}

	/**
	 * Gets the carrier mode code id.
	 *
	 * @param carrierPayload the carrier payload
	 * @param modeCode       the mode code
	 * @return the carrier mode code id
	 */
	private String getCarrierModeCodeId(CarrierMainPayload carrierPayload, String modeCode) {
		String carrierModeId = "";
		if (null != carrierPayload) {
			for (TransitModes mode : carrierPayload.getTransitModes()) {
				if (modeCode.equalsIgnoreCase(mode.getModeCode())) {
					carrierModeId = mode.getModeId();
					break;
				}
			}
		} else {
			throw new DefaultTransitTimeFailureException("Carrier payload not available in cache ");
		}
		return carrierModeId;
	}

	/**
	 * Gets the day count. --> 126 for all 6 days in a week excluding Sunday --> 62
	 * for weekdays
	 *
	 * @param isSaturdayDelivery the is saturday delivery
	 * @return the day count
	 */
	private long getDayCount(boolean isSaturdayDelivery) {
		return isSaturdayDelivery ? 126l : 62l;
	}

}
