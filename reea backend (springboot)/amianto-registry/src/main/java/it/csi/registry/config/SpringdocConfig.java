package it.csi.registry.config;

import java.util.List;

import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SpringdocConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("REEA API").version("1.0.0").description("API Registro Ex Esposti Amianto"));
	}



	@Bean
	public DefaultConfigurationCustomizer jooqConfigurationCustomizer(
	        List<ExecuteListenerProvider> providers) {

	    return configuration -> {

	        System.out.println(
	                "=================================> CONFIGURO JOOQ LISTENERS CON UN LISTENER SPENTO STANDARD <=================================");

	        providers.forEach(p ->
	                System.out.println("JOOQ PROVIDER -> " + p));

	        configuration.set(providers.toArray(new ExecuteListenerProvider[0]));

	        configuration.settings()
	                .withRenderFormatted(true)
	                .withExecuteLogging(false);
	    };
	}
	

@Bean
public ExecuteListenerProvider sqlLoggerListenerProvider() {

    System.out.println(
        "=================================> REGISTRO CUSTOM SQL LOGGER <=================================");

    return new DefaultExecuteListenerProvider(
            new SqlLoggerListener());
}


}