package com.lab2.twopc;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.postgresql.xa.PGXADataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@SpringBootApplication
public class TwoPCApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwoPCApplication.class, args);
	}

	@Primary
	@Bean(name = "dataSource")
	@ConfigurationProperties(prefix = "spring.datasource")
	public PGXADataSource firstDataSource() {
		PGXADataSource source = new PGXADataSource();
		source.setServerName("localhost");
		source.setDatabaseName("postgres1");
		source.setUser("postgres");
		source.setPassword("1");
		source.setPortNumber(5433);
		return source;
	}

	@Bean(name = "dataSource2")
	@ConfigurationProperties(prefix = "spring.datasource2")
	public PGXADataSource secondDataSource() {
		PGXADataSource source = new PGXADataSource();
		source.setServerName("localhost");
		source.setDatabaseName("postgres2");
		source.setUser("postgres");
		source.setPassword("1");
		source.setPortNumber(5433);
		return source;
	}

	@Bean(name = "dataSource3")
	@ConfigurationProperties(prefix = "spring.datasource3")
	public PGXADataSource thirdDataSource() {
		PGXADataSource source = new PGXADataSource();
		source.setServerName("localhost");
		source.setDatabaseName("postgres3");
		source.setUser("postgres");
		source.setPassword("1");
		source.setPortNumber(5433);
		return source;
	}

	@Bean
	public PlatformTransactionManager platformTransactionManager(){
		JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
		jtaTransactionManager.setUserTransaction(new UserTransactionImp());
		jtaTransactionManager.setTransactionManager(new UserTransactionManager());
		return jtaTransactionManager;
	}
}
