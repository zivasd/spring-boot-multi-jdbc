# Changelog

-------------------------------------------------------------------------------------------------------------

## 1.0.5(2024-10-15)

* 增加NamedParameterJdbcTemplate

## 1.0.4(2024-05-13)

* 增加SqlDataSourceScriptDatabaseInitializer的多数据源支持
* 在项目中定义DataSource Beans将禁用本库的多数据源支持，通常
  用于在测试中定义测试专用的数据源。

## 1.0.3(2024-04-28)

* 修改包名，适配github.
* 增加日志输出

## 1.0.2(2024-04-27)

* 增加发布到SonaType Central的支持配置

### 特性

* 读取SpringBoot配置文件，自动注册DataSourceProperties、DataSource、JdbcTemple Beans
* 支持hikari、c3p0、oracle、druid、tomcat、dbcp2连接池配置属性。
