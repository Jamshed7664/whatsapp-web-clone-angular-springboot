import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { Client, IMessage } from "@stomp/stompjs";
import { ApiService } from "./api.service";
import { AuthResponse, User, Chat, Message } from "./models";
@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.css",
})
export class AppComponent implements OnInit, OnDestroy {
  api = inject(ApiService);
  me = signal<User | null>(null);
  chats = signal<Chat[]>([]);
  activeId = signal<number | null>(null);
  messages = signal<Message[]>([]);
  search = signal("");
  text = "";
  email = "";
  password = "";
  name = "";
  loginMode = true;
  error = "";
  busy = false;
  dark = signal(localStorage.getItem("chatwave_theme") !== "light");
  panel = signal<"none" | "new" | "profile" | "info">("none");
  people = signal<User[]>([]);
  peopleQuery = "";
  groupMode = false;
  groupName = "";
  selectedUsers: number[] = [];
  profileName = "";
  profileAbout = "";
  sidebarOpen = signal(true);
  menuOpen = signal(false);
  private client?: Client;
  active = computed(
    () => this.chats().find((c) => c.id === this.activeId()) ?? null
  );
  filtered = computed(() =>
    this.chats().filter((c) => {
      const q = this.search().trim().toLowerCase();
      return (
        !q ||
        this.chatName(c).toLowerCase().includes(q) ||
        c.lastMessage?.body.toLowerCase().includes(q)
      );
    })
  );
  ngOnInit() {
    if (localStorage.getItem("chatwave_token")) this.restore();
  }
  ngOnDestroy() {
    void this.client?.deactivate();
  }
  icon(name: string) {
    return name;
  }
  private fail(e: unknown) {
    const err = e as HttpErrorResponse;
    this.error =
      err.error?.message || err.message || "Could not complete the request.";
    this.busy = false;
  }
  auth() {
    this.error = "";
    this.busy = true;
    const request = this.loginMode
      ? this.api.login(this.email, this.password)
      : this.api.register(this.name, this.email, this.password);
    request.subscribe({
      next: (a) => this.signedIn(a),
      error: (e) => this.fail(e),
    });
  }
  signedIn(a: AuthResponse) {
    localStorage.setItem("chatwave_token", a.token);
    this.me.set(a.user);
    this.busy = false;
    this.error = "";
    this.refreshChats();
    this.connectSocket();
  }
  restore() {
    this.api.me().subscribe({
      next: (u) => {
        this.me.set(u);
        this.refreshChats();
        this.connectSocket();
      },
      error: () => this.logout(),
    });
  }
  logout() {
    void this.client?.deactivate();
    this.client = undefined;
    localStorage.removeItem("chatwave_token");
    this.me.set(null);
    this.chats.set([]);
    this.activeId.set(null);
    this.panel.set("none");
    this.menuOpen.set(false);
  }
  connectSocket() {
    void this.client?.deactivate();
    const t = localStorage.getItem("chatwave_token");
    if (!t) return;
    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      connectHeaders: { Authorization: `Bearer ${t}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    client.onConnect = () =>
      client.subscribe("/user/queue/events", (_m: IMessage) => {
        this.refreshChats();
        if (this.activeId()) this.refreshMessages(this.activeId()!);
      });
    client.onStompError = (f) => console.error("STOMP:", f.headers["message"]);
    client.activate();
    this.client = client;
  }
  refreshChats() {
    this.api.chats().subscribe({
      next: (c) => {
        this.chats.set(c);
        const id = this.activeId();
        if (id && !c.some((ch) => ch.id === id)) {
          this.activeId.set(null);
          this.messages.set([]);
        }
      },
      error: (e) => this.fail(e),
    });
  }
  openChat(c: Chat) {
    this.activeId.set(c.id);
    this.panel.set("none");
    this.sidebarOpen.set(false);
    this.messages.set([]);
    this.refreshMessages(c.id);
    this.api.read(c.id).subscribe({ next: () => this.refreshChats() });
  }
  refreshMessages(id: number) {
    this.api.messages(id).subscribe({
      next: (m) => {
        if (this.activeId() === id) {
          this.messages.set(m);
          this.scrollBottom();
        }
      },
      error: (e) => this.fail(e),
    });
  }
  send() {
    const body = this.text.trim(),
      id = this.activeId();
    if (!id || !body) return;
    this.text = "";
    this.api.send(id, body).subscribe({
      next: (m) => {
        if (this.activeId() === id) {
          this.messages.update((v) =>
            v.some((x) => x.id === m.id) ? v : [...v, m]
          );
          this.scrollBottom();
        }
        this.refreshChats();
      },
      error: (e) => {
        this.text = body;
        this.fail(e);
      },
    });
  }
  chatName(c: Chat) {
    return c.groupChat
      ? c.name
      : c.members.find((u) => u.id !== this.me()?.id)?.name || "My notes";
  }
  other(c: Chat) {
    return c.members.find((u) => u.id !== this.me()?.id);
  }
  color(c: Chat) {
    return this.other(c)?.avatarColor || "#778da9";
  }
  initial(s: string) {
    return (s?.trim()?.[0] || "?").toUpperCase();
  }
  preview(c: Chat) {
    return c.lastMessage?.body || "Start a conversation";
  }
  time(s?: string) {
    if (!s) return "";
    const d = new Date(s),
      now = new Date();
    return d.toDateString() === now.toDateString()
      ? d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
      : d.toLocaleDateString([], { month: "short", day: "numeric" });
  }
  msgTime(s: string) {
    return new Date(s).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  }
  isMine(m: Message) {
    return m.senderId === this.me()?.id;
  }
  toggleTheme() {
    this.dark.update((x) => !x);
    localStorage.setItem("chatwave_theme", this.dark() ? "dark" : "light");
  }
  newChat() {
    this.panel.set("new");
    this.peopleQuery = "";
    this.groupMode = false;
    this.groupName = "";
    this.selectedUsers = [];
    this.loadPeople();
  }
  loadPeople() {
    this.api
      .users(this.peopleQuery)
      .subscribe({
        next: (p) => this.people.set(p),
        error: (e) => this.fail(e),
      });
  }
  togglePerson(id: number) {
    this.selectedUsers = this.selectedUsers.includes(id)
      ? this.selectedUsers.filter((x) => x !== id)
      : [...this.selectedUsers, id];
  }
  createWith(u: User) {
    this.api.createChat([u.id]).subscribe({
      next: (c) => {
        this.refreshChats();
        this.openChat(c);
      },
      error: (e) => this.fail(e),
    });
  }
  createGroup() {
    if (this.selectedUsers.length < 2 || !this.groupName.trim()) return;
    this.api.createChat(this.selectedUsers, this.groupName.trim()).subscribe({
      next: (c) => {
        this.refreshChats();
        this.openChat(c);
      },
      error: (e) => this.fail(e),
    });
  }
  editProfile() {
    const u = this.me();
    if (!u) return;
    this.profileName = u.name;
    this.profileAbout = u.about;
    this.panel.set("profile");
  }
  saveProfile() {
    this.api.updateProfile(this.profileName, this.profileAbout).subscribe({
      next: (u) => {
        this.me.set(u);
        this.panel.set("none");
        this.refreshChats();
      },
      error: (e) => this.fail(e),
    });
  }
  scrollBottom() {
    setTimeout(
      () =>
        document
          .getElementById("message-bottom")
          ?.scrollIntoView({ behavior: "smooth", block: "end" }),
      50
    );
  }
  trackChat(_i: number, c: Chat) {
    return c.id;
  }
  trackMsg(_i: number, m: Message) {
    return m.id;
  }
}
