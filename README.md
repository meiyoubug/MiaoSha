# 秒杀系统的每个阶段的实现过程及原因剖析

## 前言

因为**https://github.com/qqxx6661/miaosha**项目只有最终结果，所以我打算去实现蛮三刀酱博客的每个阶段，并详细记录，以及结果剖析

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

### 代码生成器模板

```java
AutoGenerator autoGenerator = new AutoGenerator();

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDbType(DbType.MYSQL);
        dataSourceConfig.setDriverName("com.mysql.cj.jdbc.Driver");
        dataSourceConfig.setUsername("root");
        dataSourceConfig.setPassword("123456");
        dataSourceConfig.setUrl("jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=UTF-8");
        autoGenerator.setDataSource(dataSourceConfig);
        GlobalConfig globalConfig = new GlobalConfig();
        //生成文件是否在磁盘中打开
        globalConfig.setOpen(false);
        //生成代码的路径
        globalConfig.setOutputDir(System.getProperty("user.dir")+"/src/main/java");
        globalConfig.setAuthor("admin");
        //service命名规则 %s是表名 表名+Service
        globalConfig.setServiceName("%sService");
        autoGenerator.setGlobalConfig(globalConfig);
        //设置包信息
        PackageConfig packageConfig = new PackageConfig();
        packageConfig.setParent("com.zc");
        packageConfig.setEntity("entity");
        packageConfig.setMapper("mapper");
        packageConfig.setController("controller");
        packageConfig.setService("service");
        packageConfig.setServiceImpl("service.impl");
        autoGenerator.setPackageInfo(packageConfig);
        StrategyConfig strategyConfig = new StrategyConfig();
        //是否为实体类添加lombok注解
        strategyConfig.setEntityLombokModel(true);
        //数据库表名下划线转驼峰
        strategyConfig.setNaming(NamingStrategy.underline_to_camel);
        //字段名下划线转驼峰
        strategyConfig.setColumnNaming(NamingStrategy.underline_to_camel);

        //设置自动生成的表名
        strategyConfig.setInclude("t_user");
        //设置自动填充创建时间和更新时间
        List<TableFill> list = new ArrayList<>();
        TableFill tableFill1 = new TableFill("create_time", FieldFill.INSERT);
        TableFill tableFill2 = new TableFill("update_time",FieldFill.INSERT_UPDATE);
        list.add(tableFill1);
        list.add(tableFill2);
        //执行代码
        strategyConfig.setTableFillList(list);
        autoGenerator.setStrategy(strategyConfig);
        autoGenerator.execute();
```

## 1、使用悲观锁解决超卖问题

此时代码生成器已经生成文件夹和简单代码了，接下来将mapper.xml文件放在resource下面

![image-20230715104129094](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715104129094.png)

> 启动类

```java
@SpringBootApplication
@MapperScan("com.zc.mapper")
public class StartApplication {
    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class,args);
    }
}
```

> service

- StockService

```java
public interface StockService extends IService<Stock> {
     int createWrongOrder(int sid);
     int createOptimisticOrder(int sid);
}
```

> impl

- StockServiceImpl

```java
private static final Logger LOGGER = LoggerFactory.getLogger(StockServiceImpl.class);
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StockOrderMapper stockOrderMapper;
    @Override
    public int createWrongOrder(int sid) {
        Stock stock=checkStock(sid);
        saleStock(sid);
        checkStock(sid);
        int id=createOrder(stock);
        return id;
    }

    private Stock checkStock(int sid){
        Stock stock=stockMapper.selectById(sid);
        if(stock.getSale().equals(stock.getCount()+1)){
            throw new RuntimeException("库存不足");
        }
        return stock;
    }


    private void saleStock(int sid){
        stockMapper.updateSaleCnt(sid);
    }

    private int createOrder(Stock stock){
        StockOrder stockOrder=new StockOrder();
        stockOrder.setSid(stock.getId());
        stockOrder.setName(stock.getName());
        int id= stockOrderMapper.insert(stockOrder);
        return id;
    }
```

> OrderController

```java
@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Resource
    private StockService stockService;

    @RequestMapping("/createWrongOrder/{sid}")
    @ResponseBody
    public String createWrongOrder(@PathVariable int sid){
        LOGGER.info("购买物品编号sid=[{}]",sid);
        int id=0;
        try {
            id=stockService.createWrongOrder(sid);
            LOGGER.info("创建订单id：[{}]",id);
        }catch (Exception e){
            LOGGER.error("Exception",e);
        }
        return String.valueOf(id);
    }
}
```

> StockMapper

```java
 void updateSaleCnt(@Param("sid") int sid);
```

> StockMapper.xml

```xml
 <update id="updateSaleCnt">
       update stock set sale=sale+1 where id=#{sid}
 </update>
```

启动启动类

使用Jmeter测试 **localhost:8081/order/createWrongOrder/1** 这个接口

如何通过JMeter进行压力测试，请参考下文，讲的非常入门但详细，包教包会：

https://www.cnblogs.com/stulzq/p/8971531.html

> 运行结果

此时你会发现你的订单表产生了超过100条的订单，接下来我们使用悲观锁来解决超卖问题，在createWrongOrder上添加**@Transactional(rollbackFor = {})**注解

- StockServiceImpl

```java
    @Transactional(rollbackFor = {})
    public int createWrongOrder(int sid) {
        Stock stock=checkStock(sid);
        saleStock(sid);
        checkStock(sid);
        int id=createOrder(stock);
        return id;
    }
```

此时我们再来测试一遍localhost:8081/order/createWrongOrder/1这个接口，发现卖了100份，也生成了100个订单，为什么加个注解就可以呢？我们来分析一下

**流程**

1、查看有没有库存->2、更新库存->3、查看有没有库存->4、创建订单

在不加注解的情况下，每一个阶段都是一个隐性事务，是针对数据库层面来说的，只要sql语句执行成功就不会回滚，还有我们在checkStock抛出的异常没有捕获，当流程3抛出异常的时候，流程4是不会执行的。

当500个请求并发执行的时候，流程1只要看到有库存就不会抛出异常，因为select执行速度是比update快的，所以可能500个都不会抛出异常，虽然流程2是当前读，且最终的结果是正确的，但是问题出现在流程3，因为在我流程3执行select的时候，可能有其它多个线程执行了更新操作，导致流程3的查询结果是不准确的，所以产生了超卖问题。

**那为什么加上注解就可以呢？**

加上注解的话**1、查看有没有库存->2、更新库存->3、查看有没有库存->4、创建订单**流程1-4都在一个事务里，因为隔离级别是可重复读，所以流程3读的是流程2的最新值，且流程2会上行锁，行锁只有在当前事务commit的时候才会释放，当当前事务还没提交的时候，其它事务都会阻塞在流程2，所以加上注解就可以防止超卖，且最后的结果也是我们所期望的结果。

## 2、使用乐观锁解决超卖问题

第一节我们说到了加上事务注解，我们就会得到我们所期待的结果，但是在Service层给更新表添加一个事务，这样每个线程更新请求的时候都会先去锁表的这一行（悲观锁），更新完库存后再释放锁，可这样就太慢了。

我们需要乐观锁。

一个最简单的办法就是，给每个商品库存一个版本号version字段

> StockService

```java
int createOptimisticOrder(int sid);
```

> StockServiceImpl

```java
 @Override
    public int createOptimisticOrder(int sid) {
        //校验库存
        Stock stock=checkStock(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        //创建订单
        int id=createOrder(stock);
        return stock.getCount()-(stock.getSale()+1);
    }

    private void saleStockOptimistic(Stock stock){
        LOGGER.info("查询数据库尝试更新库存");
        int count=stockMapper.updateByOptimistic(stock);
        if(count==0){
            throw new RuntimeException("并发更新库存失败，version不匹配");
        }
    }
```

> OrderController

```java
@RequestMapping("/createOptimisticOrder/{sid}")
    @ResponseBody
    public String createOptimisticOrder(@PathVariable int sid){
        int id=0;
        try{
            id=stockService.createOptimisticOrder(sid);
            LOGGER.info("购买成功，剩余库存为：[{}]",id);
        }catch (Exception e){
            LOGGER.error("购买失败，库存不足");
        }
        return "购买成功，剩余库存为"+id;
    }
```

> StockMapper

```java
int updateByOptimistic(Stock stock);
```

> StockMapper.xml

```xml
<update id="updateByOptimistic" parameterType="com.zc.entity.Stock">
       update stock
       <set>
           sale=sale+1,
           version=version+1,
       </set>
       where id = #{id}
       and version=#{version}
    </update>
```

接下来我们使用Jmeter对**localhost:8081/order/createOptimisticOrder/1**进行压测，结果是没有超卖，但是严重少卖了，原因就是我们在更新的时候使用了类似于java的**CAS**操作。

## 3、令牌桶限流 + 再谈超卖

