package uk.gov.cabinetoffice.csl.controller.advice;

import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    private final Environment environment;

    public GlobalControllerAdvice(Environment environment){
        this.environment = environment;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model){
        boolean nsgFlag = Boolean.parseBoolean(environment.getProperty("NSG_FLAG", "false"));
        model.addAttribute("nsgFlag", nsgFlag);
    }

}