export interface User {id:number; name:string; email:string; about:string; avatarColor:string; online:boolean}
export interface Message {id:number; senderId:number; body:string; createdAt:string; readAt:string|null}
export interface Chat {id:number; name:string; groupChat:boolean; members:User[]; lastMessage:Message|null; unreadCount:number; updatedAt:string}
export interface AuthResponse {token:string;user:User}
