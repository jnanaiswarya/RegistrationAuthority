/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.restcontroller;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;

import ug.daes.DAESService;
import ug.daes.PKICoreServiceException;
import ug.daes.Result;
import ug.daes.ra.asserts.RAServiceAsserts;
import ug.daes.ra.dto.ApiResponse;
import ug.daes.ra.dto.ExpireSubscriberCertRequestDTO;
import ug.daes.ra.dto.RARequestDTO;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.request.entity.AgentCert;
import ug.daes.ra.request.entity.CompareFaceRequest;
import ug.daes.ra.request.entity.QrData;
import ug.daes.ra.response.entity.APIResponse;
import ug.daes.ra.response.entity.ApiResponse_CertCount;
import ug.daes.ra.response.entity.Response;
import ug.daes.ra.service.iface.RAServiceIface;
import ug.daes.ra.service.iface.SubscriberRAServiceIface;
import ug.daes.ra.service.iface.implementation.RAServiceIfaceImpl;

import ug.daes.ra.utils.AppUtil;

/**
 * The Class RASubscriberRestController.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/")
public class RARestController {

	/** The Constant CLASS. */
	final static private String CLASS = "RASubscriberRestController";

	/** The Constant logger. */
	final static private Logger logger = LoggerFactory.getLogger(RARestController.class);

	/** The subscriber service iface impl. */
	@Autowired
	SubscriberRAServiceIface subscriberServiceIfaceImpl;

	/** The ra service iface impl. */
	@Autowired
	RAServiceIfaceImpl raServiceIfaceImpl;

	@Autowired
	RAServiceIface raServiceIface;

	@Autowired
	MessageSource messageSource;

	/**
	 * Gets the certificate data by certificate type.
	 *
	 * @param subscriberUid the subscriber uid
	 * @param certType      the cert type
	 * @return the certificate data by certificate type
	 */
	@GetMapping(value = "get/service/certificate/data/{subscriberUid}/{certType}", produces = "application/json")
	public String getCertificateDataByCertificateType(@PathVariable String subscriberUid,
			@PathVariable String certType) {
		try {
			logger.info(CLASS + " :: getCertificateDataByCertificateType() :: request :: suid and certtype ::"
					+ subscriberUid + certType);
			RAServiceAsserts.notNullorEmpty(subscriberUid, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.getCertificateDataByCertificateType(subscriberUid, certType);
			logger.info(CLASS + " :: getCertificateDataByCertificateType() :: response. " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.certificate.data", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateDataByCertificateType() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	@GetMapping(value = "get/service/certificate/data/organization/{orgId}/{certType}", produces = "application/json")
	public String getOrganizationCertficateDataByCertificateType(@PathVariable String orgId,
			@PathVariable String certType) {
		try {
			logger.info(CLASS + " :: getOrganizationCertficateDataByCertificateType() :: request :: orgId and certtype :: "
							+ orgId + certType);
			RAServiceAsserts.notNullorEmpty(orgId, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.getOrganizationCertificateDataByCertificateType(orgId, certType);
			logger.info(CLASS + " :: getOrganizationCertficateDataByCertificateType() :: response. " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.certificate.data", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateDataByCertificateType() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	@GetMapping(value = "get/service/certificate/data/agent/{subscriberUid}/{certType}", produces = "application/json")
	public ApiResponse getCertificateDataByCertificateTypeForAgent(@PathVariable String subscriberUid,
			@PathVariable String certType) {
		try {
			logger.info(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: request :: suid and certtype :: "
					+ subscriberUid + certType);
			RAServiceAsserts.notNullorEmpty(subscriberUid, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			ApiResponse response = raServiceIfaceImpl.getCertificateDataByCertificateTypeForAgent(subscriberUid,
					certType);
			logger.info(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: response. " + response);
			return response;
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: error :: " + e.getMessage());
			return AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.please.try.after.some.time.server.down", null, Locale.ENGLISH),
					null);
		}
	}

	@PostMapping(value = "get/service/certificate/data/agent/encrypt")
	public String getCertificateDataByCertificateTypeForAgentEncrypt(@RequestBody String request) throws PKICoreServiceException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Result result = DAESService.decryptSecureWireData(request);
			String decryptedString = new String(result.getResponse());
			AgentCert agentCert = mapper.readValue(decryptedString,AgentCert.class);
			String subscriberUid = agentCert.getId();
			String certType = agentCert.getCertType();
			logger.info(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: request :: suid and certtype :: "
					+ subscriberUid + certType);
			RAServiceAsserts.notNullorEmpty(subscriberUid, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			ApiResponse response = raServiceIfaceImpl.getCertificateDataByCertificateTypeForAgent(subscriberUid,
					certType);
			logger.info(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: response. " + response);
			String responseString = mapper.writeValueAsString(response);
			Result encrypt = DAESService.createSecureWireData(responseString);
			return new String(encrypt.getResponse());
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateDataByCertificateTypeForAgent() :: error :: " + e.getMessage());
			ApiResponse response= AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.please.try.after.some.time.server.down", null, Locale.ENGLISH),
					null);
			Result encrypt = DAESService.createSecureWireData(response.toString());
			return new String(encrypt.getResponse());
		}
	}

	@GetMapping(value = "get/service/certificate/data/organization/agent/{orgId}/{certType}", produces = "application/json")
	public ApiResponse getOrganizationCertficateDataByCertificateTypeForAgent(@PathVariable String orgId,
			@PathVariable String certType) {
		try {
			logger.info(
					CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: request :: orgId and certtype :: "
							+ orgId + certType);
			RAServiceAsserts.notNullorEmpty(orgId, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			ApiResponse response = raServiceIfaceImpl.getOrganizationCertificateDataByCertificateTypeForAgent(orgId,
					certType);
			logger.info(
					CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: response. " + response);
			return response;
		} catch (Exception e) {
			logger.error(CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: error :: "
					+ e.getMessage());
			return AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.please.try.after.some.time.server.down", null, Locale.ENGLISH),
					null);
		}
	}


	@PostMapping(value = "get/service/certificate/data/organization/agent/encrypt")
	public String getOrganizationCertficateDataByCertificateTypeForAgentEncrypt(@RequestBody String request) throws PKICoreServiceException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Result result = DAESService.decryptSecureWireData(request);
			String decryptedString = new String(result.getResponse());
			AgentCert agentCert = mapper.readValue(decryptedString,AgentCert.class);
			String orgId = agentCert.getId();
			String certType = agentCert.getCertType();
			logger.info(
					CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: request :: orgId and certtype :: "
							+ orgId + certType);
			RAServiceAsserts.notNullorEmpty(orgId, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(certType, ErrorCodes.E_INVALID_REQUEST);
			ApiResponse response = raServiceIfaceImpl.getOrganizationCertificateDataByCertificateTypeForAgent(orgId,
					certType);
			logger.info(
					CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: response. " + response);
			String responseString = mapper.writeValueAsString(response);
			Result encrypt = DAESService.createSecureWireData(responseString);
			return new String(encrypt.getResponse());
		} catch (Exception e) {
			logger.error(CLASS + " :: getOrganizationCertficateDataByCertificateTypeForAgent() :: error :: "
					+ e.getMessage());
			ApiResponse response= AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.please.try.after.some.time.server.down", null, Locale.ENGLISH),
					null);
			Result encrypt = DAESService.createSecureWireData(response.toString());
			return new String(encrypt.getResponse());
		}
	}

	/**
	 * Gets the certificate life cycle logs by subscriber unique id.
	 *
	 * @param subscriberUid the subscriber uid
	 * @return the certificate life cycle logs by subscriber unique id
	 */
	@GetMapping(value = "get/service/certificate/history/{subscriberUid}", produces = "application/json")
	public String getCertificateLifeCycleLogsBySubscriberUniqueId(@PathVariable String subscriberUid) {
		try {
			logger.info(CLASS + " :: getCertificateLifeCycleLogsBySubscriberUniqueId() :: request :: suid ::"
					+ subscriberUid);
			RAServiceAsserts.notNullorEmpty(subscriberUid, ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.getCertificateLifeCycleLogsBySubscriberUniqueId(subscriberUid);
			logger.info(CLASS + " :: getCertificateLifeCycleLogsBySubscriberUniqueId() :: response. " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.certificate.history", null, Locale.ENGLISH), response)
							.toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateLifeCycleLogsBySubscriberUniqueId() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Gets the subscriber details by search type.
	 *
	 * @param searchType  the search type
	 * @param searchValue the search value
	 * @return the subscriber details by search type
	 */
	@GetMapping(value = "get/service/subscriber/details/{searchType}/{searchValue}", produces = "application/json")
	public String getSubscriberDetailsBySearchType(@PathVariable int searchType, @PathVariable String searchValue) {
		try {
			logger.info(CLASS + " :: getSubscriberDetailsBySearchType() :: request :: serachType and searchValue ::"
					+ searchType + searchValue);
			RAServiceAsserts.notNullorEmpty(searchType, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(searchValue, ErrorCodes.E_INVALID_REQUEST);
			String raSubscriberData = subscriberServiceIfaceImpl.getSubscriberDetailsBySearchType(searchType,
					searchValue);
			logger.info(CLASS + " :: getSubscriberDetailsBySearchType().success");
			return new APIResponse(true,
					messageSource.getMessage("api.response.subscriber.details", null, Locale.ENGLISH), raSubscriberData)
							.toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getSubscriberDetailsBySearchType() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Gets the subscriber count.
	 *
	 * @return the subscriber count
	 */
	@GetMapping(value = "get/service/subscriber/count", produces = "application/json")
	public String getSubscriberCount() {
		try {
			logger.info(CLASS + " :: getSubscriberCount().");
			String count = subscriberServiceIfaceImpl.getCountOfAllSubscribers();
			logger.info(CLASS + " :: getSubscriberCount().success");
			return new APIResponse(true,
					messageSource.getMessage("api.response.subscriber.count", null, Locale.ENGLISH), count).toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getSubscriberCount() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Gets the revoke reasons.
	 *
	 * @return the revoke reasons
	 */
	@GetMapping(value = "get/service/revoke-reasons", produces = "application/json")
	public String getRevokeReasons() {
		logger.info(CLASS + " :: getRevokeReasons().");
		String reasons = raServiceIfaceImpl.getRevokeReasons();
		logger.info(CLASS + " :: getRevokeReasons() :: success.");
		return new APIResponse(true, messageSource.getMessage("api.response.revoke.reasons", null, Locale.ENGLISH),
				reasons).toString();
	}

	/**
	 * Gets the certificate details by subscriber unique id.
	 *
	 * @param subscriberUniqueId the subscriber unique id
	 * @return the certificate details by subscriber unique id
	 */
	@GetMapping(value = "get/service/certificate/details/by-subscriber-unique-id/{subscriberUniqueId}", produces = "application/json")
	public String getCertificateDetailsBySubscriberUniqueId(@PathVariable String subscriberUniqueId,HttpServletRequest httpServletRequest) {
		try {
			logger.info(CLASS + " :: getCertificateDetailsBySubscriberUniqueId() :: request :: subscriberUniqueId :: "
					+ subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriberUniqueId, ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.getCertificateDetailsBySubscriberUniqueId(subscriberUniqueId);
			logger.info(CLASS + " :: getCertificateDetailsBySubscriberUniqueId() :: success.");
			return new APIResponse(true,
					messageSource.getMessage("api.response.certificate.details", null, Locale.ENGLISH), response)
							.toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateDetailsBySubscriberUniqueId() :: error. " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Update subscriber status.
	 *
	 * @param updateSubscriberStatus the update subscriber status
	 * @return the string
	 */
	@PutMapping(value = "update/service/subscriber/status", produces = "application/json")
	public String updateSubscriberStatus(@RequestBody RARequestDTO updateSubscriberStatus) {
		try {
			logger.info(CLASS + " :: updateSubscriberStatus() :: request :: updateSubscriberStatus ::"
					+ updateSubscriberStatus);
			RAServiceAsserts.notNullorEmpty(updateSubscriberStatus, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(updateSubscriberStatus.getSubscriberUniqueId(),
					ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(updateSubscriberStatus.getSubscriberStatus(), ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(updateSubscriberStatus.getDescription(), ErrorCodes.E_INVALID_REQUEST);
			String status = subscriberServiceIfaceImpl.updateSubscriberStatus(updateSubscriberStatus);
			logger.info(CLASS + " :: updateSubscriberStatus() :: Success.");
			return new APIResponse(true,
					messageSource.getMessage("api.response.subscriber.status.updated", null, Locale.ENGLISH),
					"\"" + status + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: updateSubscriberStatus() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Update device status and subscriber status.
	 *
	 * @param subscriberUniqueId the subscriber unique id
	 * @return the string
	 */
	@PutMapping(value = "update/service/subscriber/device-status-and-subscriber-satus/{subscriberUniqueId}")
	public String updateDeviceStatusAndSubscriberStatus(@PathVariable String subscriberUniqueId) {
		try {
			logger.info(CLASS + " :: updateDeviceStatusAndSubscriberStatus() :: request :: subscriberUniqueId ::"
					+ subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriberUniqueId, ErrorCodes.E_INVALID_REQUEST);
			String status = subscriberServiceIfaceImpl.updateDeviceStatusAndSubscriberStatus(subscriberUniqueId);
			logger.info(CLASS + " :: updateDeviceStatusAndSubscriberStatus() :: Success.");
			return new APIResponse(true, messageSource
					.getMessage("api.response.subscriber.status.and.device.status.updated", null, Locale.ENGLISH),
					"\"" + status + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: updateDeviceStatusAndSubscriberStatus() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Gets the subscriber details by search type.
	 *
	 * @param searchType  the search type
	 * @param searchValue the search value
	 * @return the subscriber details by search type
	 */
	@GetMapping(value = "get/service/subscriber/list/{searchType}/{searchValue}", produces = "application/json")
	public String getSubscriberListBySearchType(@PathVariable int searchType, @PathVariable String searchValue) {
		try {
			logger.info(CLASS + " :: getSubscriberDetailsBySearchType() :: request ::searchType and searchValue :: "
					+ searchType + searchValue);
			RAServiceAsserts.notNullorEmpty(searchType, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(searchValue, ErrorCodes.E_INVALID_REQUEST);
			String raSubscriberData = subscriberServiceIfaceImpl.getSubscriberList(searchType, searchValue);
			logger.info(CLASS + " :: getSubscriberListBySearchType().success");
			return new APIResponse(true, messageSource.getMessage("api.response.subscriber.list", null, Locale.ENGLISH),
					raSubscriberData).toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: getSubscriberDetailsBySearchType() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	@GetMapping("get-certificate-count")
	public ApiResponse_CertCount getTotalCertificates() {
		try {
			return subscriberServiceIfaceImpl.getCountOfAllCertificates();
		} catch (Exception e) {
			logger.error(CLASS + " :: getCertificateCount() :: error :: " + e.getMessage());
			return new ApiResponse_CertCount(false, e.getMessage(), null);
		}
	}

	@GetMapping(value = "get/service/subscriber-certificate/count")
	public ApiResponse_CertCount getSubscriberAndCertCount() {
		try {
			return subscriberServiceIfaceImpl.getSubscriberAndCertCount();
		} catch (Exception e) {
			logger.error(CLASS + " :: getSubscriberAndCertificateCount() :: error :: " + e.getMessage());
			return new ApiResponse_CertCount(false, e.getMessage(), null);
		}
	}

	@GetMapping({ "get/service/subscriber/name/{subscriberUniqueId}" })
	public String getSubscriberFullName(@PathVariable String subscriberUniqueId) {
		try {
			logger.info(CLASS + " :: getSubscriberFullName() :: request :: subscriberUniqueId ::" + subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriberUniqueId, ErrorCodes.E_INVALID_REQUEST);
			String response = subscriberServiceIfaceImpl.getSubscriberName(subscriberUniqueId);
			return (new APIResponse(true,
					messageSource.getMessage("api.response.subscriber.full.name", null, Locale.ENGLISH), response))
							.toString();
		} catch (Exception var3) {
			logger.error( CLASS +":: getSubscriberFullName() :: error :: " + var3.getMessage());
			return (new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(var3.getMessage())),
					(String) null)).toString();
		}
	}

	@PostMapping({ "get/service/subscriber/name/encrypt" })
	public String getSubscriberFullNameEncrypt(@RequestBody String request) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Result result = DAESService.decryptSecureWireData(request);
			String subscriberUniqueId = new String(result.getResponse());
			logger.info(CLASS + " :: getSubscriberFullName() :: request :: subscriberUniqueId ::" + subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriberUniqueId, ErrorCodes.E_INVALID_REQUEST);
			String response = subscriberServiceIfaceImpl.getSubscriberName(subscriberUniqueId);
//			Response apiResponse = new Response(true, messageSource.getMessage("api.response.subscriber.full.name", null, Locale.ENGLISH), response);
			ApiResponse apiResponse = AppUtil.createApiResponse(true,messageSource.getMessage("api.response.subscriber.full.name", null, Locale.ENGLISH), response);
			String apiResponseString = mapper.writeValueAsString(apiResponse);
			Result encrypt = DAESService.createSecureWireData(apiResponseString);
			return new String(encrypt.getResponse());
		} catch (Exception var3) {
			logger.error( CLASS +":: getSubscriberFullName() :: error :: " + var3.getMessage());
			return (new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(var3.getMessage())),
					(String) null)).toString();
		}
	}

	@PostMapping("/compare/image")
	public ApiResponse compareImage(@RequestBody CompareFaceRequest compareFaceRequest){
		try{
			RAServiceAsserts.notNullorEmpty(compareFaceRequest.getSuId(), ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(compareFaceRequest.getCapturedImage(), ErrorCodes.E_INVALID_REQUEST);
			String suid = compareFaceRequest.getSuId();
			String capturedImage = compareFaceRequest.getCapturedImage();
			logger.info(CLASS + " :: compareImage() :: request :: suid() capturedImage()" ,suid , capturedImage);
			ApiResponse apiResponse = subscriberServiceIfaceImpl.compareImage(suid,capturedImage);

			return apiResponse;
		}
		catch (Exception e){
			logger.error( CLASS +":: compareImage() :: error :: " + e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false,e.getMessage(),null);
		}
	}

	@PostMapping("/get/image/features")
	public ApiResponse getFeatures(@RequestBody CompareFaceRequest compareFaceRequest){
		try{
			RAServiceAsserts.notNullorEmpty(compareFaceRequest.getCapturedImage(), ErrorCodes.E_INVALID_REQUEST);
			String capturedImage = compareFaceRequest.getCapturedImage();
			logger.info(CLASS + " :: getFeatures() :: request :: capturedImage()" , capturedImage);
			ApiResponse apiResponse = subscriberServiceIfaceImpl.getFeatures(null,capturedImage);

			return apiResponse;
		}
		catch (Exception e){
			logger.error( CLASS +":: getFeatures() :: error :: " + e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false,e.getMessage(),null);
		}
	}

	@PostMapping("/get/image/features/encrypt")
	public String getFeaturesEncrypt(@RequestBody String request){
		try{
			ObjectMapper mapper = new ObjectMapper();
			Result result = DAESService.decryptSecureWireData(request);
			String decryptedString = new String(result.getResponse());
			CompareFaceRequest compareFaceRequest = mapper.readValue(decryptedString,CompareFaceRequest.class);
			RAServiceAsserts.notNullorEmpty(compareFaceRequest.getCapturedImage(), ErrorCodes.E_INVALID_REQUEST);
			String capturedImage = compareFaceRequest.getCapturedImage();
			logger.info(CLASS + " :: getFeatures() :: request :: capturedImage()" , capturedImage);
			ApiResponse apiResponse = subscriberServiceIfaceImpl.getFeatures(null,capturedImage);
			String apiResponseString = mapper.writeValueAsString(apiResponse);
			Result encrypt = DAESService.createSecureWireData(apiResponseString);
			return new String(encrypt.getResponse());
		}
		catch (Exception e){
			logger.error( CLASS +":: getFeatures() :: error :: " + e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false,e.getMessage(),null).toString();
		}
	}

//	@GetMapping("/fetch/service/subscriber/details/{searchType}/{searchValue}")
//	public ApiResponse fetchVisitorCompleteDetail(@PathVariable int searchType,@PathVariable String searchValue){
//		return raServiceIface.fetchVisitorDetailsBySearchType(searchType,searchValue);
//	}
@GetMapping(value = "get/subscriber/photo/{suid}", produces = "application/json")
public ApiResponse getSubscriberPhoto(@PathVariable String suid) {
	try {
		logger.info(CLASS + " :: getSubscriberDetailsBySearchType() :: request :: searchValue ::"
				+ suid);
		System.out.println("SUID is "+suid);
		RAServiceAsserts.notNullorEmpty(suid, ErrorCodes.E_INVALID_REQUEST);
		return  subscriberServiceIfaceImpl.getSubscriberPhoto(suid);

	} catch (Exception e) {
		logger.error(CLASS + " :: getSubscriberDetailsBySearchType() :: error :: " + e.getMessage());
		return AppUtil.createApiResponse(false,e.getMessage(),null);

	}
}

	@PostMapping(value = "/post/get/qr/data")
	public Response getSignForQr(@RequestBody QrData qrData) throws JsonProcessingException {
		try {
			logger.info(CLASS + " :: getSignForQr :: request" + qrData);
			Response response = subscriberServiceIfaceImpl.getSignForQr(qrData);
			return response;
		}
		catch(Exception e){
			e.printStackTrace();
			return new Response(false , "Error ", e.getMessage());
		}
	}

	@PostMapping(value = "/post/get/qr/data/encrypt")
	public String getSignForQrEncrypt(@RequestBody String request) throws JsonProcessingException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Result result = DAESService.decryptSecureWireData(request);
			String decryptedString = new String(result.getResponse());
			QrData qrData = mapper.readValue(decryptedString,QrData.class);
			logger.info(CLASS + " :: getSignForQr :: request" + qrData);
			Response response = subscriberServiceIfaceImpl.getSignForQr(qrData);
			String responseString = mapper.writeValueAsString(response);
			Result encrypt = DAESService.createSecureWireData(responseString);
			return new String(encrypt.getResponse());
		}
		catch(Exception e){
			e.printStackTrace();
			return new Response(false , "Error ", e.getMessage()).toString();
		}
	}

	@PostMapping({"/post/expire/subscriber-cert"})
	public ApiResponse expireSubscriberCert(@RequestBody ExpireSubscriberCertRequestDTO expireSubscriberCertRequestDTO) {
		try {
			logger.info("RASubscriberRestController :: expireSubscriberCert() :: request :: mobilenumber ::" + expireSubscriberCertRequestDTO.getMobileNumber() + ":: email :: " + expireSubscriberCertRequestDTO.getEmail());
			System.out.println("Email id  " + expireSubscriberCertRequestDTO.getEmail());
			System.out.println("Mobile number id  " + expireSubscriberCertRequestDTO.getMobileNumber());
			return this.raServiceIface.expireSubscriberCert(expireSubscriberCertRequestDTO);
		} catch (Exception var3) {
			var3.printStackTrace();
			return AppUtil.createApiResponse(false, "Error ", var3.getMessage().toString());
		}
	}

}
