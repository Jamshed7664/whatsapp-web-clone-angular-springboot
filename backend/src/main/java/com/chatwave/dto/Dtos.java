package com.chatwave.dto;
import com.chatwave.model.*;
import java.time.Instant;
import java.util.List;
public class Dtos {
 public record UserView(Long id,String name,String email,String about,String avatarColor,boolean online){public static UserView from(AppUser u){return new UserView(u.id,u.name,u.email,u.about,u.avatarColor,false);}}
 public record MessageView(Long id,Long senderId,String body,Instant createdAt,Instant readAt){}
 public record ChatView(Long id,String name,boolean groupChat,List<UserView> members,MessageView lastMessage,long unreadCount,Instant updatedAt){}
 public record AuthResult(String token,UserView user){}
 public record Login(String email,String password){}
 public record Register(String name,String email,String password){}
 public record Profile(String name,String about){}
 public record CreateChat(List<Long> userIds,String name){}
 public record Send(String body){}
}
