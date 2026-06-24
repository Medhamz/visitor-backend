package com.abdil.visitorbackend.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class VisitorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long VISITOR_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    public Map<String, Object> trackVisitor(String ip, String userAgent) {
        // Mettre à jour ou insérer le visiteur
        String upsertSql = """
            INSERT INTO online_visitors (ip_address, user_agent, last_activity)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (ip_address)
            DO UPDATE SET last_activity = CURRENT_TIMESTAMP,
                          user_agent = EXCLUDED.user_agent
            """;
        jdbcTemplate.update(upsertSql, ip, userAgent);

        // Mettre à jour les statistiques de visite
        updateVisitStats();

        return getOnlineCount();
    }

    public Map<String, Object> getOnlineCount() {
        // Supprimer les visiteurs inactifs
        String deleteSql = """
            DELETE FROM online_visitors
            WHERE last_activity < CURRENT_TIMESTAMP - INTERVAL '5 minutes'
            """;
        jdbcTemplate.update(deleteSql);

        // Compter les visiteurs actifs
        String countSql = "SELECT COUNT(*) FROM online_visitors";
        int onlineCount = jdbcTemplate.queryForObject(countSql, Integer.class);

        // Récupérer les téléchargements du jour
        String downloadsSql = """
            SELECT 
                COUNT(*) FILTER (WHERE app_type = 'client') as client_downloads,
                COUNT(*) FILTER (WHERE app_type = 'driver') as driver_downloads
            FROM download_stats
            WHERE download_date = CURRENT_DATE
            """;
        Map<String, Object> downloads = jdbcTemplate.queryForMap(downloadsSql);

        Map<String, Object> response = new HashMap<>();
        response.put("online", onlineCount);
        response.put("clientDownloads", downloads.get("client_downloads") != null ? downloads.get("client_downloads") : 0);
        response.put("driverDownloads", downloads.get("driver_downloads") != null ? downloads.get("driver_downloads") : 0);
        response.put("totalDownloads",
                ((Number) response.get("clientDownloads")).intValue() +
                        ((Number) response.get("driverDownloads")).intValue()
        );
        response.put("timestamp", Instant.now().toString());

        return response;
    }

    private void updateVisitStats() {
        String statsSql = """
            INSERT INTO visit_stats (visit_date, total_visits, unique_visitors)
            VALUES (CURRENT_DATE, 1, 1)
            ON CONFLICT (visit_date)
            DO UPDATE SET 
                total_visits = visit_stats.total_visits + 1,
                unique_visitors = (SELECT COUNT(*) FROM online_visitors)
            """;
        jdbcTemplate.update(statsSql);
    }
}