/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.exception;

import java.util.HashMap;

import ug.daes.ra.response.entity.ServiceResponse;

// TODO: Auto-generated Javadoc
/**
 * The Class ErrorCodes.
 */
public class ErrorCodes {

	/** The response. */
	public static ServiceResponse response = null;

	/**
	 * Gets the response.
	 *
	 * @return the response
	 */
	public static ServiceResponse getResponse() {
		return response;
	}

	/**
	 * Sets the response.
	 *
	 * @param response
	 *            the new response
	 */
	public static void setResponse(ServiceResponse response) {
		ErrorCodes.response = response;
	}

	private static HashMap<String, String> messageMapping = null;
	private static HashMap<String, String> codeMapping = null;
	public static String E_OB_01 = "104561";
	public static String E_OB_02 = "104562";
	public static String E_OB_03 = "104563";
	public static String E_OB_04 = "104564";
	public static String E_RA_11 = "104565";
	public static String E_RA_12 = "104566";
	public static String E_RA_13 = "104567";
	public static String E_RA_14 = "104568";
	public static String E_RA_15 = "104569";
	public static String E_RA_16 = "104570";
	public static String E_RA_17 = "104571";
	public static String E_RA_18 = "104572";
	public static String E_RA_19 = "104573";
	public static String E_RA_20 = "104574";
	public static String E_RA_21 = "104575";
	public static String E_RA_22 = "104576";
	public static String E_RA_23 = "104577";
	public static String E_RA_24 = "104578";
	public static String E_RA_25 = "104579";
	public static String E_RA_26 = "104580";
	public static String E_RA_27 = "104581";
	public static String E_RA_28 = "104582";
	public static String E_RA_29 = "104583";
	public static String E_RA_30 = "104584";
	public static String E_RA_31 = "104585";
	public static String E_RA_32 = "104586";
	public static String E_RA_33 = "104587";
	public static String E_RA_34 = "104588";
	public static String E_RA_35 = "104589";
	public static String E_RA_36 = "104590";
	public static String E_RA_37 = "104591";
	public static String E_RA_38 = "104592";
	public static String E_RA_39 = "104593";
	public static String E_RA_100 = "104594";
	public static String E_RA_101 = "104595";
	public static String E_RA_102 = "104596";
	public static String E_RA_103 = "104597";
	public static String E_RA_200 = "104598";
	public static String E_RA_500 = "104599";
	public static String E_RA_501 = "104600";
	public static String E_SUBSCRIBER_DATA_NOT_FOUND = "Subscriber data not found";
	public static String E_ORGANIZATION_DATA_NOT_FOUND = "Organization data not found";
	public static String E_SUBSCRIBER_STATUS_DATA_NOT_FOUND = "Subscriber status data not found";
	public static String E_SUBSCRIBER_DEVICE_DATA_NOT_FOUND = "Subscriber device data not found";
	public static String E_SUBSCRIBER_NOT_ONBOARDED = "Subscriber not onboarded";
	public static String E_SUBSCRIBER_RA_DATA_NOT_FOUND = "Subscriber RA data not found";
	public static String E_SUBSCRIBER_CERTIFICATES_ARE_ACTIVE = "Subscriber certificates are active";
	public static String E_SUBSCRIBER_CERTIFICATES_ARE_REVOKED = "Subscriber certificates are revoke";
	public static String E_SUBSCRIBER_CERTIFICATES_ARE_EXPIRED = "Subscriber certificates are expired";
	public static String E_SUBSCRIBER_ISSUE_SIGNING_CERTIFICATE_FAILED = "Issuing signing certificate failed";
	public static String E_SUBSCRIBER_ISSUE_AUTHENTICATION_CERTIFICATE_FAILED = "Issuing authentication certificate failed";
	public static String E_TRANSACTION_TYPE_NOT_FOUND = "Transaction type not found";
	public static String E_REQUEST_DATA_IS_NOT_VALID = "Request data is not valid";
	public static String E_CERTIFICATES_NOT_ISSUED = "Certificates are not issued";
	public static String E_RA_SERVER_NOT_RUNNING = "RA server not running";
	public static String E_TRANSACTION_HANDLER_NOT_RUNNING = "Transaction handler not running";
	public static String E_RA_SUBSCRIBER_COMPLETE_DETAILS_NOT_FOUND = "Subscriber complete details not found";
	public static String E_ACTIVE_CERTIFICATE_NOT_FOUND = "Active certificate not found";
	public static String E_PIN_MATCHED_WITH_OLD_PIN = "Pin matched with old pin";
	public static String E_CERTIFICATE_TYPE_NOT_FOUND = "Certificate type not found";
	public static String E_LOG_INTEGRITY_FAILED = "Log integrity failed";
	public static String E_RA_POST_REQUEST_FAILED = "RA post request failed";
	public static String E_SOMETHING_WENT_WRONG = "Something went wrong";
	public static String E_NATIVE_REQUEST_FAILED = "Native request failed";
	public static String E_INVALID_REQUEST = "Invalid request";
	public static String E_SIGNING_CERTIFICATE_PIN_NOT_SET = "Signing certificate pin not set";
	public static String E_AUTHENTICATION_CERTIFICATE_PIN_NOT_SET = "Authenticate certificate pin not set";
	public static String E_REVOKE_REASON_NOT_FOUND = "Revoke reason not found";
	public static String E_CERTIFICATE_REVOCATION_FAILED = "Certificate revocation failed";
	public static String E_NIN_NOT_FOUND = "NIN not found";
	public static String E_PASSPORT_NOT_FOUND = "Passport not found";
	public static String E_EMAIL_NOT_FOUND = "Email not found";
	public static String E_MOBILE_NUMBER_NOT_FOUND = "Mobile Number not found";
	public static String E_SUBSCRIBER_NOT_ACTIVE = "Subscriber not active";
	public static String E_PIN_NOT_MATCHED_WITH_OLD_PIN = "Pin not matched with old pin";
	public static String E_SIGNING_PIN_NOT_MATCHED = "Signing pin not matched";
	public static String E_AUTH_PIN_NOT_MATCHED = "Current authentication pin not matched";
	public static String E_NEW_SIGNING_PIN_MATCHED_WITH_OLD_SIGNING_PIN = "New signing pin matched with old signing pin";
	public static String E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN = "New signing pin matched with current authentication pin";
	public static String E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_OLD_AUTHENTICATION_PIN = "New authentication pin matched with old authentication pin";
	public static String E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN = "New authentication pin matched with current signing pin";

	static {
		messageMapping = new HashMap<String, String>();
		messageMapping.put(E_SUBSCRIBER_DATA_NOT_FOUND, E_OB_01);
		messageMapping.put(E_SUBSCRIBER_STATUS_DATA_NOT_FOUND, E_OB_02);
		messageMapping.put(E_SUBSCRIBER_NOT_ONBOARDED, E_OB_03);
		messageMapping.put(E_SUBSCRIBER_DEVICE_DATA_NOT_FOUND, E_OB_04);

		messageMapping.put(E_SUBSCRIBER_RA_DATA_NOT_FOUND, E_RA_11);
		messageMapping.put(E_SUBSCRIBER_CERTIFICATES_ARE_ACTIVE, E_RA_12);
		messageMapping.put(E_SUBSCRIBER_CERTIFICATES_ARE_REVOKED, E_RA_13);
		messageMapping.put(E_SUBSCRIBER_CERTIFICATES_ARE_EXPIRED, E_RA_14);
		messageMapping.put(E_SUBSCRIBER_ISSUE_SIGNING_CERTIFICATE_FAILED, E_RA_15);
		messageMapping.put(E_SUBSCRIBER_ISSUE_AUTHENTICATION_CERTIFICATE_FAILED, E_RA_16);
		messageMapping.put(E_TRANSACTION_TYPE_NOT_FOUND, E_RA_17);
		messageMapping.put(E_REQUEST_DATA_IS_NOT_VALID, E_RA_18);
		messageMapping.put(E_CERTIFICATES_NOT_ISSUED, E_RA_19);
		messageMapping.put(E_ACTIVE_CERTIFICATE_NOT_FOUND, E_RA_20);
		messageMapping.put(E_PIN_MATCHED_WITH_OLD_PIN, E_RA_21);
		messageMapping.put(E_CERTIFICATE_TYPE_NOT_FOUND, E_RA_22);
		messageMapping.put(E_LOG_INTEGRITY_FAILED, E_RA_23);
		messageMapping.put(E_SIGNING_CERTIFICATE_PIN_NOT_SET, E_RA_24);
		messageMapping.put(E_REVOKE_REASON_NOT_FOUND, E_RA_25);
		messageMapping.put(E_CERTIFICATE_REVOCATION_FAILED, E_RA_26);
		messageMapping.put(E_NIN_NOT_FOUND, E_RA_27);
		messageMapping.put(E_PASSPORT_NOT_FOUND, E_RA_28);
		messageMapping.put(E_EMAIL_NOT_FOUND, E_RA_29);
		messageMapping.put(E_MOBILE_NUMBER_NOT_FOUND, E_RA_30);
		messageMapping.put(E_SUBSCRIBER_NOT_ACTIVE, E_RA_31);
		messageMapping.put(E_AUTHENTICATION_CERTIFICATE_PIN_NOT_SET, E_RA_32);
		messageMapping.put(E_PIN_NOT_MATCHED_WITH_OLD_PIN, E_RA_33);
		messageMapping.put(E_SIGNING_PIN_NOT_MATCHED, E_RA_34);
		messageMapping.put(E_AUTH_PIN_NOT_MATCHED, E_RA_35);
		messageMapping.put(E_NEW_SIGNING_PIN_MATCHED_WITH_OLD_SIGNING_PIN, E_RA_36);
		messageMapping.put(E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN, E_RA_37);
		messageMapping.put(E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_OLD_AUTHENTICATION_PIN, E_RA_38);
		messageMapping.put(E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN, E_RA_39);
		messageMapping.put(E_RA_SERVER_NOT_RUNNING, E_RA_100);
		messageMapping.put(E_RA_SUBSCRIBER_COMPLETE_DETAILS_NOT_FOUND, E_RA_101);
		messageMapping.put(E_INVALID_REQUEST, E_RA_102);
		messageMapping.put(E_TRANSACTION_HANDLER_NOT_RUNNING, E_RA_103);
		messageMapping.put(E_RA_POST_REQUEST_FAILED, E_RA_200);
		messageMapping.put(E_NATIVE_REQUEST_FAILED, E_RA_500);
		messageMapping.put(E_SOMETHING_WENT_WRONG, E_RA_501);

		codeMapping = new HashMap<String, String>();
		codeMapping.put(E_OB_01, E_SUBSCRIBER_DATA_NOT_FOUND);
		codeMapping.put(E_OB_02, E_SUBSCRIBER_STATUS_DATA_NOT_FOUND);
		codeMapping.put(E_OB_03, E_SUBSCRIBER_NOT_ONBOARDED);
		codeMapping.put(E_OB_04, E_SUBSCRIBER_DEVICE_DATA_NOT_FOUND);

		codeMapping.put(E_RA_11, E_SUBSCRIBER_RA_DATA_NOT_FOUND);
		codeMapping.put(E_RA_12, E_SUBSCRIBER_CERTIFICATES_ARE_ACTIVE);
		codeMapping.put(E_RA_13, E_SUBSCRIBER_CERTIFICATES_ARE_REVOKED);
		codeMapping.put(E_RA_14, E_SUBSCRIBER_CERTIFICATES_ARE_EXPIRED);
		codeMapping.put(E_RA_15, E_SUBSCRIBER_ISSUE_SIGNING_CERTIFICATE_FAILED);
		codeMapping.put(E_RA_16, E_SUBSCRIBER_ISSUE_AUTHENTICATION_CERTIFICATE_FAILED);
		codeMapping.put(E_RA_17, E_TRANSACTION_TYPE_NOT_FOUND);
		codeMapping.put(E_RA_18, E_REQUEST_DATA_IS_NOT_VALID);
		codeMapping.put(E_RA_19, E_CERTIFICATES_NOT_ISSUED);
		codeMapping.put(E_RA_20, E_ACTIVE_CERTIFICATE_NOT_FOUND);
		codeMapping.put(E_RA_21, E_PIN_MATCHED_WITH_OLD_PIN);
		codeMapping.put(E_RA_22, E_CERTIFICATE_TYPE_NOT_FOUND);
		codeMapping.put(E_RA_23, E_LOG_INTEGRITY_FAILED);
		codeMapping.put(E_RA_24, E_SIGNING_CERTIFICATE_PIN_NOT_SET);
		codeMapping.put(E_RA_25, E_REVOKE_REASON_NOT_FOUND);
		codeMapping.put(E_RA_26, E_CERTIFICATE_REVOCATION_FAILED);
		codeMapping.put(E_RA_27, E_NIN_NOT_FOUND);
		codeMapping.put(E_RA_28, E_PASSPORT_NOT_FOUND);
		codeMapping.put(E_RA_29, E_EMAIL_NOT_FOUND);
		codeMapping.put(E_RA_30, E_MOBILE_NUMBER_NOT_FOUND);
		codeMapping.put(E_RA_31, E_SUBSCRIBER_NOT_ACTIVE);
		codeMapping.put(E_RA_32, E_AUTHENTICATION_CERTIFICATE_PIN_NOT_SET);
		codeMapping.put(E_RA_33, E_PIN_NOT_MATCHED_WITH_OLD_PIN);
		codeMapping.put(E_RA_34, E_SIGNING_PIN_NOT_MATCHED);
		codeMapping.put(E_RA_35, E_AUTH_PIN_NOT_MATCHED);
		codeMapping.put(E_RA_36, E_NEW_SIGNING_PIN_MATCHED_WITH_OLD_SIGNING_PIN);
		codeMapping.put(E_RA_37, E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN);
		codeMapping.put(E_RA_38, E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_OLD_AUTHENTICATION_PIN);
		codeMapping.put(E_RA_39, E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN);
		codeMapping.put(E_RA_100, E_RA_SERVER_NOT_RUNNING);
		codeMapping.put(E_RA_101, E_RA_SUBSCRIBER_COMPLETE_DETAILS_NOT_FOUND);
		codeMapping.put(E_RA_102, E_INVALID_REQUEST);
		codeMapping.put(E_RA_103, E_TRANSACTION_HANDLER_NOT_RUNNING);

		codeMapping.put(E_RA_200, E_RA_POST_REQUEST_FAILED);
		codeMapping.put(E_RA_500, E_NATIVE_REQUEST_FAILED);
		codeMapping.put(E_RA_501, E_SOMETHING_WENT_WRONG);
	}

	/**
	 * Gets the error code.
	 *
	 * @param message
	 *            the message
	 * @return the error code
	 */
	public static String getErrorCode(String message) {
		String errorCode = messageMapping.get(message);
		if (errorCode != null)
			return errorCode;
		else
			return response.getError_code();
	}

	/**
	 * Gets the error message.
	 *
	 * @param errorCode
	 *            the error code
	 * @return the error message
	 */
	public static String getErrorMessage(String errorCode) {
		String errorMessage = codeMapping.get(errorCode);
		if (errorMessage != null)
			return errorMessage;
		else
			return response.getError_message();
	}
}
