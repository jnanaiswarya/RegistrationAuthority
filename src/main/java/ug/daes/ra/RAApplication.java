//
//package ug.daes.ra;
//
//import java.io.IOException;
//
//import java.security.cert.X509Certificate;
//
//import javax.net.ssl.SSLContext;
//
//import jakarta.annotation.PostConstruct;
//import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
//
//import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
//import org.jasypt.encryption.StringEncryptor;
//import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
//import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
////import springfox.documentation.builders.RequestHandlerSelectors;
////import springfox.documentation.spi.DocumentationType;
////import springfox.documentation.spring.web.plugins.Docket;
////import springfox.documentation.swagger2.annotations.EnableSwagger2;
//import ug.daes.DAESService;
//import ug.daes.PKICoreServiceException;
//import ug.daes.Result;
//
//import org.apache.hc.client5.http.impl.classic.HttpClients;
//import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
//import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
//import org.apache.hc.core5.ssl.SSLContextBuilder;
//import org.apache.hc.core5.ssl.TrustStrategy;
//import org.apache.hc.core5.ssl.SSLContexts;
//import org.apache.hc.core5.http.io.SocketConfig;
//import org.apache.hc.core5.util.TimeValue;
//import org.apache.hc.client5.http.io.HttpClientConnectionManager;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//import javax.net.ssl.SSLContext;
//import java.security.cert.X509Certificate;
//
//		/**
// * The Class RAApplication.
// */
//@SpringBootApplication
////@EnableSwagger2
//public class RAApplication extends SpringBootServletInitializer {
//
//	/** The logger. */
//	static Logger logger = LoggerFactory.getLogger(RAApplication.class);
//	@Override
//	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
//				return builder.sources(RAApplication.class);
//			}
//			public static void main(String[] args) throws IOException {
//				SpringApplication.run(RAApplication.class, args);
//				System.out.println("Application started");
//			}
//
//			public static CloseableHttpClient getCloseableHttpClient() {
//				try {
//					SSLContext sslContext = SSLContextBuilder.create()
//							.loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
//							.build();
//
//					var tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
//
//					HttpClientConnectionManager connectionManager =
//							PoolingHttpClientConnectionManagerBuilder.create()
//									.setTlsSocketStrategy(tlsStrategy)
//									.build();
//
//					return HttpClients.custom()
//							.setConnectionManager(connectionManager)
//							.build();
//
//				} catch (Exception e) {
//					logger.error("Exception while creating HTTP client", e);
//					return null;
//				}
//			}
//
//			/*
//	 * (non-Javadoc)
//	 *
//	 * @see
//	 * org.springframework.boot.web.servlet.support.SpringBootServletInitializer#
//	 * configure(org.springframework.boot.builder.SpringApplicationBuilder)
//	 */
//	@Override
//	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
//		return builder.sources(RAApplication.class);
//	}
//
//	/**
//	 * The main method.
//	 *
//	 * @param args the arguments
//	 * @throws IOException Signals that an I/O exception has occurred.
//	 */
//	public static void main(String[] args) throws IOException {
//		SpringApplication.run(RAApplication.class, args);
//		System.out.println("Application started");
//	}
//
//	/**
//	 * Rest template.
//	 *
//	 //* @param builder the builder
//	 * @return the rest template
//	 */
//	// @Bean
//	// public RestTemplate restTemplate(RestTemplateBuilder builder) {
//	// return builder.build();
//	// }
//
////	@Bean
////    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
////        TrustStrategy acceptingTrustStrategy = (chain, authType) -> {
////            return true;
////        };
////        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((KeyStore)null, acceptingTrustStrategy).build();
////        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
////        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
////        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
////        requestFactory.setHttpClient(httpClient);
////        RestTemplate restTemplate = new RestTemplate(requestFactory);
////        return restTemplate;
////    }
//
//	@Bean
//	public RestTemplate restTemplate() throws Exception {
//		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
//
//		SSLContext sslContext = SSLContextBuilder.create()
//				.loadTrustMaterial(null, acceptingTrustStrategy)
//				.build();
//
////		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
//		var tlsStrategy = new DefaultClientTlsStrategy(sslContext);
//
//		HttpClientConnectionManager connectionManager =
//				PoolingHttpClientConnectionManagerBuilder.create()
//						.setTlsSocketStrategy(tlsStrategy)
//						.build();
//
//		CloseableHttpClient httpClient = HttpClients.custom()
//				.setConnectionManager(connectionManager)
//				.build();
//
//		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//
//		requestFactory.setConnectionRequestTimeout(300_000);
//		requestFactory.setConnectTimeout(300_000);
//		requestFactory.setConnectTimeout(300_000);
//
//		return new RestTemplate(requestFactory);
//	}
//	/**
//	 * Product api.
//	 *
//	 * @return the docket
//	 */
////	@Bean
////	public Docket productApi() {
////		return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.basePackage("ug.daes.ra"))
////				.build();
////	}
//
//	/**
//	 * Signatue service initilize.
//	 *
//	 * @throws InterruptedException
//	 */
//
//	// REMOVE @Bean
//	@Component
//	public class SignatureServiceInitializer {
//
//		@PostConstruct
//		public void init() {
//			try {
//				Result result = DAESService.initPKINativeUtils();
//				if (result.getStatus() == 0){
//					System.out.println("started: " + new String(result.getStatusMessage()));
//				} else {
//					System.out.println(new String(result.getResponse()));
//					System.exit(1);
//				}
//			} catch (PKICoreServiceException e) {
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//	}
//
//
//
//			@Bean("jasyptStringEncryptor")
//	public StringEncryptor stringEncryptor() {
//		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
//		config.setPassword("$DttKycImplEngin@@r");
//		config.setAlgorithm("PBEWithHMACSHA512AndAES_256");
//		config.setKeyObtentionIterations("1000");
//		config.setPoolSize("1");
//		config.setProviderName("SunJCE");
//		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
//		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
//		config.setStringOutputType("base64");
//		encryptor.setConfig(config);
//		return encryptor;
//	}
//
//
//
//}


package ug.daes.ra;

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import jakarta.annotation.PostConstruct;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ug.daes.DAESService;
import ug.daes.PKICoreServiceException;
import ug.daes.Result;

@SpringBootApplication
public class RAApplication extends SpringBootServletInitializer {

	static Logger logger = LoggerFactory.getLogger(RAApplication.class);

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(RAApplication.class);
	}

	public static void main(String[] args) throws IOException {
		SpringApplication.run(RAApplication.class, args);
		System.out.println("Application started");
	}

	@Bean
	public RestTemplate restTemplate() throws Exception {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

		SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(null, acceptingTrustStrategy)
				.build();

		var tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);

		HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(tlsStrategy)
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return new RestTemplate(requestFactory);
	}

	@Bean("jasyptStringEncryptor")
	public StringEncryptor stringEncryptor() {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
		config.setPassword("$DttKycImplEngin@@r");
		config.setAlgorithm("PBEWithHMACSHA512AndAES_256");
		config.setKeyObtentionIterations("1000");
		config.setPoolSize("1");
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
		config.setStringOutputType("base64");
		encryptor.setConfig(config);
		return encryptor;
	}

	@Component
	public class SignatureServiceInitializer {

		@PostConstruct
		public void init() {
			try {
				Result result = DAESService.initPKINativeUtils();
				if (result.getStatus() == 0) {
					System.out.println("started: " + new String(result.getStatusMessage()));
				} else {
					System.out.println(new String(result.getResponse()));
					System.exit(1);
				}
			} catch (PKICoreServiceException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
