package ug.daes.ra.service.iface;

import ug.daes.ra.dto.ApiResponse;

public interface VisitorCompleteDetailsIface {
	ApiResponse getDocumentDetailsBydocumentId(String documentNumber,String documentType);

}
