package gr.helix.httpfsproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Application extends SpringBootServletInitializer {

    /**
     * Used when packaging as a JAR application
     */
    public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
     * Used when packaging as a WAR application
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder)
    {
        return builder.sources(Application.class);
}
}
