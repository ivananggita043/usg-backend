package com.garmentqc.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SystemApplication {
	// bismillah aman tenang sejahtera tdk error amin
	public static void main(String[] args) {
		SpringApplication.run(SystemApplication.class, args);
	}
}