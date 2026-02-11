///*
// * @copyright (DigitalTrust Technologies Private Limited, Hyderabad) 2021,
// * All rights reserved.
// */
//package ug.daes.ra.utils;
//
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//// TODO: Auto-generated Javadoc
///**
// * The Class RabbitMQConfig.
// */
//@Configuration
//public class RabbitMQConfig {
//
//	/** The queue name. */
//	@Value("${com.dt.rabbitmq.queue}")
//	String queueName;
//
//	/** The exchange. */
//	@Value("${com.dt.rabbitmq.exchange}")
//	String exchange;
//
//	/** The routingkey. */
//	@Value("${com.dt.rabbitmq.routingkey}")
//	private String routingkey;
//
//	/**
//	 * Queue.
//	 *
//	 * @return the queue
//	 */
//	@Bean
//	Queue queue() {
//		return new Queue(queueName, true);
//	}
//
//	/**
//	 * Exchange.
//	 *
//	 * @return the direct exchange
//	 */
//	@Bean
//	DirectExchange exchange() {
//		return new DirectExchange(exchange);
//	}
//
//	/**
//	 * Binding.
//	 *
//	 * @param queue the queue
//	 * @param exchange the exchange
//	 * @return the binding
//	 */
//	@Bean
//	Binding binding(Queue queue, DirectExchange exchange) {
//		return BindingBuilder.bind(queue).to(exchange).with(routingkey);
//	}
//
//	/**
//	 * Json message converter.
//	 *
//	 * @return the message converter
//	 */
//	@Bean
//	public MessageConverter jsonMessageConverter() {
//		return new Jackson2JsonMessageConverter();
//	}
//
//	/**
//	 * Rabbit template.
//	 *
//	 * @param connectionFactory the connection factory
//	 * @return the amqp template
//	 */
//	public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//		rabbitTemplate.setMessageConverter(jsonMessageConverter());
//		return rabbitTemplate;
//	}
//}
