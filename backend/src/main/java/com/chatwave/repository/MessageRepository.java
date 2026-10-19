package com.chatwave.repository;
import com.chatwave.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MessageRepository extends JpaRepository<ChatMessage,Long>{List<ChatMessage> findTop200ByChatIdOrderByCreatedAtDesc(Long chatId); Optional<ChatMessage> findTopByChatIdOrderByCreatedAtDesc(Long chatId);}
