package pl.zespolowe.splix.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import pl.zespolowe.splix.config.security.AuthProvider;
import pl.zespolowe.splix.domain.user.User;
import pl.zespolowe.splix.services.UserService;

import java.sql.Date;
import java.util.Calendar;

/**
 * Main configuration
 *
 * @author Tomasz
 */
@Configuration
@EnableWebMvc
@EnableAsync
@Slf4j
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    private UserService userService;

    /**
     * @see AuthenticationFailureHandler
     */
    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (req, res, e) -> {
            res.setStatus(400);
            res.setContentType("text/plain");
            res.getWriter().write(e.getMessage());
            //res.getWriter().flush();
            res.getWriter().close();
        };
    }

    /**
     * @see AuthenticationSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (req, res, authentication) -> {
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                log.info("User logged: " + user.getUsername() + ", " + req.getRemoteAddr());
                user.setLastLogged(new Date(Calendar.getInstance().getTime().getTime()));
                userService.saveUser(user);
            }
        };
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver vr = new InternalResourceViewResolver();
        vr.setPrefix("/WEB-INF/jsp/");
        vr.setSuffix(".jsp");
        return vr;
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**",
                "/css/**",
                "/img/**",
                "/js/**",
                "/font/**")
                .addResourceLocations("classpath:/static/",
                        "classpath:/static/css/",
                        "classpath:/static/img/",
                        "classpath:/static/js/",
                        "classpath:/static/font/");
    }

    /**
     * @return Password encoder using BCrypt algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthProvider authenticationProvider() {
        return new AuthProvider();
    }
}
