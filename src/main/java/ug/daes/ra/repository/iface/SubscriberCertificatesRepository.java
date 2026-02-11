package ug.daes.ra.repository.iface;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Ensure this is imported

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ug.daes.ra.model.SubscriberCertificates;

@Repository
public interface SubscriberCertificatesRepository extends JpaRepository<SubscriberCertificates, String> {


	List<SubscriberCertificates> findBysubscriberUniqueId(String subscriberRADataId);

	Optional<SubscriberCertificates> findBycertificateSerialNumber(String serialNumber);

	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.certificateStatus = ?1 AND sc.subscriberUniqueId = ?2")
	List<SubscriberCertificates> findByCertificateStatusAndsubscriberUniqueId(String certificateStatus,
																			  String subscriberUniqueId);

	Optional<SubscriberCertificates> findFirstBySubscriberUniqueIdOrderByCreationDateDesc(String subscriberUid); // Corrected property name


	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.subscriberUniqueId = ?1 ORDER BY sc.creationDate DESC") // Corrected property name
	Optional<SubscriberCertificates> findLatestBySubscriberUniqueIdJPQL(String subscriberUid);


	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.certificateStatus = ?1")
	List<SubscriberCertificates> findByCertificateStatus(String certificateStatus);

	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.certificateStatus = ?1 AND sc.subscriberUniqueId = ?2 AND sc.certificateType = ?3")
	SubscriberCertificates findByCertificateStatusAndsubscriberUniqueIdAndCertificateType(String certificateStatus,
																						  String subscriberUniqueId, String certificateType);


	@Query("SELECT COUNT(sc) FROM SubscriberCertificates sc")
	int getAllCertificateCount();


	@Query("SELECT COUNT(sc.certificateType) FROM SubscriberCertificates sc WHERE sc.certificateStatus = 'ACTIVE'")
	int getIssuedCertificatesCount();


	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.certificateEndDate <= CURRENT_TIMESTAMP AND sc.certificateStatus = 'ACTIVE'")
	List<SubscriberCertificates> findByCertificateStatusExpired();

	@Query("""
    SELECT 
        scv.subscriberCount,
        scv.activeSubscriberCount,
        scv.inactiveSubscriberCount,
        scv.disableSubscriberCount,
        scv.certRevokeSubscriberCount,
        scv.certExpiredSubscriberCount,
        scv.onboardedSubscriberCount,
        ccv.activeCertCount,
        ccv.revokeCertCount,
        ccv.expiredCertCount,
        ccv.certCount
    FROM SubscriberCountView scv, CertificateCountView ccv
""")
	Object[] getSubscriberAndCertCount();


//	@Query("SELECT new map(" +
//			"s.activeSubscriberCount AS activeSubscriberCount, " +
//			"s.inactiveSubscriberCount AS inactiveSubscriberCount, " +
//			"s.onboardedSubscriberCount AS onboardedSubscriberCount, " +
//			"s.subscriberCount AS subscriberCount, " +
//			"s.disableSubscriberCount AS disableSubscriberCount, " +
//			"s.certRevokeSubscriberCount AS certRevokeSubscriberCount, " +
//			"s.registeredSubscriberCount AS registeredSubscriberCount, " +
//			"s.certExpiredSubscriberCount AS certExpiredSubscriberCount, " +
//			"c.activeCertificateCount AS activeCertificateCount, " +
//			"c.revokedCertificateCount AS revokedCertificateCount, " +
//			"c.expiredCertificateCount AS expiredCertificateCount) " +
//			"FROM CertificateCountView c, SubscriberCountView s")
//	Map<String, BigInteger> getSubscriberAndCertCount();


	@Query("SELECT sc FROM SubscriberCertificates sc WHERE sc.subscriberUniqueId = ?1")
	List<SubscriberCertificates> findBySubscriberUniqueIdToExpireCert(String subscriberUid);

//	@Query("SELECT sc FROM SubscriberCertificates sc " +
//			"WHERE sc.subscriberUniqueId = :subscriberUid " +
//			"ORDER BY sc.creationDate DESC")
//	SubscriberCertificates findBySubscriberUniqueId(@Param("subscriberUid") String subscriberUid);

	SubscriberCertificates findTopBySubscriberUniqueIdOrderByCreationDateDesc(String subscriberUniqueId);
}