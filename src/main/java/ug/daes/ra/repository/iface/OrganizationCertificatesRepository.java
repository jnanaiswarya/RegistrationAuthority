package ug.daes.ra.repository.iface;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ug.daes.ra.model.OrganizationCertificates;

@Repository
public interface OrganizationCertificatesRepository extends JpaRepository<OrganizationCertificates, String> {

	// JPQL version: find by certificateStatus and organizationUid
	@Query("SELECT i FROM OrganizationCertificates i WHERE i.certificateStatus = ?1 AND i.organizationUid = ?2")
	OrganizationCertificates findByCertificateStatusAndOrganizationUniqueId(String paramString1, String paramString2);

	// JPQL version: same as above, but returns list
	@Query("SELECT i FROM OrganizationCertificates i WHERE i.certificateStatus = ?1 AND i.organizationUid = ?2")
	List<OrganizationCertificates> findByCertificateStatusAndOrganizationUid(String paramString1, String paramString2);

	// JPQL version: expired certificates (you must compare with CURRENT_DATE)
	@Query("SELECT i FROM OrganizationCertificates i WHERE i.certificateEndDate <= CURRENT_DATE AND i.certificateStatus = 'ACTIVE'")
	List<OrganizationCertificates> findByCertificateStatusExpired();

	// JPQL version: get certificateData (single column)
	@Query("SELECT i.certificateData FROM OrganizationCertificates i WHERE i.organizationUid = ?1 AND i.certificateStatus = 'ACTIVE'")
	String getOrgCertData(String ouid);
}
