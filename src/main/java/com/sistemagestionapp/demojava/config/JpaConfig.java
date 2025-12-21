package com.sistemagestionapp.demojava.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("!mongo")
@EnableJpaRepositories(
        basePackages = "com.sistemagestionapp.demojava.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class JpaConfig {

    @Value("${app.db.engine:mysql}")
    private String engine;

    @Value("${app.db.host:localhost}")
    private String host;

    @Value("${app.db.port:3306}")
    private int port;

    @Value("${app.db.name:demo}")
    private String dbName;

    @Value("${app.db.user:root}")
    private String user;

    @Value("${app.db.password:root}")
    private String password;

    @Value("${spring.jpa.hibernate.ddl-auto:create}")
    private String ddlAuto;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String url;
        String driverClassName;

        if ("postgres".equalsIgnoreCase(engine) || "postgresql".equalsIgnoreCase(engine)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            driverClassName = "org.postgresql.Driver";
        } else {
            // por defecto mysql
            url = String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
            driverClassName = "com.mysql.cj.jdbc.Driver";
        }

        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(user);
        config.setPassword(password);

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.sistemagestionapp.demojava.model");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        if ("postgres".equalsIgnoreCase(engine) || "postgresql".equalsIgnoreCase(engine)) {
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        }

        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}