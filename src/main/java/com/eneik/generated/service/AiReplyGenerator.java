package com.eneik.generated.service;

import com.eneik.generated.model.Message;
import java.util.List;

public interface AiReplyGenerator {
    String generateReply(List<Message> context, String aiState);
}
