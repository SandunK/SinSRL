package com.dcs.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootApplication
public class PosApplication {
	public static void main(String[] args) {
		SpringApplication.run(PosApplication.class, args);
	}
}
