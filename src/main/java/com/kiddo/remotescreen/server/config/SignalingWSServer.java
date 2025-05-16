package com.kiddo.remotescreen.server.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiddo.remotescreen.server.entity.Device;
import com.kiddo.remotescreen.server.service.DeviceService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/auth/signaling/{deviceId}")
public class SignalingWSServer {

    private static final Logger log = LoggerFactory.getLogger(SignalingWSServer.class);

    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper()
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceId") String deviceId) {
        try {
            DeviceService deviceService = SpringContext.getBean(DeviceService.class);

            Device asPcDevice = deviceService.getDeviceById(deviceId);
            if (asPcDevice != null) {
                // ✅ Đây là thiết bị PC
//                if (!Boolean.TRUE.equals(asPcDevice.getAllowRemote())) {
//                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Remote access disabled"));
//                    return;
//                }

                sessionMap.put(deviceId, session);
                log.info("✅ PC connected: {}", deviceId);
                return;
            }

            // ✅ Đây là Android (PixelTest) → kiểm tra xem nó có được phép không
            for (String pcId : sessionMap.keySet()) {
                Device pc = deviceService.getDeviceById(pcId);
                if (pc != null && deviceId.equals(pc.getConnectedAndroid())) {
                    sessionMap.put(deviceId, session);
                    log.info("✅ Android '{}' connected to PC '{}'", deviceId, pcId);
                    return;
                }
            }

            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized Android"));

        } catch (Exception e) {
            log.error("💥 WebSocket onOpen error: {}", e.getMessage(), e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal error"));
            } catch (Exception ignore) {}
        }
    }

    @OnClose
    public void onClose(Session session) {
        String disconnectedId = null;

        try {
            for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
                if (entry.getValue().equals(session)) {
                    disconnectedId = entry.getKey();
                    break;
                }
            }

            if (disconnectedId == null) {
                log.warn("🔌 Unknown session closed (not found in sessionMap)");
                return;
            }

            sessionMap.remove(disconnectedId);
            log.info("🔌 WebSocket closed: {}", disconnectedId);

            DeviceService deviceService = SpringContext.getBean(DeviceService.class);
            List<String> allDeviceIds = deviceService.getAllDeviceIds();

            for (String deviceId : allDeviceIds) {
                Device device = deviceService.getDeviceById(deviceId);
                if (device == null) continue;

                // TH1: Android disconnect
                if (disconnectedId.equals(device.getConnectedAndroid())) {
                    device.setConnectedAndroid(null);
                    deviceService.save(device);
                    log.info("🧹 Android '{}' disconnected → cleared from PC '{}'", disconnectedId, deviceId);
                    break;
                }

                // TH2: PC disconnect
                if (disconnectedId.equals(device.getDeviceId()) && device.getConnectedAndroid() != null) {
                    String oldAndroid = device.getConnectedAndroid(); // lưu trước khi xóa
                    device.setConnectedAndroid(null);
                    deviceService.save(device);
                    log.info("🧹 PC '{}' disconnected → cleared connectedAndroid '{}'", disconnectedId, oldAndroid);

                    // Ngắt kết nối Android nếu còn đang mở
                    Session androidSession = sessionMap.get(oldAndroid);
                    if (androidSession != null && androidSession.isOpen()) {
                        androidSession.close(new CloseReason(
                                CloseReason.CloseCodes.NORMAL_CLOSURE,
                                "PC disconnected"
                        ));
                        sessionMap.remove(oldAndroid);
                        log.info("🔌 Android '{}' forcibly disconnected due to PC '{}'", oldAndroid, disconnectedId);
                    }
                    break;
                }
            }

        } catch (Exception e) {
            log.error("💥 Error in @OnClose '{}': {}", disconnectedId, e.getMessage(), e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String disconnectedId = null;

        try {
            for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
                if (entry.getValue().equals(session)) {
                    disconnectedId = entry.getKey();
                    break;
                }
            }

            log.error("💥 WebSocket error [{}]: {}", disconnectedId != null ? disconnectedId : session.getId(), error.getMessage(), error);

            if (disconnectedId == null) {
                return;
            }

            sessionMap.remove(disconnectedId);
            log.warn("❌ Removed session after error for '{}'", disconnectedId);

            DeviceService deviceService = SpringContext.getBean(DeviceService.class);
            List<String> allDeviceIds = deviceService.getAllDeviceIds();

            for (String deviceId : allDeviceIds) {
                Device device = deviceService.getDeviceById(deviceId);
                if (device == null) continue;

                // TH1: Android bị lỗi → clear khỏi PC
                if (disconnectedId.equals(device.getConnectedAndroid())) {
                    device.setConnectedAndroid(null);
                    deviceService.save(device);
                    log.info("🧹 Android '{}' error-disconnected → cleared from PC '{}'", disconnectedId, deviceId);
                    break;
                }

                // TH2: PC bị lỗi → clear Android và ngắt Android WebSocket
                if (disconnectedId.equals(device.getDeviceId()) && device.getConnectedAndroid() != null) {
                    String oldAndroid = device.getConnectedAndroid();
                    device.setConnectedAndroid(null);
                    deviceService.save(device);
                    log.info("🧹 PC '{}' error-disconnected → cleared connectedAndroid '{}'", disconnectedId, oldAndroid);

                    Session androidSession = sessionMap.get(oldAndroid);
                    if (androidSession != null && androidSession.isOpen()) {
                        androidSession.close(new CloseReason(
                                CloseReason.CloseCodes.NORMAL_CLOSURE,
                                "PC disconnected due to error"
                        ));
                        sessionMap.remove(oldAndroid);
                        log.info("🔌 Android '{}' forcibly disconnected due to PC error '{}'", oldAndroid, disconnectedId);
                    }
                    break;
                }
            }

            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "WebSocket error"));
            }

        } catch (Exception e) {
            log.error("💥 Error during @OnError cleanup for '{}': {}", disconnectedId, e.getMessage(), e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("📥 Incoming message: {}", message);
        log.info("📌 Current sessionMap: {}", sessionMap.keySet());

        try {
            Map<String, Object> messageMap = mapper.readValue(message, new TypeReference<>() {});
            String type = (String) messageMap.get("type");
            String fromUser = (String) messageMap.get("fromUser");

            // ✅ Xử lý riêng type = "hello"
            if ("hello".equals(type)) {
                // ✅ Tìm PC đang kết nối với Android này
                String pcId = sessionMap.entrySet().stream()
                        .filter(e -> {
                            Device pc = SpringContext.getBean(DeviceService.class).getDeviceById(e.getKey());
                            return pc != null && fromUser.equals(pc.getConnectedAndroid());
                        })
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (pcId == null) {
                    log.warn("❌ No PC found for Android '{}'. Skipping HELLO.", fromUser);
                    return;
                }

                Session pcSession = sessionMap.get(pcId);
                if (pcSession != null && pcSession.isOpen()) {
                    Map<String, Object> helloMsg = new HashMap<>();
                    helloMsg.put("type", "hello");
                    helloMsg.put("fromUser", fromUser);

                    String jsonHello = mapper.writeValueAsString(helloMsg);
                    pcSession.getBasicRemote().sendText(jsonHello);
                    log.info("👋 Forwarded HELLO from '{}' → PC '{}'", fromUser, pcId);
                }
                return; // ✅ kết thúc tại đây, không xử lý tiếp bên dưới
            }

            // ⬇️ Xử lý bình thường cho offer, answer, ice_candidate
            String toUser = (String) messageMap.get("toUser");
            Session targetSession = sessionMap.get(toUser);
            if (targetSession == null) {
                log.warn("❌ Target session '{}' not found. sessionMap keys: {}", toUser, sessionMap.keySet());
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            response.put("fromUser", fromUser);
            response.put("toUser", toUser);

            switch (type) {
                case "offer", "answer" -> {
                    response.put("sdp", messageMap.get("sdp"));
                    log.info("🔁 Forwarding {} from {} → {}", type, fromUser, toUser);
                }
                case "ice_candidate" -> {
                    response.put("candidate", messageMap.get("candidate"));
                    log.info("❄️ Forwarding ICE candidate from {} → {}", fromUser, toUser);
                }
                default -> {
                    log.warn("⚠️ Unknown message type: {}", type);
                    return;
                }
            }

            String jsonResponse = mapper.writeValueAsString(response);
            targetSession.getBasicRemote().sendText(jsonResponse);
            log.info("📤 Sent message to {}: {}", toUser, jsonResponse);

        } catch (Exception e) {
            log.error("💥 Error handling message: {}", message, e);
        }
    }

}
