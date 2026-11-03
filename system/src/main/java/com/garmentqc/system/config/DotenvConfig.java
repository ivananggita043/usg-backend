// package com.garmentqc.system.config;

// import io.github.cdimascio.dotenv.Dotenv;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.env.EnvironmentPostProcessor;
// import org.springframework.core.env.ConfigurableEnvironment;
// import org.springframework.core.env.MapPropertySource;

// import java.util.HashMap;
// import java.util.Map;

// public class DotenvConfig implements EnvironmentPostProcessor {

//     @Override
//     public void postProcessEnvironment(
//             ConfigurableEnvironment environment,
//             SpringApplication application) {

//         Dotenv dotenv = Dotenv.configure()
//                 .directory(".")
//                 .ignoreIfMissing()
//                 .load();

//         Map<String, Object> properties = new HashMap<>();

//         dotenv.entries().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));

//         environment.getPropertySources().addFirst(
//                 new MapPropertySource("dotenvProperties", properties));
//     }
// }