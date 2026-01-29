package com.example.trading.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class QuotesController {

  @GetMapping("/quotes")
  public String quotes(@RequestParam(required = false) String symbols, Model model) {
    model.addAttribute("symbols", symbols == null ? "" : symbols);
    return "quotes";
  }
}
