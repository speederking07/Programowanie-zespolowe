package pl.zespolowe.splix.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pl.zespolowe.splix.domain.User;
import pl.zespolowe.splix.services.UserService;

import javax.security.auth.login.AccountException;
import javax.validation.Valid;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class MainController implements ErrorController {

    @Autowired
    private UserService service;


    @GetMapping("/")
    public String getMainPage() {
        return "index";
    }

    @PostMapping(value = "/register", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        try {
            service.registerUser(user);
            return ResponseEntity.ok("Registration successful");
        } catch (AccountException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    //TODO
    @GetMapping("/admin")
    public String getAdminConsole() {
        return "admin";
    }

    //TODO
    @GetMapping(value = "/admin/set", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody String username) {
        try {
            service.setAdmin(username);
            return ResponseEntity.ok("Success");
        } catch (AccountException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "/leaders", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Long> getLeaders() {
        return service.getLeaders();
    }


    //ERRORS

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @GetMapping("/error")
    public String err() {
        return "redirect:/";
    }

    @Override
    public String getErrorPath() {
        return "/";
    }

}
