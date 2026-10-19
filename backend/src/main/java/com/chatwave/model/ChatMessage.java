package com.chatwave.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="chat_messages",indexes={@Index(name="idx_chat_time",columnList="chat_id,created_at")}) public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="chat_id") public ChatRoom chat;
 @ManyToOne(optional=false) public AppUser sender;
 @Column(nullable=false,length=5000) public String body;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 public ChatMessage(){} public ChatMessage(ChatRoom chat,AppUser sender,String body){this.chat=chat;this.sender=sender;this.body=body;}
}
