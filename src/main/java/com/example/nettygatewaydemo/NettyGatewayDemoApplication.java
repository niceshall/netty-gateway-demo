package com.example.nettygatewaydemo;

import com.example.nettygatewaydemo.core.HttpServer;
import com.example.nettygatewaydemo.util.TaskSchedule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NettyGatewayDemoApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(NettyGatewayDemoApplication.class, args);
		HttpServer.getInstance().start();
	}

	@Bean
	public TaskSchedule taskRunner() {
		return new TaskSchedule();
	}

}
