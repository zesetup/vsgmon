package com.gmail.zerosetup.vsgmon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @Autowired
    private TemperatureService temperatureService;

    @GetMapping("/")
    public String hello(Model model) {
        String temperature = temperatureService.getTemperature();
        String ttsTemperature = temperatureService.getTtsTemperature();
        model.addAttribute("message", "");
        model.addAttribute("temperature", temperature);
        model.addAttribute("ttsTemperature", ttsTemperature);
        return "hello";
    }
}
