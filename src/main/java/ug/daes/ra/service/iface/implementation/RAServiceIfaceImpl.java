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
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ug.daes.DAESService;
import ug.daes.Result;
import ug.daes.ra.asserts.RAServiceAsserts;
import ug.daes.ra.config.SentryClientExceptions;
import ug.daes.ra.dto.*;
import ug.daes.ra.dto.EmailReqDto;
import ug.daes.ra.enums.CertificateStatus;
import ug.daes.ra.enums.CertificateType;
import ug.daes.ra.enums.LogMessageType;
import ug.daes.ra.enums.RevokeReason;
import ug.daes.ra.enums.ServiceName;
import ug.daes.ra.enums.TransactionType;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.exception.RAServiceException;
import ug.daes.ra.model.*;
import ug.daes.ra.repository.iface.*;
import ug.daes.ra.request.entity.*;
import ug.daes.ra.response.entity.APIResponse;
import ug.daes.ra.response.entity.CertificateData;
import ug.daes.ra.response.entity.ServiceResponse;
import ug.daes.ra.service.iface.RAServiceIface;
import ug.daes.ra.utils.AppUtil;
import ug.daes.ra.utils.Constant;
import ug.daes.ra.utils.NativeUtils;
import ug.daes.ra.utils.PropertiesConstants;
import ug.daes.ra.utils.KafkaSender;

/**
 * The Class RAServiceIfaceImpl.
 */
@Service
@EnableScheduling
public class RAServiceIfaceImpl implements RAServiceIface {

	/** The Constant CLASS. */
	final static private String CLASS = "RAServiceIfaceImpl";

	/** The Constant objectMapper. */
	final static private ObjectMapper objectMapper = new ObjectMapper();

	/** The Constant logger. */
	final static private Logger logger = LoggerFactory.getLogger(RAServiceIfaceImpl.class);

	boolean notificationSent = false;

	int emailSentCount = 0;

	/** The rest template. */
	@Autowired
	RestTemplate restTemplate;

	/** The rabbit MQ sender. */
	@Autowired
	KafkaSender rabbitMQSender;

	/** The subscriber certificates repository. */
	@Autowired
	SubscriberCertificatesRepository subscriberCertificatesRepository;

	@Autowired
	OrganizationCertificatesRepository organizationCertificatesRepository;

	@Autowired
	OrganizationDetailsRepository organizationDetailsRepository;

	/** The subscriber certificate pin history repository. */
	@Autowired
	SubscriberCertificatePinHistoryRepository subscriberCertificatePinHistoryRepository;

	/** The subscriber RA data repository. */
	@Autowired
	SubscriberRADataRepository subscriberRADataRepository;

	/** The subscriber certificate life cycle repository. */
	@Autowired
	SubscriberCertificateLifeCycleRepository subscriberCertificateLifeCycleRepository;

	/** The subscriber repository. */
	@Autowired
	SubscriberRepository subscriberRepository;

	/** The subscriber status repository. */
	@Autowired
	SubscriberStatusRepository subscriberStatusRepository;

	/** The subscriber fcm token repository. */
	@Autowired
	SubscriberFcmTokenRepository subscriberFcmTokenRepository;

	/** The subscriber wrapped key repository. */
	@Autowired
	SubscriberWrappedKeyRepository subscriberWrappedKeyRepository;
	@Autowired
	MessageSource messageSource;


	@Autowired
	SentryClientExceptions sentryClientExceptions;


//	@Autowired
//	VisitorCompleteDetailsRepository visitorCompleteDetailsRepository;

//	@Autowired
//	SubscriberTravelHistoryRepo subscriberTravelHistoryRepo;

	/** base url for send email **/
//	@Value(value = "${email.url}")
	private String emailBaseUrl;
	/**
	 * Issue certificate.
	 *
	 * @param raRequestModel the ra request model
	 * @return the string
	 * @throws RAServiceException the RA service exception
	 * @throws Exception          the exception
	 * @return the string
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dt.ra.service.iface.RAServiceIface#issueCertificate(com.dt.ra.service
	 * .requestentity.IssueCertificateRequest)
	 */
	@Override
	public String issueCertificate(RARequestDTO raRequestModel) throws RAServiceException, Exception {
		try {
			sentryClientExceptions.captureTags(raRequestModel.getSubscriberUniqueId(),null,"issueCertificate","RAServiceIfaceImpl");
			logger.info(CLASS + " :: issueCertificate() :: request :: raRequestModel ::" + raRequestModel);
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(raRequestModel.getSubscriberUniqueId());
			logger.info(CLASS + " :: issueCertificate() :: request :: subscriberData :: uid :: "
					+ subscriber.getSubscriberUid() + " :: data :: " + subscriber);
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setEndTime(null);
			logModelDTO.setIdentifier(subscriber.getSubscriberUid());
			logModelDTO.setServiceName(ServiceName.CERTIFICATE_GENERATED.toString());
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(NativeUtils.getUUId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(raRequestModel.getGeoLocation());
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(null);
			logModelDTO.seteSealUsed(false);

//			int certificateCount = 0;
//			certificateCount = subscriberCertificatesRepository.getAllCertificateCount();
//			if(certificateCount >= 499500 && emailSentCount<=10){
//				//send mail
//				EmailReqDto emailReqDto = new EmailReqDto();
//				String mail = emailUser;
//				String[] mailList = mail.split(",");
//				for(int i =0 ; i< mailList.length ; i++){
//					emailReqDto.setEmailId(mail);
//					emailReqDto.setOrg(true);
//					sendEmail(emailReqDto);
//				}
//			}

			SubscriberStatus subscriberStatus = subscriberStatusRepository
					.findBysubscriberUid(raRequestModel.getSubscriberUniqueId());
			logger.info(CLASS + " :: issueCertificate() :: request :: statusdata :: uid :: "
					+ subscriber.getSubscriberUid() + " :: data :: " + subscriberStatus.getSubscriberStatus());
			RAServiceAsserts.notNullorEmpty(subscriberStatus, ErrorCodes.E_SUBSCRIBER_STATUS_DATA_NOT_FOUND);
			if (!subscriberStatus.getSubscriberStatus().equals(Constant.CERT_GENERATING))
				throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_NOT_ONBOARDED);
			SubscriberRaData subscriberRaData = subscriberRADataRepository
					.findBysubscriberUniqueId(raRequestModel.getSubscriberUniqueId());
			logger.info(CLASS + " :: issueCertificate() :: request :: raRequestData :: uid :: "
					+ subscriber.getSubscriberUid() + " :: data :: " + subscriberRaData);
			RAServiceAsserts.notNullorEmpty(subscriberRaData, ErrorCodes.E_SUBSCRIBER_RA_DATA_NOT_FOUND);
			List<SubscriberCertificates> subscriberCertificates = subscriberCertificatesRepository
					.findByCertificateStatusAndsubscriberUniqueId(CertificateStatus.ACTIVE.toString(),
							raRequestModel.getSubscriberUniqueId());
			if (subscriberCertificates.size() == 2)
				throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_CERTIFICATES_ARE_ACTIVE);
			if (subscriberCertificates.size() == 0) {
				String keyId = NativeUtils.generatePKIKeyId();
				IssueCertificateRequest issueCertificateRequest = new IssueCertificateRequest();
				issueCertificateRequest.setSubscriberUniqueId(raRequestModel.getSubscriberUniqueId());
				issueCertificateRequest.setKeyID(keyId);

//				String  commonName = subscriberRaData.getCommonName();
//				String[] splited = commonName.split("\\s+");
//				System.out.println("OriganalName :: " + commonName);
//				commonName=splited[0];
//				System.out.println("ModifiedName ::" + commonName);

				issueCertificateRequest.setCommonName(subscriberRaData.getCommonName());
				issueCertificateRequest.setCountryName(subscriberRaData.getCountryName());

				PostRequest issueCertificatePostRequest = new PostRequest();
				issueCertificatePostRequest.setRequestBody(issueCertificateRequest.toString());
				issueCertificatePostRequest.setHashdata(issueCertificateRequest.toString().hashCode());
				issueCertificatePostRequest.setPkiKeyID(keyId);
				issueCertificatePostRequest.setCertificateType(CertificateType.SIGN.toString());
				issueCertificatePostRequest.setCallbackURI(PropertiesConstants.ISSUECERTIFICATECALLBACKURL);
				String response = processRequest(PropertiesConstants.PKIURL, issueCertificatePostRequest, subscriber,
						logModelDTO);
				if (response.equals(Constant.SUCCESS)) {
					logger.info(CLASS + " :: issueCertificate :: Request for issuing Sign process Successfully");
					String keyId2 = NativeUtils.generatePKIKeyId();
					issueCertificateRequest.setKeyID(keyId2);
					issueCertificatePostRequest.setRequestBody(issueCertificateRequest.toString());
					issueCertificatePostRequest.setPkiKeyID(keyId2);
					issueCertificatePostRequest.setHashdata(issueCertificateRequest.toString().hashCode());
					issueCertificatePostRequest.setCertificateType(CertificateType.AUTH.toString());
					String response2 = processRequest(PropertiesConstants.PKIURL, issueCertificatePostRequest,
							subscriber, logModelDTO);
					if (response2.equals(Constant.SUCCESS)) {
						logger.info(CLASS + " :: issueCertificate() :: Request for issuing Auth process Successfully.");
						return Constant.REQUEST_FOR_ISSUING_SIGN_AND_AUTH_PROCESS_SUCCESSFULLY;
					} else
						throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_ISSUE_AUTHENTICATION_CERTIFICATE_FAILED);
				} else
					throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_ISSUE_SIGNING_CERTIFICATE_FAILED);
			} else if (subscriberCertificates.size() == 1) {
				for (SubscriberCertificates certificates : subscriberCertificates) {
					String keyId = NativeUtils.generatePKIKeyId();
					IssueCertificateRequest issueCertificateRequest = new IssueCertificateRequest();
					issueCertificateRequest.setSubscriberUniqueId(raRequestModel.getSubscriberUniqueId());
					issueCertificateRequest.setKeyID(keyId);

//					String  commonName = subscriberRaData.getCommonName();
//					String[] splited = commonName.split("\\s+");
//					System.out.println("OriganalName :: " + commonName);
//					commonName=splited[0];
//					System.out.println("ModifiedName ::" + commonName);

					issueCertificateRequest.setCommonName(subscriberRaData.getCommonName());
					issueCertificateRequest.setCountryName(subscriberRaData.getCountryName());
					PostRequest issueCertificatePostRequest = new PostRequest();
					issueCertificatePostRequest.setRequestBody(issueCertificateRequest.toString());
					issueCertificatePostRequest.setHashdata(issueCertificateRequest.toString().hashCode());
					issueCertificatePostRequest.setPkiKeyID(keyId);
					if (certificates.getCertificateType().equals(CertificateType.AUTH.toString()))
						issueCertificatePostRequest.setCertificateType(CertificateType.SIGN.toString());
					else
						issueCertificatePostRequest.setCertificateType(CertificateType.AUTH.toString());
					issueCertificatePostRequest.setCallbackURI(PropertiesConstants.ISSUECERTIFICATECALLBACKURL);
					String response = processRequest(PropertiesConstants.PKIURL, issueCertificatePostRequest,
							subscriber, logModelDTO);
					if (response.equals(Constant.SUCCESS)) {
						logger.info(CLASS + " :: issueCertificate() :: Request for issuing"
								+ issueCertificatePostRequest.getCertificateType() + " process Successfully");
						return Constant.REQUEST_FOR_ISSUING_SIGN_AND_AUTH_PROCESS_SUCCESSFULLY;
					} else {
						if (certificates.getCertificateType().equals(CertificateType.AUTH.toString()))
							throw new RAServiceException(
									ErrorCodes.E_SUBSCRIBER_ISSUE_AUTHENTICATION_CERTIFICATE_FAILED);
						else
							throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_ISSUE_SIGNING_CERTIFICATE_FAILED);
					}
				}
			}
			throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_CERTIFICATES_ARE_ACTIVE);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + " issueCertificate() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + " issueCertificate() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + " issueCertificate() ::  EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/**
	 * Process request.
	 *
	 * @param baseUrl                     the base url
	 * @param issueCertificatePostRequest the issue certificate post request
	 * @param subscriber                  the subscriber
	 * @param logModelDTO                 the log model DTO
	 * @return the string
	 * @throws Exception the exception
	 */
	private String processRequest(String baseUrl, PostRequest issueCertificatePostRequest, Subscriber subscriber,
			LogModelDTO logModelDTO) throws Exception {
		try {
			logger.info(CLASS + " :: issuecertificate() :: processRequest().");
			logModelDTO.setTimestamp(null);
			logModelDTO.setCallStack(issueCertificatePostRequest.getRequestBody());
			logModelDTO.setChecksum(null);
			issueCertificatePostRequest.setRequestBody(logModelDTO.toString());
			issueCertificatePostRequest.setHashdata(logModelDTO.toString().hashCode());
			RequestEntity requestEntity = new RequestEntity();
			requestEntity.setPostRequest(issueCertificatePostRequest);
			requestEntity.setTransactionType(Constant.ISSUE_CERTIFICATE);
			logger.info(CLASS + " issuecertificate() :: processRequest() :: requestBody. "
					+ requestEntity.getPostRequest().getRequestBody());
			ResponseEntity<String> httpResponse = restTemplate.postForEntity(baseUrl, requestEntity, String.class);
			logger.info(
					CLASS + " issuecertificate() :: processRequest() :: native response. " + httpResponse.getBody());
			RAServiceAsserts.notNullorEmpty(httpResponse, ErrorCodes.E_RA_POST_REQUEST_FAILED);
			if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
				throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
			if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
				throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
			String totalTime = NativeUtils.getTotalTime(logModelDTO.getStartTime(), NativeUtils.getTimeStampString());
			logModelDTO.setLogMessage("Total Time Taken " + totalTime + " seconds");
			logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
			logModelDTO.setEndTime(NativeUtils.getTimeStampString());
			logModelDTO.setCallStack(null);
			LogModel logModel = NativeUtils.getLogModel(logModelDTO);
			rabbitMQSender.send(logModel);
			return Constant.SUCCESS;
		} catch (RAServiceException e) {
			logger.error(CLASS + " :: processRequest :: RAServiceException :: " + e.getMessage());
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}

	}

	/**
	 * Check certificate status.
	 *
	 * @return the string
	 * @throws RAServiceException the RA service exception
	 * @throws Exception          the exception
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see com.dt.ra.service.iface.RAServiceIface#checkCertificateStatus(java.lang.
	 * String)
	 */
	@Override
	@Scheduled(cron = "0 0 0 * *  ?")
	public String checkCertificateStatus() throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: checkCertificateStatus() :: request :: ");
			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			List<SubscriberCertificates> subscriberCertificates = subscriberCertificatesRepository
					.findByCertificateStatusExpired();

			logModelDTO.setServiceName(null);
			logModelDTO.setLogMessage(Constant.REQUEST);
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(NativeUtils.getUUId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(null);
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(null);
			logModelDTO.seteSealUsed(false);
			CheckCertificateStatus checkCertificateStatus = new CheckCertificateStatus();
			for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
				logModelDTO.setIdentifier(subscriberCertificate.getSubscriberUniqueId());
				checkCertificateStatus.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
				checkCertificateStatus.setCertificate(subscriberCertificate.getCertificateData());
				String requestbody = checkCertificateStatus.toString();
				logModelDTO.setCallStack(requestbody);
				logModelDTO.setCallStack(null);
				logModelDTO.setLogMessage(Constant.RESPONSE);
				subscriberCertificate.setCertificateStatus(CertificateStatus.EXPIRED.toString());
				subscriberCertificate.setUpdatedDate(NativeUtils.getTimeStamp());
				subscriberCertificatesRepository.save(subscriberCertificate);
				SubscriberCertificateLifeCycle subscriberCertificateLifeCycle = new SubscriberCertificateLifeCycle();
				subscriberCertificateLifeCycle
						.setCertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
				subscriberCertificateLifeCycle.setCertificateStatus(CertificateStatus.EXPIRED.toString());
				subscriberCertificateLifeCycle.setSubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
				subscriberCertificateLifeCycle.setCertificateType(subscriberCertificate.getCertificateType());
				subscriberCertificateLifeCycle.setCreationDate(NativeUtils.getTimeStamp());
				subscriberCertificateLifeCycleRepository.save(subscriberCertificateLifeCycle);
				SubscriberStatus subscriberStatus = subscriberStatusRepository
						.findBysubscriberUid(subscriberCertificate.getSubscriberUniqueId());
				subscriberStatus.setSubscriberStatus(Constant.CERT_EXPIRED);
				subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
				subscriberStatus.setSubscriberStatusDescription(Constant.CERTIFICATES_ARE_EXPIRED);
				subscriberStatusRepository.save(subscriberStatus);

				Subscriber subscriber = subscriberRepository
						.findBysubscriberUid(subscriberCertificate.getSubscriberUniqueId());
				String subscriberFcmToken = subscriberFcmTokenRepository
						.findBysubscriberUid(subscriberCertificate.getSubscriberUniqueId());

				NotificationContextDTO notificationContextDTO = new NotificationContextDTO();
				notificationContextDTO.setpREF_CERTIFICATE_STATUS(CertificateStatus.EXPIRED);

				NotificationDataDTO notificationDataDTO = new NotificationDataDTO();
				notificationDataDTO.setTitle(Constant.HI + subscriber.getFullName());
				notificationDataDTO.setBody(
						Constant.YOUR + subscriberCertificate.getCertificateType() + Constant.CERTIFIACTE_IS_EXPIRED);
				notificationDataDTO.setNotificationContext(notificationContextDTO);

				PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
				pushNotificationRequest.setTo(subscriberFcmToken);
				pushNotificationRequest.setPriority(Constant.HIGH);
				pushNotificationRequest.setData(notificationDataDTO.getExpiredCertNotificationData());

//				String request1 = "{" + "\"requestBody\":" + pushNotificationRequest.getNotificationRquest() + ","
//						+ "\"serviceMethod\":\"setSendNotification\"" + "}";

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				HttpEntity<String> entity = new HttpEntity<String>(pushNotificationRequest.getNotificationRquest(), headers);
				if (notificationSent == false) {
					try {
						ResponseEntity<String> httpResponse1 = restTemplate
								.postForEntity(PropertiesConstants.NOTIFICATION, entity, String.class);
						notificationSent = true;
					} catch (Exception e) {
						System.out.println("FCM Notification failed.");
					}
				}
				
//				if(subscriber.getEmailId() != null) {
//					APIResponse apiResponse = sendEmail(subscriber.getEmailId());
//				}
				

				logModelDTO.setLogMessage(Constant.RESPONSE);
				logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
				logModelDTO.setEndTime(NativeUtils.getTimeStampString());
				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
				try {
					rabbitMQSender.send(logModel);
				} catch (Exception e) {
					System.out.println("Service log failed ");
				}
			}
			logger.info(CLASS + " :: checkCertificateStatus() :: status :: completed.");

			return Constant.COMPLETED;
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " checkCertificateStatus() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " checkCertificateStatus() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " checkCertificateStatus() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}
	
	
	public APIResponse sendEmail(String email) {
		try {
			EmailReqDto emailReqDto = new EmailReqDto();
			emailReqDto.setUserSubscription(true);
			emailReqDto.setEmailId(email);
			String url = emailBaseUrl;
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestEntity = new HttpEntity<>(emailReqDto, headers);

			logger.info(CLASS + " sendEmailToSpoc() requestEntity >> " + requestEntity);
			ResponseEntity<ApiResponse> res = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					ApiResponse.class);

			logger.info(CLASS + " sendEmailToSpoc() res >> " + res);
			if (res.getStatusCodeValue() == 200) {
				return new APIResponse(true, "email send successfully", res.toString());
			} else if (res.getStatusCodeValue() == 400) {
				return new APIResponse(false, "Bad Request", null);
			} else if (res.getStatusCodeValue() == 500) {
				return  new APIResponse(false, "Internal server error", null);
			}else {
				return new APIResponse(false,messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",null, Locale.ENGLISH), null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new APIResponse(false,messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",null, Locale.ENGLISH), null);
		}
	}
//					logger.info(CLASS + " :: checkCertificateStatus() :: response :: " + httpResponse1.getBody());
//					String baseUrl = PropertiesConstants.PKIURL;
//					PostRequest request = new PostRequest();
//					request.setRequestBody(logModelDTO.toString());
//					request.setHashdata(logModelDTO.toString().hashCode());
//					RequestEntity requestEntity = new RequestEntity();
//					requestEntity.setPostRequest(request);
//					requestEntity.setTransactionType("CertificateStatus");
//					ResponseEntity<String> httpResponse = restTemplate.postForEntity(baseUrl, requestEntity,
//							String.class);
//					if (httpResponse.getBody().equals("Transaction type not found"))
//						throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
//					if (httpResponse.getBody().equals("Request is not Valid"))
//						throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
//					logger.info(CLASS + " :: checkCertificateStatus() :: native response :: " + httpResponse.getBody());
//					ServiceResponse serviceResponse = objectMapper.readValue(httpResponse.getBody(),
//							ServiceResponse.class);

//					if (serviceResponse.getStatus().equals("fail")) {
//						logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
//						logModelDTO.setEndTime(NativeUtils.getTimeStampString());
//						LogModel logModel = NativeUtils.getLogModel(logModelDTO);
//						rabbitMQSender.send(logModel);
//						logger.info(CLASS + " :: checkCertificateStatus() :: error :: "
//								+ serviceResponse.getError_message());
//					}
//					if (serviceResponse.getStatus().equals("success")
//							&& serviceResponse.getCertificate_status().equals("Expired")) {

//					if (serviceResponse.getStatus().equals("success")
//							&& serviceResponse.getCertificate_status().equals("valid")
//							&& (!(serviceResponse.getRevocation_reason().equals("not Revoked")))) {
//						logger.info(CLASS + " :: checkCertificateStatus() :: status :: "
//								+ serviceResponse.getCertificate_status());
//					} else
//						logger.info(CLASS + " :: checkCertificateStatus() :: Reason :: "
//								+ serviceResponse.getRevocation_reason());

	/**
	 * Revoke certificate.
	 *
	 * @param requestBody the request body
	 * @return the string
	 * @throws Exception the exception
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see com.dt.ra.service.iface.RAServiceIface#revokeCertificate(com.dt.ra.
	 * service.requestentity.RevokeCertificateRequest)
	 */
	@Override
	public String revokeCertificate(RARequestDTO requestBody) throws Exception {
		try {
			logger.info(CLASS + " :: revokeCertificate() :: request :: " + requestBody.toString());
			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setIdentifier(requestBody.getSubscriberUniqueId());
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(requestBody.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			SubscriberStatus subscriberStatus = subscriberStatusRepository
					.findBysubscriberUid(requestBody.getSubscriberUniqueId());
			RAServiceAsserts.notNullorEmpty(subscriberStatus, ErrorCodes.E_SUBSCRIBER_STATUS_DATA_NOT_FOUND);
			List<SubscriberCertificates> subscriberCertificates = subscriberCertificatesRepository
					.findByCertificateStatusAndsubscriberUniqueId(CertificateStatus.ACTIVE.toString(),
							requestBody.getSubscriberUniqueId());
			
			if (subscriberCertificates.size() == 0) {
				throw new RAServiceException(ErrorCodes.E_SUBSCRIBER_CERTIFICATES_ARE_REVOKED);
			} else {
				boolean result = false;
				String revokeRequest = null;
				for (SubscriberCertificates subscriberCertificate : subscriberCertificates) {
					switch (requestBody.getReasonId()) {
					case "1":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.KEY_COMPROMISED.toString());
						break;
					case "-2":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.NO_REASON.toString());
					case "3":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.AFFILIATION_CHANGED.toString());
						break;
					case "4":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.SUPERSEDED.toString());
						break;
					case "5":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.CESSATION_OF_OPERATION.toString());
						break;
					case "6":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.CERTIFICATE_HOLD.toString());
						break;
					case "9":
						requestBody.setReasonId(requestBody.getReasonId());
						subscriberCertificate.setRevocationReason(RevokeReason.PRIVILEGE_WITHDRAWN.toString());
						break;
					default:
						throw new RAServiceException(ErrorCodes.E_REVOKE_REASON_NOT_FOUND);
					}
					logModelDTO.setServiceName(ServiceName.CERTIFICATE_REVOKED.toString());
					logModelDTO.setLogMessage(Constant.REQUEST);
					logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
					logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
					logModelDTO.setTransactionSubType(null);
					logModelDTO.setCorrelationID(NativeUtils.getUUId());
					logModelDTO.setTransactionID(NativeUtils.getUUId());
					logModelDTO.setSubTransactionID(null);
					logModelDTO.setGeoLocation(requestBody.getGeoLocation());
					logModelDTO.setServiceProviderName(null);
					logModelDTO.setServiceProviderAppName(null);
					logModelDTO.setSignatureType(null);
					logModelDTO.seteSealUsed(false);
					logModelDTO.setEndTime(null);
					logModelDTO.setTimestamp(null);

					RevokeCertificateRequest revokeCertificateRequest = new RevokeCertificateRequest();
					revokeCertificateRequest.setReasonId(requestBody.getReasonId());
					revokeCertificateRequest.setSerialNumber(subscriberCertificate.getCertificateSerialNumber());
					revokeRequest = revokeCertificateRequest.toString();
					logModelDTO.setCallStack(revokeRequest);

					logger.info(CLASS + " :: revokeCertificate() :: native request :: " + revokeRequest);
					String baseUrl = PropertiesConstants.PKIURL;
					PostRequest request = new PostRequest();
					request.setRequestBody(logModelDTO.toString());
					request.setHashdata(logModelDTO.toString().hashCode());
					RequestEntity requestEntity = new RequestEntity();
					requestEntity.setPostRequest(request);
					requestEntity.setTransactionType(Constant.REVOKE_CERTIFICATE);
					ResponseEntity<String> httpResponse = restTemplate.postForEntity(baseUrl, requestEntity,
							String.class);
					if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
						throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
					if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
						throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
					logger.info(CLASS + " :: revokeCertificate() :: native response :: " + httpResponse.getBody());
					ServiceResponse serviceResponse = objectMapper.readValue(httpResponse.getBody(),
							ServiceResponse.class);
					logModelDTO.setCallStack(null);
					logModelDTO.setLogMessage(Constant.RESPONSE);
					if (serviceResponse.getStatus().equals(Constant.FAIL)) {
						logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
						logModelDTO.setEndTime(NativeUtils.getTimeStampString());
						LogModel logModel = NativeUtils.getLogModel(logModelDTO);
						rabbitMQSender.send(logModel);
						ErrorCodes.setResponse(serviceResponse);
						throw new RAServiceException(serviceResponse.getError_message());
					} else {
						subscriberCertificate.setCertificateStatus(CertificateStatus.REVOKED.toString());
						subscriberCertificate.setUpdatedDate(NativeUtils.getTimeStamp());
						subscriberCertificatesRepository.save(subscriberCertificate);
						
						SubscriberCertificateLifeCycle subscriberCertificateLifeCycle= new SubscriberCertificateLifeCycle();
						
						subscriberCertificateLifeCycle
								.setCertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
						subscriberCertificateLifeCycle.setCertificateStatus(CertificateStatus.REVOKED.toString());
						subscriberCertificateLifeCycle.setRevokedReason(subscriberCertificate.getRevocationReason());
						subscriberCertificateLifeCycle
								.setSubscriberUniqueId(subscriberCertificate.getSubscriberUniqueId());
						subscriberCertificateLifeCycle.setCertificateType(subscriberCertificate.getCertificateType());
						subscriberCertificateLifeCycle.setCreationDate(NativeUtils.getTimeStamp());
						subscriberCertificateLifeCycleRepository.save(subscriberCertificateLifeCycle);
						result = true;
					}
				}
				if (result) {
					subscriberStatus.setSubscriberStatus(Constant.CERT_REVOKED);
					subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
					subscriberStatus.setSubscriberStatusDescription(Constant.CERTIFICATES_ARE_REVOKED_SUCCESSFULLY);
					subscriberStatusRepository.save(subscriberStatus);

					String subscriberFcmToken = subscriberFcmTokenRepository
							.findBysubscriberUid(requestBody.getSubscriberUniqueId());
					logger.info(CLASS + " :: revokeCertificate() :: fcmToken :: " + subscriberFcmToken);
					NotificationContextDTO notificationContextDTO = new NotificationContextDTO();
					notificationContextDTO.setpREF_CERTIFICATE_STATUS(CertificateStatus.REVOKED);

					NotificationDataDTO notificationDataDTO = new NotificationDataDTO();
					notificationDataDTO.setTitle(Constant.HI + subscriber.getFullName());
					notificationDataDTO.setBody(Constant.YOUR_CERTIFICATES_ARE_REVOKED_SUCCESSFULLY);
					notificationDataDTO.setNotificationContext(notificationContextDTO);

					PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
					pushNotificationRequest.setTo(subscriberFcmToken);
					pushNotificationRequest.setPriority(Constant.HIGH);
					pushNotificationRequest.setData(notificationDataDTO.getRevokeCertNotificationData());

//					String request = "{" + "\"requestBody\":" + pushNotificationRequest.getNotificationRquest() + ","
//							+ "\"serviceMethod\":\"setSendNotification\"" + "}";

					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);

					HttpEntity<String> entity = new HttpEntity<>(pushNotificationRequest.getNotificationRquest() , headers);
					ResponseEntity<String> httpResponse = restTemplate.postForEntity(PropertiesConstants.NOTIFICATION,
							entity, String.class);
					logger.info(
							CLASS + " :: revokeCertificate() :: notification response :: " + httpResponse.getBody());

					logModelDTO.setLogMessage(Constant.RESPONSE);
					logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
					logModelDTO.setEndTime(NativeUtils.getTimeStampString());
					logModelDTO.setTimestamp(null);
					logModelDTO.setCallStack(null);
					LogModel logModel = NativeUtils.getLogModel(logModelDTO);
					rabbitMQSender.send(logModel);
					return Constant.SUCCESS;
				} else
					throw new RAServiceException(ErrorCodes.E_CERTIFICATE_REVOCATION_FAILED);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " revokeCertificate() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " revokeCertificate() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " revokeCertificate() ::  EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/**
	 * Issue certificate call back.
	 *
	 * @param response the response
	 * @return the string
	 * @throws Exception the exception
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dt.ra.service.iface.RAServiceIface#issueCertificateCallBack(java.lang
	 * .String[])
	 */
	@Override
	public String issueCertificateCallBack(Map<String, String> response) throws Exception {
		try {
			logger.info(CLASS + " :: issueCertificateCallBack() :: response ::" + response);
			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			ServiceResponse serviceResponse = objectMapper.readValue(response.get(Constant.CALLBACK_RESPONSE),
					ServiceResponse.class);
			logger.info(CLASS + " :: issueCertificateCallBack(). status :: " + serviceResponse.getStatus());
			SubscriberCertificates subscriberCertificate = new SubscriberCertificates();
			SubscriberCertificateLifeCycle subscriberCertificateLifeCycle = new SubscriberCertificateLifeCycle();
			String status = serviceResponse.getStatus();
			if (status.equals(Constant.CALLBACK_SUCCESS)) {
				subscriberCertificate.setPkiKeyId(response.get(Constant.PKI_KEY_ID));
				subscriberCertificate.setCertificateData(serviceResponse.getCertificate());
				subscriberCertificate.setCertificateStatus(CertificateStatus.ACTIVE.toString());
				switch (response.get(Constant.CALLBACK_CERT_TYPE)) {
				case "AUTH":
					subscriberCertificateLifeCycle.setCertificateType(CertificateType.AUTH.toString());
					subscriberCertificate.setCertificateType(CertificateType.AUTH.toString());
					break;
				case "SIGN":
					subscriberCertificateLifeCycle.setCertificateType(CertificateType.SIGN.toString());
					subscriberCertificate.setCertificateType(CertificateType.SIGN.toString());
					break;
				default:
					throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
				}
				subscriberCertificate.setCertificateSerialNumber(serviceResponse.getCertificate_serial_number());
				subscriberCertificate.setCertificateStartDate(NativeUtils.getTimeStamp(serviceResponse.getIssueDate()));
				subscriberCertificate.setCertificateEndDate(NativeUtils.getTimeStamp(serviceResponse.getExpiryDate()));
				subscriberCertificate.setSubscriberUniqueId(response.get(Constant.CALLBACK_SUID));
				subscriberCertificate.setCreationDate(NativeUtils.getTimeStamp());
				subscriberCertificatesRepository.save(subscriberCertificate);

				SubscriberWrappedKey subscriberWrappedKey = new SubscriberWrappedKey();
				subscriberWrappedKey.setCertificateSerialNumber(subscriberCertificate.getCertificateSerialNumber());
				subscriberWrappedKey.setWrappedKey(serviceResponse.getWrappedKey());
				subscriberWrappedKeyRepository.save(subscriberWrappedKey);

				subscriberCertificateLifeCycle
						.setCertificateSerialNumber(serviceResponse.getCertificate_serial_number());
				subscriberCertificateLifeCycle.setCertificateStatus(CertificateStatus.ACTIVE.toString());
				subscriberCertificateLifeCycle.setCreationDate(NativeUtils.getTimeStamp());
				subscriberCertificateLifeCycle.setSubscriberUniqueId(response.get(Constant.CALLBACK_SUID));
				subscriberCertificateLifeCycleRepository.save(subscriberCertificateLifeCycle);

				List<SubscriberCertificates> certStatusData = subscriberCertificatesRepository
						.findByCertificateStatusAndsubscriberUniqueId(CertificateStatus.ACTIVE.toString(),
								response.get(Constant.CALLBACK_SUID));
				if (certStatusData.size() == 2) {
					SubscriberStatus subscriberStatus = subscriberStatusRepository
							.findBysubscriberUid(response.get(Constant.CALLBACK_SUID));
					subscriberStatus.setSubscriberStatus(Constant.PIN_SET_REQUIRED);
					subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
					subscriberStatus.setSubscriberStatusDescription(Constant.CERTIFICATES_ISSUED_SUCCESSFULLY);
					subscriberStatusRepository.save(subscriberStatus);

					logger.info(CLASS + " :: issueCertificateCallback() :: Notification Service url :: "
							+ PropertiesConstants.NOTIFICATION);

					Subscriber subscriber = subscriberRepository
							.findBysubscriberUid(response.get(Constant.CALLBACK_SUID));
					String subscriberFcmToken = subscriberFcmTokenRepository
							.findBysubscriberUid(response.get(Constant.CALLBACK_SUID));

					NotificationContextDTO notificationContextDTO = new NotificationContextDTO();
					notificationContextDTO.setpREF_CERTIFICATE_STATUS(CertificateStatus.ACTIVE);

					NotificationDataDTO notificationDataDTO = new NotificationDataDTO();
					notificationDataDTO.setTitle(Constant.HI + subscriber.getFullName());
					notificationDataDTO.setNotificationContext(notificationContextDTO);
					notificationDataDTO.setBody(
							Constant.CERTIFICATES_ARE_ISSUED_SUCCESSFULLY_PLEASE_SET_THE_PIN_FOR_FURTHER_USAGES);

					PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
					pushNotificationRequest.setTo(subscriberFcmToken);
					pushNotificationRequest.setPriority(Constant.HIGH);
					pushNotificationRequest.setData(notificationDataDTO.getIssueCertNotificationData());

//					String request = "{" + "\"requestBody\":" + pushNotificationRequest.getNotificationRquest() + ","
//							+ "\"serviceMethod\":\"setSendNotification\"" + "}";

					logger.info(CLASS + " :: issueCertificateCallback() :: Notification request :: " + pushNotificationRequest.getNotificationRquest());

					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					HttpEntity<String> entity = new HttpEntity<String>(pushNotificationRequest.getNotificationRquest(), headers);
					logger.info(CLASS + " :: issueCertificateCallback() :: Notification URI :: "
							+ PropertiesConstants.NOTIFICATION);
					ResponseEntity<String> notificationResponse = restTemplate
							.postForEntity(PropertiesConstants.NOTIFICATION, entity, String.class);

					logger.info(CLASS + " :: issueCertificateCallback() :: callback response :: "
							+ notificationResponse.getBody());

					if (notificationResponse.getStatusCodeValue() == 200) {
						LogModelDTO pushNotificationLog = new LogModelDTO();
						pushNotificationLog.setIdentifier(subscriberCertificate.getSubscriberUniqueId());
						pushNotificationLog.setLogMessageType(LogMessageType.INFO.toString());
						pushNotificationLog.setTransactionType(TransactionType.BUSINESS.toString());
						pushNotificationLog.setCorrelationID(NativeUtils.getUUId());
						pushNotificationLog.setTransactionID(NativeUtils.getUUId());
						pushNotificationLog.setStartTime(NativeUtils.getTimeStampString());
						pushNotificationLog
								.setLogMessage("RESPONSE ->> SUID :: " + subscriberCertificate.getSubscriberUniqueId()
										+ " | Notification send succssfully for Certficates issued");
						pushNotificationLog.setLogMessageType(LogMessageType.SUCCESS.toString());
						pushNotificationLog.setEndTime(NativeUtils.getTimeStampString());
						pushNotificationLog.setServiceName(ServiceName.OTHER.toString());
						LogModel logModel = NativeUtils.getLogModel(pushNotificationLog);
						rabbitMQSender.send(logModel);
					} else {
						LogModelDTO pushNotificationLog = new LogModelDTO();
						pushNotificationLog.setIdentifier(subscriberCertificate.getSubscriberUniqueId());
						pushNotificationLog.setLogMessageType(LogMessageType.INFO.toString());
						pushNotificationLog.setTransactionType(TransactionType.BUSINESS.toString());
						pushNotificationLog.setCorrelationID(NativeUtils.getUUId());
						pushNotificationLog.setTransactionID(NativeUtils.getUUId());
						pushNotificationLog.setStartTime(NativeUtils.getTimeStampString());
						pushNotificationLog
								.setLogMessage("RESPONSE ->> SUID :: " + subscriberCertificate.getSubscriberUniqueId()
										+ " | Notification send failed for Certficates issued");
						pushNotificationLog.setLogMessageType(LogMessageType.FAILURE.toString());
						pushNotificationLog.setEndTime(NativeUtils.getTimeStampString());
						pushNotificationLog.setServiceName(ServiceName.OTHER.toString());
						LogModel logModel = NativeUtils.getLogModel(pushNotificationLog);
						rabbitMQSender.send(logModel);
					}

					logModelDTO.setIdentifier(subscriber.getSubscriberUid());
					logModelDTO.setEndTime(NativeUtils.getTimeStampString());
					String totalTime = NativeUtils.getTotalTime(logModelDTO.getStartTime(), logModelDTO.getEndTime());
					logModelDTO.setLogMessage("Total Time Taken " + totalTime);
					logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
					logModelDTO.setTransactionSubType(null);
					logModelDTO.setCorrelationID(NativeUtils.getUUId());
					logModelDTO.setTransactionID(NativeUtils.getUUId());
					logModelDTO.setSubTransactionID(null);
					logModelDTO.setServiceProviderName(null);
					logModelDTO.setServiceProviderAppName(null);
					logModelDTO.setSignatureType(null);
					logModelDTO.seteSealUsed(false);
					logModelDTO.setServiceName(ServiceName.CERTIFICATE_PAIR_ISSUED.toString());
					logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
					logModelDTO.setEndTime(NativeUtils.getTimeStampString());
					logModelDTO.setTimestamp(NativeUtils.getTimeStampString());
					logModelDTO.setCallStack(null);
					LogModel logModel = NativeUtils.getLogModel(logModelDTO);
					rabbitMQSender.send(logModel);
					logger.info(CLASS + " :: issueCertificateCallBack() :: response :: logModel :: "
							+ logModelDTO.toString());
				}
				return status;
			} else {
				subscriberCertificateLifeCycle.setCertificateSerialNumber(serviceResponse.getStatus());
				subscriberCertificateLifeCycle.setCertificateStatus(serviceResponse.getStatus());
				subscriberCertificateLifeCycle.setCreationDate(NativeUtils.getTimeStamp());
				switch (response.get(Constant.CALLBACK_CERT_TYPE)) {
				case "AUTH":
					subscriberCertificateLifeCycle.setCertificateType(CertificateType.AUTH.toString());
					break;
				case "SIGN":
					subscriberCertificateLifeCycle.setCertificateType(CertificateType.SIGN.toString());
					break;
				}
				subscriberCertificateLifeCycle.setSubscriberUniqueId(response.get(Constant.CALLBACK_SUID));
				subscriberCertificateLifeCycleRepository.save(subscriberCertificateLifeCycle);

				logModelDTO.setIdentifier(response.get(Constant.CALLBACK_SUID));
				logModelDTO.setEndTime(NativeUtils.getTimeStampString());
				logModelDTO.setLogMessage(Constant.RESPONSE);
				logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
				logModelDTO.setTransactionSubType(null);
				logModelDTO.setCorrelationID(NativeUtils.getUUId());
				logModelDTO.setTransactionID(NativeUtils.getUUId());
				logModelDTO.setSubTransactionID(null);
				logModelDTO.setServiceProviderName(null);
				logModelDTO.setServiceProviderAppName(null);
				logModelDTO.setSignatureType(null);
				logModelDTO.seteSealUsed(false);
				logModelDTO.setServiceName(ServiceName.CERTIFICATE_PAIR_ISSUED.toString());
				logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
				logModelDTO.setEndTime(NativeUtils.getTimeStampString());
				logModelDTO.setTimestamp(NativeUtils.getTimeStampString());
				logModelDTO.setCallStack(null);
				LogModel logModel = NativeUtils.getLogModel(logModelDTO);
				rabbitMQSender.send(logModel);

				List<SubscriberCertificateLifeCycle> subscriberCertLifeCycle = subscriberCertificateLifeCycleRepository
						.findBySubscriberUniqueIdAndCertificateStatus(response.get(Constant.CALLBACK_SUID),
								serviceResponse.getStatus());

				if (subscriberCertLifeCycle.size() > 0 && subscriberCertLifeCycle.size() % 2 == 0) {
					Subscriber subscriber = subscriberRepository
							.findBysubscriberUid(response.get(Constant.CALLBACK_SUID));
					String subscriberFcmToken = subscriberFcmTokenRepository
							.findBysubscriberUid(response.get(Constant.CALLBACK_SUID));

					NotificationContextDTO notificationContextDTO = new NotificationContextDTO();
					notificationContextDTO.setpREF_CERTIFICATE_STATUS(CertificateStatus.FAILED);

					NotificationDataDTO notificationDataDTO = new NotificationDataDTO();
					notificationDataDTO.setTitle(Constant.HI + subscriber.getFullName());
					notificationDataDTO.setNotificationContext(notificationContextDTO);
					notificationDataDTO.setBody(Constant.CERTIFICATES_ISSUANCE_FAILED_PLEASE_TRY_AFTER_SOME_TIME);

					PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
					pushNotificationRequest.setTo(subscriberFcmToken);
					pushNotificationRequest.setPriority(Constant.HIGH);
					pushNotificationRequest.setData(notificationDataDTO.getFailedCertNotificationData());

//					String request = "{" + "\"requestBody\":" + pushNotificationRequest.getNotificationRquest() + ","
//							+ "\"serviceMethod\":\"setSendNotification\"" + "}";

					logger.info(CLASS + " :: issueCertificateCallback() :: Notification request :: " + pushNotificationRequest.getNotificationRquest());

					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					HttpEntity<String> entity = new HttpEntity<String>(pushNotificationRequest.getNotificationRquest(), headers);
					logger.info(CLASS + " :: issueCertificateCallback() :: failed :: Notification URI :: "
							+ PropertiesConstants.NOTIFICATION);
					ResponseEntity<String> notificationResponse = restTemplate
							.postForEntity(PropertiesConstants.NOTIFICATION, entity, String.class);
					logger.info(CLASS + " :: issueCertificateCallback() :: callback response :: "
							+ notificationResponse.getBody());
					if (notificationResponse.getStatusCodeValue() == 200) {
						LogModelDTO pushNotificationLog = new LogModelDTO();
						pushNotificationLog.setIdentifier(subscriberCertificate.getSubscriberUniqueId());
						pushNotificationLog.setLogMessageType(LogMessageType.INFO.toString());
						pushNotificationLog.setTransactionType(TransactionType.BUSINESS.toString());
						pushNotificationLog.setCorrelationID(NativeUtils.getUUId());
						pushNotificationLog.setTransactionID(NativeUtils.getUUId());
						pushNotificationLog.setStartTime(NativeUtils.getTimeStampString());
						pushNotificationLog
								.setLogMessage("RESPONSE ->> SUID :: " + subscriberCertificate.getSubscriberUniqueId()
										+ " | Notification send succssfully for Certficates Failed");
						pushNotificationLog.setLogMessageType(LogMessageType.SUCCESS.toString());
						pushNotificationLog.setEndTime(NativeUtils.getTimeStampString());
						pushNotificationLog.setServiceName(ServiceName.OTHER.toString());
						LogModel logModel1 = NativeUtils.getLogModel(pushNotificationLog);
						rabbitMQSender.send(logModel1);
					} else {
						LogModelDTO pushNotificationLog = new LogModelDTO();
						pushNotificationLog.setIdentifier(subscriberCertificate.getSubscriberUniqueId());
						pushNotificationLog.setLogMessageType(LogMessageType.INFO.toString());
						pushNotificationLog.setTransactionType(TransactionType.BUSINESS.toString());
						pushNotificationLog.setCorrelationID(NativeUtils.getUUId());
						pushNotificationLog.setTransactionID(NativeUtils.getUUId());
						pushNotificationLog.setStartTime(NativeUtils.getTimeStampString());
						pushNotificationLog
								.setLogMessage("RESPONSE ->> SUID :: " + subscriberCertificate.getSubscriberUniqueId()
										+ " | Notification send failed for Certficates Failed");
						pushNotificationLog.setLogMessageType(LogMessageType.FAILURE.toString());
						pushNotificationLog.setEndTime(NativeUtils.getTimeStampString());
						pushNotificationLog.setServiceName(ServiceName.OTHER.toString());
						LogModel logModel2 = NativeUtils.getLogModel(pushNotificationLog);
						rabbitMQSender.send(logModel2);
					}
				}
				return status;
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " issueCertificateCallback() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " issueCertificateCallback() :: IN RAServiceException{}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " issueCertificateCallback() ::  EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/**
	 * Gets the revoke reasons.
	 *
	 * @return the revoke reasons
	 */
	public String getRevokeReasons() {
		String str = "[{" + "\"index\" 		: \"1\"," + "\"reason\" 	 : \"KEY_COMPROMISED\"" + "}," + "{"
				+ "\"index\" 		: \"-2\"," + "\"reason\" 	 : \"NO_REASON_CODE\"" + "}," + "{"
				+ "\"index\" 		: \"3\"," + "\"reason\" 	 : \"AFFILIATION_CHANGED\"" + "}," + "{"
				+ "\"index\" 		: \"4\"," + "\"reason\" 	 : \"SUPERSEDED\"" + "}," + "{"
				+ "\"index\" 		: \"5\"," + "\"reason\" 	 : \"CESSATION_OF_OPERATION\"" + "}," + "{"
				+ "\"index\" 		: \"6\"," + "\"reason\" 	 : \"CERTIFICATE_HOLD\"" + "}," + "{"
				+ "\"index\" 		: \"9\"," + "\"reason\" 	 : \"PRIVILEGE_WITHDRAWN\"" + "}]";
		return str;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dt.ra.service.iface.RAServiceIface#
	 * getCertificateDetailsBySubscriberUniqueId(java.lang.String)
	 */
	@Override
	public String getCertificateDetailsBySubscriberUniqueId(String subscriberUniqueId)
			throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: getCertificateDetailsBySubscriberUniqueId():: id :: " + subscriberUniqueId);
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			SubscriberCertificates subscriberCertificates = subscriberCertificatesRepository
					.findTopBySubscriberUniqueIdOrderByCreationDateDesc(subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriberCertificates, ErrorCodes.E_CERTIFICATES_NOT_ISSUED);
			CertificateData certificateData = new CertificateData();
			certificateData.setCertStatus(subscriberCertificates.getCertificateStatus().toString());
			String[] expDate = subscriberCertificates.getCertificateEndDate().toString().split(" ");
			certificateData.setExpiryDate(expDate[0]);
			String[] issueDate = subscriberCertificates.getCertificateStartDate().toString().split(" ");
			certificateData.setIssueDate(issueDate[0]);
			if (subscriberCertificates.getUpdatedDate() != null) {
				String[] revokeDate = subscriberCertificates.getUpdatedDate().toString().split(" ");
				certificateData.setRevokeDate(revokeDate[0]);
			}
			certificateData.setStatus(true);
			logger.info(CLASS + " :: getCertificateDetailsBySubscriberUniqueId() :: response :: success.");
			return certificateData.toString();
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDetailsBySubscriberUniqueId() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDetailsBySubscriberUniqueId() :: IN RAServiceException {}",
					e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDetailsBySubscriberUniqueId() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/**
	 * Gets the certificate life cycle logs by subscriber id.
	 *
	 * @param subscriberUniqueId the subscriber unique id
	 * @return the certificate life cycle logs by subscriber id
	 * @throws RAServiceException the RA service exception
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see com.dt.ra.service.iface.RAServiceIface#
	 * getCertificateLifeCycleLogsBySubscriberId(com.dt.ra.service.model.
	 * RAPKISubscriberdata)
	 */
	@Override
	public String getCertificateLifeCycleLogsBySubscriberUniqueId(String subscriberUniqueId)
			throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: getCertificateLifeCycleLogsBySubscriberId():: id :: " + subscriberUniqueId);
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			List<SubscriberCertificateLifeCycle> lifeCyclesLogs = subscriberCertificateLifeCycleRepository
					.findBysubscriberUniqueId(subscriberUniqueId);
			RAServiceAsserts.notNullorEmpty(lifeCyclesLogs.size(), Constant.CERTIFICATE_LOGS_NOT_FOUND);
			logger.info(CLASS + " :: getCertificateLifecycleLogs():: Success.");
			return lifeCyclesLogs.toString();
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateLifeCycleLogsBySubscriberUniqueId() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateLifeCycleLogsBySubscriberUniqueId() :: IN RAServiceException {}",
					e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateLifeCycleLogsBySubscriberUniqueId() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dtt.ra.service.iface.RAServiceIface#verifyCertficatesPins(com.dtt.ra.
	 * request.entity.VerifyCertificatesPins)
	 */
	@Override
	public String verifyCertficatesPins(VerifyCertificatesPins certificatesPins) throws RAServiceException, Exception {
		logger.info(CLASS + " :: verifyCertficatesPins(). :: req :: certificatesPins ::" + certificatesPins);
		try {
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(certificatesPins.getSubscriberUId());
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			List<SubscriberCertificates> subscriberCertificates = subscriberCertificatesRepository
					.findByCertificateStatusAndsubscriberUniqueId(CertificateStatus.ACTIVE.toString(),
							certificatesPins.getSubscriberUId());
			if (subscriberCertificates.size() == 0)
				throw new RAServiceException(ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			else {
				boolean result = false;
				for (SubscriberCertificates certificate : subscriberCertificates) {
					SubscriberCertificatePinHistory subscriberCertificatePinHistory = subscriberCertificatePinHistoryRepository
							.findBysubscriberUniqueId(certificate.getSubscriberUniqueId());
					logModelDTO.setIdentifier(certificatesPins.getSubscriberUId());
					logModelDTO.setServiceName(null);
					logModelDTO.setLogMessage(Constant.REQUEST);
					logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
					logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
					logModelDTO.setTransactionSubType(null);
					logModelDTO.setCorrelationID(NativeUtils.getUUId());
					logModelDTO.setTransactionID(NativeUtils.getUUId());
					logModelDTO.setSubTransactionID(null);
					logModelDTO.setGeoLocation(null);
					logModelDTO.setServiceProviderName(null);
					logModelDTO.setServiceProviderAppName(null);
					logModelDTO.setSignatureType(null);
					logModelDTO.seteSealUsed(false);

					if (certificate.getCertificateType().equals(CertificateType.SIGN.toString())) {
						String signingPin = subscriberCertificatePinHistory.getSigningCertificatePinList();
						List<String> pinList = Arrays.asList(signingPin.split(", "));
						certificatesPins.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
						certificatesPins.setSigningPassword(certificatesPins.getSigningPin());
					}
					if (certificate.getCertificateType().equals(CertificateType.AUTH.toString())) {
						String authenticationPin = subscriberCertificatePinHistory
								.getAuthenticationCertificatePinList();
						List<String> pinList = Arrays.asList(authenticationPin.split(", "));
						certificatesPins.setCurrentSigningPassword(pinList.get(pinList.size() - 1));
						certificatesPins.setSigningPassword(certificatesPins.getAuthPin());
					}
					SubscriberWrappedKey subscriberWrappedKey = subscriberWrappedKeyRepository
							.findBycertificateSerialNumber(certificate.getCertificateSerialNumber());
					certificatesPins.setWrappedKey(subscriberWrappedKey.getWrappedKey());
					logModelDTO.setCallStack(certificatesPins.toString());

					String baseUrl = PropertiesConstants.PKIURL;
					PostRequest request = new PostRequest();
					request.setRequestBody(logModelDTO.toString());
					request.setHashdata(logModelDTO.toString().hashCode());
					RequestEntity requestEntity = new RequestEntity();
					requestEntity.setPostRequest(request);
					requestEntity.setTransactionType(Constant.VERIFY_PIN);
					ResponseEntity<String> httpResponse = restTemplate.postForEntity(baseUrl, requestEntity,
							String.class);
					if (httpResponse.getBody().equals(Constant.TRANSACTION_TYPE_NOT_FOUND))
						throw new RAServiceException(ErrorCodes.E_TRANSACTION_TYPE_NOT_FOUND);
					if (httpResponse.getBody().equals(Constant.REQUEST_IS_NOT_VALID))
						throw new RAServiceException(ErrorCodes.E_REQUEST_DATA_IS_NOT_VALID);
					ServiceResponse serviceResponse = objectMapper.readValue(httpResponse.getBody(),
							ServiceResponse.class);
					logger.info(
							CLASS + " :: verifyCertficatesPins() :: native response :: " + serviceResponse.getStatus());
					logModelDTO.setCallStack(null);
					logModelDTO.setLogMessage(Constant.RESPONSE);
					if (serviceResponse.getStatus().equals(Constant.FAIL)) {
						ErrorCodes.setResponse(serviceResponse);
						logger.info(
								CLASS + " :: verifyCertficatesPins :: error :: " + serviceResponse.getError_message());
						logModelDTO.setLogMessageType(LogMessageType.ERROR.toString());
						logModelDTO.setEndTime(NativeUtils.getTimeStampString());
						LogModel logModel = NativeUtils.getLogModel(logModelDTO);
						rabbitMQSender.send(logModel);
						throw new RAServiceException(serviceResponse.getError_message());
					} else {
						logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
						logModelDTO.setEndTime(NativeUtils.getTimeStampString());
						Result checksumResult = DAESService.addChecksumToTransaction(logModelDTO.toString());
						logger.info(CLASS + " :: verifyCertficatesPins() :: checksumResult :: "
								+ new String(checksumResult.getResponse()));
						LogModel logModel = objectMapper.readValue(new String(checksumResult.getResponse()),
								LogModel.class);
//						logModel.setTransactionSubType(null);
//						logModel.setSubTransactionID(null);
//						logModel.setGeoLocation(null);
//						logModel.setServiceProviderName(null);
//						logModel.setServiceProviderAppName(null);
//						logModel.setSignatureType(null);
//						logModel.setTimestamp(null);
						rabbitMQSender.send(logModel);
						result = true;
					}
				}
				if (result)
					return Constant.PIN_VERIFICATION_SUCCESS;
				else
					return Constant.PIN_VERIFICATION_FAILED;
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " verifyCertficatesPins() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " verifyCertficatesPins() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " verifyCertficatesPins() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	@Override
	public String getCertificateDataByCertificateType(String subscriberUid, String certType)
			throws RAServiceException, Exception {
		try {
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(subscriberUid);
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			SubscriberCertificates subscriberCertificate = subscriberCertificatesRepository
					.findByCertificateStatusAndsubscriberUniqueIdAndCertificateType(CertificateStatus.ACTIVE.toString(),
							subscriberUid, certType);
			RAServiceAsserts.notNullorEmpty(subscriberCertificate, ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			if (subscriberCertificate.getCertificateType().equals(certType))
				return subscriberCertificate.getCertificateData();
			else
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateType() :: IN DATABASE EXCEPTION {}", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateType() :: IN RAServiceException {}", e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateType() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	public String getOrgCertDetails(String orgId, String certType) throws Exception {
		try {
			String certData = organizationCertificatesRepository.getOrgCertData(orgId);
			return certData;
		} catch (Exception e) {
			throw new Exception(
					messageSource.getMessage("api.error.please.try.after.some.time.server.down", null, Locale.ENGLISH));
		}
	}

//	public ApiResponse sendEmail(EmailReqDto otpDto) {
//		try {
//			logger.info(CLASS + "sendEmail >> Email " + otpDto.getEmailId());
//			if (otpDto.getEmailId().isEmpty()) {
//				return AppUtil.createApiResponse(false, "mail id can't be empty", null);
//			}
//
//			ApiResponse res = sendEmailToSubscriber(otpDto);
//
//			System.out.println(otpDto.toString());
//			if (res.isSuccess()) {
//				System.out.println("email res >> " + res.getMessage());
//				System.out.println("Email Sent Successfully");
//				logger.info(CLASS + "sendOtp >> Sent ");
//				emailSentCount = emailSentCount+1;
//				return AppUtil.createApiResponse(true, "ok", null);
//
//			} else {
//				//System.out.println("IN Email Excption >> " + res);
//				logger.info(CLASS + "sendOtp >> IN Email Excption >> " + res);
//				return AppUtil.createApiResponse(false, "Something went wrong. Try after sometime", null);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			logger.error(CLASS + "sendOtp >> ERROR:: >> " + e.getMessage());
//			return AppUtil.createApiResponse(false, e.getMessage(), null);
//		}
//	}

//	public ApiResponse sendEmailToSubscriber(EmailReqDto emailReqDto) {
//		try {
//			//add email base url in app.props
//			String url = emailBaseUrl;
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_JSON);
//			HttpEntity<Object> requestEntity = new HttpEntity<>(emailReqDto, headers);
//			System.out.println("requestEntity >> " + requestEntity);
//			ResponseEntity<ApiResponse> res = restTemplate.exchange(url, HttpMethod.POST, requestEntity, ApiResponse.class);
//			System.out.println("res >> " + res);
//			if (res.getStatusCodeValue() == 200) {
//				return AppUtil.createApiResponse(true, res.getBody().getMessage(), res);
//			} else if (res.getStatusCodeValue() == 400) {
//				return AppUtil.createApiResponse(false, "Bad Request", null);
//			} else if (res.getStatusCodeValue() == 500) {
//				return AppUtil.createApiResponse(false, "Internal server error", null);
//			}
//			return AppUtil.createApiResponse(false, res.getBody().getMessage(), null);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return AppUtil.createApiResponse(false, e.getMessage(), null);
//		}
//
//	}

	@Override
	public String getOrganizationCertificateDataByCertificateType(String orgId, String certType)
			throws RAServiceException, Exception {
		try {
			logger.info(CLASS + " :: getOrganizationCertificateDataByCertificateType(), orgId >> " + orgId
					+ "certType >> " + certType);
			OrganizationDetails organizationDetails = organizationDetailsRepository.findByOrganizationUid(orgId);
			RAServiceAsserts.notNullorEmpty(organizationDetails, ErrorCodes.E_ORGANIZATION_DATA_NOT_FOUND);
			OrganizationCertificates organizationCertificates = organizationCertificatesRepository
					.findByCertificateStatusAndOrganizationUniqueId(CertificateStatus.ACTIVE.toString(),
							organizationDetails.getOrganizationUid());
			RAServiceAsserts.notNullorEmpty(organizationCertificates, ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			if (organizationCertificates.getCertificateType().equals(certType))
				return organizationCertificates.getCertificateData();
			else
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateType() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateType() :: RAServiceException {} ",
					e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateType() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	@Override
	public ApiResponse getCertificateDataByCertificateTypeForAgent(String subscriberUid, String certType)
			throws RAServiceException, Exception {
		try {
			Subscriber subscriber = subscriberRepository.findBysubscriberUid(subscriberUid);
			RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_SUBSCRIBER_DATA_NOT_FOUND);
			SubscriberCertificates subscriberCertificate = subscriberCertificatesRepository
					.findByCertificateStatusAndsubscriberUniqueIdAndCertificateType(CertificateStatus.ACTIVE.toString(),
							subscriberUid, certType);
			String certificateSerialNumber = subscriberCertificate.getCertificateSerialNumber();

			SubscriberWrappedKey subscriberWrappedKey = subscriberWrappedKeyRepository
					.findBycertificateSerialNumber(certificateSerialNumber);

			SubscriberCertForAgentDto subscriberCertForAgentDto = new SubscriberCertForAgentDto();
			subscriberCertForAgentDto.setSubscriberCertificate(subscriberCertificate.getCertificateData());
			subscriberCertForAgentDto.setWrappedKey(subscriberWrappedKey.getWrappedKey());

			RAServiceAsserts.notNullorEmpty(subscriberCertificate, ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			if (subscriberCertificate.getCertificateType().equals(certType))
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.subcriber.certificate.details", null, Locale.ENGLISH),
						subscriberCertForAgentDto);
			else
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateTypeForAgent() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateTypeForAgent() :: IN RAServiceException {}",
					e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getCertificateDataByCertificateTypeForAgent() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	@Override
	public ApiResponse getOrganizationCertificateDataByCertificateTypeForAgent(String orgId, String certType)
			throws Exception {
		try {
			logger.info(CLASS + " :: getOrganizationCertificateDataByCertificateTypeForAgent(), orgId >> " + orgId
					+ "certType >> " + certType);
			OrganizationDetails organizationDetails = organizationDetailsRepository.findByOrganizationUid(orgId);
			RAServiceAsserts.notNullorEmpty(organizationDetails, ErrorCodes.E_ORGANIZATION_DATA_NOT_FOUND);
			OrganizationCertificates organizationCertificates = organizationCertificatesRepository
					.findByCertificateStatusAndOrganizationUniqueId(CertificateStatus.ACTIVE.toString(),
							organizationDetails.getOrganizationUid());
			RAServiceAsserts.notNullorEmpty(organizationCertificates, ErrorCodes.E_ACTIVE_CERTIFICATE_NOT_FOUND);
			SubscriberCertForAgentDto subscriberCertForAgentDto = new SubscriberCertForAgentDto();
			subscriberCertForAgentDto.setOrganizationCertificate(organizationCertificates.getCertificateData());
			subscriberCertForAgentDto.setOrgWrappedKey(organizationCertificates.getWrappedKey());
			if (organizationCertificates.getCertificateType().equals(certType))
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.organization.certificate", null, Locale.ENGLISH),
						subscriberCertForAgentDto);
			else
				throw new RAServiceException(ErrorCodes.E_CERTIFICATE_TYPE_NOT_FOUND);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateTypeForAgent() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (RAServiceException e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateTypeForAgent() :: IN RAServiceException {}",
					e.getMessage());
			throw new RAServiceException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getOrganizationCertificateDataByCertificateTypeForAgent() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

	@Transactional(
			rollbackFor = {Exception.class}
	)
	@Override
	public ApiResponse expireSubscriberCert(ExpireSubscriberCertRequestDTO expireSubscriberCertRequestDTO) throws Exception {

		try {

			Subscriber subscriber = null;
			if (expireSubscriberCertRequestDTO.getEmail() != null && !expireSubscriberCertRequestDTO.getEmail().isEmpty()) {
				subscriber = (Subscriber) this.subscriberRepository.getbyEmailId(expireSubscriberCertRequestDTO.getEmail());
				RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_EMAIL_NOT_FOUND);}

			if (expireSubscriberCertRequestDTO.getMobileNumber() != null && !expireSubscriberCertRequestDTO.getMobileNumber().isEmpty()) {
				subscriber = subscriberRepository.findLatestByMobileNo(expireSubscriberCertRequestDTO.getMobileNumber());


				RAServiceAsserts.notNullorEmpty(subscriber, ErrorCodes.E_MOBILE_NUMBER_NOT_FOUND);}



			LogModelDTO logModelDTO = new LogModelDTO();
			logModelDTO.setStartTime(NativeUtils.getTimeStampString());
			logModelDTO.setServiceName(null);
			logModelDTO.setLogMessage("REQUEST");
			logModelDTO.setLogMessageType(LogMessageType.INFO.toString());
			logModelDTO.setTransactionType(TransactionType.BUSINESS.toString());
			logModelDTO.setTransactionSubType(null);
			logModelDTO.setCorrelationID(NativeUtils.getUUId());
			logModelDTO.setTransactionID(NativeUtils.getUUId());
			logModelDTO.setSubTransactionID(null);
			logModelDTO.setGeoLocation(null);
			logModelDTO.setServiceProviderName(null);
			logModelDTO.setServiceProviderAppName(null);
			logModelDTO.setSignatureType(null);
			logModelDTO.seteSealUsed(false);

			List<SubscriberCertificates> subscriberCertificates =
					subscriberCertificatesRepository.findBySubscriberUniqueIdToExpireCert(subscriber.getSubscriberUid());

			RAServiceAsserts.notNullorEmpty(subscriberCertificates, ErrorCodes.E_CERTIFICATES_NOT_ISSUED);

			for (SubscriberCertificates cert : subscriberCertificates) {
//				if (CertificateStatus.EXPIRED.toString().equals(cert.getCertificateStatus())) {
//					return AppUtil.createApiResponse(true, "Certificates are already expired", null);
//				}
				if (cert.getCertificateStatus().equals("EXPIRED")) {
					return AppUtil.createApiResponse(true, "Certificates are already expired", (Object)null);
				}

				// Update certificate status and expiry date
				System.out.println("It came here updating status of certificate here");
				cert.setCertificateStatus("EXPIRED");
				Date currentExpiry = cert.getCertificateEndDate();
				if (currentExpiry != null) {
					LocalDateTime localExpiry = currentExpiry.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					LocalDateTime updatedExpiry = localExpiry.minusYears(2);
					Date newExpiryDate = Date.from(updatedExpiry.atZone(ZoneId.systemDefault()).toInstant());
					cert.setCertificateEndDate(newExpiryDate);
				}

				subscriberCertificatesRepository.save(cert);

				// Update lifecycle
				SubscriberCertificateLifeCycle lifeCycle = new SubscriberCertificateLifeCycle();
				lifeCycle.setCertificateSerialNumber(cert.getCertificateSerialNumber());
				lifeCycle.setCertificateStatus(CertificateStatus.EXPIRED.toString());
				lifeCycle.setSubscriberUniqueId(cert.getSubscriberUniqueId());
				lifeCycle.setCertificateType(cert.getCertificateType());
				lifeCycle.setCreationDate(NativeUtils.getTimeStamp());
				subscriberCertificateLifeCycleRepository.save(lifeCycle);

				// Update subscriber status
				SubscriberStatus subscriberStatus =
						subscriberStatusRepository.findBysubscriberUid(cert.getSubscriberUniqueId());
				String subscriberFcmToken =
						subscriberFcmTokenRepository.findBysubscriberUid(cert.getSubscriberUniqueId());

				if (subscriberStatus != null) {
					subscriberStatus.setSubscriberStatus(Constant.CERT_EXPIRED);
					subscriberStatus.setUpdatedDate(NativeUtils.getTimeStamp());
					subscriberStatus.setSubscriberStatusDescription("Certificates are Expired.");
					subscriberStatusRepository.save(subscriberStatus);
				}

				// Send FCM notification
				NotificationContextDTO context = new NotificationContextDTO();
				context.setpREF_CERTIFICATE_STATUS(CertificateStatus.EXPIRED);

				NotificationDataDTO notificationData = new NotificationDataDTO();
				notificationData.setTitle("Hi " + subscriber.getFullName());
				notificationData.setBody("Your " + cert.getCertificateType() + " Certificate is expired.");
				notificationData.setNotificationContext(context);

				PushNotificationRequest notificationRequest = new PushNotificationRequest();
				notificationRequest.setTo(subscriberFcmToken);
				notificationRequest.setPriority("high");
				notificationRequest.setData(notificationData.getExpiredCertNotificationData());

//				String requestJson = "{\"requestBody\":" + notificationRequest.getNotificationRquest()
//						+ ",\"serviceMethod\":\"setSendNotification\"}";

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> entity = new HttpEntity<>(notificationRequest.getNotificationRquest(), headers);

				if (!notificationSent) {
					try {
						ResponseEntity<String> response = restTemplate.postForEntity(
								PropertiesConstants.NOTIFICATION, entity, String.class);
						notificationSent = true;
					} catch (Exception ex) {
						logger.warn("FCM Notification failed.", ex);
					}
				}

				// Logging to RabbitMQ
				logModelDTO.setLogMessage("RESPONSE");
				logModelDTO.setLogMessageType(LogMessageType.SUCCESS.toString());
				logModelDTO.setEndTime(NativeUtils.getTimeStampString());

				try {
					LogModel logModel = NativeUtils.getLogModel(logModelDTO);
					rabbitMQSender.send(logModel);
				} catch (Exception ex) {
					logger.warn("Service log failed", ex);
				}
			}
			return AppUtil.createApiResponse(true,"Certificate Expired suceesfully",null);

		}catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " expireSubscriberCert() :: IN DATABASE EXCEPTION {}",
					e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " expireSubscriberCert() :: exception {} ", e.getMessage());
			throw new Exception(messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
					null, Locale.ENGLISH));
		}
	}

//	@Override
//	public ApiResponse fetchVisitorDetailsBySearchType(int searchType, String searchValue) {
//		try{
//			VisitorCompleteDetails visitorCompleteDetails= new VisitorCompleteDetails();
//			switch (searchType) {
//				case 1:
//					visitorCompleteDetails = visitorCompleteDetailsRepository.fetchVisitorByIdocNumber(searchValue,"1");
//					if(visitorCompleteDetails==null){
//						return AppUtil.createApiResponse(false,"National Id Not Found",null);
//					}
//
//					break;
//				case 2:
//					visitorCompleteDetails = visitorCompleteDetailsRepository.fetchVisitorByIdocNumber(searchValue,"3");
//					if(visitorCompleteDetails==null){
//						return AppUtil.createApiResponse(false,"Passport Not Found",null);
//					}
//
//					break;
//				case 3:
//					visitorCompleteDetails = visitorCompleteDetailsRepository.fetchVisitorByEmail(searchValue);
//					if(visitorCompleteDetails==null){
//						return AppUtil.createApiResponse(false,"Email Id Not Found",null);
//					}
//
//					break;
//				case 4:
//					visitorCompleteDetails=visitorCompleteDetailsRepository.fetchVisitorByMobileNumber(searchValue);
//					if(visitorCompleteDetails==null){
//						return AppUtil.createApiResponse(false,"Mobile Number Not Found",null);
//					}
//					break;
//				default:
//					return AppUtil.createApiResponse(false,"Please Select Valid Identifier",null);
//			}
//
//
//			if(visitorCompleteDetails.getSubscriberType().equals("Visitor")){
//				System.out.println("DATATDA "+visitorCompleteDetails.getDob());
//				;
//				String passportNumber=visitorCompleteDetails.getIdDocNumber();
//				int count= subscriberTravelHistoryRepo.noOfEntries("CLEARED","ENTRY",passportNumber);
//				SubscriberTravelHistory subscriberTravelHistoryEntry=subscriberTravelHistoryRepo.findLastTravelDate("CLEARED","ENTRY",passportNumber);
//				SubscriberTravelHistory subscriberTravelHistoryExit=subscriberTravelHistoryRepo.findLastTravelDate("CLEARED","EXIT",passportNumber);
//				SubscriberTravelHistory subscriberTravelHistoryLatest=subscriberTravelHistoryRepo.findLatestCleared(passportNumber);
//				VisitorCompleteDetailsDto visitorCompleteDetailsDto=new VisitorCompleteDetailsDto();
//				if(visitorCompleteDetails.getAuthPinSetDate()!=null ){
//					visitorCompleteDetailsDto.setAuthPinSetDate(visitorCompleteDetails.getAuthPinSetDate().substring(0,10));
//				}
//				if(visitorCompleteDetails.getCertificateExpiryDate()!=null ){
//					visitorCompleteDetailsDto.setCertificateExpiryDate(visitorCompleteDetails.getCertificateExpiryDate().substring(0,10));
//				}
//				if(visitorCompleteDetails.getCertificateIssueDate()!=null ){
//					visitorCompleteDetailsDto.setCertificateIssueDate(visitorCompleteDetails.getCertificateIssueDate().substring(0,10));
//				}
//
//
//				visitorCompleteDetailsDto.setDob(visitorCompleteDetails.getDob().substring(0,10));
//				System.out.println(visitorCompleteDetailsDto.getDob());
//				if(visitorCompleteDetails.getCreatedOn()!=null ){
//					visitorCompleteDetailsDto.setCreatedOn(visitorCompleteDetails.getCreatedOn().substring(0,10));
//				}
//				visitorCompleteDetailsDto.setCertificateSerialNumber(visitorCompleteDetails.getCertificateSerialNumber());
//				visitorCompleteDetailsDto.setCertificateStatus(visitorCompleteDetails.getCertificateStatus());
//				visitorCompleteDetailsDto.setCountryName(visitorCompleteDetails.getCountryName());
//				if(visitorCompleteDetails.getDeviceRegistrationTime()!=null ){
//					visitorCompleteDetailsDto.setDeviceRegistrationTime(visitorCompleteDetails.getDeviceRegistrationTime().substring(0,10));
//				}
//				visitorCompleteDetailsDto.setDeviceStatus(visitorCompleteDetails.getDeviceStatus());
//				visitorCompleteDetailsDto.seteMail(visitorCompleteDetails.geteMail());
//				visitorCompleteDetailsDto.setEmployer(visitorCompleteDetails.getEmployer());
//				visitorCompleteDetailsDto.setFullName(visitorCompleteDetails.getFullName());
//				visitorCompleteDetailsDto.setGender(visitorCompleteDetails.getGender());
//				visitorCompleteDetailsDto.setGeoLocation(visitorCompleteDetails.getGeoLocation());
//				visitorCompleteDetailsDto.setIdDocImage(visitorCompleteDetails.getIdDocImage());
//				visitorCompleteDetailsDto.setIdDocNumber(visitorCompleteDetails.getIdDocNumber());
//				visitorCompleteDetailsDto.setPassportNumber(visitorCompleteDetails.getIdDocNumber());
//				visitorCompleteDetailsDto.setIdDocType(visitorCompleteDetails.getIdDocType());
//
//				visitorCompleteDetailsDto.setNoOfEntries(count);
//				visitorCompleteDetailsDto.setNonResidentCardStatus(visitorCompleteDetails.getNonResidentCardStatus());
//				visitorCompleteDetailsDto.setNonResidentId(visitorCompleteDetails.getNonResidentId());
//				visitorCompleteDetailsDto.setLevelOfAssurance(visitorCompleteDetails.getLevelOfAssurance());
//				visitorCompleteDetailsDto.setMobileNumber(visitorCompleteDetails.getMobileNumber());
//				visitorCompleteDetailsDto.setOnBoardingMethod(visitorCompleteDetails.getOnBoardingMethod());
//				if(visitorCompleteDetails.getOnBoardingTime()!=null ){
//					visitorCompleteDetailsDto.setOnBoardingTime(visitorCompleteDetails.getOnBoardingTime().substring(0,10));
//				}
//				if(visitorCompleteDetails.getPassportExpiryDate()!=null ){
//					visitorCompleteDetailsDto.setPassportExpiryDate(visitorCompleteDetails.getPassportExpiryDate().substring(0,10));
//				}
//
//				visitorCompleteDetailsDto.setPhoto(visitorCompleteDetails.getPhoto());
//				if(visitorCompleteDetails.getRevocationDate()!=null ){
//					visitorCompleteDetailsDto.setRevocationDate(visitorCompleteDetails.getRevocationDate().substring(0,10));
//				}
//
//				visitorCompleteDetailsDto.setRevocationReason(visitorCompleteDetails.getRevocationReason());
//				visitorCompleteDetailsDto.setSelfieUri(visitorCompleteDetails.getSelfieUri());
//
//				if(visitorCompleteDetails.getSignPinSetDate()!=null ){
//					visitorCompleteDetailsDto.setSignPinSetDate(visitorCompleteDetails.getSignPinSetDate().substring(0,10));
//				}
//				visitorCompleteDetailsDto.setSubscriberStatus(visitorCompleteDetails.getSubscriberStatus());
//				visitorCompleteDetailsDto.setSubscriberType(visitorCompleteDetails.getSubscriberType());
//				visitorCompleteDetailsDto.setSubscriberUid(visitorCompleteDetails.getSubscriberUid());
//				visitorCompleteDetailsDto.setVideoType(visitorCompleteDetails.getVideoType());
//				visitorCompleteDetailsDto.setVideoUrl(visitorCompleteDetails.getVideoUrl());
//				if(visitorCompleteDetails.getVisaExpiryDate()!=null ){
//					visitorCompleteDetailsDto.setVisaExpiryDate(visitorCompleteDetails.getVisaExpiryDate().substring(0,10));
//				}
//
//				visitorCompleteDetailsDto.setVisaNumber(visitorCompleteDetails.getVisaNumber());
//				visitorCompleteDetailsDto.setBlackListed(visitorCompleteDetails.getBlacklisted());
//				visitorCompleteDetailsDto.setVisaType(visitorCompleteDetails.getVisaType());
//				visitorCompleteDetailsDto.setOnboardingDocument(visitorCompleteDetails.getOnboardingDocumnet());
//				if(subscriberTravelHistoryLatest!=null) {
//					visitorCompleteDetailsDto.setImmigrationStatus(subscriberTravelHistoryLatest.getImmigrationType());
//				}
//				if(subscriberTravelHistoryEntry!=null) {
//					visitorCompleteDetailsDto.setLastEntryDate(subscriberTravelHistoryEntry.getTravelDate().substring(0,10));
//				}
//				if(subscriberTravelHistoryExit!=null){
//					visitorCompleteDetailsDto.setLastExitDate(subscriberTravelHistoryExit.getTravelDate().substring(0,10));
//
//
//				}
//				if(visitorCompleteDetails.getVisaIssueDate()!=null){
//					visitorCompleteDetailsDto.setVisaIssueDate(visitorCompleteDetails.getVisaIssueDate().substring(0,10));
//				}
//				visitorCompleteDetailsDto.setResidentId(visitorCompleteDetails.getResidentId());
//
//							ObjectMapper objectMapper= new ObjectMapper();
//			String subscriberDetails = objectMapper.writeValueAsString(visitorCompleteDetailsDto);
//			return AppUtil.createApiResponse(true,"Subscriber Details Fetched Successfully",subscriberDetails);
//			}
//			VisitorCompleteDetailsDto visitorCompleteDetailsDto=new VisitorCompleteDetailsDto();
//			if(visitorCompleteDetails.getAuthPinSetDate()!=null ){
//				visitorCompleteDetailsDto.setAuthPinSetDate(visitorCompleteDetails.getAuthPinSetDate().substring(0,10));
//			}
//			if(visitorCompleteDetails.getCertificateExpiryDate()!=null ){
//				visitorCompleteDetailsDto.setCertificateExpiryDate(visitorCompleteDetails.getCertificateExpiryDate().substring(0,10));
//			}
//			if(visitorCompleteDetails.getCertificateIssueDate()!=null ){
//				visitorCompleteDetailsDto.setCertificateIssueDate(visitorCompleteDetails.getCertificateIssueDate().substring(0,10));
//			}
//			if(visitorCompleteDetails.getCertificateIssueDate()!=null ){
//				visitorCompleteDetailsDto.setCertificateIssueDate(visitorCompleteDetails.getCertificateIssueDate().substring(0,10));
//			}
//			System.out.println("DATATDA "+visitorCompleteDetails.getDob());
//			visitorCompleteDetailsDto.setDob(visitorCompleteDetails.getDob().substring(0,10));
//
//			if(visitorCompleteDetails.getCreatedOn()!=null ){
//				visitorCompleteDetailsDto.setCreatedOn(visitorCompleteDetails.getCreatedOn().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setCertificateSerialNumber(visitorCompleteDetails.getCertificateSerialNumber());
//			visitorCompleteDetailsDto.setCertificateStatus(visitorCompleteDetails.getCertificateStatus());
//			visitorCompleteDetailsDto.setCountryName(visitorCompleteDetails.getCountryName());
//			if(visitorCompleteDetails.getDeviceRegistrationTime()!=null ){
//				visitorCompleteDetailsDto.setDeviceRegistrationTime(visitorCompleteDetails.getDeviceRegistrationTime().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setDeviceStatus(visitorCompleteDetails.getDeviceStatus());
//			visitorCompleteDetailsDto.seteMail(visitorCompleteDetails.geteMail());
//			visitorCompleteDetailsDto.setEmployer(visitorCompleteDetails.getEmployer());
//			visitorCompleteDetailsDto.setFullName(visitorCompleteDetails.getFullName());
//			visitorCompleteDetailsDto.setGender(visitorCompleteDetails.getGender());
//			visitorCompleteDetailsDto.setGeoLocation(visitorCompleteDetails.getGeoLocation());
//			visitorCompleteDetailsDto.setIdDocImage(visitorCompleteDetails.getIdDocImage());
//			visitorCompleteDetailsDto.setIdDocNumber(visitorCompleteDetails.getIdDocNumber());
//			if(visitorCompleteDetails.getIdDocType().equals("3")) {
//				visitorCompleteDetailsDto.setPassportNumber(visitorCompleteDetails.getIdDocNumber());
//			}
//			visitorCompleteDetailsDto.setIdDocType(visitorCompleteDetails.getIdDocType());
//			visitorCompleteDetailsDto.setNonResidentCardStatus(visitorCompleteDetails.getNonResidentCardStatus());
//			visitorCompleteDetailsDto.setNonResidentId(visitorCompleteDetails.getNonResidentId());
//			visitorCompleteDetailsDto.setLevelOfAssurance(visitorCompleteDetails.getLevelOfAssurance());
//			visitorCompleteDetailsDto.setMobileNumber(visitorCompleteDetails.getMobileNumber());
//			visitorCompleteDetailsDto.setOnBoardingMethod(visitorCompleteDetails.getOnBoardingMethod());
//
//			if(visitorCompleteDetails.getOnBoardingTime()!=null ){
//				visitorCompleteDetailsDto.setOnBoardingTime(visitorCompleteDetails.getOnBoardingTime().substring(0,10));
//			}
//			if(visitorCompleteDetails.getPassportExpiryDate()!=null ){
//				visitorCompleteDetailsDto.setPassportExpiryDate(visitorCompleteDetails.getPassportExpiryDate().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setPhoto(visitorCompleteDetails.getPhoto());
//			if(visitorCompleteDetails.getRevocationDate()!=null ){
//				visitorCompleteDetailsDto.setRevocationDate(visitorCompleteDetails.getRevocationDate().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setRevocationReason(visitorCompleteDetails.getRevocationReason());
//			visitorCompleteDetailsDto.setSelfieUri(visitorCompleteDetails.getSelfieUri());
//			if(visitorCompleteDetails.getSignPinSetDate()!=null ){
//				visitorCompleteDetailsDto.setSignPinSetDate(visitorCompleteDetails.getSignPinSetDate().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setSubscriberStatus(visitorCompleteDetails.getSubscriberStatus());
//			visitorCompleteDetailsDto.setSubscriberType(visitorCompleteDetails.getSubscriberType());
//			visitorCompleteDetailsDto.setSubscriberUid(visitorCompleteDetails.getSubscriberUid());
//			visitorCompleteDetailsDto.setVideoType(visitorCompleteDetails.getVideoType());
//			visitorCompleteDetailsDto.setVideoUrl(visitorCompleteDetails.getVideoUrl());
//			if(visitorCompleteDetails.getVisaExpiryDate()!=null ){
//				visitorCompleteDetailsDto.setVisaExpiryDate(visitorCompleteDetails.getVisaExpiryDate().substring(0,10));
//			}
//			if(visitorCompleteDetails.getVisaIssueDate()!=null){
//				visitorCompleteDetailsDto.setVisaIssueDate(visitorCompleteDetails.getVisaIssueDate().substring(0,10));
//			}
//			visitorCompleteDetailsDto.setVisaNumber(visitorCompleteDetails.getVisaNumber());
//			visitorCompleteDetailsDto.setBlackListed(visitorCompleteDetails.getBlacklisted());
//			visitorCompleteDetailsDto.setOnboardingDocument(visitorCompleteDetails.getOnboardingDocumnet());
//			visitorCompleteDetailsDto.setVisaType(visitorCompleteDetails.getVisaType());
//			ObjectMapper objectMapper= new ObjectMapper();
//			String subscriberDetails = objectMapper.writeValueAsString(visitorCompleteDetailsDto);
//			visitorCompleteDetailsDto.setResidentId(visitorCompleteDetails.getResidentId());
//			return AppUtil.createApiResponse(true,"Subscriber Details Fetched Successfully",subscriberDetails);
//
//		}catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
//				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
//			e.printStackTrace();
//
//			return AppUtil.createApiResponse(false, "Something went wrong please try after sometime", null);
//		} catch (Exception e) {
//
//			e.printStackTrace();
//			return AppUtil.createApiResponse(false, "Something went wrong please try after sometime", null);
//		}
//	}



}
