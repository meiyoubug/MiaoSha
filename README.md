# 秒杀系统的每个阶段的实现过程及原因剖析

## 前言

因为https://github.com/qqxx6661/miaosha项目只有最终结果，所以我打算去实现蛮三刀酱博客的每个阶段，并详细记录，以及结果剖析

## 项目准备

### 项目目录结构

![image-20230714210301902](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230714210301902.png)

### pom.xml

```xml
<parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.5</version>
    </parent>


    <dependencies>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>23.0</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.83</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba.otter</groupId>
            <artifactId>canal.client</artifactId>
            <version>1.1.1</version>
        </dependency>


        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>


        <!--与springboot整合的依赖-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.4.3.1</version>
        </dependency>
        <!--代码自动生成的依赖-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-generator</artifactId>
            <version>3.3.2</version>
        </dependency>
        <!--代码自动生成模板的依赖-->
        <dependency>
            <groupId>org.apache.velocity</groupId>
            <artifactId>velocity</artifactId>
            <version>1.7</version>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-extension</artifactId>
            <version>3.4.3.4</version>
        </dependency>
```

### application.yml

```yml
server:
  port: 8081 #端口号
  tomcat:
    max-connections: 10000
    threads:
      max: 10000
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/m4a_miaosha?characterEncoding=utf-8&rewriteBatchedStatements=true&&allowMultiQueries=true
    username: 用户名
    password: 密码
    driver-class-name: com.mysql.cj.jdbc.Driver #如果是5.x 请改成com.mysql.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 5MB
  redis:
    host: localhost #redis服务器的IP地址
    port: 6379 #默认端口6379
    database: 0
  rabbitmq:
    host: localhost #rabitmq的服务器ip地址
    port: 5672 #默认端口5672
    username: guest #默认用户名
    password: guest #默认密码
mybatis-plus:
  configuration:
    # 日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto
  mapper-locations: classpath:/xml/*.xml
```

