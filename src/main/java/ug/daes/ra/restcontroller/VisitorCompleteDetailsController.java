package ug.daes.ra.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ug.daes.ra.dto.ApiResponse;
import ug.daes.ra.service.iface.VisitorCompleteDetailsIface;

@RestController
public class VisitorCompleteDetailsController {
	
	@Autowired
	VisitorCompleteDetailsIface visitorCompleteDetailsIface;
	
	@GetMapping("/api/fetch-doc-by-document-id/{documentId}")
	public ApiResponse getDocumentDetailsBydocumentId(@PathVariable String documentId) {
		// Get a Base64 Decoder
      //  Base64.Decoder decoder = Base64.getDecoder();

        // Decode the Base64 encoded string
      //  byte[] decodedBytes = decoder.decode(documentId);
        
      //  String decodedString = new String(decodedBytes);
        
        String[] documentString = documentId.split("@");
        System.out.println(" documentString "+documentString+"  hhhhhh "+documentString.length);
        System.out.println(" documentString "+documentString[0]+" documentString[1] "+documentString[1]);
		
		return visitorCompleteDetailsIface.getDocumentDetailsBydocumentId(documentString[0],documentString[1]);
	}
	

}