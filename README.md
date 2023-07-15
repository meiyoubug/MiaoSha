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

**为什么不加注解就不行？**

在不加注解的情况下，每一个阶段都是一个隐性事务，是针对数据库层面来说的，只要sql语句执行成功就不会回滚，还有我们在checkStock抛出的异常没有捕获，当流程3抛出异常的时候，流程4是不会执行的。

当500个请求并发执行的时候，流程1只要看到有库存就不会抛出异常，因为select执行速度是比update快的，所以可能500个都不会抛出异常，流程2是当前读，那么理论上流程3获取到的库存一定是<=流程2执行结束的时候的，那么我们期望的结果应该是少卖，但是结果却是多卖了，具体原因我查了资料，目前还不清楚。

**那为什么加上注解就可以呢？**

加上注解的话**1、查看有没有库存->2、更新库存->3、查看有没有库存->4、创建订单**流程1-4都在一个事务里，因为隔离级别是可重复读，所以流程3读的是流程2的最新值，且流程2会上行锁，行锁只有在当前事务commit的时候才会释放，当当前事务还没提交的时候，其它事务都会阻塞在流程2，所以加上注解就可以防止超卖，且最后的结果也是我们所期望的结果。

`注意：`因为我是在可重复读的隔离级别下，原博主使用的是

```sql
select xxxxxxx for update;
```

这样会在执行select语句的时候就加了行锁，那么开始的时候上锁，结束的时候释放锁，这样不利于mysql程面的并发，所以我们应当把上锁时机尽量的往后放，update语句是会自动添加行锁的，所以不需要在select 后面加上for update。

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

#### 令牌桶限流

> 令牌桶限流

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640.png)

> 令牌桶与漏桶算法

漏桶算法思路很简单，水（请求）先进入到漏桶里，漏桶以一定的速度出水，当水流入速度过大会直接溢出，可以看出漏桶算法能强行限制数据的传输速率。

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640-20230715130813316.png)

令牌桶算法不能与另外一种常见算法漏桶算法相混淆。这两种算法的主要区别在于：

漏桶算法能够强行限制数据的传输速率，而令牌桶算法在能够限制数据的平均传输速率外，**还允许某种程度的突发传输**。在令牌桶算法中，只要令牌桶中存在令牌，那么就允许突发地传输数据直到达到用户配置的门限，**因此它适合于具有突发特性的流量**。

`提示：`漏桶算法的出水速度是固定的，所以说能够强行限制数据的传输速率，而令牌桶算法只要是桶内有令牌那么就可以在固定传输速率的基础上继续突发的传输数据直到达到用户配置的门限。

> #### 使用Guava的RateLimiter实现令牌桶限流接口

Guava是Google开源的Java工具类，里面包罗万象，也提供了限流工具类RateLimiter，该类里面实现了令牌桶算法。

我们拿出源码，在之前讲过的乐观锁抢购接口上增加该令牌桶限流代码：

- OrderController

```java
 /**
     * 乐观锁更新库存+令牌桶限流
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOptimisticOrder/{sid}")
    @ResponseBody
    public String createOptimisticOrder(@PathVariable int sid) {
        //阻塞式获取令牌
        //LOGGER.info("等待时间" + rateLimiter.acquire());
        
        //非阻塞式获取令牌
        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("你被限流了，真不幸，直接返回失败");
            return "购买失败，库存不足";
        }


        int id = 0;
        try {
            id = stockService.createOptimisticOrder(sid);
            LOGGER.info("购买成功，剩余库存为：[{}]", id);
        } catch (Exception e) {
            LOGGER.error("购买失败，库存不足");
        }
        return "购买成功，剩余库存为" + id;
    }
```

代码中，`RateLimiter rateLimiter = RateLimiter.create(10);`这里初始化了令牌桶类，每秒放行10个请求。

在接口中，可以看到有两种使用方法：

- 阻塞式获取令牌：请求进来后，若令牌桶里没有足够的令牌，就在这里阻塞住，等待令牌的发放。
- 非阻塞式获取令牌：请求进来后，若令牌桶里没有足够的令牌，会尝试等待设置好的时间（这里写了1000ms），其会自动判断在1000ms后，这个请求能不能拿到令牌，如果不能拿到，直接返回抢购失败。如果timeout设置为0，则等于阻塞时获取令牌。

我们使用JMeter设置200个线程，来同时抢购数据库里库存100个的iphone。

我们将请求响应结果为“你被限流了，真不幸，直接返回失败”的请求单独断言出来：

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640.jpeg)

我们使用`rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)`，非阻塞式的令牌桶算法，来看看购买结果：

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640-20230715132634825.jpeg)

可以看到，**绿色的请求代表被令牌桶拦截掉的请求**，红色的则是购买成功下单的请求。通过JMeter的请求汇总报告，可以得知，在这种情况下请求能够没被限流的比率在42.67%。

![image-20230715133043152](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715133043152.png)

可以看到，200个请求中没有被限流的请求里，由于乐观锁的原因，会出现一些并发更新数据库失败的问题，导致商品没有被卖出。

我们再试一试令牌桶算法的阻塞式使用，我们将代码换成`rateLimiter.acquire();`，然后将数据库恢复成100个库存，订单表清零。

响应断言设置为没被限流的返回结果，此时controller的返回结果改成**return "购买成功";**

![image-20230715134127313](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715134127313.png)

看一下汇总报告：

![image-20230715135056861](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715135056861.png)

所有的请求都没被拦截

`总结：`

- 首先，所有请求进入了处理流程，但是被限流成每秒处理10个请求。
- 在刚开始的请求里，令牌桶里一下子被取了10个令牌，所以出现了第二张图中的，乐观锁并发更新失败，然而在后面的请求中，由于令牌一旦生成就被拿走，所以请求进来的很均匀，没有再出现并发更新库存的情况。**这也符合“令牌桶”的定义，可以应对突发请求（只是由于乐观锁，所以购买冲突了）。而非“漏桶”的永远恒定的请求限制。**
- 200个请求，**在乐观锁的情况下**，卖出了全部100个商品，如果没有该限流，而请求又过于集中的话，会卖不出去几个。就像第一篇文章中的那种情况一样。

![image-20230715135129234](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715135129234.png)

![image-20230715135146251](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230715135146251.png)

#### 再谈防止超卖

讲完了令牌桶限流算法，我们再回头思考超卖的问题，在**海量请求**的场景下，如果**使用乐观锁**，会导致大量的请求返回抢购失败，用户体验极差。

然而使用悲观锁，比如数据库事务，则可以让数据库一个个处理库存数修改，修改成功后再迎接下一个请求，所以在不同情况下，应该根据实际情况使用悲观锁和乐观锁。

> 悲观锁(Pessimistic Lock), 顾名思义，就是很悲观，每次去拿数据的时候都认为别人会修改，所以每次在拿数据的时候都会上锁，这样别人想拿这个数据就会block直到它拿到锁。传统的关系型数据库里边就用到了很多这种锁机制，比如行锁，表锁等，读锁，写锁等，都是在做操作之前先上锁。
>
> 乐观锁(Optimistic Lock), 顾名思义，就是很乐观，每次去拿数据的时候都认为别人不会修改，所以不会上锁，但是在更新的时候会判断一下在此期间别人有没有去更新这个数据，可以使用版本号等机制。乐观锁适用于多读的应用类型，这样可以提高吞吐量，像数据库如果提供类似于write_condition机制的其实都是提供的乐观锁。

**两种锁各有优缺点，不能单纯的定义哪个好于哪个。**

- 乐观锁比较适合数据修改比较少，读取比较频繁的场景，即使出现了少量的冲突，这样也省去了大量的锁的开销，故而提高了系统的吞吐量。
- 但是如果经常发生冲突（写数据比较多的情况下），上层应用不不断的retry，这样反而降低了性能，对于这种情况使用悲观锁就更合适。

> 实现不需要版本号字段的乐观锁

- StockMapper.xml

```xml
<update id="updateByOptimistic" parameterType="com.zc.entity.Stock">
       update stock
       <set>
           sale=sale+1,
           version=version+1,
       </set>
       where id = #{id}
       and sale=#{sale}
    </update>
```

