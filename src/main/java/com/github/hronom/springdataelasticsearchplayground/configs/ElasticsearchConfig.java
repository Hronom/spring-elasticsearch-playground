package com.github.hronom.springdataelasticsearchplayground.configs;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public TransportClient transportClient() throws UnknownHostException {
        Settings settings = Settings
            .builder()
            .put("client.transport.ignore_cluster_name", true)
            .build();
        return new PreBuiltTransportClient(settings)
            .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
    }
}
