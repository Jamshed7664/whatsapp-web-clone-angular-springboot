package com.chatwave.config;
import com.chatwave.repository.*;
import com.chatwave.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.config.annotation.*;
import java.security.Principal;
@Configuration @EnableWebSocketMessageBroker public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 private final JwtService jwt;private final UserRepository users;private final String origin;
 public WebSocketConfig(JwtService jwt,UserRepository users,@Value("${app.cors-origin}") String origin){this.jwt=jwt;this.users=users;this.origin=origin;}
 @Override public void registerStompEndpoints(StompEndpointRegistry r){r.addEndpoint("/ws").setAllowedOrigins(origin);}
 @Override public void configureMessageBroker(MessageBrokerRegistry r){r.enableSimpleBroker("/queue");r.setUserDestinationPrefix("/user");}
 @Override public void configureClientInboundChannel(ChannelRegistration r){r.interceptors(new ChannelInterceptor(){@Override public Message<?> preSend(Message<?> message,MessageChannel channel){StompHeaderAccessor a=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);if(a==null)return message;
 if(StompCommand.CONNECT.equals(a.getCommand())){String bearer=a.getFirstNativeHeader("Authorization");if(bearer==null||!bearer.startsWith("Bearer "))throw new IllegalArgumentException("Missing authentication");try{String email=jwt.email(bearer.substring(7));if(users.findByEmail(email).isEmpty())throw new IllegalArgumentException("Unknown user");a.setUser(()->email);}catch(Exception e){throw new IllegalArgumentException("Invalid token");}}
 else if(StompCommand.SUBSCRIBE.equals(a.getCommand())){Principal p=a.getUser();if(p==null||!"/user/queue/events".equals(a.getDestination()))throw new IllegalArgumentException("Unauthorized subscription");}
 else if(StompCommand.SEND.equals(a.getCommand()))throw new IllegalArgumentException("Client sends not supported");return message;}});}
}
