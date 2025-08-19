package org.zemo.omninet.chat.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.zemo.omninet.chat.dto.MessageView;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange chatExchange;
    private final RabbitQueueManager queueManager;

    public void publishUndelivered(MessageView view) {
        queueManager.ensureQueueFor(view.getReceiverEmail());
        rabbitTemplate.convertAndSend(chatExchange.getName(), view.getReceiverEmail(), view);
    }

    public List<MessageView> drainFor(String email) {
        String qName = queueManager.ensureQueueFor(email);
        List<MessageView> out = new ArrayList<>();
        while (true) {
            Object msg = rabbitTemplate.receiveAndConvert(qName);
            if (msg == null) break;
            out.add((MessageView) msg);
        }
        return out;
    }
}
