package com.chatwave.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="chat_rooms") public class ChatRoom {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String name="";
 public boolean groupChat=false;
 @ManyToMany(fetch=FetchType.EAGER) @JoinTable(name="chat_members",joinColumns=@JoinColumn(name="chat_id"),inverseJoinColumns=@JoinColumn(name="user_id")) public Set<AppUser> members=new HashSet<>();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
}
