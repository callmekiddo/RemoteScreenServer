package com.kiddo.remotescreen.server.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.kiddo.remotescreen.server.entity.Device;
import com.kiddo.remotescreen.server.entity.Token;
import com.kiddo.remotescreen.server.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DynamoDBConfig {

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

//    @Value("${amazon.aws.accesskey}")
//    private String awsAccessKey;
//
//    @Value("${amazon.aws.secretkey}")
//    private String awsSecretKey;

    @Value("${amazon.aws.region}")
    private String awsRegion;


    @Bean
    public AWSCredentialsProvider amazonAWSCredentialsProvider() {
//        if (awsAccessKey != null && !awsAccessKey.isBlank()
//            && awsSecretKey != null && !awsSecretKey.isBlank()) {
//            return new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
//        } else {
            return InstanceProfileCredentialsProvider.getInstance();
//        }
    }

    @Bean
    public AmazonSimpleEmailService emailClientBuilder() {
        return AmazonSimpleEmailServiceClientBuilder.standard()
            .withCredentials(amazonAWSCredentialsProvider())
            .withRegion(awsRegion)
            .build();
    }


    @Bean
    public AmazonDynamoDB amazonDynamoDB(){
        return AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(amazonDynamoDBEndpoint, awsRegion))
            .withCredentials(amazonAWSCredentialsProvider())
            .build();
    }

    @Bean
    public DynamoDBMapper mapper(){
        return new DynamoDBMapper(amazonDynamoDB());
    }

    @Bean
    public CommandLineRunner init(DynamoDBMapper mapper, AmazonDynamoDB dynamoDB) {
        return args -> {
            List<String> tables = dynamoDB.listTables().getTableNames();
            if (!tables.contains("devices")) {
                CreateTableRequest request = mapper.generateCreateTableRequest(Device.class);
                request.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
                dynamoDB.createTable(request);
            }

            if (!tables.contains("token")) {
                CreateTableRequest request = mapper.generateCreateTableRequest(Token.class);
                request.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
                dynamoDB.createTable(request);
            }

            if (!tables.contains("users")) {
                CreateTableRequest request = mapper.generateCreateTableRequest(User.class);
                request.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
                dynamoDB.createTable(request);
            }
        };
    }


}
