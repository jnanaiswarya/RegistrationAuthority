package ug.daes.ra.service.iface.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ug.daes.ra.dto.ApiResponse;
import ug.daes.ra.dto.DocumentDto;
import ug.daes.ra.exception.ErrorCodes;
import ug.daes.ra.exception.RAServiceException;
import ug.daes.ra.model.VisitorCompleteDetails;
import ug.daes.ra.repository.iface.VisitorCompleteDetailsRepository;
import ug.daes.ra.service.iface.VisitorCompleteDetailsIface;
import ug.daes.ra.utils.AppUtil;

@Service
public class VisitorCompleteDetailsImpl implements VisitorCompleteDetailsIface {

	@Autowired
	VisitorCompleteDetailsRepository visitorCompleteDetailsRepository;

	@Override
	public ApiResponse getDocumentDetailsBydocumentId(String documentNumber, String documentType) {
		try {
			DocumentDto documentDto = new DocumentDto();
			VisitorCompleteDetails visitorCompleteDetails = visitorCompleteDetailsRepository
					.idDocNumber(documentNumber);
			if (visitorCompleteDetails != null) {
				switch (documentType) {
				case "Visa_Card":
					documentDto.setDocument(visitorCompleteDetails.getSignedVisaDocument());
					documentDto.setDocumentName("Visa_Card");
					break;
				case "Non_Resident_Card":
					documentDto.setDocument(visitorCompleteDetails.getNonResidentIdDocumnet());
					documentDto.setDocumentName("Non_Resident_Card");
					break;
				case "Visitor_Card":
					documentDto.setDocument(visitorCompleteDetails.getVisitorCardPdf());
					documentDto.setDocumentName("Visitor_Card");
					break;
				case "Establishment_Card":
					documentDto.setDocument(visitorCompleteDetails.getEstablishmentCard());
					documentDto.setDocumentName("Establishment_Card");
					break;
				case "Trade_Card":
					documentDto.setDocument(visitorCompleteDetails.getTradelicenseDocumentCard());
					documentDto.setDocumentName("Trade_Card");
					break;
				case "Resident_Card":
					documentDto.setDocument(visitorCompleteDetails.getResidenceIdDocument());
					documentDto.setDocumentName("Resident_Card");
					break;
				default:
					throw new RAServiceException(ErrorCodes.E_INVALID_REQUEST);
				}
				return AppUtil.createApiResponse(true, "data found successfully", documentDto);

			} else {
				return AppUtil.createApiResponse(false, "Data not found", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, "Something went wrong.Please try after sometime", null);
			// return new ApiResponse(false,"Something went wrong.Please try after
			// sometime.",null);
		}
	}

}