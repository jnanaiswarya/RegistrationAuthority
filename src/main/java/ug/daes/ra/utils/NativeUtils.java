/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.utils;

import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ug.daes.DAESService;
import ug.daes.Result;
import ug.daes.ra.dto.LogModelDTO;
import ug.daes.ra.exception.RAServiceException;
import ug.daes.ra.request.entity.LogModel;

/**
 * The Class NativeUtils.
 */
public class NativeUtils {

	/** The Constant CLASS. */
	final static private String CLASS = "NativeUtils";

	/** The Constant logger. */
	final static private Logger logger = LoggerFactory.getLogger(NativeUtils.class);

	/** The uuid. */
	static UUID uuid;

	/** The upper alphabet. */
	static String upperAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/** The lower alphabet. */
	static String lowerAlphabet = "abcdefghijklmnopqrstuvwxyz";

	/** The numbers. */
	static String numbers = "0123456789";

	static RestTemplate restTemplate = new RestTemplate();

	/**
	 * Generate PKI key id.
	 *
	 * @return the string
	 */
	public static String generatePKIKeyId() {
		String alphaNumeric = upperAlphabet;
		return genRandomNumber(alphaNumeric);
	}

	/**
	 * Gen random number.
	 *
	 * @param alphaNumeric
	 *            the alpha numeric
	 * @return the string
	 */
	private static String genRandomNumber(String alphaNumeric) {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();

		// specify length of random string
		int length = 10;

		for (int i = 0; i < length; i++) {

			// generate random index number
			int index = random.nextInt(alphaNumeric.length());

			// get character specified by index
			// from the string
			char randomChar = alphaNumeric.charAt(index);

			// append the character to string builder
			sb.append(randomChar);
		}
		return sb.toString();
	}

	/**
	 * Gets the UU id.
	 *
	 * @return the UU id
	 */
	public static String getUUId() {
		uuid = UUID.randomUUID();
		return uuid.toString();
	}

	/**
	 * Validate request body.
	 *
	 * @param requestBody
	 *            the request body
	 * @param hashdata
	 *            the hashdata
	 * @return true, if successful
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
	public static boolean validateRequestBody(String requestBody, int hashdata) throws NoSuchAlgorithmException {
		if (hashdata == requestBody.hashCode())
			return true;
		else
			return false;
	}

	/**
	 * Gets the time stamp.
	 *
	 * @return the time stamp
	 * @throws ParseException
	 *             the parse exception
	 */
	public static Date getTimeStamp() throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = format.parse(new Timestamp(System.currentTimeMillis()).toString());
		return date;
	}

	/**
	 * Gets the time stamp.
	 *
	 * @param date1
	 *            the date 1
	 * @return the time stamp
	 * @throws ParseException
	 *             the parse exception
	 */
	@SuppressWarnings("deprecation")
	public static Date getTimeStamp(String date) throws ParseException {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date certDates = f.parse(date);
		return certDates;
	}

	/**
	 * Gets the time stamp string.
	 *
	 * @return the time stamp string
	 * @throws ParseException
	 *             the parse exception
	 */
	public static String getTimeStampString() throws ParseException {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		System.out.println(f.format(new Date()));
		return f.format(new Date());
	}

	public static void checkOldPasswords(String pin, String setPin, String message) throws RAServiceException {
		String[] pins = pin.split(", ");
		List<String> pinList = Arrays.asList(pins);
		Iterator<String> pinListIterator = pinList.iterator();
		while (pinListIterator.hasNext()) {
			String oldPin = pinListIterator.next();
			if (oldPin != null) {
				if (oldPin.equals(setPin))
					throw new RAServiceException(message);
			}
		}
	}

	public static void checkCurrentPassword(String oldPins, String setPin, String message) throws RAServiceException {
		String[] pins = oldPins.split(", ");
		List<String> pinList = Arrays.asList(pins);
		String lastPin = pinList.get(pinList.size() - 1);
		if (setPin.equals(lastPin)) {
			throw new RAServiceException(message);
		}
	}

	public static void checkOldCurrentPasswords(String oldPins, String setPin, String message)
			throws RAServiceException {
		String[] pins = oldPins.split(", ");
		List<String> pinList = Arrays.asList(pins);
		String lastPin = pinList.get(pinList.size() - 1);
		if (!setPin.equals(lastPin)) {
			throw new RAServiceException(message);
		}
	}

	public static String getTotalTime(String t1, String t2) throws ParseException {

		SimpleDateFormat smpdate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date time1 = smpdate.parse(t1);

		System.out.println("time1 ::" + time1);

		SimpleDateFormat smpdate2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date time2 = smpdate2.parse(t2);

		System.out.println("time2 ::" + time2);

		long difference_In_Time = time2.getTime() - time1.getTime();
		System.out.println("difference_In_Time milisec :: " + difference_In_Time);
		long difference_In_Seconds = (difference_In_Time / 1000) % 60;
		System.out.println("difference_In_Seconds ::" + difference_In_Seconds);

		String strLong = Long.toString(difference_In_Seconds);

		return strLong;

	}

	// public static String getBase64String(String uri) {
	// try {
	// System.out.println("insideNativeUtils");
	// System.out.println("url ::" + uri);
	//
	// HttpHeaders headersForGet = new HttpHeaders();
	// HttpEntity<Object> requestEntityForGet = new HttpEntity<>(headersForGet);
	// ResponseEntity<Resource> downloadUrlResult = restTemplate.exchange(uri,
	// HttpMethod.GET,requestEntityForGet,Resource.class);
	// System.out.println("SelfiUrlResponse ::" +
	// downloadUrlResult.getBody().getInputStream());
	// byte[] buffer =
	// IOUtils.toByteArray(downloadUrlResult.getBody().getInputStream());
	// String image2 = new String(Base64.getEncoder().encode(buffer));
	// return image2;
	// } catch (Exception e) {
	// e.printStackTrace();
	// return e.getMessage();
	// }
	// }

	public static LogModel getLogModel(LogModelDTO logModelDTO) throws Exception {
		System.out.println("LogModel "+ logModelDTO);
		LogModel logModel = new LogModel();
		logModel.setIdentifier(logModelDTO.getIdentifier());
		logModel.setCorrelationID(logModelDTO.getCorrelationID());
		logModel.setTransactionID(logModelDTO.getTransactionID());
		logModel.setSubTransactionID(logModelDTO.getSubTransactionID());
		logModel.setTimestamp(logModelDTO.getTimestamp());
		logModel.setStartTime(logModelDTO.getStartTime());
		logModel.setEndTime(logModelDTO.getEndTime());
		logModel.setGeoLocation(logModelDTO.getGeoLocation());
		logModel.setCallStack(logModelDTO.getCallStack());
		logModel.setServiceName(logModelDTO.getServiceName());
		logModel.setTransactionType(logModelDTO.getTransactionType());
		logModel.setTransactionSubType(logModelDTO.getTransactionSubType());
		logModel.setLogMessageType(logModelDTO.getLogMessageType());
		logModel.setLogMessage(logModelDTO.getLogMessage());
		logModel.setServiceProviderName(logModelDTO.getServiceProviderName());
		logModel.setServiceProviderAppName(logModelDTO.getServiceProviderAppName());
		logModel.setSignatureType(logModelDTO.getSignatureType());
		logModel.seteSealUsed(logModelDTO.iseSealUsed());
		logModel.setChecksum(logModelDTO.getChecksum());

		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(logModel);
		logger.info(CLASS + " :: getLogModel() :: checksum :: request :: " + json);
		Result checksumResult = DAESService.addChecksumToTransaction(json);
		logger.info(CLASS + " :: getLogModel() :: checksum :: response :: " + new String(checksumResult.getResponse()));
		String push = new String(checksumResult.getResponse());
		LogModel log = objectMapper.readValue(push, LogModel.class);
		return log;
	}

	public static String getDate(String date) {
		String[] dates = date.split(" ");
		return dates[0];
	}
}
