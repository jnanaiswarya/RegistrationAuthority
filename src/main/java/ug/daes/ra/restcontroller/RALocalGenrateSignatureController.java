package ug.daes.ra.restcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ug.daes.ra.asserts.RAServiceAsserts;
import ug.daes.ra.dto.LogModelDTO;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.response.entity.APIResponse;
import ug.daes.ra.service.iface.implementation.RALocalGenrateSignatureIfaceImpl;

import java.util.Locale;

@RestController
@RequestMapping(value = "/api/")
public class RALocalGenrateSignatureController {

	/** The logger. */
	static Logger logger = LoggerFactory.getLogger(RALocalGenrateSignatureController.class);

	/** The class. */
	final private String CLASS = "RALocalGenrateSignatureController";

	@Autowired
	PKIRestController controller;

	@Autowired
	RALocalGenrateSignatureIfaceImpl raLocalGenrateSignatureIfaceImpl;
	
	@Autowired
	MessageSource messageSource;

	@PostMapping(value = "post/service/certificate/generate-signatur/local-agent/subscriber", consumes = "application/json")
	public String generateSignatureForLocalAgentSub(@RequestBody LogModelDTO generateSignatureRequest) {
		try {
			logger.info(CLASS + " :: generateSignatureForLocalAgentSub :: request. :: " + generateSignatureRequest);
			APIResponse apiResponse = controller.getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(generateSignatureRequest, ErrorCodes.E_INVALID_REQUEST);
			String response = raLocalGenrateSignatureIfaceImpl.generateSignatureForAgentSubscriber(generateSignatureRequest);
			logger.info(CLASS + " :: generateSignatureForLocalAgentSub() :: response :: " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.generate.signature.response", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.info(CLASS + " :: generateSignatureForLocalAgentSub() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

	
	@PostMapping(value = "post/service/certificate/generate-signatur/local-agent/org", consumes = "application/json")
	public String generateSignatureForLocalAgentOrg(@RequestBody LogModelDTO generateSignatureRequest) {
		try {
			logger.info(CLASS + " :: generateSignatureForLocalAgentOrg :: request. :: " + generateSignatureRequest);
			APIResponse apiResponse = controller.getServiceStatus();
			if (!apiResponse.getStatus())
				throw new Exception(apiResponse.getMessage());
			RAServiceAsserts.notNullorEmpty(generateSignatureRequest, ErrorCodes.E_INVALID_REQUEST);
			String response = raLocalGenrateSignatureIfaceImpl.generateSignatureForAgentOrganization(generateSignatureRequest);
			logger.info(CLASS + " :: generateSignatureForLocalAgentOrg() :: response :: " + response);
			return new APIResponse(true,
					messageSource.getMessage("api.response.generate.signature.response", null, Locale.ENGLISH),
					"\"" + response + "\"").toString();
		} catch (Exception e) {
			logger.info(CLASS + " :: generateSignatureForLocalAgentOrg() :: error :: " + e.getMessage());
			return new APIResponse(false, ErrorCodes.getErrorMessage(ErrorCodes.getErrorCode(e.getMessage())), null)
					.toString();
		}
	}

}
