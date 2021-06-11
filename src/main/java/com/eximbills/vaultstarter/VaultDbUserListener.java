package com.eximbills.vaultstarter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.event.LeaseListenerAdapter;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;
import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnBean(SecretLeaseContainer.class)
public class VaultDbUserListener {

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private SecretLeaseContainer leaseContainer;

    @Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    Logger logger = LoggerFactory.getLogger(VaultDbUserListener.class);

    @PostConstruct
    private void postConstruct() {

        String vaultCredsPath = "database/creds/" + databaseRole;
        logger.info("Vault Creds Path: " + vaultCredsPath);

        leaseContainer.addLeaseListener(new LeaseListenerAdapter() {

            @Override
            public void onLeaseEvent(SecretLeaseEvent secretLeaseEvent) {

                logger.info("Secret Lease Event: " + secretLeaseEvent.toString());

                if (secretLeaseEvent.getSource().getPath().equals(vaultCredsPath)) {

                    if (secretLeaseEvent instanceof SecretLeaseCreatedEvent) {
                        logger.info("Secret Lease Created: " + secretLeaseEvent.getLease());
                        SecretLeaseCreatedEvent secretLeaseCreatedEvent = (SecretLeaseCreatedEvent) secretLeaseEvent;
                        String username = (String) secretLeaseCreatedEvent.getSecrets().get("username");
                        String password = (String) secretLeaseCreatedEvent.getSecrets().get("password");

                        logger.info("Update System properties username & password");
                        System.setProperty("spring.datasource.username", username);
                        System.setProperty("spring.datasource.password", password);

                        logger.info("Update HikariCP username & password");
                        HikariConfigMXBean hikariconfigMXBean = hikariDataSource.getHikariConfigMXBean();
                        hikariconfigMXBean.setUsername(username);
                        hikariconfigMXBean.setPassword(password);

                        logger.info("Evict database connections in pool");
                        HikariPoolMXBean hikariPoolMXBean = hikariDataSource.getHikariPoolMXBean();
                        if (hikariPoolMXBean != null) {
                            hikariPoolMXBean.softEvictConnections();
                        }
                    }

                    if (secretLeaseEvent instanceof SecretLeaseExpiredEvent) {
                        leaseContainer.requestRotatingSecret(vaultCredsPath);
                        logger.info("Request Vault to rotate secret");
                    }
                }
            }
        });

    }

}