package com.abdil.visitorbackend.controller;

import com.abdil.visitorbackend.service.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/visitors")
@CrossOrigin(origins = "*")
public class VisitorController {

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> trackVisitor(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> payload) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String appType = payload != null ? payload.getOrDefault("appType", "unknown") : "unknown";

        // ✅ LOG POUR VOIR CE QUI EST REÇU
        System.out.println("📥 [Controller] appType reçu: '" + appType + "'");
        System.out.println("📥 [Controller] IP: " + ip);
        System.out.println("📥 [Controller] User-Agent: " + userAgent);

        return ResponseEntity.ok(visitorService.trackVisitor(ip, userAgent, appType));
    }

    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineCount() {
        return ResponseEntity.ok(visitorService.getOnlineCount());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}