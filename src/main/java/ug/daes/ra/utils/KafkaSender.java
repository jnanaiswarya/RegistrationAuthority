package ug.daes.ra.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import ug.daes.ra.request.entity.LogModel;

@Service
public class KafkaSender {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${com.dt.kafka.topic.central}")
    private String topic;

    @Value("${com.dt.kafka.ra.topic}")
    private String raTopic;

    public void send(LogModel logmodel) {
        kafkaTemplate.send(topic, logmodel);
        kafkaTemplate.send(raTopic, logmodel);
        System.out.println("Kafka messages sent to topics: " + topic + ", " + topic);
        System.out.println("Kafka messages sent to topics: " + topic + ", " + raTopic);
    }
}
