package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.GoogleIdentityClient;
import br.ufrn.dimap.siglab.integration.ScholarClient;
import br.ufrn.dimap.siglab.dto.RegistrationRequest;
import br.ufrn.dimap.siglab.dto.GoogleLoginRequest;
import br.ufrn.dimap.siglab.dto.ScholarCandidate;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock UserService userService;
    @Mock InterestService interests;
    @Mock GoogleIdentityClient google;
    @Mock PasswordEncoder encoder;
    @Mock ScholarClient scholar;
    AuthService service;
    @BeforeEach void setup() { service = new AuthService(users,userService,interests,google,encoder,scholar); }

    @Test void completesMissingScholarAreasWithProvidedAreas() {
        when(interests.withScholar(eq("author"), any(String[].class))).thenReturn(new String[]{"IA", "Robótica", "Visão"});
        assertArrayEquals(new String[]{"IA", "Robótica", "Visão"}, service.areas("author",new String[]{"Visão"}));
    }
    @Test void acceptsThreeManualAreasWithoutScholarAuthorId() {
        String[] manual = {"IA", "Robótica", "Visão"};
        when(interests.three(manual)).thenReturn(manual);
        assertArrayEquals(manual, service.areas(null, manual));
        verify(interests, never()).withScholar(anyString(), any());
    }
    @Test void registersWithoutScholarAuthorId() {
        String[] manual = {"IA", "Robótica", "Visão"};
        when(interests.three(manual)).thenReturn(manual);
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.register(new RegistrationRequest("Ada", "ada@lab.org", "secret", null, manual));

        assertNull(created.getAuthorId());
        assertArrayEquals(manual, created.getInterest_areas());
    }
    @Test void rejectsWrongPassword() {
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(User.builder().email("a@b.com").password("hash").build()));
        when(encoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(SecurityException.class, () -> service.login("a@b.com", "wrong"));
    }
    @Test void linksOnlyTheSameVerifiedGoogleEmail() {
        User current = User.builder().id(3L).email("one@lab.org").build();
        when(google.verify("credential")).thenReturn(new GoogleIdentityClient.Identity("google-sub", "other@lab.org", "Outro", null));
        assertThrows(SecurityException.class, () -> service.linkGoogle(current, "credential"));
        verify(users, never()).save(any());
    }
    @Test void linksGoogleSubjectToCurrentUser() {
        User current = User.builder().id(3L).email("one@lab.org").build();
        when(google.verify("credential")).thenReturn(new GoogleIdentityClient.Identity("google-sub", "one@lab.org", "Um", null));
        when(users.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(users.save(current)).thenReturn(current);
        assertEquals("google-sub", service.linkGoogle(current, "credential").getGoogleSubject());
    }
    @Test void discoversScholarCandidatesAfterGoogleVerification() {
        var candidate = new ScholarCandidate("author", "Ada", "UFRN", "ufrn.br", java.util.List.of("IA"));
        when(google.verify("credential")).thenReturn(new GoogleIdentityClient.Identity("sub", "ada@ufrn.br", "Ada", "ufrn.br"));
        when(users.findByGoogleSubject("sub")).thenReturn(Optional.empty());
        when(scholar.searchProfiles("Ada")).thenReturn(java.util.List.of(candidate));
        var preview=service.previewGoogle("credential");
        assertFalse(preview.existingUser());
        assertEquals(java.util.List.of(candidate), preview.candidates());
    }
    @Test void marksManagedGoogleAccountAsInstitutional() {
        String[] manual={"IA", "Robótica", "Visão"};
        when(google.verify("credential")).thenReturn(new GoogleIdentityClient.Identity("sub", "ada@ufrn.br", "Ada", "ufrn.br"));
        when(users.findByGoogleSubject("sub")).thenReturn(Optional.empty());
        when(interests.three(manual)).thenReturn(manual);
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User created=service.google(new GoogleLoginRequest("credential", null, manual));
        assertEquals("INSTITUCIONAL", created.getVerificationLevel());
        assertEquals("ufrn.br", created.getInstitutionalDomain());
    }
}
