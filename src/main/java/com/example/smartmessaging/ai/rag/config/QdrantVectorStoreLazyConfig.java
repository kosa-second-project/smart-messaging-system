package com.example.smartmessaging.ai.rag.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class QdrantVectorStoreLazyConfig {

    @Bean
    public static BeanFactoryPostProcessor qdrantVectorStoreLazyPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory.containsBeanDefinition("vectorStore")) {
                    // Qdrant가 꺼져 있어도 애플리케이션 시작은 막지 않고, RAG API 호출 시점에 연결 실패를 처리한다.
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition("vectorStore");
                    beanDefinition.setLazyInit(true);
                }
            }
        };
    }
}
