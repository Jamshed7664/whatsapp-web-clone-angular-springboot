package com.chatwave.model;
import jakarta.persistence.*;
@Entity @Table(name="app_users") public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=100) public String name;
 @Column(nullable=false,unique=true,length=190) public String email;
 @Column(nullable=false) public String passwordHash;
 @Column(length=255) public String about="Hey there! I am using ChatWave.";
 @Column(nullable=false) public String avatarColor="#568a9b";
 public AppUser(){} public AppUser(String name,String email,String hash){this.name=name;this.email=email;this.passwordHash=hash;}
}
