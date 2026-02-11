/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.restcontroller;

import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import ug.daes.ra.asserts.RAServiceAsserts;
import ug.daes.ra.config.SentryClientExceptions;
import ug.daes.ra.dto.RARequestDTO;
import ug.daes.ra.request.entity.SetPinModelDto;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.exception.RAServiceException;
import ug.daes.ra.request.entity.AuthenticatePKIModel;
import ug.daes.ra.request.entity.GenerateSignature;
import ug.daes.ra.request.entity.SetPinModel;
import ug.daes.ra.request.entity.VerifyCertificatesPins;
import ug.daes.ra.response.entity.APIResponse;
import ug.daes.ra.service.iface.implementation.RAPKIServiceIfaceImpl;
import ug.daes.ra.service.iface.implementation.RAServiceIfaceImpl;
import ug.daes.ra.utils.Constant;
import ug.daes.ra.utils.PropertiesConstants;

// TODO: Auto-generated Javadoc
/**
 * The Class RACertificatesRestController.
 */
@RestController
@RequestMapping(value = "/api/")
public class PKIRestController {
	/** The logger. */
	static Logger logger = LoggerFactory.getLogger(PKIRestController.class);

	/** The class. */
	final private String CLASS = "RACertificatesRestController";

	/** The ra service iface impl. */
	@Autowired
	RAServiceIfaceImpl raServiceIfaceImpl;

	/** The rapki service iface impl. */
	@Autowired
	RAPKIServiceIfaceImpl rapkiServiceIfaceImpl;

	/** The rest template. */
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	MessageSource messageSource;

	/**
	 * Request certificate.
	 *
	 * @return the string
	 */

	@Autowired
	SentryClientExceptions sentryClientExceptions;

	@GetMapping(value = "get/service/status", produces = "application/json")
	public APIResponse getServiceStatus() {
		try {
			try {
				ResponseEntity<String> response = restTemplate.getForEntity(PropertiesConstants.STATUS, String.class);
				if (response.getBody().contains("true")) {
					logger.info(CLASS + " getServiceStatus >  ra.service.is.running");
					return new APIResponse(true,
							messageSource.getMessage("api.response.ra.service.is.running", null, Locale.ENGLISH), null);
				} else {
					throw new Exception(ErrorCodes.E_RA_SERVER_NOT_RUNNING);
				}
			} catch (Exception e) {
				throw new Exception(ErrorCodes.E_TRANSACTION_HANDLER_NOT_RUNNING);
			}
		} 
		catch (Exception e) {
			logger.error(CLASS + " :: getServiceStatus() :: error. " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null);

		}
	}

	/**
	 * Certificate request.
	 *
	 * @param requestBody the request body
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/request", consumes = "application/json", produces = "application/json")
	public String certificateRequest(@RequestBody RARequestDTO requestBody) throws UnknownHostException {
		try {
			logger.info(CLASS + " :: requestCertificate() :: request {}", requestBody);
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(requestBody, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getSubscriberUniqueId(), ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.issueCertificate(requestBody);
			logger.info(CLASS + " :: requestCertificate() :: response. " + response);
			return new APIResponse(true,messageSource.getMessage("api.response.issuing.signing.and.authentication.certificates.are.process.successfully",
							null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			sentryClientExceptions.captureTags(requestBody.getSubscriberUniqueId(),null,"generateSignature","PKIRESTController");
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + " :: requestCertificate() :: error :: {}", e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Request certificate call back.
	 *
	 * @param callbackResponse the callback response
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/request/callback")
	public String requestCertificateCallBack(@RequestBody Map<String, String> callbackResponse) throws UnknownHostException {
		try {
			logger.info(CLASS + " :: requestCertificateCallBack(). response :: " + callbackResponse);
			RAServiceAsserts.notNullorEmpty(callbackResponse, ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.issueCertificateCallBack(callbackResponse);
			logger.info(CLASS + " :: requestCertificateCallBack() :: response. " + response);
			return new APIResponse(true, messageSource
					.getMessage("api.response.issue.certificate.callback.process.successfully", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			sentryClientExceptions.captureTags(callbackResponse.get(Constant.CALLBACK_SUID),null,"requestCertificateCallBack","PKIRestController");
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + " :: requestCertificateCallBack() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Revoke certificate.
	 *
	 * @param requestBody the request body
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/revoke", produces = "application/json")
	public String revokeCertificate(@RequestBody RARequestDTO requestBody,HttpServletRequest httpServletRequest) {
		try {
			logger.info(CLASS + " :: revokeCertificate() :: request. ");
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(requestBody, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getSubscriberUniqueId(), ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getReasonId(), ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.revokeCertificate(requestBody);
			logger.info(CLASS + " :: revokeCertificate :: response. " + response);
			return new APIResponse(true, messageSource.getMessage("api.response.certificates.are.revoked.successfully",
					null, Locale.ENGLISH), "\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: revokeCertificate() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Sets the pin.
	 *
	 * @param setPinModel the set pin model
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/set-pin", consumes = "application/json")
	public String setPin(@RequestBody SetPinModel setPinModel,HttpServletRequest httpServletRequest) {
		try {
			logger.info(CLASS + " :: setPin() :: request :: " + setPinModel);
			// APIResponse apiResponse = getServiceStatus();
			// if (!apiResponse.getStatus())
			// throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(setPinModel, ErrorCodes.E_INVALID_REQUEST);
			String response = rapkiServiceIfaceImpl.setPin(setPinModel);
			logger.info(CLASS + " :: setPin() :: response. " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.pin.operation.successful", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.info(CLASS + " :: SetPin() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Generate signature.
	 *
	 * @param generateSignatureRequest the generate signature request
	 * @return the string
	 */
	//@Async
//	@PostMapping(value = "post/service/certificate/generate-signature", consumes = "application/json")
//	public CompletableFuture<String> generateSignature(@RequestBody GenerateSignature generateSignatureRequest) {
//		try {
//			logger.info(CLASS + " :: generateSignature :: request. :: " + generateSignatureRequest);
//			APIResponse apiResponse = getServiceStatus();
//			if (!apiResponse.getStatus())
//				throw new Exception(apiResponse.getMessage());
//			logger.info(CLASS + " :: generateSignature() :: generateSignatureRequest :: " + generateSignatureRequest);
//			RAServiceAsserts.notNullorEmpty(generateSignatureRequest, ErrorCodes.E_INVALID_REQUEST);
//			//String response = rapkiServiceIfaceImpl.generateSignature(generateSignatureRequest);
//			//logger.info(CLASS + " :: generateSignature() :: response :: " + response);
//			return rapkiServiceIfaceImpl.generateSignature(generateSignatureRequest)
//					.thenApply(response -> new APIResponse(true,
//					messageSource.getMessage("api.response.generate.signature.response", null, Locale.ENGLISH),
//					"\"" + response + "\"").toString());
//		} catch (Exception e) {
//			logger.info(CLASS + " :: generateSignature() :: error :: " + e.getMessage());
//			return CompletableFuture.completedFuture(new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
//					.toString());
//		}
//	}
	/**
	 * Sets Auth and Sign the pin.
	 *
	 * @param setPinModel the set pin model
	 * @return the string
	 */
	@PostMapping(value = "post/certificate/set-pins", consumes = "application/json")
	public String setPins(@RequestBody SetPinModelDto setPinModel, HttpServletRequest httpServletRequest) {
		try {
			logger.info(CLASS + " :: setAuthSignPin() :: request :: " + setPinModel);
			RAServiceAsserts.notNullorEmpty(setPinModel, ErrorCodes.E_INVALID_REQUEST);
			String response = rapkiServiceIfaceImpl.setPins(setPinModel);
			return new APIResponse(true,
					messageSource.getMessage("api.response.pin.operation.successful", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.info(CLASS + " :: setAuthSignPin() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}
	@PostMapping(value = "post/service/certificate/generate-signature", consumes = "application/json")
	public String generateSignature(@RequestBody GenerateSignature generateSignatureRequest) throws UnknownHostException {
		try {
			logger.info(CLASS + " :: generateSignature :: request. :: " + generateSignatureRequest);
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			logger.info(CLASS + " :: generateSignature() :: generateSignatureRequest :: " + generateSignatureRequest);
			RAServiceAsserts.notNullorEmpty(generateSignatureRequest, ErrorCodes.E_INVALID_REQUEST);
			String response = rapkiServiceIfaceImpl.generateSignature(generateSignatureRequest);
			//logger.info(CLASS + " :: generateSignature() :: response :: " + response);
//			return rapkiServiceIfaceImpl.generateSignature(generateSignatureRequest)
//					.thenApply(response -> new APIResponse(true,
//					messageSource.getMessage("api.response.generate.signature.response", null, Locale.ENGLISH),
//					"\"" + response + "\"").toString());
			return new APIResponse(true, "Generate Signature Response.", "\"" + response + "\"").toString();
		} catch (Exception e) {
			sentryClientExceptions.captureTags(generateSignatureRequest.getSubscriberUniqueId(),null,"generateSignature","PKIRESTController");
			sentryClientExceptions.captureExceptions(e);
			logger.info(CLASS + " :: generateSignature() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
//			return CompletableFuture.completedFuture(new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
//					.toString());
		}
	}

	@PostMapping(value = { "post/service/certificate/generate-signature-org" }, consumes = { "application/json" })
	public String generateSignatureOrganization(@RequestBody GenerateSignature generateSignatureRequest) throws UnknownHostException {
		try {
			logger.info(CLASS + " :: generateSignatureOrganization :: request :: " + generateSignatureRequest);
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus()) {
				throw new Exception(apiResponse.getMessage());
			}
			RAServiceAsserts.notNullorEmpty(generateSignatureRequest, ErrorCodes.E_INVALID_REQUEST);
			String response = rapkiServiceIfaceImpl.generateSignatureOrganization(generateSignatureRequest);
			logger.info("RACertificatesRestController :: generateSignatureOrganization() :: response :: " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.generate.signature.response", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			sentryClientExceptions.captureTags(generateSignatureRequest.getSubscriberUniqueId(),null,"generateSignature","PKIRESTController");
			sentryClientExceptions.captureExceptions(e);
			logger.info(
					"RACertificatesRestController :: generateSignatureOrganization() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Authenticate PKI.
	 *
	 * @param authenticatePKIModel the authenticate PKI model
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/authenticate-pki", consumes = "application/json")
	public String authenticatePKI(@RequestBody AuthenticatePKIModel authenticatePKIModel) {
		try {
			logger.info(CLASS + " :: authenticatePKI() :: authenticatePKIRequest :: " + authenticatePKIModel);
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(authenticatePKIModel, ErrorCodes.E_INVALID_REQUEST);
			String response = rapkiServiceIfaceImpl.authenticatePKI(authenticatePKIModel);
			logger.info(CLASS + " :: authenticatePKI() :: response :: " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.authenticate.pki.response", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.info(CLASS + " :: authenticatePKI() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	/**
	 * Verify certficate pin.
	 *
	 * @param requestBody the request body
	 * @return the string
	 */
	@PostMapping(value = "post/service/certificate/verify-certificate-pin", produces = "application/json")
	public String verifyCertficatePin(@RequestBody VerifyCertificatesPins requestBody,HttpServletRequest httpServletRequest) {
		try {
			logger.info(CLASS + " :: verifyCertficatePin() :: request :: " + requestBody);
			APIResponse apiResponse = getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(requestBody, ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getSubscriberUId(), ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getSigningPin(), ErrorCodes.E_INVALID_REQUEST);
			RAServiceAsserts.notNullorEmpty(requestBody.getAuthPin(), ErrorCodes.E_INVALID_REQUEST);
			String response = raServiceIfaceImpl.verifyCertficatesPins(requestBody);
			logger.info(CLASS + " :: verifyCertficatePin() :: response. " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.pin.verification.successful", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.error(CLASS + " :: verifyCertficatePin() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	@GetMapping(value = "get/servcie/certificate/status")
	public String checkCertificateStatus() throws RAServiceException, Exception {

		String status = raServiceIfaceImpl.checkCertificateStatus();
		return status;
	}
}
