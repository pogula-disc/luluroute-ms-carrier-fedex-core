package com.luluroute.ms.carrier.fedex.util;

import static com.luluroute.ms.carrier.fedex.util.Constants.PDD_DATE_FORMAT;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.luluroute.ms.carrier.fedex.exception.DefaultTransitTimeFailureException;
import com.luluroute.ms.carrier.fedex.exception.MappingFormatException;

public class DateUtil {

	public static long getCurrentTime() {
		return Instant.now().toEpochMilli();
	}

	public static long currentDateTimeInLong() {
		return new Date().getTime();
	}

	public static long utcCurrentDatetime() {
		return Instant.now().getEpochSecond();
	}

	public static String formatEpochToStringWithFormat(long plannedShipDate, String timezone, String dateFormat) {
		Instant instant = Instant.ofEpochSecond(plannedShipDate);
		ZoneId zoneId = ZoneId.of(timezone);
		ZonedDateTime zdt = instant.atZone(zoneId);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
		return zdt.format(formatter);
	}

	public static long formatStringWithFormatToEpoch(String plannedDeliveryDate, String timezone, String dateFormat) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
		try {
			return simpleDateFormat.parse(plannedDeliveryDate).toInstant().atZone(ZoneId.of(timezone)).toEpochSecond();
		} catch (ParseException e) {
			e.printStackTrace();
			return -1; // Return a default value or handle the error appropriately
		}
	}

	public static String convertLongDateToString(long value, String format) throws MappingFormatException {
		String returnVal = null;
		try {
			returnVal = new SimpleDateFormat(format).format(new Date((value) * 1000));
		} catch (Exception e) {
			throw new MappingFormatException(
					String.format(ExceptionConstants.PARSER_ERROR_FORMAT, value, ExceptionUtils.getStackTrace(e)));
		}
		return returnVal;
	}

	public static String formatEpochToStringWithShortFormat(long plannedShipDate, String timezone, String dateFormat) {
		Instant instant = Instant.ofEpochSecond(plannedShipDate);
		ZoneId zoneId = ZoneId.of(timezone, ZoneId.SHORT_IDS);
		ZonedDateTime zdt = instant.atZone(zoneId);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
		return zdt.format(formatter);
	}

	public static boolean checkDateOfWeekForEPOCDate(long date, String timezone) {
		Instant instant = Instant.ofEpochSecond(date);
		ZoneId zoneId = ZoneId.of(timezone);
		ZonedDateTime zdt = instant.atZone(zoneId);
		return zdt.getDayOfWeek() == DayOfWeek.SATURDAY;
	}

	public static String compareTwoDates(String date1, String date2) {
		String deliverDate = "";
		SimpleDateFormat formatter = new SimpleDateFormat(PDD_DATE_FORMAT);
		try {
			Date firstDate = formatter.parse(date1);
			Date secondDate = formatter.parse(date2);
			deliverDate = firstDate.after(secondDate) ? date2 : date1;
		} catch (ParseException e) {
			throw new DefaultTransitTimeFailureException(ExceptionUtils.getMessage(e), e);
		}
		return deliverDate;
	}

}
