
package com.chatwave.controller;

import com.chatwave.dto.Dtos.*;
import com.chatwave.model.*;
import com.chatwave.repository.*;
import com.chatwave.service.JwtService;

import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

 private final UserRepository users;
 private final ChatRepository chats;
 private final MessageRepository messages;
 private final ReceiptRepository receipts;
 private final JwtService jwt;
 private final PasswordEncoder encoder;
 private final SimpMessagingTemplate broker;

 public ApiController(
         UserRepository u,
         ChatRepository c,
         MessageRepository m,
         ReceiptRepository r,
         JwtService j,
         PasswordEncoder e,
         SimpMessagingTemplate b
 ) {
  users = u;
  chats = c;
  messages = m;
  receipts = r;
  jwt = j;
  encoder = e;
  broker = b;
 }

 private ResponseStatusException bad(String msg) {
  return new ResponseStatusException(
          HttpStatus.BAD_REQUEST, msg
  );
 }

 private AppUser me(Authentication auth) {
  if (auth == null) {
   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED
   );
  }

  return users.findByEmail(auth.getName())
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.UNAUTHORIZED
                  )
          );
 }

 private ChatRoom chat(Long id, AppUser u) {
  ChatRoom c = chats.findById(id)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  )
          );

  boolean isMember = c.members.stream()
          .anyMatch(x -> x.id.equals(u.id));

  if (!isMember) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN
   );
  }

  return c;
 }

 private MessageView msg(ChatMessage m) {
  return new MessageView(
          m.id,
          m.sender.id,
          m.body,
          m.createdAt,
          null
  );
 }

 private ChatView view(ChatRoom c, AppUser u) {
  var last = messages
          .findTopByChatIdOrderByCreatedAtDesc(c.id)
          .map(this::msg)
          .orElse(null);

  return new ChatView(
          c.id,
          c.name,
          c.groupChat,
          c.members.stream()
                  .map(UserView::from)
                  .sorted(Comparator.comparing(UserView::name))
                  .toList(),
          last,
          receipts.countByMessageChatIdAndUserIdAndReadAtIsNull(
                  c.id, u.id
          ),
          c.updatedAt
  );
 }

 private void notifyMembers(ChatRoom c) {
  for (AppUser u : c.members) {
   broker.convertAndSendToUser(
           u.email,
           "/queue/events",
           Map.of(
                   "chatId", c.id,
                   "updated", true
           )
   );
  }
 }

 // ==============================
 // AUTHENTICATION
 // ==============================

 @PostMapping("/auth/register")
 @Transactional
 public AuthResult register(@RequestBody Register body) {

  if (body == null
          || body.name() == null
          || body.name().trim().length() < 2
          || body.name().length() > 100
          || body.email() == null
          || !body.email().matches(
          "(?i)^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
  )
          || body.password() == null
          || body.password().length() < 6) {

   throw bad(
           "Provide a name, valid email and password of at least 6 characters"
   );
  }

  String email = body.email()
          .trim()
          .toLowerCase(Locale.ROOT);

  if (users.findByEmail(email).isPresent()) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Email already registered"
   );
  }

  AppUser u = new AppUser(
          body.name().trim(),
          email,
          encoder.encode(body.password())
  );

  String[] palette = {
          "#5d8393",
          "#936f9e",
          "#bd8465",
          "#649a83",
          "#87966e"
  };

  u.avatarColor = palette[
          new Random().nextInt(palette.length)
          ];

  users.save(u);

  return new AuthResult(
          jwt.issue(email),
          UserView.from(u)
  );
 }

 @PostMapping("/auth/login")
 public AuthResult login(@RequestBody Login body) {

  if (body == null) {
   throw bad("Email and password are required");
  }

  String email = Objects.toString(
          body.email(), ""
  ).trim().toLowerCase(Locale.ROOT);

  AppUser u = users.findByEmail(email)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.UNAUTHORIZED,
                          "Incorrect email or password"
                  )
          );

  if (!encoder.matches(
          Objects.toString(body.password(), ""),
          u.passwordHash
  )) {
   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Incorrect email or password"
   );
  }

  return new AuthResult(
          jwt.issue(u.email),
          UserView.from(u)
  );
 }

 // ==============================
 // USER PROFILE
 // ==============================

 @GetMapping("/me")
 public UserView meGet(Authentication a) {
  return UserView.from(me(a));
 }

 @PutMapping("/me")
 @Transactional
 public UserView edit(
         Authentication a,
         @RequestBody Profile p
 ) {
  AppUser u = me(a);

  if (p == null
          || p.name() == null
          || p.name().trim().isEmpty()
          || p.name().length() > 100
          || (p.about() != null
          && p.about().length() > 255)) {

   throw bad("Invalid profile details");
  }

  u.name = p.name().trim();
  u.about = Objects.toString(
          p.about(), ""
  );

  users.save(u);

  return UserView.from(u);
 }

 @GetMapping("/users")
 public List<UserView> users(
         Authentication a,
         @RequestParam(defaultValue = "") String q
 ) {
  return users
          .findTop50ByNameContainingIgnoreCaseAndIdNotOrderByNameAsc(
                  q, me(a).id
          )
          .stream()
          .map(UserView::from)
          .toList();
 }

 // ==============================
 // CHAT MANAGEMENT
 // ==============================

 @GetMapping("/chats")
 public List<ChatView> chats(Authentication a) {

  AppUser u = me(a);

  return chats
          .findDistinctByMembersIdOrderByUpdatedAtDesc(u.id)
          .stream()
          .map(c -> view(c, u))
          .toList();
 }

 @PostMapping("/chats")
 @Transactional
 public ChatView create(
         Authentication a,
         @RequestBody CreateChat request
 ) {
  AppUser u = me(a);

  if (request == null
          || request.userIds() == null
          || request.userIds().isEmpty()
          || request.userIds().size() > 50) {

   throw bad("Select 1 to 50 contacts");
  }

  Set<Long> ids = new HashSet<>(
          request.userIds()
  );

  ids.remove(u.id);

  if (ids.isEmpty()) {
   throw bad("Choose other users");
  }

  List<AppUser> others = users.findAllById(ids);

  if (others.size() != ids.size()) {
   throw bad("Some contacts do not exist");
  }

  // Reuse existing one-to-one conversation.
  if (ids.size() == 1) {

   Long peerId = ids.iterator().next();

   for (ChatRoom c : chats
           .findDistinctByMembersIdOrderByUpdatedAtDesc(u.id)) {

    boolean peerExists = c.members.stream()
            .anyMatch(v -> v.id.equals(peerId));

    if (!c.groupChat
            && c.members.size() == 2
            && peerExists) {

     return view(c, u);
    }
   }
  }

  ChatRoom c = new ChatRoom();

  c.groupChat = ids.size() > 1;

  if (c.groupChat
          && (request.name() == null
          || request.name().trim().isEmpty()
          || request.name().length() > 100)) {

   throw bad("Group name required (max 100 characters)");
  }

  c.name = c.groupChat
          ? request.name().trim()
          : "";

  c.members.add(u);
  c.members.addAll(others);

  chats.save(c);

  notifyMembers(c);

  return view(c, u);
 }

 // ==============================
 // CHAT MESSAGES
 // ==============================

 @GetMapping("/chats/{id}/messages")
 public List<MessageView> messages(
         Authentication a,
         @PathVariable Long id
 ) {
  chat(id, me(a));

  List<ChatMessage> list = new ArrayList<>(
          messages.findTop200ByChatIdOrderByCreatedAtDesc(id)
  );

  Collections.reverse(list);

  return list.stream()
          .map(this::msg)
          .toList();
 }

 @PostMapping("/chats/{id}/messages")
 @Transactional
 public MessageView send(
         Authentication a,
         @PathVariable Long id,
         @RequestBody Send request
 ) {
  AppUser u = me(a);

  ChatRoom c = chat(id, u);

  if (request == null
          || request.body() == null
          || request.body().trim().isEmpty()
          || request.body().length() > 5000) {

   throw bad(
           "Message must be 1-5000 characters"
   );
  }

  ChatMessage m = messages.save(
          new ChatMessage(
                  c,
                  u,
                  request.body().trim()
          )
  );

  for (AppUser member : c.members) {
   if (!member.id.equals(u.id)) {
    receipts.save(
            new MessageReceipt(
                    m,
                    member,
                    null
            )
    );
   }
  }

  c.updatedAt = Instant.now();

  chats.save(c);

  notifyMembers(c);

  return msg(m);
 }

 // ==============================
 // MESSAGE READ RECEIPTS
 // ==============================

 @PostMapping("/chats/{id}/read")
 @Transactional
 public void read(
         Authentication a,
         @PathVariable Long id
 ) {
  AppUser u = me(a);

  ChatRoom c = chat(id, u);

  List<MessageReceipt> pending =
          receipts.findByMessageChatIdAndUserIdAndReadAtIsNull(
                  id, u.id
          );

  Instant readTime = Instant.now();

  pending.forEach(
          r -> r.readAt = readTime
  );

  receipts.saveAll(pending);

  if (!pending.isEmpty()) {
   notifyMembers(c);
  }
 }

 // ==============================
 // ERROR HANDLING
 // ==============================

 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String, String>> handle(
         ResponseStatusException e
 ) {
  return ResponseEntity
          .status(e.getStatusCode())
          .body(
                  Map.of(
                          "message",
                          Objects.toString(
                                  e.getReason(),
                                  "Request failed"
                          )
                  )
          );
 }
}
