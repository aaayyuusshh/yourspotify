package com.spotifyproject.spotirecap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpotiRecapApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotiRecapApplication.class, args);
	}

}
