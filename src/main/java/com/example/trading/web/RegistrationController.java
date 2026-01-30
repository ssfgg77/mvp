package com.example.trading.web;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.AppUserRepository;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegistrationController {

  private final AppUserRepository users;
  private final PasswordEncoder encoder;

  public RegistrationController(AppUserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @GetMapping({"/register", "/login/register"})
  public String registerForm(Model model) {
    model.addAttribute("form", new RegisterForm());
    return "register";
  }

  @PostMapping({"/register", "/login/register"})
  public String register(@ModelAttribute("form") RegisterForm form, Model model) {
    if (form.username == null || form.username.isBlank()
        || form.email == null || form.email.isBlank()
        || form.password == null || form.password.length() < 8) {
      model.addAttribute("error", "Invalid registration input (min password length 8).");
      return "register";
    }
    if (users.findByUsername(form.username).isPresent() || users.findByEmail(form.email).isPresent()) {
      model.addAttribute("error", "Username or email already exists.");
      return "register";
    }

    AppUser u = new AppUser();
    u.setUsername(form.username.trim());
    u.setEmail(form.email.trim());
    u.setPasswordHash(encoder.encode(form.password));
    u.setRoles(Set.of("USER"));
    users.save(u);

    return "redirect:/login?registered";
  }

  public static class RegisterForm {
    public String username;
    public String email;
    public String password;
  }
}
