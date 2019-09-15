package gr.helix.httpfsproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfiguration
{
    @Bean({"objectMapper", "jsonMapper"})
    public ObjectMapper objectMapper()
    {
        ObjectMapper objectmapper = new ObjectMapper();
        objectmapper.registerModule(new JavaTimeModule());
        return objectmapper;
    }
    
    @Bean({"propertiesMapper"})
    public JavaPropsMapper propertiesMapper()
    {
        JavaPropsMapper propertiesMapper = new JavaPropsMapper();
        propertiesMapper.registerModule(new JavaTimeModule());
        return propertiesMapper;
    }
}