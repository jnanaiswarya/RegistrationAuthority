///*
// * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021,
// * All rights reserved.
// */
//package ug.daes.ra.utils;
//
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import ug.daes.ra.request.entity.LogModel;
//
//// TODO: Auto-generated Javadoc
///**
// * The Class RabbitMQSender.
// */
//@Service
//public class RabbitMQSender {
//
//	/** The amqp template. */
//	@Autowired
//	private AmqpTemplate amqpTemplate;
//
//	/** The exchange. */
//	@Value("${com.dt.rabbitmq.exchange}")
//	private String exchange;
//
//
//	/** The routingkey. */
//	@Value("${com.dt.rabbitmq.routingkey}")
//	private String routingkey;
//
//	@Value("${com.dt.rabbitmq.ra.routingkey}")
//	private String raRoutingkey;
//
//
//	/**
//	 * Send.
//	 *
//	 * @param logmodel the logmodel
//	 */
//	public void send(LogModel logmodel) {
//		amqpTemplate.convertAndSend(exchange, routingkey, logmodel);
//		amqpTemplate.convertAndSend(exchange, raRoutingkey, logmodel);
//
//	}
//}