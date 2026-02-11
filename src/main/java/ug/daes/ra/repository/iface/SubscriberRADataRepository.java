/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.repository.iface;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ug.daes.ra.model.SubscriberRaData;

// TODO: Auto-generated Javadoc
/**
 * The Interface SubscriberCertificateDataRepository.
 */
@Repository
public interface SubscriberRADataRepository extends JpaRepository<SubscriberRaData, String> {

	/**
	 * Find bysubscriber unique id.
	 *
	 * @param subscriberUniqueId
	 *            the subscriber unique id
	 * @return the subscriber ra data
	 */
	SubscriberRaData findBysubscriberUniqueId(String subscriberUniqueId);
}
