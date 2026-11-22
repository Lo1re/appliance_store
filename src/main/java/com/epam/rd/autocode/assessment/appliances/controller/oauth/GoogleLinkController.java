package com.epam.rd.autocode.assessment.appliances.controller.oauth;

import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.config.ConditionalOnGoogleLogin;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor @ConditionalOnGoogleLogin
public class GoogleLinkController {
    private final CurrentActor actor;
    private final GoogleIdentityService identities;
    private final GoogleFlowSession flows;
    private final AuthTokenCookies cookies;
    private final ResetTokenCodec hashes;
    @ModelAttribute public void noCache(HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    @GetMapping("/account/google")
    public String page(Model model) {
        model.addAttribute("googleLinked", identities.linked(actor.requireUser().getId())); return "oauth/google-link";
    }
    @PostMapping("/account/google/link")
    public String begin(HttpServletRequest request, RedirectAttributes redirects) {
        Long id = actor.requireUser().getId();
        String access = cookies.read(request);
        if (access == null) {
            throw new AccessDeniedException("Access denied");
        }
        flows.beginLink(request, id, hashes.hash(access));
        return "redirect:/oauth2/authorization/google";
    }
}
