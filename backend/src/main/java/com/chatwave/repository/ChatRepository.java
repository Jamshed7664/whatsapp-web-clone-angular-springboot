package com.chatwave.repository;
import com.chatwave.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ChatRepository extends JpaRepository<ChatRoom,Long>{List<ChatRoom> findDistinctByMembersIdOrderByUpdatedAtDesc(Long userId);}
