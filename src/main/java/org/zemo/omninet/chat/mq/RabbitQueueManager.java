package org.zemo.omninet.chat.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RabbitQueueManager {

    private final RabbitAdmin admin;

    @Value("${chat.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${chat.rabbitmq.queue.prefix}")
    private String queuePrefix;

    private String sanitizeForQueue(String email) {
        return email.replace("@", "_at_").replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public String ensureQueueFor(String email) {
        String qName = queuePrefix + sanitizeForQueue(email);
        Queue q = new Queue(qName, true, false, false);
        admin.declareQueue(q);
        Binding b = BindingBuilder.bind(q).to(new DirectExchange(exchangeName, true, false)).with(email);
        admin.declareBinding(b);
        return qName;
    }
}
