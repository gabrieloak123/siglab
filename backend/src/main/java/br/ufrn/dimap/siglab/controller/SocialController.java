package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.ConferenceSuggestionDTO;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.service.ConferenceCountries;
import br.ufrn.dimap.siglab.service.ConferenceService;
import br.ufrn.dimap.siglab.service.SocialService;
import br.ufrn.dimap.siglab.dto.ResearcherFollowRequest;
import br.ufrn.dimap.siglab.exception.InvalidConferenceSearchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialController {
    private final SocialService social;
    private final ConferenceService conferences;
    public SocialController(SocialService social, ConferenceService conferences) {
        this.social = social;
        this.conferences = conferences;
    }
    private Long me(HttpServletRequest request) {
        var session=request.getSession(false);
        return social.userId(session == null ? null : session.getAttribute("userId"));
    }
    @GetMapping("/suggestions/researchers")
    public List<User> researchers(HttpServletRequest req) { return social.researchers(me(req)); }
    @GetMapping("/suggestions/groups")
    public List<ResearchGroup> groups(HttpServletRequest req) { return social.groups(me(req)); }
    @GetMapping("/suggestions/conferences")
    public List<ConferenceSuggestionDTO> suggestionConferences(HttpServletRequest req) {
        return conferences.recommend(me(req));
    }
    @GetMapping("/conferences/search")
    public List<ConferenceSuggestionDTO> searchConferences(HttpServletRequest req,
            @RequestParam(required = false) String start, @RequestParam(required = false) String end,
            @RequestParam(required = false) String field, @RequestParam(required = false) String country) {
        me(req);
        return conferences.search(field, date(start, "início"), date(end, "fim"), country);
    }
    @GetMapping("/conferences/countries")
    public List<String> conferenceCountries(HttpServletRequest req) {
        me(req);
        return ConferenceCountries.names();
    }
    private static LocalDate date(String value, String label) {
        if (value == null || value.isBlank())
            throw new InvalidConferenceSearchException("Informe a data de " + label + ".");
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidConferenceSearchException("Data de " + label + " inválida; use o formato AAAA-MM-DD.");
        }
    }
    @GetMapping("/feed")
    public List<Post> feed(HttpServletRequest req, @RequestParam(defaultValue="false") boolean followedOnly) {
        return social.feed(me(req), followedOnly);
    }
    @GetMapping("/recent-posts")
    public List<Post> recentPosts() { return social.recentPosts(); }
    @GetMapping("/following")
    public Map<String,Object> following(HttpServletRequest req) {
        Long id=me(req);
        return Map.of("researchers",social.followedResearchers(id),"researcherFollows",social.researcherFollows(id),"groups",social.followedGroups(id));
    }
    @PostMapping("/researchers/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followResearcher(HttpServletRequest req, @PathVariable Long id, @RequestBody(required=false) ResearcherFollowRequest body) {
        social.followResearcher(me(req), id, body == null ? null : body.interestAreas());
    }
    @DeleteMapping("/researchers/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowResearcher(HttpServletRequest req, @PathVariable Long id) { social.unfollowResearcher(me(req), id); }
    @PostMapping("/groups/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followGroup(HttpServletRequest req, @PathVariable String id) { social.followGroup(me(req), id); }
    @DeleteMapping("/groups/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowGroup(HttpServletRequest req, @PathVariable String id) { social.unfollowGroup(me(req), id); }
    @GetMapping("/posts/{id}/like")
    public SocialService.PostLike postLike(HttpServletRequest req, @PathVariable Long id) { return social.postLike(me(req), id); }
    @PostMapping("/posts/{id}/like")
    public SocialService.PostLike likePost(HttpServletRequest req, @PathVariable Long id) { return social.likePost(me(req), id); }
    @DeleteMapping("/posts/{id}/like")
    public SocialService.PostLike unlikePost(HttpServletRequest req, @PathVariable Long id) { return social.unlikePost(me(req), id); }
}
