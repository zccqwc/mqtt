package com.example.mqtt.demo.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT配置，生产者
 */
@Slf4j
@Configuration
public class MqttConfig {

    private static final byte[] WILL_DATA;

    static {
        WILL_DATA = "offline".getBytes();
    }

    /**
     * 订阅的bean名称，入库通道（消费者）
     */
    public static final String INBOUND_CHANNEL = "inboundChannel";
    /**
     * 发布的bean名称，出库通道（生产者）
     */
    public static final String OUTBOUND_CHANNEL = "outboundChannel";

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.url}")
    private String url;

    @Value("${mqtt.producer.clientId}")
    private String producerClientId;

    @Value("${mqtt.producer.defaultTopic}")
    private String producerDefaultTopic;

    @Value("${mqtt.consumer.clientId}")
    private String consumerClientId;

    @Value("${mqtt.consumer.defaultTopic}")
    private String consumerDefaultTopic;

    /**
     * Description:
     * 〈MQTT连接器选项〉
     *
    []
     * @return : org.eclipse.paho.client.mqttv3.MqttConnectOptions
     */
    @Bean
    public MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
        // 这里设置为true表示每次连接到服务器都以新的身份连接
        options.setCleanSession(true);
        // 设置连接的用户名
        options.setUserName(username);
        // 设置连接的密码
        options.setPassword(password.toCharArray());

        options.setServerURIs(url.split(","));
        // 设置超时时间 单位为秒
        options.setConnectionTimeout(10);
        // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送心跳判断客户端是否在线，但这个方法并没有重连的机制
        options.setKeepAliveInterval(20);
        // 设置“遗嘱”消息的话题，若客户端与服务器之间的连接意外中断，服务器将发布客户端的“遗嘱”消息。
        options.setWill("willTopic", WILL_DATA, 2, false);

        return options;
    }


    /**
     * Description:
     * 〈MQTT客户端〉
     *
    []
     * @return : org.springframework.integration.mqtt.core.MqttPahoClientFactory
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getMqttConnectOptions());
        return factory;
    }

    /**
     * Description:
     * 〈MQTT信息通道（生产者）〉
     *
    []
     * @return : org.springframework.messaging.MessageChannel
     */
    @Bean(name = OUTBOUND_CHANNEL)
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * Description:
     * 〈MQTT消息处理器（生产者）〉
     *
    []
     * @return : org.springframework.messaging.MessageHandler
     */
    @Bean
    @ServiceActivator(inputChannel = OUTBOUND_CHANNEL)
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(producerClientId, mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(producerDefaultTopic);
        return messageHandler;
    }

    /**
     * Description:
     * 〈MQTT消息订阅绑定（消费者）〉
     * <p>
     * []
     *
     * @return : org.springframework.integration.core.MessageProducer
     */
    @Bean
    public MessageProducer inbound() {
        // 可以同时消费（订阅）多个Topic
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(consumerClientId,
                        mqttClientFactory(), consumerDefaultTopic.split(","));

        adapter.setCompletionTimeout(5000);

        adapter.setConverter(new DefaultPahoMessageConverter());

        // 发布消息等级：0：最多一次的传输（可能丢失数据），1：至少一次的传输（可能重复）
        adapter.setQos(1);

        // 设置订阅通道
        adapter.setOutputChannel(mqttInboundChannel());

        return adapter;
    }

    /**
     * Description:
     * 〈MQTT信息通道（消费者）〉
     * <p>
     * []
     *
     * @return : org.springframework.messaging.MessageChannel
     */
    @Bean(name = INBOUND_CHANNEL)
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }
}
