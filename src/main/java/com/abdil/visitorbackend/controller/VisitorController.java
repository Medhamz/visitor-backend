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
            HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(visitorService.trackVisitor(ip, userAgent));
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