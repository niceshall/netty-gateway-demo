package com.example.nettygatewaydemo;

import com.example.nettygatewaydemo.core.HttpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NettyGatewayDemoApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(NettyGatewayDemoApplication.class, args);
		HttpServer.getInstance().start();
	}

}
