package org.harenoasa.summerframework.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.harenoasa.summerframework.context.annotation.Autowired;
import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Configuration;
import org.harenoasa.summerframework.context.annotation.Value;

import javax.sql.DataSource;

@Configuration
public class JdbcConfiguration {

    @Bean(destroyMethod = "close") //why does destoryMethod is needed ,when application is about to close ,and everything inside is about to be closed
    DataSource dataSource(
        @Value("${summer.datasource.url}")String url,
        @Value("${summer.datasource.username}")String username,
        @Value("${summer.datasource.password}")String password,
        @Value("${summer.datasource.driver-class-name:}")String driver,
        @Value("${summer.datasource.maximum-pool-size:20}")int maximumPoolSize,
        @Value("${summer.datasource.minimum-pool-size:1}") int minimumPoolSize,
        @Value("${summer.datasource.connection-timeout:30000}")int connTimeout ) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
