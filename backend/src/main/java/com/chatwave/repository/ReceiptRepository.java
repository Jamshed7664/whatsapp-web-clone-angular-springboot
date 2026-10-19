package com.chatwave.repository;
import com.chatwave.model.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReceiptRepository extends JpaRepository<MessageReceipt,Long>{Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId,Long userId);long countByMessageChatIdAndUserIdAndReadAtIsNull(Long chatId,Long userId);List<MessageReceipt> findByMessageChatIdAndUserIdAndReadAtIsNull(Long chatId,Long userId);}
