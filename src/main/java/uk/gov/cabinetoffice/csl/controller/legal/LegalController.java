package uk.gov.cabinetoffice.csl.controller.legal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_EMAIL_ATTRIBUTE;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_NUMBER_ATTRIBUTE;

@Slf4j
@Controller
@RequestMapping
public class LegalController {

    private static final String COOKIES_TEMPLATE = "legal/cookies";
    private static final String PRIVACY_TEMPLATE = "legal/privacy";
    private static final String ACCESSIBILITY_STATEMENT_TEMPLATE = "legal/accessibility-statement";
    private static final String CONTACT_US_TEMPLATE = "legal/contact-us";

    @Value("${lpg.contactEmail}")
    private String contactEmail;

    @Value("${lpg.contactNumber}")
    private String contactNumber;

    @GetMapping("/cookies")
    public String cookiesPage() {
        return COOKIES_TEMPLATE;
    }

    @GetMapping("/privacy")
    public String privacyPage(Model model) {
        model.addAttribute(CONTACT_EMAIL_ATTRIBUTE, contactEmail);
        return PRIVACY_TEMPLATE;
    }

    @GetMapping("/accessibility-statement")
    public String accessibilityStatement(Model model) {
        model.addAttribute(CONTACT_EMAIL_ATTRIBUTE, contactEmail);
        model.addAttribute(CONTACT_NUMBER_ATTRIBUTE, contactNumber);
        return ACCESSIBILITY_STATEMENT_TEMPLATE;
    }

    @GetMapping("/contact-us")
    public String contactUs(Model model) {
        model.addAttribute(CONTACT_EMAIL_ATTRIBUTE, contactEmail);
        model.addAttribute(CONTACT_NUMBER_ATTRIBUTE, contactNumber);
        return CONTACT_US_TEMPLATE;
    }
}
