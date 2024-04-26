# spring-boot-multi-jdbc

Provide automatic configuration of multiple data sources for Spring boot

# Getting Started

### Reference Documentation

### Guides

SpringBoot Configuration Sample

```yaml
spring:
  application:
    name: sample
  datasources:
    primary:
      type: com.mchange.v2.c3p0.ComboPooledDataSource
      driver-class-name: ${driverClass}
      url: ${jdbc connection rul}
      username: ${username}
      password: ${password}
      c3p0:
        initialPoolSize: 3
        minPoolSize: 5
        maxPoolSize: 20
    secondary:
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: ${driverClass}
      url: ${jdbc connection rul}
      username: ${username}
      password: ${password}
      hikari:
        connection-timeout: 3000
        idle-timeout: 6000
        max-lifetime: 18000
        minimum-idle: 10
        maximum-pool-size: 20      
```
