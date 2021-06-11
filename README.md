# Rotate dynamic relational database credentials of a Spring Boot app when the Hashicorp Vault `max_ttl` expires

This is Java version of code example used in the blog post
[Heavy Rotation of Relational Hashicorp Vault Database Secrets in Spring](https://secrets-as-a-service.com/posts/hashicorp-vault/rotate-dynamic-relational-database-connection-in-spring-at-runtime/) 
on https://secrets-as-a-service.com/.

It ensures that the Spring Boot application is rotating the the dynamic database credentials, whenever they expire. 
This approach only works for relational databases using HikariCP as connection pool, which only supports JDBC. 

# Please note that:
## Put Vault configurations in bootstrap.yml
## Set the value of spring.cloud.vaut.config.lifecycle.min-renewal less than but close to TTL of credentials generated by Vault