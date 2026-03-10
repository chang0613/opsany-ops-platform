package com.opsany.replica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.mybatis.spring.annotation.MapperScan;

import com.opsany.replica.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@MapperScan("com.opsany.replica.repository")
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
