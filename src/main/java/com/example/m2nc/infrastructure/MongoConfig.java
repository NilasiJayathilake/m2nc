package com.example.m2nc.infrastructure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    @Bean
    BeanPostProcessor mongoFieldNamingPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof MappingMongoConverter converter) {
                    converter.setMapKeyDotReplacement("_DOT_");

                    if (converter.getMappingContext() instanceof MongoMappingContext context) {
                        context.setFieldNamingStrategy(new SnakeCaseFieldNamingStrategy());
                    }
                }
                return bean;
            }
        };
    }

}
