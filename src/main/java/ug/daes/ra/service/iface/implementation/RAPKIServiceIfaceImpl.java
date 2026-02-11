/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.service.iface.implementation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ug.daes.ra.asserts.RAServiceAsserts;
import ug.daes.ra.dto.ApiResponse;
import ug.daes.ra.dto.LogModelDTO;
import ug.daes.ra.request.entity.SetPinModelDto;
import ug.daes.ra.enums.CertificateStatus;
import ug.daes.ra.enums.CertificateType;
import ug.daes.ra.enums.LogMessageType;
import ug.daes.ra.enums.ServiceName;
import ug.daes.ra.enums.SignatureType;
import ug.daes.ra.enums.TransactionType;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.exception.RAServiceException;
import ug.daes.ra.model.OrganizationCertificates;
import ug.daes.ra.model.OrganizationDetails;
import ug.daes.ra.model.OrganizationWrappedKey;
import ug.daes.ra.model.Subscriber;
import ug.daes.ra.model.SubscriberCertificatePinHistory;
import ug.daes.ra.model.SubscriberCertificates;
import ug.daes.ra.model.SubscriberStatus;
import ug.daes.ra.model.SubscriberWrappedKey;
import ug.daes.ra.repository.iface.OrganizationCertificatesRepository;
import ug.daes.ra.repository.iface.OrganizationDetailsRepository;
import ug.daes.ra.repository.iface.OrganizationWrappedKeyRepository;
import ug.daes.ra.repository.iface.SubscriberCertificatePinHistoryRepository;
import ug.daes.ra.repository.iface.SubscriberCertificatesRepository;
import ug.daes.ra.repository.iface.SubscriberRepository;
import ug.daes.ra.repository.iface.SubscriberStatusRepository;
import ug.daes.ra.repository.iface.SubscriberWrappedKeyRepository;
import ug.daes.ra.request.entity.AuthenticatePKIModel;
import ug.daes.ra.request.entity.GenerateSignature;
import ug.daes.ra.request.entity.LogModel;
import ug.daes.ra.request.entity.PostRequest;
import ug.daes.ra.request.entity.RequestEntity;
import ug.daes.ra.request.entity.SetPinModel;
import ug.daes.ra.response.entity.ServiceResponse;
import ug.daes.ra.service.iface.RAPKIServiceIface;
import ug.daes.ra.utils.Constant;
import ug.daes.ra.utils.NativeUtils;
import ug.daes.ra.utils.PropertiesConstants;
import ug.daes.ra.utils.KafkaSender;

/**
 * The Class RAPKIServiceIfaceImpl.
 */
@Service
public class RAPKIServiceIfaceImpl implements RAPKIServiceIface {

	@Value("${com.dt.pin.history.size}")
	private int pinHistorySize;
	
	@Value("${url.unlocked.AuthResetPin}")
	private String urlunlockedAuthResetPin;

	/** The Constant CLASS. */
	final static private String CLASS = "RAPKIServiceIfaceImpl";

	/** The Constant logger. */
	final static private Logger logger = LoggerFactory.getLogger(RAPKIServiceIfaceImpl.class);

	/** The Constant objectMapper. */
	final static private ObjectMapper objectMapper = new ObjectMapper();

	/** The signing pin. */
	private static String signingPin;

	/** The authentication pin. */
	private static String authenticationPin;

	/** The pin list. */
	private static List<String> pinList;

	/** The subscriber. */
	private static Subscriber subscriber;

	/** The log model DTO. */
	private static LogModelDTO logModelDTO;

	/** The certificate. */
	private static SubscriberCertificates certificate;

	/** The subscriber certificates. */
	private static List<SubscriberCertificates> subscriberCertificates;

	/** The subscriber certificate pin history. */
	private static SubscriberCertificatePinHistory subscriberCertificatePinHistory;

	/** The subscriber wrapped key. */
	private static SubscriberWrappedKey subscriberWrappedKey;

	/** The rabbit MQ sender. */
	@Autowired
	private KafkaSender rabbitMQSender;

	/** The rest template. */
	@Autowired
	private RestTemplate restTemplate;

	/** The subscriber repository. */
	@Autowired
	private SubscriberRepository subscriberRepository;

	/** The subscriber certificate data repository. */
	@Autowired
	private SubscriberCertificatesRepository subscriberCertificatesRepository;

	/** The pin history repository. */
	@Autowired
	private SubscriberCertificatePinHistoryRepository pinHistoryRepository;

	/** The subscriber status repository. */
	@Autowired
	private SubscriberStatusRepository subscriberStatusRepository;

	/** The subscriber wrapped key repository. */
	@Autowired
	private SubscriberWrappedKeyRepository subscriberWrappedKeyRepository;

	@Autowired
	private OrganizationDetailsRepository organizationDetailsRepository;

	@Autowired
	private OrganizationCertificatesRepository organizationCertificatesRepository;

	@Autowired
	private OrganizationWrappedKeyRepository organizationWrappedKeyRepository;
	@Autowired
	MessageSource messageSource;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dtt.ra.service.iface.RAPKIServiceIface#setPin(com.dtt.ra.request.entity.
	 * SetPinModel)
	 */
	@Override
	public String setPin(SetPinModel setPinModel) throws RAServiceException, Exception {
		try {
			System.out.println("Pin his size :: " + pinHistorySize);
			logger.info(CLASS + " :: setPin() :: request :: " + setPinModel.toString());
			subscriber = subscriberRepository.findBysubscriberUid(setPinModel.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setIdentifier(subscriber.getSubscriberUid());
			logModelDTO.setServiceName(null);
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(NativeUtils.getUUId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(setPinModel.getGeoLocation());
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(null);
			logModelDTO.seteSealUsed(false);


			subscriberCertificates = subscriberCertificatesRepository.findByCertificateStatusAndsubscriberUniqueId(
					CertificateStatus.ACTIVE.toString(), setPinModel.getSubscriberUniqueId());


			RAServiceAsserts.notNullorEmpty(subscriberCertificates.size(), ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
				certificate = subscriberCertificate;

				subscriberCertificatePinHistory = pinHistoryRepository
						.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());

				if (setPinModel.getCertType() == 0
						&& subscriberCertificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
					if (setPinModel.isResetPIN()) {
						RAServiceAsserts.notNullorEmpty(subscriberCertificatePinHistory,
								ErrorCodes.E_SIGNING_CERTIFICATE_PIN_NOT_SET);
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						if (null != signingPin)
							NativeUtils.checkOldPasswords(signingPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_OLD_SIGNING_PIN);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						if (null != authenticationPin)
							NativeUtils.checkCurrentPassword(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN);
						List<String> pinList = Arrays.asList(signingPin.split(", "));
						setPinModel.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
					}
					if (setPinModel.isChangePin()) {
						RAServiceAsserts.notNullorEmpty(subscriberCertificatePinHistory,
								ErrorCodes.E_SIGNING_CERTIFICATE_PIN_NOT_SET);
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						NativeUtils.checkOldCurrentPasswords(signingPin, setPinModel.getOldSigningPassword(),
								ErrorCodes.E_SIGNING_PIN_NOT_MATCHED);
						if (null != signingPin)
							NativeUtils.checkOldPasswords(signingPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_OLD_SIGNING_PIN);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						if (null != authenticationPin)
							NativeUtils.checkCurrentPassword(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN);
					}
					if (null != subscriberCertificatePinHistory) {
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						if (null != authenticationPin)
							NativeUtils.checkCurrentPassword(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN);
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					setPinModel.setWrappedKey(subscriberWrappedKey.getWrappedKey());
					break;
				} else if (setPinModel.getCertType() == 1
						&& subscriberCertificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
					if (setPinModel.isResetPIN()) {
						
						//unlockedAuthResetPin(setPinModel.getSubscriberUniqueId());
							
						RAServiceAsserts.notNullorEmpty(subscriberCertificatePinHistory,
								ErrorCodes.E_AUTHENTICATION_CERTIFICATE_PIN_NOT_SET);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						if (null != authenticationPin)
							NativeUtils.checkOldPasswords(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_OLD_AUTHENTICATION_PIN);
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						if (null != signingPin)
							NativeUtils.checkCurrentPassword(signingPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN);
						List<String> pinList = Arrays.asList(authenticationPin.split(", "));
						setPinModel.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
					}
					if (setPinModel.isChangePin()) {
						RAServiceAsserts.notNullorEmpty(subscriberCertificatePinHistory,
								ErrorCodes.E_AUTHENTICATION_CERTIFICATE_PIN_NOT_SET);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						NativeUtils.checkOldCurrentPasswords(authenticationPin, setPinModel.getOldSigningPassword(),
								ErrorCodes.E_AUTH_PIN_NOT_MATCHED);
						if (null != authenticationPin)
							NativeUtils.checkOldPasswords(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_OLD_AUTHENTICATION_PIN);
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						if (null != signingPin)
							NativeUtils.checkCurrentPassword(signingPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN);
					}
					if (null != subscriberCertificatePinHistory) {
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						if (null != signingPin)
							NativeUtils.checkCurrentPassword(signingPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					setPinModel.setWrappedKey(subscriberWrappedKey.getWrappedKey());
					break;
				}
			}
			if (setPinModel.isChangePin())
				logModelDTO.setCallStack(setPinModel.getSettingPin());
			else
				logModelDTO.setCallStack(setPinModel.getChangePin());

			logModelDTO.setChecksum(null);
			logModelDTO.setEndTime(null);
			logModelDTO.setTimestamp(null);

			PostRequest postRequest = new PostRequest();
			postRequest.setRequestBody(logModelDTO.toString());
			postRequest.setHashdata(logModelDTO.toString().hashCode());

			RequestEntity requestEntity = new RequestEntity();
			requestEntity.setPostRequest(postRequest);
			requestEntity.setTransactionType("SetPin");
			logger.info(CLASS + " :: setPin() :: request :: " + requestEntity.getPostRequest().getRequestBody());
			ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.PKIURL, requestEntity,
					String.class);
			if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
				throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
			if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
				throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
			ServiceResponse serviceResponse = objectMapper.readValue(new String(httpResponse.getBody()),
					ServiceResponse.class);
			logger.info(CLASS + " :: SetPin :: Native response :: " + serviceResponse.getStatus());
			if (serviceResponse.getStatus().equals(Constant.FAIL)) {
				ErrorCodes.setResponse(serviceResponse);
				logger.info(CLASS + " :: SetPin :: error :: " + serviceResponse.getError_message());
				throw new RAServiceException(serviceResponse.getError_message());
			} else {
				if (setPinModel.isResetPIN()) {
					if (setPinModel.getCertType() == 0) {
						pinList = new LinkedList<String>(Arrays.asList(signingPin.split(", ")));
						pinList.remove(pinList.size() - 1);

						if (pinList.size() == 0)
							subscriberCertificatePinHistory
									.setSigningCertificatePinList(setPinModel.getSigningPassword());
						else {
							signingPin = pinList.toString().replace("[", "").replace("]", "");
							subscriberCertificatePinHistory
									.setSigningCertificatePinList(signingPin + ", " + setPinModel.getSigningPassword());
						}
						subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
					} else {
						unlockedAuthResetPin(setPinModel.getSubscriberUniqueId());
						
						pinList = new LinkedList<String>(Arrays.asList(authenticationPin.split(", ")));
						pinList.remove(pinList.size() - 1);
						if (pinList.size() == 0)
							subscriberCertificatePinHistory
									.setAuthenticationCertificatePinList(setPinModel.getSigningPassword());
						else {
							authenticationPin = pinList.toString().replace("[", "").replace("]", "");
							subscriberCertificatePinHistory.setAuthenticationCertificatePinList(
									authenticationPin + ", " + setPinModel.getSigningPassword());
						}
						subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
					}
					pinHistoryRepository.save(subscriberCertificatePinHistory);
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
					subscriberWrappedKey.setWrappedKey(serviceResponse.getWrappedKey());
					subscriberWrappedKeyRepository.save(subscriberWrappedKey);
					logger.info(CLASS + " :: changePin()  :: success.");
					return Constant.SUCCESS;
				}
				if (setPinModel.isChangePin()) {
					if (setPinModel.getCertType() == 0) {
						pinList = new LinkedList<String>(Arrays.asList(signingPin.split(", ")));
						if (pinList.size() < pinHistorySize) {
							subscriberCertificatePinHistory
									.setSigningCertificatePinList(signingPin + ", " + setPinModel.getSigningPassword());
							subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
							pinHistoryRepository.save(subscriberCertificatePinHistory);
						} else {
							pinList.remove(0);
							signingPin = pinList.toString().replace("[", "").replace("]", "");
							subscriberCertificatePinHistory
									.setSigningCertificatePinList(signingPin + ", " + setPinModel.getSigningPassword());
							subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
							pinHistoryRepository.save(subscriberCertificatePinHistory);
						}
					} else {
						pinList = new LinkedList<String>(Arrays.asList(authenticationPin.split(", ")));
						logger.info(CLASS
								+ " :: reSetSigningPin()  :: before reSet AuthenticationPin :: pinList :: size :: "
								+ pinList.size() + "/nPins :: " + pinList);
						if (pinList.size() < pinHistorySize) {
							subscriberCertificatePinHistory.setAuthenticationCertificatePinList(
									authenticationPin + ", " + setPinModel.getSigningPassword());
							subscriberCertificatePinHistory.setAuthPinSetDate(NativeUtils.getTimeStamp());
							pinHistoryRepository.save(subscriberCertificatePinHistory);
						} else {
							pinList.remove(0);
							authenticationPin = pinList.toString().replace("[", "").replace("]", "");
							subscriberCertificatePinHistory.setAuthenticationCertificatePinList(
									authenticationPin + ", " + setPinModel.getSigningPassword());
							subscriberCertificatePinHistory.setAuthPinSetDate(NativeUtils.getTimeStamp());
							pinHistoryRepository.save(subscriberCertificatePinHistory);
						}
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
					subscriberWrappedKey.setWrappedKey(serviceResponse.getWrappedKey());
					subscriberWrappedKeyRepository.save(subscriberWrappedKey);
					logger.info(CLASS + " :: reSetPin()  :: success.");
					return Constant.SUCCESS;

				} else {
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(setPinModel.getSubscriberUniqueId());
					if (null == subscriberCertificatePinHistory)
						subscriberCertificatePinHistory = new SubscriberCertificatePinHistory();
					if (setPinModel.getCertType() == 0) {
						subscriberCertificatePinHistory.setSigningCertificatePinList(setPinModel.getSigningPassword());
						subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
						subscriberCertificatePinHistory.setSubscriberUniqueId(setPinModel.getSubscriberUniqueId());
						pinHistoryRepository.save(subscriberCertificatePinHistory);
					} else {
						subscriberCertificatePinHistory
								.setAuthenticationCertificatePinList(setPinModel.getSigningPassword());
						subscriberCertificatePinHistory.setAuthPinSetDate(NativeUtils.getTimeStamp());
						subscriberCertificatePinHistory.setSubscriberUniqueId(setPinModel.getSubscriberUniqueId());
						pinHistoryRepository.save(subscriberCertificatePinHistory);
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
					subscriberWrappedKey.setWrappedKey(serviceResponse.getWrappedKey());
					subscriberWrappedKeyRepository.save(subscriberWrappedKey);
					if (null != subscriberCertificatePinHistory.getSignPinSetDate()
							&& null != subscriberCertificatePinHistory.getAuthPinSetDate()) {
						SubscriberStatus subscriberStatus = subscriberStatusRepository
								.findBysubscriberUid(setPinModel.getSubscriberUniqueId());
						subscriberStatus.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
						subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
						subscriberStatus.setSubscriberStatusDescription(Constant.SET_PIN_SUCCESS);
						subscriberStatusRepository.save(subscriberStatus);
					}
					logger.info(CLASS + " :: setPin()  :: success.");
					return Constant.SUCCESS;
				}
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}
	


	public void unlockedAuthResetPin(String suid) throws Exception {
		ResponseEntity<ApiResponse> res = null;
		try {
			logger.info(CLASS + " unlockedAuthResetPin() :: {} ", suid);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
			String unlockAuthPinUrl = urlunlockedAuthResetPin + "/" + suid;

			res = restTemplate.exchange(unlockAuthPinUrl, HttpMethod.POST, requestEntity, ApiResponse.class);

			logger.info(CLASS + " unlockedAuthResetPin() rest response urlunlockedAuthResetPin " + res);

			if (res.getStatusCodeValue() == 400) {

				logger.info(CLASS + " unlockedAuthResetPin() :: Invalid request. Please check your input and try again for Driving Licence Service.");
				resetAuthPinlog(res.getStatusCodeValue(), suid);

			} else if (res.getStatusCodeValue() == 401) {

				logger.info(CLASS + " unlockedAuthResetPin() :: Access denied. You are not authorized to view this content. Please authenticate for Driving Licence Service.");

				resetAuthPinlog(res.getStatusCodeValue(), suid);

			} else if (res.getStatusCodeValue() == 403) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Access denied. You don't have permission to view this page.");

				resetAuthPinlog(res.getStatusCodeValue(), suid);

			} else if (res.getStatusCodeValue() == 404) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Oops! The page you're looking for cannot be found.");
				resetAuthPinlog(res.getStatusCodeValue(), suid);

			} else if (res.getStatusCodeValue() == 415) {
				logger.info(CLASS + " unlockedAuthResetPin() :: It seems like you're using an unsupported content type in your request. Please choose a valid one and resubmit your request.");

				resetAuthPinlog(res.getStatusCodeValue(), suid);

			} else if (res.getStatusCodeValue() == 500) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Something unexpected happened. Please refresh the page or try again later");

				resetAuthPinlog(res.getStatusCodeValue(), suid);


			} else if (res.getStatusCodeValue() == 501) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Service temporarily unavailable. Please try again later or contact support for assistance.");

				resetAuthPinlog(res.getStatusCodeValue(), suid);

			}

		}catch(HttpServerErrorException ex) {
			if (ex.getStatusCode().value() == 500) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Something unexpected happened. Please refresh the page or try again later");

				resetAuthPinlog(ex.getStatusCode().value(), suid);


			} else if (ex.getStatusCode().value() == 501) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Service temporarily unavailable. Please try again later or contact support for assistance.");

				resetAuthPinlog(ex.getStatusCode().value(), suid);

			}else if (ex.getStatusCode().value() == 404) {
				logger.info(CLASS + " unlockedAuthResetPin() :: Oops! The page you're looking for cannot be found.");
				resetAuthPinlog(ex.getStatusCode().value(), suid);

			}else {
				logger.info(CLASS + " unlockedAuthResetPin() :: HttpServerErrorException");
				resetAuthPinlog(ex.getStatusCode().value(), suid);
			}
			
		}catch (HttpClientErrorException ex) {
			logger.info(CLASS + " unlockedAuthResetPin() :: HttpClientErrorException");
			resetAuthPinlog(ex.getStatusCode().value(), suid);
		}
		catch (Exception e) {
			logger.info(CLASS + " unlockedAuthResetPin() :: Exception");
			resetAuthPinlog(0, suid);
		}
	}
	
	public void resetAuthPinlog(int responseValue,String suid) {
		try {
			logger.info(CLASS + " resetAuthPinlog() :: {} ",suid,responseValue);
			String message= "RESET_AUTH_PIN_LOCKED | "+responseValue;
			    logModelDTO = new LogModelDTO();
				logModelDTO.setStartTime(NativeUtils.getTimeStampString());
				logModelDTO.setIdentifier(suid);
				logModelDTO.setServiceName(ServiceName.OTHER.toString());
				logModelDTO.setLogMessage(message);
				logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
				logModelDTO.setCorrelationID(NativeUtils.getUUId());
				logModelDTO.setTransactionID(NativeUtils.getUUId());
				logModelDTO.seteSealUsed(false);

				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
				rabbitMQSender.send(logModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dtt.ra.service.iface.RAPKIServiceIface#generateSignature(com.dtt.ra.
	 * request.entity.GenerateSignature)
	 */
	@Override
	public String generateSignature(GenerateSignature generateSignature) throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: generateSignature() :: request." + generateSignature.toString());
			subscriber = subscriberRepository.findBysubscriberUid(generateSignature.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			boolean result = false;
			logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setIdentifier(subscriber.getSubscriberUid());
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setServiceName(ServiceName.OTHER.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(generateSignature.getCorrelationId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(null);
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(SignatureType.DATA.toString());
			logModelDTO.seteSealUsed(false);
			subscriberCertificates = subscriberCertificatesRepository.findByCertificateStatusAndsubscriberUniqueId(
					CertificateStatus.ACTIVE.toString(), generateSignature.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriberCertificates.size(), ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
				if (generateSignature.getCertType() == 1
						&& subscriberCertificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
					generateSignature.setKeyId(subscriberCertificate.getPkiKeyId());
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					generateSignature.setWrappedKey(subscriberWrappedKey.getWrappedKey());
					generateSignature.setCertificate(subscriberCertificate.getCertificateData());
					generateSignature.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
					authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
					pinList = new LinkedList<String>(Arrays.asList(authenticationPin.split(", ")));
					generateSignature.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
					result = true;
					break;
				}
				if (generateSignature.getCertType() == 0
						&& subscriberCertificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
					generateSignature.setKeyId(subscriberCertificate.getPkiKeyId());
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					generateSignature.setWrappedKey(subscriberWrappedKey.getWrappedKey());
					generateSignature.setCertificate(subscriberCertificate.getCertificateData());
					generateSignature.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
					signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
					pinList = new LinkedList<String>(Arrays.asList(signingPin.split(", ")));
					generateSignature.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
					result = true;
					break;
				}
			}
			if (result == false)
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);

			logModelDTO.setCallStack(generateSignature.getGenerateSignatureData());

			PostRequest issueCertificatePostRequest = new PostRequest();
			issueCertificatePostRequest.setRequestBody(logModelDTO.toString());
			issueCertificatePostRequest.setHashdata(logModelDTO.toString().hashCode());

			RequestEntity requestEntity = new RequestEntity();
			requestEntity.setPostRequest(issueCertificatePostRequest);
			requestEntity.setTransactionType(Constant.GENERATE_SIGNATURE);
			logger.info(
					CLASS + " :: generateSignature() :: request :: " + requestEntity.getPostRequest().getRequestBody());
			ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.PKIURL, requestEntity,
					String.class);
			if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
				throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
			if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
				throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
			ServiceResponse serviceResponse = objectMapper.readValue(new String(httpResponse.getBody()),
					ServiceResponse.class);
			logger.info(CLASS + " :: generateSignature() :: Native response :: " + serviceResponse.getStatus());
			logModelDTO.setCallStack(null);
			logModelDTO.setLogMessage(Constant.GENERATE_SIGNATURE);
			logModelDTO.setEndTime(NativeUtils.getTimeStampString());
			if (serviceResponse.getStatus().equals(Constant.FAIL)) {
				ErrorCodes.setResponse(serviceResponse);
				logger.info(CLASS + " :: generateSignature() :: error :: " + serviceResponse.getError_message());
				logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
				logModelDTO.setServiceName(ServiceName.DIGITALLY_SIGNING_FAILED.toString());
				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
				//rabbitMQSender.send(logModel);
				throw new RAServiceException(serviceResponse.getError_message());
			}
			logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
			LogModel logModel = NativeUtils.getLogModel(logModelDTO);
			//rabbitMQSender.send(logModel);
			//return CompletableFuture.completedFuture(serviceResponse.getSignature());
			return serviceResponse.getSignature();
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
//			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
//					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: exception {} ", e.getMessage());
			throw new RAServiceException(e.getMessage());
//			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
//					null, Locale.ENGLISH));
		}
	}
	
//	@Override
//	public CompletableFuture<String> generateSignature(GenerateSignature generateSignature) throws RAServiceException,Exception {
//		try {
//			logger.info(CLASS + " :: generateSignature() :: request." + generateSignature.toString());
//			subscriber = subscriberRepository.findBysubscriberUid(generateSignature.getSubscriberUniqueId());
//			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
//			boolean result = false;
//			logModelDTO = new LogModelDTO();
//			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
//			logModelDTO.setIdentifier(subscriber.getSubscriberUid());
//			logModelDTO.setLogMessage(Constant.REQUEST);
//			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
//			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
//			logModelDTO.setServiceName(ServiceName.OTHER.toString());
//			logModelDTO.setTransactionSubType(null);
//			logModelDTO.setCorrelationID(generateSignature.getCorrelationId());
//			logModelDTO.setTransactionID(NativeUtils.getUUId());
//			logModelDTO.setSubTransactionID(null);
//			logModelDTO.setGeoLocation(null);
//			logModelDTO.setServiceProviderName(null);
//			logModelDTO.setServiceProviderAppName(null);
//			logModelDTO.setSignatureType(SignatureType.DATA.toString());
//			logModelDTO.seteSealUsed(false);
//			subscriberCertificates = subscriberCertificatesRepository.findByCertificateStatusAndsubscriberUniqueId(
//					CertificateStatus.ACTIVE.toString(), generateSignature.getSubscriberUniqueId());
//			RAServiceAsserts.notNullorEmpty(subscriberCertificates.size(), ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
//			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
//				if (generateSignature.getCertType() == 1
//						&& subscriberCertificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
//					subscriberCertificatePinHistory = pinHistoryRepository
//							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
//					generateSignature.setKeyId(subscriberCertificate.getPkiKeyId());
//					subscriberWrappedKey = subscriberWrappedKeyRepository
//							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
//					generateSignature.setWrappedKey(subscriberWrappedKey.getWrappedKey());
//					generateSignature.setCertificate(subscriberCertificate.getCertificateData());
//					generateSignature.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
//					subscriberCertificatePinHistory = pinHistoryRepository
//							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
//					authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
//					pinList = new LinkedList<String>(Arrays.asList(authenticationPin.split(", ")));
//					generateSignature.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
//					result = true;
//					break;
//				}
//				if (generateSignature.getCertType() == 0
//						&& subscriberCertificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
//					subscriberCertificatePinHistory = pinHistoryRepository
//							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
//					generateSignature.setKeyId(subscriberCertificate.getPkiKeyId());
//					subscriberWrappedKey = subscriberWrappedKeyRepository
//							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
//					generateSignature.setWrappedKey(subscriberWrappedKey.getWrappedKey());
//					generateSignature.setCertificate(subscriberCertificate.getCertificateData());
//					generateSignature.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
//					subscriberCertificatePinHistory = pinHistoryRepository
//							.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
//					signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
//					pinList = new LinkedList<String>(Arrays.asList(signingPin.split(", ")));
//					generateSignature.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
//					result = true;
//					break;
//				}
//			}
//			if (result == false)
//				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
//
//			logModelDTO.setCallStack(generateSignature.getGenerateSignatureData());
//
//			PostRequest issueCertificatePostRequest = new PostRequest();
//			issueCertificatePostRequest.setRequestBody(logModelDTO.toString());
//			issueCertificatePostRequest.setHashdata(logModelDTO.toString().hashCode());
//
//			RequestEntity requestEntity = new RequestEntity();
//			requestEntity.setPostRequest(issueCertificatePostRequest);
//			requestEntity.setTransactionType(Constant.GENERATE_SIGNATURE);
//			logger.info(
//					CLASS + " :: generateSignature() :: request :: " + requestEntity.getPostRequest().getRequestBody());
//			ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.PKIURL, requestEntity,
//					String.class);
//			if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
//				throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
//			if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
//				throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
//			ServiceResponse serviceResponse = objectMapper.readValue(new String(httpResponse.getBody()),
//					ServiceResponse.class);
//			logger.info(CLASS + " :: generateSignature() :: Native response :: " + serviceResponse.getStatus());
//			logModelDTO.setCallStack(null);
//			logModelDTO.setLogMessage(Constant.RESPONSE);
//			logModelDTO.setEndTime(NativeUtils.getTimeStampString());
//			if (serviceResponse.getStatus().equals(Constant.FAIL)) {
//				ErrorCodes.setResponse(serviceResponse);
//				logger.info(CLASS + " :: generateSignature() :: error :: " + serviceResponse.getError_message());
//				logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
//				logModelDTO.setServiceName(ServiceName.DIGITALLY_SIGNING_FAILED.toString());
//				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
//				rabbitMQSender.send(logModel);
//				throw new RAServiceException(serviceResponse.getError_message());
//			}
//			logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
//			LogModel logModel = NativeUtils.getLogModel(logModelDTO);
//			rabbitMQSender.send(logModel);
//			return CompletableFuture.completedFuture(serviceResponse.getSignature());
//		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
//				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
//			e.printStackTrace();
//			logger.error(CLASS + " setPin() :: IN DATABASE EXCEPTION {}", e.getMessage());
//			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
//					null, Locale.ENGLISH));
//		} catch (RAServiceException e) {
//			e.printStackTrace();
//			logger.error(CLASS + " setPin() :: IN RAServiceException {}", e.getMessage());
//			throw new RAServiceException(e.getMessage());
//		} catch (Exception e) {
//			e.printStackTrace();
//			logger.error(CLASS + " setPin() :: exception {} ", e.getMessage());
//			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
//					null, Locale.ENGLISH));
//		}
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dtt.ra.service.iface.RAPKIServiceIface#generateSignatureOrganiztion(com.
	 * dtt.ra. request.entity.GenerateSignature)
	 */
	@Override
	public String generateSignatureOrganization(GenerateSignature generateSignature)
			throws RAServiceException, Exception {
		try {
			logger.info(CLASS + ":: generateSignatureOrganization() :: request." + generateSignature.toString());

			OrganizationDetails organizationDetails = organizationDetailsRepository
					.findByOrganizationUid(generateSignature.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(organizationDetails, ErrorCodes.E_ORGANIZATION_DATA_NOT_FOUND);

			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setIdentifier(organizationDetails.getOrganizationUid());
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setServiceName(ServiceName.OTHER.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(generateSignature.getCorrelationId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(null);
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(SignatureType.DATA.toString());
			logModelDTO.seteSealUsed(false);

			OrganizationCertificates organizationCertificates = organizationCertificatesRepository
					.findByCertificateStatusAndOrganizationUniqueId(CertificateStatus.ACTIVE.toString(),
							generateSignature.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(organizationCertificates, ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			if ((generateSignature.getCertType() == 0)
					&& (organizationCertificates.getCertificateType().equals(CertificateType.SIGN.toString()))) {
				generateSignature.setKeyId(organizationCertificates.getPkiKeyId());

				OrganizationWrappedKey organizationWrappedKey = organizationWrappedKeyRepository
						.findBycertificateSerialNumber(organizationCertificates.getCertificateSerialNumber());

				generateSignature.setWrappedKey(organizationWrappedKey.getWrappedKey());
				generateSignature.setCertificate(organizationCertificates.getCertificateData());
				generateSignature.setSerialNumber(organizationCertificates.getCertificateSerialNumber());

				logModelDTO.setCallStack(generateSignature.getGenerateSignatureData());

				PostRequest issueCertificatePostRequest = new PostRequest();
				issueCertificatePostRequest.setRequestBody(logModelDTO.toString());
				issueCertificatePostRequest.setHashdata(logModelDTO.toString().hashCode());

				RequestEntity requestEntity = new RequestEntity();
				requestEntity.setPostRequest(issueCertificatePostRequest);
				requestEntity.setTransactionType(Constant.GENERATE_SIGNATURE);
				ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.PKIURL,
						requestEntity, String.class);
				if ((httpResponse.getBody()).equals(Constant.TRANSACTION_TYPE_NOT_FOUND)) {
					throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
				}
				if ((httpResponse.getBody()).equals(Constant.REQUEST_IS_NOT_VALID)) {
					throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
				}
				ServiceResponse serviceResponse = objectMapper.readValue(new String(httpResponse.getBody()),
						ServiceResponse.class);

				logger.info("RAPKIServiceIfaceImpl :: generateSignatureOrganization() :: Native response :: "
						+ serviceResponse.getStatus());
				logModelDTO.setCallStack(null);
				logModelDTO.setLogMessage(Constant.GENERATE_SIGNATURE);
				logModelDTO.setEndTime(NativeUtils.getTimeStampString());
				if (serviceResponse.getStatus().equals(Constant.FAIL)) {
					ErrorCodes.setResponse(serviceResponse);
					logger.info("RAPKIServiceIfaceImpl :: generateSignatureOrganization() :: error :: "
							+ serviceResponse.getError_message());
					logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
					logModelDTO.setServiceName(ServiceName.OTHER.toString());
					LogModel logModel = NativeUtils.getLogModel(logModelDTO);

					String msg = ErrorCodes
							.getErrorMessage(ErrorCodes.getErrorCode(serviceResponse.getError_message()));
					if (msg == null) {
						throw new RAServiceException(
								"Something went wrong. (Code : " + serviceResponse.getError_code() + ")");
					}
					throw new RAServiceException(msg);
				}
				LogModel logModel = NativeUtils.getLogModel(logModelDTO);

				return serviceResponse.getSignature();
			} else {
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " generateSignatureOrganization() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " generateSignatureOrganization() :: IN RAServiceException{}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " generateSignatureOrganization() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dtt.ra.service.iface.RAPKIServiceIface#authenticatePKI(com.dtt.ra.request
	 * .entity.AuthenticatePKIModel)
	 */
	@Override
	public String authenticatePKI(AuthenticatePKIModel authenticatePKIModel) throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: authenticatePKI() :: request.");
			subscriber = subscriberRepository.findBysubscriberUid(authenticatePKIModel.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			boolean result = false;
			logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setIdentifier(subscriber.getSubscriberUid());
			logModelDTO.setServiceName(ServiceName.PKI_AUTHENTICATED.toString());
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(authenticatePKIModel.getCorrelationId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(null);
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(null);
			logModelDTO.seteSealUsed(false);
			subscriberCertificates = subscriberCertificatesRepository.findByCertificateStatusAndsubscriberUniqueId(
					CertificateStatus.ACTIVE.toString(), authenticatePKIModel.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriberCertificates.size(), ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
				if (authenticatePKIModel.getCertType() == 1
						&& subscriberCertificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
					authenticatePKIModel.setKeyId(subscriberCertificate.getPkiKeyId());
					authenticatePKIModel.setCertificate(subscriberCertificate.getCertificateData());
					result = true;
					break;
				}
				if (authenticatePKIModel.getCertType() == 0
						&& subscriberCertificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
					authenticatePKIModel.setKeyId(subscriberCertificate.getPkiKeyId());
					authenticatePKIModel.setCertificate(subscriberCertificate.getCertificateData());
					result = true;
					break;
				}
			}
			if (result == false)
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);

			logModelDTO.setCallStack(authenticatePKIModel.getauthenticatePKIData());
			PostRequest issueCertificatePostRequest = new PostRequest();
			issueCertificatePostRequest.setRequestBody(logModelDTO.toString());
			issueCertificatePostRequest.setHashdata(logModelDTO.toString().hashCode());

			RequestEntity requestEntity = new RequestEntity();
			requestEntity.setPostRequest(issueCertificatePostRequest);
			requestEntity.setTransactionType(Constant.AUTHENTICATE_PKI);
			logger.info(
					CLASS + " :: authenticatePKI() :: request :: " + requestEntity.getPostRequest().getRequestBody());
			ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.PKIURL, requestEntity,
					String.class);
			if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
				throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
			if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
				throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
			ServiceResponse serviceResponse = objectMapper.readValue(new String(httpResponse.getBody()),
					ServiceResponse.class);
			logger.info(CLASS + " :: authenticatePKI() :: Native response :: " + serviceResponse.getStatus());
			logModelDTO.setCallStack(null);
			logModelDTO.setLogMessage(Constant.RESPONSE);
			logModelDTO.setEndTime(NativeUtils.getTimeStampString());
			if (serviceResponse.getStatus().equals(Constant.FAIL)) {
				ErrorCodes.setResponse(serviceResponse);
				logger.info(CLASS + " :: authenticatePKI :: error :: " + serviceResponse.getError_message());
				logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
				rabbitMQSender.send(logModel);
				throw new RAServiceException(serviceResponse.getError_message());
			}
			logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
			LogModel logModel = NativeUtils.getLogModel(logModelDTO);
			rabbitMQSender.send(logModel);
			return serviceResponse.getStatus();
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " authenticatePKI() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " authenticatePKI() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " authenticatePKI() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}
	@Override
	public String setPins(SetPinModelDto setPinModel) throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: setPin() :: request :: " + setPinModel.toString());
			subscriber = subscriberRepository.findBysubscriberUid(setPinModel.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			subscriberCertificates = subscriberCertificatesRepository.findByCertificateStatusAndsubscriberUniqueId(
					CertificateStatus.ACTIVE.toString(), setPinModel.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriberCertificates.size(), ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
				certificate = subscriberCertificate;
				subscriberCertificatePinHistory = pinHistoryRepository
						.findBysubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
				if (subscriberCertificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
					if (null != subscriberCertificatePinHistory) {
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
						if (null != authenticationPin)
							NativeUtils.checkCurrentPassword(authenticationPin, setPinModel.getSigningPassword(),
									ErrorCodes.E_NEW_SIGNING_PIN_MATCHED_WITH_CURRENT_AUTHENTICATION_PIN);
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());

					// setPinModel.setSignWrappedKey(subscriberWrappedKey.getWrappedKey());
					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(setPinModel.getSubscriberUniqueId());

					if (null == subscriberCertificatePinHistory)
						subscriberCertificatePinHistory = new SubscriberCertificatePinHistory();

					subscriberCertificatePinHistory.setSigningCertificatePinList(setPinModel.getSigningPassword());
					subscriberCertificatePinHistory.setSignPinSetDate(NativeUtils.getTimeStamp());
					subscriberCertificatePinHistory.setSubscriberUniqueId(setPinModel.getSubscriberUniqueId());
					subscriberCertificatePinHistory.setAuthPinSetDate(
							Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
					);
					subscriberCertificatePinHistory.setSignPinSetDate(
							Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
					);
					pinHistoryRepository.save(subscriberCertificatePinHistory);

//					subscriberWrappedKey = subscriberWrappedKeyRepository
//							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
//					subscriberWrappedKey.setWrappedKey(subscriberWrappedKey.getWrappedKey());
//					System.out.println("wraapped key"+subscriberWrappedKey+"value");
//					subscriberWrappedKeyRepository.save(subscriberWrappedKey);

// 🔹 Fetch SubscriberWrappedKey record from DB
					SubscriberWrappedKey subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());

// 🔹 Check if record exists
					if (subscriberWrappedKey != null) {
						// Log current wrapped key value
						System.out.println("Wrapped Key (Before Update): " + subscriberWrappedKey.getWrappedKey());

						// Update the wrapped key (if needed)
						subscriberWrappedKey.setWrappedKey(subscriberWrappedKey.getWrappedKey());

						// Save back to DB
						subscriberWrappedKeyRepository.save(subscriberWrappedKey);
						System.out.println("Wrapped Key record saved successfully.");
					}

					if (null != subscriberCertificatePinHistory.getSignPinSetDate()
							&& null != subscriberCertificatePinHistory.getAuthPinSetDate()) {
						SubscriberStatus subscriberStatus = subscriberStatusRepository
								.findBysubscriberUid(setPinModel.getSubscriberUniqueId());
						subscriberStatus.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
						subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
						subscriberStatus.setSubscriberStatusDescription(Constant.SET_PIN_SUCCESS);
						subscriberStatusRepository.save(subscriberStatus);
					}
					logger.info(CLASS + " :: setPin()  :: success.");

				}
				if (subscriberCertificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
					if (null != subscriberCertificatePinHistory) {
						signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						if (null != signingPin)
							NativeUtils.checkCurrentPassword(signingPin, setPinModel.getAuthPassword(),
									ErrorCodes.E_NEW_AUTHENTICATION_PIN_MATCHED_WITH_CURRENT_SIGNING_PIN);
						authenticationPin = subscriberCertificatePinHistory.getAuthenticationCertificatePinList();
					}
					subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());

					// setPinModel.setAuthWrappedKey(subscriberWrappedKey.getWrappedKey());

					subscriberCertificatePinHistory = pinHistoryRepository
							.findBysubscriberUniqueId(setPinModel.getSubscriberUniqueId());
//					if (null == subscriberCertificatePinHistory)
//						subscriberCertificatePinHistory = new SubscriberCertificatePinHistory();
//
//					subscriberCertificatePinHistory.setAuthenticationCertificatePinList(setPinModel.getAuthPassword());
//					subscriberCertificatePinHistory.setAuthPinSetDate(NativeUtils.getTimeStamp());
//					subscriberCertificatePinHistory.setSubscriberUniqueId(setPinModel.getSubscriberUniqueId());
//
//					//subscriberCertificatePinHistory.setAuthPinSetDate(LocalDateTime.now());
//					subscriberCertificatePinHistory.setAuthPinSetDate(
//							Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
//					);

					if (null == subscriberCertificatePinHistory)
						subscriberCertificatePinHistory = new SubscriberCertificatePinHistory();

					subscriberCertificatePinHistory.setAuthenticationCertificatePinList(setPinModel.getAuthPassword());
					subscriberCertificatePinHistory.setSubscriberUniqueId(setPinModel.getSubscriberUniqueId());

					pinHistoryRepository.save(subscriberCertificatePinHistory);

//					subscriberWrappedKey = subscriberWrappedKeyRepository
//							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
//
//					// subscriberWrappedKey.setWrappedKey(serviceResponse.getWrappedKey());
//					subscriberWrappedKey.setWrappedKey(subscriberWrappedKey.getWrappedKey());
//
//					subscriberWrappedKeyRepository.save(subscriberWrappedKey);

					SubscriberWrappedKey subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());

// 🔹 Check if record exists
					if (subscriberWrappedKey != null) {
						// Log current wrapped key value
						System.out.println("Wrapped Key (Before Update): " + subscriberWrappedKey.getWrappedKey());

						// Update the wrapped key (if needed)
						subscriberWrappedKey.setWrappedKey(subscriberWrappedKey.getWrappedKey());

						// Save back to DB
						subscriberWrappedKeyRepository.save(subscriberWrappedKey);
						System.out.println("Wrapped Key record saved successfully.");
					}
					if (null != subscriberCertificatePinHistory.getSignPinSetDate()
							&& null != subscriberCertificatePinHistory.getAuthPinSetDate()) {
						SubscriberStatus subscriberStatus = subscriberStatusRepository
								.findBysubscriberUid(setPinModel.getSubscriberUniqueId());
						subscriberStatus.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
						subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
						subscriberStatus.setSubscriberStatusDescription(Constant.SET_PIN_SUCCESS);
						subscriberStatusRepository.save(subscriberStatus);
					}
					logger.info(CLASS + " :: setPin()  :: success.");
				}

			}
			return Constant.SUCCESS;
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				 | PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " setPin() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}
}
