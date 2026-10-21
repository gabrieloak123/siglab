package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.DiscoverySelectionRequest;
import br.ufrn.dimap.siglab.service.DiscoveryService;
import br.ufrn.dimap.siglab.service.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {
    public record DiscoveryPost(Long id, String title, String description, String groupName,
                                List<String> keywords, List<String> tags, boolean indexed) {}
    public record DiscoveryPage(List<DiscoveryPost> posts, boolean hasMore) {}
    private final JdbcTemplate jdbc;
    private final DiscoveryService discovery;
    private final SessionUser session;
    public DiscoveryController(JdbcTemplate jdbc, DiscoveryService discovery, SessionUser session) {
        this.jdbc=jdbc; this.discovery=discovery; this.session=session;
    }

    @GetMapping("/posts")
    public DiscoveryPage posts(@RequestParam(defaultValue="0") int page) {
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Página inválida.");
        var result = jdbc.query("""
            SELECT p.id,p.title,p.description,g.name,p.keywords,p.tags,(v.post_id IS NOT NULL) AS indexed
            FROM posts p JOIN research_group g ON g.group_id=p.group_id
            LEFT JOIN post_vectors v ON v.post_id=p.id
            ORDER BY p.created_at DESC NULLS LAST,p.id DESC LIMIT 21 OFFSET ?
            """, (rs, row) -> new DiscoveryPost(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                array(rs.getArray(5)), array(rs.getArray(6)), rs.getBoolean(7)), (long) page * 20);
        return new DiscoveryPage(result.stream().limit(20).toList(), result.size() > 20);
    }

    private static List<String> array(java.sql.Array value) throws java.sql.SQLException {
        return value == null ? List.of() : java.util.Arrays.asList((String[]) value.getArray());
    }

    @PostMapping("/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@Valid @RequestBody DiscoverySelectionRequest body, HttpServletRequest request) {
        discovery.save(session.require(request).getId(), body.postIds());
    }
}
