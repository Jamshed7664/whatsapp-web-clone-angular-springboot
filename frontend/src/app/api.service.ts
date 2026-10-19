import { Injectable,inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthResponse, User, Chat, Message } from './models';
@Injectable({providedIn:'root'}) export class ApiService {
 private http=inject(HttpClient); readonly base='http://localhost:8080/api';
 login(email:string,password:string){return this.http.post<AuthResponse>(`${this.base}/auth/login`,{email,password})}
 register(name:string,email:string,password:string){return this.http.post<AuthResponse>(`${this.base}/auth/register`,{name,email,password})}
 me(){return this.http.get<User>(`${this.base}/me`)}
 updateProfile(name:string,about:string){return this.http.put<User>(`${this.base}/me`,{name,about})}
 users(q=''){return this.http.get<User[]>(`${this.base}/users`,{params:{q}})}
 chats(){return this.http.get<Chat[]>(`${this.base}/chats`)}
 createChat(userIds:number[],name=''){return this.http.post<Chat>(`${this.base}/chats`,{userIds,name})}
 messages(id:number){return this.http.get<Message[]>(`${this.base}/chats/${id}/messages`)}
 send(id:number,body:string){return this.http.post<Message>(`${this.base}/chats/${id}/messages`,{body})}
 read(id:number){return this.http.post<void>(`${this.base}/chats/${id}/read`,{})}
}
