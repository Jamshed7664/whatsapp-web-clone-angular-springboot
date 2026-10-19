package com.chatwave.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="message_receipts",uniqueConstraints=@UniqueConstraint(columnNames={"message_id","user_id"})) public class MessageReceipt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="message_id") public ChatMessage message;
 @ManyToOne(optional=false) @JoinColumn(name="user_id") public AppUser user;
 public Instant readAt;
 public MessageReceipt(){} public MessageReceipt(ChatMessage m,AppUser u,Instant d){message=m;user=u;readAt=d;}
}
