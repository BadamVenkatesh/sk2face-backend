package com.sk2face.sk2faceeurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class Sk2faceEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(Sk2faceEurekaServerApplication.class, args);
    }

}
