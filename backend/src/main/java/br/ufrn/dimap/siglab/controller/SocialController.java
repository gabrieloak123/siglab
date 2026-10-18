package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.service.SocialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialController {
    private final SocialService social;
    public SocialController(SocialService social) { this.social=social; }
    private Long me(HttpServletRequest request) {
        var session=request.getSession(false);
        return social.userId(session == null ? null : session.getAttribute("userId"));
    }
    @GetMapping("/suggestions/researchers")
    public List<User> researchers(HttpServletRequest req) { return social.researchers(me(req)); }
    @GetMapping("/suggestions/groups")
    public List<ResearchGroup> groups(HttpServletRequest req) { return social.groups(me(req)); }
    @GetMapping("/feed")
    public List<Post> feed(HttpServletRequest req, @RequestParam(defaultValue="false") boolean followedOnly) {
        return social.feed(me(req), followedOnly);
    }
    @GetMapping("/following")
    public Map<String,Object> following(HttpServletRequest req) {
        Long id=me(req);
        return Map.of("researchers",social.followedResearchers(id),"groups",social.followedGroups(id));
    }
    @PostMapping("/researchers/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followResearcher(HttpServletRequest req, @PathVariable Long id) { social.followResearcher(me(req), id); }
    @DeleteMapping("/researchers/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowResearcher(HttpServletRequest req, @PathVariable Long id) { social.unfollowResearcher(me(req), id); }
    @PostMapping("/groups/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followGroup(HttpServletRequest req, @PathVariable String id) { social.followGroup(me(req), id); }
    @DeleteMapping("/groups/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowGroup(HttpServletRequest req, @PathVariable String id) { social.unfollowGroup(me(req), id); }
}
