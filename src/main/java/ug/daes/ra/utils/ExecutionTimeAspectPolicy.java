package ug.daes.ra.utils;


import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import ug.daes.ra.model.OnboardingAgents;
import ug.daes.ra.model.SubscriberDevice;
import ug.daes.ra.model.SubscriberDeviceHistory;
import ug.daes.ra.repository.iface.OnboardingAgentsRepo;
import ug.daes.ra.repository.iface.SubscriberDeviceHistoryRepoIface;
import ug.daes.ra.repository.iface.SubscriberDeviceRepository;
import ug.daes.ra.response.entity.APIResponse;

@Aspect
@Component
public class ExecutionTimeAspectPolicy {

	@Autowired
	MessageSource messageSource;

	@Autowired
	SubscriberDeviceRepository subscriberDeviceRepoIface;

	@Autowired
	SubscriberDeviceHistoryRepoIface subscriberDeviceHistoryRepoIface;
	
	@Autowired
	OnboardingAgentsRepo onboardingAgentsRepo;

	@Pointcut("execution(* ug.daes.ra.restcontroller.PKIRestController.revokeCertificate(..))")
	private void forrevokeCertificate() {
	};

	@Pointcut("execution(* ug.daes.ra.restcontroller.PKIRestController.verifyCertficatePin(..))")
	private void forverifyCertficatePin() {
	};

	@Pointcut("execution(* ug.daes.ra.restcontroller.PKIRestController.setPin(..))")
	private void forsetPin() {
	};

//	@Pointcut("execution(* ug.daes.ra.restcontroller.PKIRestController.certificateRequest(..))")
//	private void forcertificateRequest() {
//	};

	@Pointcut("execution(* ug.daes.ra.restcontroller.RARestController.getCertificateDetailsBySubscriberUniqueId(..))")
	private void forgetCertificateDetailsBySubscriberUniqueId() {
	};

	@Around("forrevokeCertificate() || forverifyCertficatePin() || forsetPin() || forgetCertificateDetailsBySubscriberUniqueId()") // methods
	public Object controllerPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
		return checkPolicy(joinPoint);
	}

	private Object checkPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().toShortString();

		// System.out.println("method name: " + methodName);
		String deviceUid = "";
		String appVersion = "";
		for (Object arg : joinPoint.getArgs()) {
			if (arg instanceof HttpServletRequest) {
				HttpServletRequest httpServletRequest = (HttpServletRequest) arg;

				deviceUid = httpServletRequest.getHeader("deviceId");
				appVersion = httpServletRequest.getHeader("appVersion");

				break;
			}
		}
		Optional<SubscriberDeviceHistory> subscriberDeviceHistoryOptional = Optional
				.ofNullable(subscriberDeviceHistoryRepoIface.findBydeviceUid(deviceUid));
		SubscriberDevice checkSubscriberDetails = null;
		List<SubscriberDevice> subscriberDeviceDetailsList = subscriberDeviceRepoIface.findBydeviceUid(deviceUid);
		SubscriberDevice subscriberDeviceDetails = subscriberDeviceDetailsList.isEmpty() ? null : subscriberDeviceDetailsList.get(0);


		//	SubscriberDevice subscriberDeviceDetails = (SubscriberDevice) subscriberDeviceRepoIface.findBydeviceUid(deviceUid);
		
		List<OnboardingAgents> onboardingAgents = onboardingAgentsRepo.findByAgentdeviceUid(deviceUid);

		Object result;
		boolean checkPolicy = true;
		boolean deviceEmpty = false;
		System.out.println("appVersion and deviceUid " + appVersion +"---"+ deviceUid);
		if(deviceUid.equals("WEB")) {
			checkPolicy = true;
		} else {
			if (appVersion == null || appVersion.equals("") || appVersion == "") {

				deviceEmpty = true;

			} else if (subscriberDeviceHistoryOptional.isPresent()) {

				checkSubscriberDetails = subscriberDeviceRepoIface.getSubscriber(subscriberDeviceHistoryOptional.get().getSubscriberUid());
				SubscriberDevice subscriberDevice = subscriberDeviceRepoIface.findBydeviceUidAndStatus(deviceUid, "ACTIVE");
				if(subscriberDevice == null) {
					if(onboardingAgents.size() == 0 || onboardingAgents.isEmpty()) {
						checkPolicy = false;
					}else {
						checkPolicy = true;
					}
					
				}else if (subscriberDevice.getDeviceStatus() == "DISABLED"
						|| subscriberDevice.getDeviceStatus().equalsIgnoreCase("DISABLED")) {
					checkPolicy = false;
					System.out.println("inside else if");

				} else {
					checkPolicy = true;
				}

			} else if (subscriberDeviceDetails == null) {
				System.out.println("subscriberDeviceDetails is null");
				if(onboardingAgents.size() == 0 || onboardingAgents.isEmpty()) {
					checkPolicy = false;
				}else {
					checkPolicy = true;
				}

			} else if (subscriberDeviceDetails.getDeviceStatus() == "ACTIVE"
					|| subscriberDeviceDetails.getDeviceStatus().equalsIgnoreCase("ACTIVE")) {
				checkPolicy = true;
				System.out.println("inside else if active");

			} else if (subscriberDeviceDetails.getDeviceStatus() == "DISABLED"
					|| subscriberDeviceDetails.getDeviceStatus().equalsIgnoreCase("DISABLED")) {
				checkPolicy = false;
				System.out.println("inside else if DISABLED");

			} else {
				checkPolicy = false;
				System.out.println("inside else");
			}
		}
			
			
		

		if (deviceEmpty) {
			return new APIResponse(false,
					messageSource.getMessage(
							"api.error.please.update.your.app", null,
							Locale.ENGLISH),
					null).toString();

		} else if (checkPolicy) {
			result = joinPoint.proceed();
		} else {

			if (subscriberDeviceDetails == null && checkSubscriberDetails == null) {
				return new APIResponse(false,
						messageSource.getMessage(
								"api.error.subscriber.not.found", null,
								Locale.ENGLISH),
						null).toString();

			} else {
				return new APIResponse(false,
						messageSource.getMessage(
								"api.error.account.registered.on.new.device.services.disabled.on.this.device", null,
								Locale.ENGLISH),
						null).toString();
			}

//			result = AppUtil.createApiResponse(false,"We apologize for any inconvenience.  You can use the service after " +remainHour+ " hours, as it seems you changed your Device.",null);
		}

		return result;
	}

}
