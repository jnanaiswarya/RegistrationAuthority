/*
 * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021, 
 * All rights reserved.
 */
package ug.daes.ra.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// TODO: Auto-generated Javadoc
/**
 * The Class PropertiesConstants.
 */
@Component
public class PropertiesConstants implements CommandLineRunner {

	/** The properties configuration. */
	@Autowired
	PropertiesConfiguration propertiesConfiguration;

	/** The pkiurl. */
	public static String PKIURL;

	/** The issuecertificatecallbackurl. */
	public static String ISSUECERTIFICATECALLBACKURL;

	/** The notification. */
	public static String NOTIFICATION;

	/** The notification. */
	public static String STATUS;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... args) throws Exception {
		STATUS = propertiesConfiguration.getStatus();
		PKIURL = propertiesConfiguration.getPki();
		ISSUECERTIFICATECALLBACKURL = propertiesConfiguration.getIssuecertificatecallbackurl();
		NOTIFICATION = propertiesConfiguration.getNotification();
	}
}
