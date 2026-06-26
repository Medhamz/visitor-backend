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

    public Map<String, Object> trackVisitor(String ip, String userAgent, String appType) {
        System.out.println("📊 [Service] trackVisitor appelé avec appType: '" + appType + "'");

        // 1. Mettre à jour ou insérer le visiteur
        String upsertSql = """
            INSERT INTO online_visitors (ip_address, user_agent, last_activity)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (ip_address)
            DO UPDATE SET last_activity = CURRENT_TIMESTAMP,
                          user_agent = EXCLUDED.user_agent
            """;
        int visitorRows = jdbcTemplate.update(upsertSql, ip, userAgent);
        System.out.println("📊 [Service] Visiteur mis à jour: " + visitorRows + " ligne(s)");

        // 2. INCÉMENTER LE COMPTEUR DE TÉLÉCHARGEMENT (historique complet)
        try {
            String incrementSql = """
                INSERT INTO download_stats (app_type, download_date, count)
                VALUES (?, CURRENT_DATE, 1)
                ON CONFLICT (app_type, download_date)
                DO UPDATE SET count = download_stats.count + 1
                """;
            int downloadRows = jdbcTemplate.update(incrementSql, appType);
            System.out.println("📊 [Service] Téléchargement incrémenté: " + downloadRows + " ligne(s) affectée(s) pour '" + appType + "'");
        } catch (Exception e) {
            System.err.println("❌ [Service] Erreur lors de l'incrémentation: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Mettre à jour les statistiques de visite
        updateVisitStats();

        Map<String, Object> result = getOnlineCount();
        System.out.println("📊 [Service] Résultat retourné: " + result);
        return result;
    }

    public Map<String, Object> getOnlineCount() {
        // Supprimer les visiteurs inactifs
        String deleteSql = """
            DELETE FROM online_visitors
            WHERE last_activity < CURRENT_TIMESTAMP - INTERVAL '5 minutes'
            """;
        int deleted = jdbcTemplate.update(deleteSql);
        if (deleted > 0) {
            System.out.println("🧹 [Service] " + deleted + " visiteur(s) inactif(s) supprimé(s)");
        }

        // Compter les visiteurs actifs
        String countSql = "SELECT COUNT(*) FROM online_visitors";
        int onlineCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        System.out.println("👥 [Service] Visiteurs en ligne: " + onlineCount);

        // ✅ CHANGEMENT IMPORTANT ICI : Récupérer TOUS les téléchargements (pas seulement aujourd'hui)
        String downloadsSql = """
            SELECT 
                COALESCE(SUM(CASE WHEN app_type = 'client' THEN count ELSE 0 END), 0) as client_downloads,
                COALESCE(SUM(CASE WHEN app_type = 'driver' THEN count ELSE 0 END), 0) as driver_downloads
            FROM download_stats
            """;
        Map<String, Object> downloads = jdbcTemplate.queryForMap(downloadsSql);
        System.out.println("📊 [Service] Téléchargements totaux (historique complet): client=" + downloads.get("client_downloads") + ", driver=" + downloads.get("driver_downloads"));

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
            VALUES (CURRENT_DATE, 1, (SELECT COUNT(*) FROM online_visitors))
            ON CONFLICT (visit_date)
            DO UPDATE SET 
                total_visits = visit_stats.total_visits + 1,
                unique_visitors = (SELECT COUNT(*) FROM online_visitors)
            """;
        int statsRows = jdbcTemplate.update(statsSql);
        System.out.println("📊 [Service] Statistiques de visite mises à jour: " + statsRows + " ligne(s)");
    }
}