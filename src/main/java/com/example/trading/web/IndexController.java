package com.example.trading.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

  @GetMapping("/home")
  public String home() {
    return "index";
  }
}
