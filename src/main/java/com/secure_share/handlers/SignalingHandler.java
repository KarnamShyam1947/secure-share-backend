package com.secure_share.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secure_share.config.custom.CustomUserDetails;
import com.secure_share.dto.WebsocketUserInfo;
import com.secure_share.entities.UserEntity;

@SuppressWarnings("null")  
public class SignalingHandler extends TextWebSocketHandler {
    // roomId -> sessions with user info
    private static final Map<String, Map<WebSocketSession, WebsocketUserInfo>> rooms = new ConcurrentHashMap<>();
    // sessionId -> roomId (for cleanup)
    private static final Map<String, String> sessionRoom = new ConcurrentHashMap<>();
    // Maximum users per room
    private static final int MAX_USERS_PER_ROOM = 2;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[CONNECTED] " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            String roomId = jsonNode.get("room").asText();
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;
            
            System.out.printf("Room: %s, Type: %s, Session: %s\n", roomId, type, session.getId());

            // Check if this is a new user trying to join
            if (!sessionRoom.containsKey(session.getId())) {
                Map<WebSocketSession, WebsocketUserInfo> roomSessions = rooms.get(roomId);
                
                // Check room capacity
                if (roomSessions != null && roomSessions.size() >= MAX_USERS_PER_ROOM) {
                    // Room is full, send error message
                    sendErrorMessage(session, "ROOM_FULL", "This room is full. Maximum 2 users allowed.");
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Room is full"));
                    return;
                }
                
                // Get current user information
                WebsocketUserInfo currentWebsocketUserInfo = getCurrentWebsocketUserInfo();
                
                // Add user to room
                rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session, currentWebsocketUserInfo);
                sessionRoom.put(session.getId(), roomId);
                
                // Notify about room status with user information
                Map<WebSocketSession, WebsocketUserInfo> currentRoomSessions = rooms.get(roomId);
                sendRoomStatusUpdate(roomId, currentRoomSessions);
                
                System.out.println("User '" + currentWebsocketUserInfo.getUsername() + "' joined room " + roomId + 
                                 ". Current users: " + currentRoomSessions.size());
            }

            // Forward signaling messages to other users in the room
            Map<WebSocketSession, WebsocketUserInfo> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                for (Map.Entry<WebSocketSession, WebsocketUserInfo> entry : roomSessions.entrySet()) {
                    WebSocketSession s = entry.getKey();
                    if (!s.getId().equals(session.getId()) && s.isOpen()) {
                        try {
                            s.sendMessage(new TextMessage(payload));
                        } catch (Exception e) {
                            System.err.println("Error sending message to session " + s.getId() + ": " + e.getMessage());
                            // Remove broken session
                            roomSessions.remove(s);
                            sessionRoom.remove(s.getId());
                        }
                    }
                }
            }

            if ("leave".equals(type)) {
                closeSession(session);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage(session, "INVALID_MESSAGE", "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("Closing connection: " + session.getId() + ", Status: " + status);
        closeSession(session);
    }

    private void closeSession(WebSocketSession session) {
        String roomId = sessionRoom.remove(session.getId());
        if (roomId != null) {
            Map<WebSocketSession, WebsocketUserInfo> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                WebsocketUserInfo leavingUser = roomSessions.remove(session);
                
                int remainingUsers = roomSessions.size();
                String username = leavingUser != null ? leavingUser.getUsername() : "Unknown";
                System.out.println("User '" + username + "' left room " + roomId + 
                                 ". Remaining users: " + remainingUsers);
                
                // Notify remaining users about room status
                sendRoomStatusUpdate(roomId, roomSessions);
                
                // Clean up empty rooms
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                    System.out.println("Room " + roomId + " removed (empty)");
                }
            }
        }
    }
    
    private WebsocketUserInfo getCurrentWebsocketUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                var principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    UserEntity currentUser = ((CustomUserDetails) principal).getUserEntity();
                    if (currentUser != null) 
                        return new WebsocketUserInfo(
                            currentUser.getFirstName(), 
                            Integer.toString(currentUser.getId()).toString(), 
                            currentUser.getWalletAddress()
                        );
                    
                }
                // Fallback to principal name if CustomUserDetails not available
                String principalName = authentication.getName();
                if (principalName != null && !principalName.equals("anonymousUser")) 
                    return new WebsocketUserInfo(principalName, "unknown", "unknown-wallet-address");
                
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }
        
        // Default fallback
        return new WebsocketUserInfo("Unknown User", "unknown", "unknown-wallet-address");
    }
    
    private void sendErrorMessage(WebSocketSession session, String errorType, String message) {
        try {
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "error");
            errorMsg.put("errorType", errorType);
            errorMsg.put("message", message);
            
            String errorJson = objectMapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            System.err.println("Error sending error message: " + e.getMessage());
        }
    }
    
    private void sendRoomStatusUpdate(String roomId, Map<WebSocketSession, WebsocketUserInfo> roomSessions) {
        try {
            List<Map<String, Object>> users = new ArrayList<>();
            for (WebsocketUserInfo websocketUserInfo : roomSessions.values()) {
                Map<String, Object> user = new HashMap<>();
                user.put("username", websocketUserInfo.getUsername());
                user.put("userId", websocketUserInfo.getUserId());
                user.put("joinedAt", websocketUserInfo.getJoinedAt());
                user.put("walletAddress", websocketUserInfo.getWalletAddress());

                users.add(user);
            }
            
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "room_status");
            statusMsg.put("room", roomId);
            statusMsg.put("userCount", roomSessions.size());
            statusMsg.put("maxUsers", MAX_USERS_PER_ROOM);
            statusMsg.put("isFull", roomSessions.size() >= MAX_USERS_PER_ROOM);
            statusMsg.put("users", users);
            
            String statusJson = objectMapper.writeValueAsString(statusMsg);
            
            for (WebSocketSession session : roomSessions.keySet()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(statusJson));
                    } catch (Exception e) {
                        System.err.println("Error sending room status to session " + session.getId());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending room status update: " + e.getMessage());
        }
    }
    
    // Get current room statuses with user information (useful for debugging)
    public static Map<String, List<String>> getRoomStatusesWithUsers() {
        Map<String, List<String>> statuses = new HashMap<>();
        for (Map.Entry<String, Map<WebSocketSession, WebsocketUserInfo>> entry : rooms.entrySet()) {
            List<String> usernames = new ArrayList<>();
            for (WebsocketUserInfo websocketUserInfo : entry.getValue().values()) {
                usernames.add(websocketUserInfo.getUsername());
            }
            statuses.put(entry.getKey(), usernames);
        }
        return statuses;
    }
}
