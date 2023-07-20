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



## 4、抢购接口隐藏+单用户限制频率

**抢购接口隐藏（接口加盐）的具体做法**：

- 每次点击秒杀按钮，先从服务器获取一个秒杀验证值（接口内判断是否到秒杀时间）。
- Redis以缓存用户ID和商品ID为Key，秒杀地址为Value缓存验证值
- 用户请求秒杀商品的时候，要带上秒杀验证值进行校验。



**代码实现**

- 获取验证值接口

该接口要求传用户id和商品id，返回验证值，并且该验证值

- UserService

```java
public interface UserService extends IService<User> {
     String getVerifyHash(Integer sid,Integer userId) throws Exception;
}
```



- UserServiceImpl

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static  final Logger LOGGER=  LoggerFactory.getLogger(UserServiceImpl.class);
    @Resource
    private UserMapper userMapper;
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String SALT=CacheKey.HASH_KEY.getKey();

    
    @Override
    public String getVerifyHash(Integer sid, Integer userId) throws Exception {

        //检查用户合法性
        User user=userMapper.selectById(userId);
        if(user==null){
            throw new Exception("用户不存在");
        }
        LOGGER.info("用户信息：[{}]",user.toString());

        //检查商品合法性
        Stock stock=stockMapper.selectById(sid);
        if(stock==null){
            throw new Exception("商品不存在");
        }
        LOGGER.info("用户信息：[{}]",stock.toString());

        //生成hash
        String verify=SALT+sid+userId;
        String verifyHash= DigestUtils.md5DigestAsHex(verify.getBytes());

        //将hash和用户商品信息存入redis
        String hashKey= CacheKey.HASH_KEY.getKey()+"_"+sid+"_"+userId;
        stringRedisTemplate.opsForValue().set(hashKey,verifyHash,3600, TimeUnit.SECONDS);
        LOGGER.info("Redis写入：[{}] [{}]",hashKey,verifyHash);
        return verifyHash;
    }
}
```



- UserController

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    private static final Logger LOGGER= LoggerFactory.getLogger(UserController.class);
    @GetMapping("/getVerifyHash/{sid}/{userId}")
    @ResponseBody
    public String getVerifyHash(@PathVariable("sid") Integer sid,
                                @PathVariable("userId") Integer userId){
        String hash;
        try{
            hash= userService.getVerifyHash(sid,userId);
        }catch (Exception e){
            LOGGER.error("获取验证hash失败，原因：[{}]",e.getMessage());
            return  "获取验证hash失败";
        }
        return String.format("请求抢购验证hash值为：%s",hash);
    }
}
```

代码解释：

可以看到在Service中，我们拿到用户id和商品id后，会检查商品和用户信息是否在表中存在，并且会验证现在的时间（我这里为了简化，只是写了一行LOGGER，大家可以根据需求自行实现）。在这样的条件过滤下，才会给出hash值。**并且将Hash值写入了Redis中，缓存3600秒（1小时），如果用户拿到这个hash值一小时内没下单，则需要重新获取hash值。**

下面又到了动小脑筋的时间了，想一下，这个hash值，如果每次都按照商品+用户的信息来md5，是不是不太安全呢。毕竟用户id并不一定是用户不知道的（就比如我这种用自增id存储的，肯定不安全），而商品id，万一也泄露了出去，那么坏蛋们如果再知到我们是简单的md5，那直接就把hash算出来了！

在代码里，我给hash值加了个前缀，也就是一个salt（盐），相当于给这个固定的字符串撒了一把盐，这个盐是`HASH_KEY("miaosha_hash")`，写死在了代码里。这样黑产只要不猜到这个盐，就没办法算出来hash值。

**这也只是一种例子，实际中，你可以把盐放在其他地方， 并且不断变化，或者结合时间戳，这样就算自己的程序员也没法知道hash值的原本字符串是什么了。**

#### 携带验证值下单接口

用户在前台拿到了验证值后，点击下单按钮，前端携带着特征值，即可进行下单操作。

- StockService

```java
 int createVerifiedOrder(Integer sid,Integer userId,String verifyHash) throws Exception;
```



- StockServiceImpl

```java
@Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {
        //判断是否在秒杀时间内
        LOGGER.info("请自行验证是否在秒杀时间内");

        //验证hash值合法性
        String hashKey= CacheKey.HASH_KEY.getKey()+"_"+sid+"_"+userId;
        String verifyHashInRedis=stringRedisTemplate.opsForValue().get(hashKey);
        if(!verifyHash.equals(verifyHashInRedis)){
            throw new Exception("hash与Redis中不符合");
        }
        LOGGER.info("验证hash值合法性成功");

        Stock stock=stockMapper.selectById(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        LOGGER.info("乐观锁更新库存成功");

        //创建订单
        StockOrder stockOrder=new StockOrder();
        stockOrder.setSid(sid);
        stockOrder.setName(stock.getName());
        stockOrder.setUserId(userId);
        stockOrderMapper.insert(stockOrder);
        LOGGER.info("创建订单成果");
        return stock.getCount()-(stock.getSale()+1);
    }
```



- OrderController

```java
    @GetMapping("/createOrderWithVerifiedUrl/{sid}/{userId}/{verifyHash}")
    @ResponseBody
    public String createOrderWithVerifiedUrl(@PathVariable("sid") Integer sid,
                                             @PathVariable("userId") Integer userId,
                                             @PathVariable("verifyHash") String verifyHash) {
        int stockLeft;
        try {
            stockLeft = stockService.createVerifiedOrder(sid, userId, verifyHash);
            LOGGER.info("购买成功，剩余库存为：[{}]", stockLeft);
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return "购买成功，剩余库存为：" + stockLeft;
    }
```

**单用户限制频率**

假设我们做好了接口隐藏，但是像我上面说的，总有无聊的人会写一个复杂的脚本，先请求hash值，再立刻请求购买，如果你的app下单按钮做的很差，大家都要开抢后0.5秒才能请求成功，那可能会让脚本依然能够在大家前面抢购成功。

我们需要在做一个额外的措施，来限制单个用户的抢购频率。

其实很简单的就能想到用redis给每个用户做访问统计，甚至是带上商品id，对单个商品做访问统计，这都是可行的。

我们先实现一个对用户的访问频率限制，我们在用户申请下单时，检查用户的访问次数，超过访问次数，则不让他下单！

使用Redis/Memcached

我们使用外部缓存来解决问题，这样即便是分布式的秒杀系统，请求被随意分流的情况下，也能做到精准的控制每个用户的访问次数。

- StockService

```java
     int addUserCount(Integer userId) throws Exception;

     boolean getUserIsBanned(Integer userId);
```



- Impl

```java
    private static final Integer ALLOW_COUNT = 10;

    @Override
    public int addUserCount(Integer userId) throws Exception {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        int limit = -1;
        if (limitNum == null) {
            stringRedisTemplate.opsForValue().set(limitKey, "0", 3600, TimeUnit.SECONDS);
        } else {
            limit = Integer.parseInt(limitNum) + 1;
            stringRedisTemplate.opsForValue().set(limitKey, String.valueOf(limit), 3600, TimeUnit.SECONDS);
        }
        return limit == -1 ? 0 : limit;
    }

    @Override
    public boolean getUserIsBanned(Integer userId) {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        if (limitNum == null) {
            LOGGER.error("该用户没有访问申请验证值记录，疑似异常");
            return true;
        }
        return Integer.parseInt(limitNum) > ALLOW_COUNT;
    }
```



- OrderController

```java
    @GetMapping("/createOrderWithVerifiedUrlAndLimit/{sid}/{userId}/{verifyHash}")
    @ResponseBody
    public String createOrderWithVerifiedUrlAndLimit(@PathVariable("sid") Integer sid,
                                                     @PathVariable("userId") Integer userId,
                                                     @PathVariable("verifyHash") String verifyHash) {
        int stockLeft;
        try {
            int count = stockService.addUserCount(userId);
            LOGGER.info("用户截至该次的访问次数为: [{}]", count);
            boolean isBanned = stockService.getUserIsBanned(userId);
            if (isBanned) {
                return "购买失败，超过频率限制";
            }
            stockLeft = stockService.createVerifiedOrder(sid, userId, verifyHash);
            LOGGER.info("购买成功，剩余库存为：[{}]", stockLeft);
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return "购买成功，剩余库存为：" + stockLeft;
    }
```

使用jmeter自行测试

## 5、缓存与数据库双写问题

在秒杀实际的业务中，一定有很多需要做缓存的场景，比如售卖的商品，包括名称，详情等。访问量很大的数据，可以算是“热点”数据了，尤其是一些读取量远大于写入量的数据，更应该被缓存，而不应该让请求打到数据库上。

> 为什么要使用缓存

缓存是为了追求“快”而存在的。

在之前的代码基础上，在其中增加两个查询库存的接口getStockByDB和getStockByCache，分别表示从数据库和缓存查询某商品的库存量。随后我们用JMeter进行并发请求测试。

- StockService

```java
    void setStockCountToCache(int sid, int count);

    Integer getStockCountByCache(int sid);

    int getStockCountByDB(int sid);
```



- Impl

```java
    @Override
    public void setStockCountToCache(int sid, int count) {
        String hashKey = CacheKey.GoodsKey.getKey() + "_" + sid;
        stringRedisTemplate.opsForValue().set(hashKey, String.valueOf(count));
    }

    @Override
    public Integer getStockCountByCache(int sid) {
        String hashKey = CacheKey.GoodsKey.getKey() + "_" + sid;
        String count = stringRedisTemplate.opsForValue().get(hashKey);
        return Integer.parseInt(count);
    }

    @Override
    public int getStockCountByDB(int sid) {
        Stock stock = stockMapper.selectById(sid);
        return stock.getCount() - stock.getSale();
    }
```



- OrderController

```java
    /**
     * 查询库存：通过数据库查询库存
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/getStockByDB/{sid}")
    @ResponseBody
    public String getStockByDB(@PathVariable int sid) {
        int count;
        try {
            count = stockService.getStockCountByDB(sid);
        } catch (Exception e) {
            LOGGER.error("查询库存失败：[{}]", e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品id：[{}] 剩余库存为：[{}]", sid, count);
        return String.format("商品Id: %d 剩余库存为：%d", sid, count);
    }

    /**
     * 查询库存：通过缓存查询库存
     * 缓存命中：返回库存
     * 缓存未命中：查询数据库写入缓存并返回
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/getStockByCache/{sid}")
    @ResponseBody
    public String getStockByCache(@PathVariable int sid) {
        Integer count;
        try {
            count = stockService.getStockCountByCache(sid);
            if (count == null) {
                count = stockService.getStockCountByDB(sid);
                LOGGER.info("缓存未命中，查询数据库，并写入缓存");
                stockService.setStockCountToCache(sid, count);
            }
        } catch (Exception e) {
            LOGGER.error("查询库存失败：[{}]", e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品Id: [{}] 剩余库存为: [{}]", sid, count);
        return String.format("商品Id: %d 剩余库存为：%d", sid, count);
    }
```

在设置为10000个并发请求的情况下，运行JMeter，我们在配置Application.yml的时候已经配置Tomcat的最大并发为10000了

- 不使用缓存

不使用缓存的情况下，吞吐量为141.1个请求每秒，并且有3.57%的请求由于服务压力实在太大，没有返回库存数据：

![image-20230720135016127](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230720135016127.png)

- 使用缓存

使用缓存的情况下，吞吐量为315.7，性能差不多提升3倍

![image-20230720141109063](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230720141109063.png)

> 哪类数据适合缓存

缓存量大但又不常变化的数据，比如详情，评论等。对于那些经常变化的数据，其实并不适合缓存，一方面会增加系统的复杂性（缓存的更新，缓存脏数据），另一方面也给系统带来一定的不稳定性（缓存系统的维护）。

**「但一些极端情况下，你需要将一些会变动的数据进行缓存，比如想要页面显示准实时的库存数，或者其他一些特殊业务场景。这时候你需要保证缓存不能（一直）有脏数据，这就需要再深入讨论一下。」**

> 缓存的利与弊

我们到底该不该上缓存的，这其实也是个trade-off的问题。

上缓存的优点：

- 能够缩短服务的响应时间，给用户带来更好的体验。
- 能够增大系统的吞吐量，依然能够提升用户体验。
- 减轻数据库的压力，防止高峰期数据库被压垮，导致整个线上服务BOOM！

上了缓存，也会引入很多额外的问题：

- 缓存有多种选型，是内存缓存，memcached还是redis，你是否都熟悉，如果不熟悉，无疑增加了维护的难度（本来是个纯洁的数据库系统）。
- 缓存系统也要考虑分布式，比如redis的分布式缓存还会有很多坑，无疑增加了系统的复杂性。
- 在特殊场景下，如果对缓存的准确性有非常高的要求，就必须考虑**「缓存和数据库的一致性问题」**。



> 缓存与数据库双写一致性

- 不使用更新缓存而是删除缓存

**「大部分观点认为，做缓存不应该是去更新缓存，而是应该删除缓存，然后由下个请求去去缓存，发现不存在后再读取数据库，写入缓存。」**

《分布式之数据库和缓存双写一致性方案解析》孤独烟：

> ❝
>
> **「原因一：线程安全角度」**
>
> 同时有请求A和请求B进行更新操作，那么会出现
>
> （1）线程A更新了数据库
>
> （2）线程B更新了数据库
>
> （3）线程B更新了缓存
>
> （4）线程A更新了缓存
>
> 这就出现请求A更新缓存应该比请求B更新缓存早才对，但是因为网络等原因，B却比A更早更新了缓存。这就导致了脏数据，因此不考虑。
>
> **「原因二：业务场景角度」**
>
> 有如下两点：
>
> （1）如果你是一个写数据库场景比较多，而读数据场景比较少的业务需求，采用这种方案就会导致，数据压根还没读到，缓存就被频繁的更新，浪费性能。
>
> （2）如果你写入数据库的值，并不是直接写入缓存的，而是要经过一系列复杂的计算再写入缓存。那么，每次写入数据库后，都再次计算写入缓存的值，无疑是浪费性能的。显然，删除缓存更为适合。
>
> ❞

**「其实如果业务非常简单，只是去数据库拿一个值，写入缓存，那么更新缓存也是可以的。但是，淘汰缓存操作简单，并且带来的副作用只是增加了一次cache miss，建议作为通用的处理方式。」**

-  先删除缓存，还是先删除数据库

**「那么问题就来了，我们是先删除缓存，然后再更新数据库，还是先更新数据库，再删缓存呢？」**

先来看看大佬们怎么说。

《【58沈剑架构系列】缓存架构设计细节二三事》58沈剑：

> ❝
>
> 对于一个不能保证事务性的操作，一定涉及“哪个任务先做，哪个任务后做”的问题，解决这个问题的方向是：如果出现不一致，谁先做对业务的影响较小，就谁先执行。
>
> 假设先淘汰缓存，再写数据库：第一步淘汰缓存成功，第二步写数据库失败，则只会引发一次Cache miss。
>
> 假设先写数据库，再淘汰缓存：第一步写数据库操作成功，第二步淘汰缓存失败，则会出现DB中是新数据，Cache中是旧数据，数据不一致。
>
> ❞

沈剑老师说的没有问题，不过**「没完全考虑好并发请求时的数据脏读问题」**，让我们再来看看孤独烟老师《分布式之数据库和缓存双写一致性方案解析》：

> ❝
>
> **「先删缓存，再更新数据库」**
>
> 该方案会导致请求数据不一致
>
> 同时有一个请求A进行更新操作，另一个请求B进行查询操作。那么会出现如下情形:
>
> （1）请求A进行写操作，删除缓存
>
> （2）请求B查询发现缓存不存在
>
> （3）请求B去数据库查询得到旧值
>
> （4）请求B将旧值写入缓存
>
> （5）请求A将新值写入数据库
>
> 上述情况就会导致不一致的情形出现。而且，如果不采用给缓存设置过期时间策略，该数据永远都是脏数据。
>
> ❞

**「所以先删缓存，再更新数据库并不是一劳永逸的解决方案，再看看先更新数据库，再删缓存」**

> ❝
>
> **「先更新数据库，再删缓存」**这种情况不存在并发问题么？
>
> 不是的。假设这会有两个请求，一个请求A做查询操作，一个请求B做更新操作，那么会有如下情形产生
>
> （1）缓存刚好失效
>
> （2）请求A查询数据库，得一个旧值
>
> （3）请求B将新值写入数据库
>
> （4）请求B删除缓存
>
> （5）请求A将查到的旧值写入缓存
>
> ok，如果发生上述情况，确实是会发生脏数据。
>
> 然而，发生这种情况的概率又有多少呢？
>
> 发生上述情况有一个先天性条件，就是步骤（3）的写数据库操作比步骤（2）的读数据库操作耗时更短，才有可能使得步骤（4）先于步骤（5）。可是，大家想想，**「数据库的读操作的速度远快于写操作的（不然做读写分离干嘛，做读写分离的意义就是因为读操作比较快，耗资源少），因此步骤（3）耗时比步骤（2）更短，这一情形很难出现。」**
>
> ❞

**「先更新数据库，再删缓存」**依然会有问题，不过，问题出现的可能性会因为上面说的原因，变得比较低！

所以，如果你想实现基础的缓存数据库双写一致的逻辑，那么在大多数情况下，在不想做过多设计，增加太大工作量的情况下，请**「先更新数据库，再删缓存!」**

- 一定要数据库和缓存数据一致怎么办

那么，如果我tm非要保证绝对一致性怎么办，先给出结论：

**「没有办法做到绝对的一致性，这是由CAP理论决定的，缓存系统适用的场景就是非强一致性的场景，所以它属于CAP中的AP。」**

所以，我们得委曲求全，可以去做到BASE理论中说的**「最终一致性」**。

> ❝
>
> 最终一致性强调的是系统中所有的数据副本，在经过一段时间的同步后，最终能够达到一个一致的状态。因此，最终一致性的本质是需要系统保证最终数据能够达到一致，而不需要实时保证系统数据的强一致性
>
> ❞

大佬们给出了到达最终一致性的解决思路，主要是针对上面两种双写策略（先删缓存，再更新数据库/先更新数据库，再删缓存）导致的脏数据问题，进行相应的处理，来保证最终一致性。

> 延时双删

问：先删除缓存，再更新数据库中避免脏数据？

答案：采用延时双删策略。

上文我们提到，在先删除缓存，再更新数据库的情况下，如果不采用给缓存设置过期时间策略，该数据永远都是脏数据。

**「那么延时双删怎么解决这个问题呢？」**

> ❝
>
> （1）先淘汰缓存
>
> （2）再写数据库（这两步和原来一样）
>
> （3）休眠1秒，再次淘汰缓存
>
> 这么做，可以将1秒内所造成的缓存脏数据，再次删除。
>
> ❞

**「那么，这个1秒怎么确定的，具体该休眠多久呢？」**

> ❝
>
> 针对上面的情形，读者应该自行评估自己的项目的读数据业务逻辑的耗时。然后写数据的休眠时间则在读数据业务逻辑的耗时基础上，加几百ms即可。这么做的目的，就是确保读请求结束，写请求可以删除读请求造成的缓存脏数据。
>
> ❞

**「如果你用了mysql的读写分离架构怎么办？」**

> ❝
>
> ok，在这种情况下，造成数据不一致的原因如下，还是两个请求，一个请求A进行更新操作，另一个请求B进行查询操作。
>
> （1）请求A进行写操作，删除缓存
>
> （2）请求A将数据写入数据库了，
>
> （3）请求B查询缓存发现，缓存没有值
>
> （4）请求B去从库查询，这时，还没有完成主从同步，因此查询到的是旧值
>
> （5）请求B将旧值写入缓存
>
> （6）数据库完成主从同步，从库变为新值
>
> 上述情形，就是数据不一致的原因。还是使用双删延时策略。只是，睡眠时间修改为在主从同步的延时时间基础上，加几百ms。
>
> ❞

**「采用这种同步淘汰策略，吞吐量降低怎么办？」**

> ❝
>
> ok，那就将第二次删除作为异步的。自己起一个线程，异步删除。这样，写的请求就不用沉睡一段时间后了，再返回。这么做，加大吞吐量。
>
> ❞

**「所以在先删除缓存，再更新数据库的情况下」**，可以使用延时双删的策略，来保证脏数据只会存活一段时间，就会被准确的数据覆盖。

**「在先更新数据库，再删缓存的情况下」**，缓存出现脏数据的情况虽然可能性极小，但也会出现。我们依然可以用延时双删策略，在请求A对缓存写入了脏的旧值之后，再次删除缓存。来保证去掉脏缓存。

> 删缓存失败了怎么办：重试机制

看似问题都已经解决了，但其实，还有一个问题没有考虑到，那就是删除缓存的操作，失败了怎么办？比如延时双删的时候，第二次缓存删除失败了，那不还是没有清除脏数据吗？

**「解决方案就是再加上一个重试机制，保证删除缓存成功。」**

参考孤独烟老师给的方案图：

**「方案一：」**

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640-20230720141802970.png)

> ❝
>
> 流程如下所示
>
> （1）更新数据库数据；
>
> （2）缓存因为种种问题删除失败
>
> （3）将需要删除的key发送至消息队列
>
> （4）自己消费消息，获得需要删除的key
>
> （5）继续重试删除操作，直到成功
>
> 然而，该方案有一个缺点，对业务线代码造成大量的侵入。于是有了方案二，在方案二中，启动一个订阅程序去订阅数据库的binlog，获得需要操作的数据。在应用程序中，另起一段程序，获得这个订阅程序传来的信息，进行删除缓存操作。
>
> ❞

方案二：

![图片](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/640-20230720141803021.png)

> ❝
>
> 流程如下图所示：
>
> （1）更新数据库数据
>
> （2）数据库会将操作信息写入binlog日志当中
>
> （3）订阅程序提取出所需要的数据以及key
>
> （4）另起一段非业务代码，获得该信息
>
> （5）尝试删除缓存操作，发现删除失败
>
> （6）将这些信息发送至消息队列
>
> （7）重新从消息队列中获得该数据，重试操作。
>
> ❞

**「而读取binlog的中间件，可以采用阿里开源的canal」**

好了，到这里我们已经把缓存双写一致性的思路彻底梳理了一遍，下面就是我对这几种思路徒手写的实战代码，方便有需要的朋友参考。

### 实战

> 先删除缓存，再更新数据库

- StockService

```java
void delStockCountCache(int sid);
```

- Impl

```java
    @Override
    public void delStockCountCache(int sid) {
        String hashKey=CacheKey.GoodsKey.getKey()+"_"+sid;
        stringRedisTemplate.delete(hashKey);
        LOGGER.info("删除商品id：[{}]缓存",sid);
    }
```

- OrderController

```java
    /**
     * 下单接口：先删除缓存，再更新数据库
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV1/{sid}")
    @ResponseBody
    public String createOrderWithCacheV1(@PathVariable int sid) {
        int count = 0;
        try {
            //删除缓存
            stockService.delStockCountCache(sid);
            //完成扣库存下单事务
            stockService.createWrongOrder(sid);
        } catch (Exception e) {
            LOGGER.info("购买失败：[{}]", e.getMessage());
            return "购买失败";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }
```

> 先更新数据库，再删除缓存

- OrderController

```java
 /**
     * 下单接口：先更新数据库，再删缓存
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV2/{sid}")
    @ResponseBody
    public String createOrderWithCacheV2(@PathVariable int sid) {
        int count = 0;
        try {

            //完成扣库存下单事务
            stockService.createWrongOrder(sid);

            //删除缓存
            stockService.delStockCountCache(sid);
        } catch (Exception e) {
            LOGGER.info("购买失败：[{}]", e.getMessage());
            return "购买失败";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }
```

> 缓存延时双删

如何做延时双删呢，最好的方法是开设一个线程池，在线程中删除key，而不是使用Thread.sleep进行等待，这样会阻塞用户的请求。

更新前先删除缓存，然后更新数据，再延时删除缓存。

![image-20230720145804785](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230720145804785.png)

将线程池交给Spring容器管理

- MyThreadFactory

```java
public class MyThreadFactory  implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread newThread=new Thread(r);
        return  newThread;
    }
}
```

- ThreadRejectedExecutionHandler

```java
public class ThreadRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
    * @Description: 饱和策略一：调用者线程执行策略
    * @Param: 在该策略下,在调用者中执行被拒绝任务的run方法。除非线程池showdown，否则直接丢弃线程
    * @return:
    * @Author: ZC
    * @Date: 2023/5/21
    */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {}


    /**
     * 饱和策略一：调用者线程执行策略
     * 在该策略下，在调用者中执行被拒绝任务的run方法。除非线程池showdown，否则直接丢弃线程
     */
    public static class CallerRunsPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //判断线程池是否在正常运行，如果线程池在正常运行则由调用者线程执行被拒绝的任务。如果线程池停止运行，则直接丢弃该任务
            if (!executor.isShutdown()){
                r.run();
            }
        }
    }


    /**
     * 饱和策略二：终止策略
     * 在该策略下，丢弃被拒绝的任务，并抛出拒绝执行异常
     */
    public static class AbortPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            throw new RejectedExecutionException("请求任务：" + r.toString() + "，线程池负载过高执行饱和终止策略！");
        }
    }


    /**
     * 饱和策略三：丢弃策略
     * 在该策略下，什么都不做直接丢弃被拒绝的任务
     */
    public static class DiscardPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        }
    }


    /**
     * 饱和策略四：弃老策略
     * 在该策略下，丢弃最早放入阻塞队列中的线程，并尝试将拒绝任务加入阻塞队列
     */
    public static class DiscardOldestPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //判断线程池是否正常运行，如果线程池正常运行则弹出（或丢弃）最早放入阻塞队列中的任务，并尝试将拒绝任务加入阻塞队列。如果线程池停止运行，则直接丢弃该任务
            if (!executor.isShutdown()){
                executor.getQueue().poll();
                executor.execute(r);
            }
        }
    }

}
```

- ThreadPool

```java
@Component
public class ThreadPool{
    /**
     * 系统可用计算资源
     */
    private static final  int CPU_COUNT=Runtime.getRuntime().availableProcessors();


    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE=Math.max(2,Math.min(CPU_COUNT-1,4));

    /**
     * 最大线程数
     */
    private static final int MAXIMUM_POOL_SIZE=CPU_COUNT*2+1;

    /**
     * 线程最大空闲存活时间
     */
    private static final int KEEP_ALIVE_SECONDS = 30;

    /**
     * 工作队列
     */
    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE = new LinkedBlockingQueue<>(2);


    /**
     * 工厂模式
     */
    private static final MyThreadFactory MY_THREAD_FACTORY = new MyThreadFactory();


    /**
     * 饱和策略
     */
    private static final ThreadRejectedExecutionHandler THREAD_REJECTED_EXECUTION_HANDLER = new ThreadRejectedExecutionHandler.CallerRunsPolicy();


    /**
     * 线程池对象
     */
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    /**
     * 声明式定义线程池工具类对象静态变量，在所有线程中同步
     */
    private static volatile ThreadPool threadPool = null;


    /**
     * 初始化线程池静态代码块
     */
    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                //核心线程数
                CORE_POOL_SIZE,
                //最大线程数
                MAXIMUM_POOL_SIZE,
                //空闲线程执行时间
                KEEP_ALIVE_SECONDS,
                //空闲线程执行时间单位
                TimeUnit.SECONDS,
                //工作队列（或阻塞队列）
                POOL_WORK_QUEUE,
                //工厂模式
                MY_THREAD_FACTORY,
                //饱和策略
                THREAD_REJECTED_EXECUTION_HANDLER
        );
    }

    /**
     * 线程池工具类空参构造方法
     */
    public ThreadPool() {}



    /**
     * 获取线程池工具类实例
     */
    @Bean
    public ThreadPool getNewInstance(){
        if (threadPool == null) {
            synchronized (ThreadPool.class) {
                if (threadPool == null) {
                    threadPool = new ThreadPool();
                }
            }
        }
        return threadPool;
    }

    /**
     * 获得当前活动线程数
     *
     * @return int
     */
    public int getCorePoolSize(){
        return THREAD_POOL_EXECUTOR.getActiveCount();
    }


    /**
     * 获得全当前任务总数
     *
     * @return int
     */
    public int getTaskNum(){
        return THREAD_POOL_EXECUTOR.getQueue().size();
    }

    /**
     * 执行任务线程
     */
    public void execut(Runnable runnable) {
        THREAD_POOL_EXECUTOR.execute(runnable);
    }

    public <T> Future<T> submit(Callable<T> callable){
        return THREAD_POOL_EXECUTOR.submit(callable);
    }


    /**
     * 获取线程池状态
     * @return 返回线程池状态
     */
    public boolean isShutDown(){
        return THREAD_POOL_EXECUTOR.isShutdown();
    }

    /**
     * 停止正在执行的线程任务
     * @return 返回等待执行的任务列表
     */
    public List<Runnable> shutDownNow(){
        return THREAD_POOL_EXECUTOR.shutdownNow();
    }

    /**
     * 关闭线程池
     */
    public void shutDown(){
        THREAD_POOL_EXECUTOR.shutdown();
    }


    /**
     * 关闭线程池后判断所有任务是否都已完成
     * @return
     */
    public boolean isTerminated(){
        return THREAD_POOL_EXECUTOR.isTerminated();
    }
}
```

- DelCacheByThread

![image-20230720153618024](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230720153618024.png)

```java
public class DelCacheByThread implements Runnable {

    @Resource
    private StockService stockService;
    private static final Logger LOGGER = LoggerFactory.getLogger(DelCacheByThread.class);
    private static final int DELAY_MILLSECONDS = 1000;
    private int sid;

    public DelCacheByThread(int sid) {
        this.sid = sid;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("异步执行缓存再删除，商品id：[{}]， 首先休眠：[{}] 毫秒", sid, DELAY_MILLSECONDS);
            Thread.sleep(DELAY_MILLSECONDS);
            stockService.delStockCountCache(sid);
            LOGGER.info("再次删除商品id：[{}] 缓存", sid);
        } catch (Exception e) {
            LOGGER.error("delCacheByThread执行出错", e);
        }
    }
}
```



- OrderController

```java
 @Resource
    private ThreadPool threadPool;


    /**
     * 每秒放行10个请求
     */
    RateLimiter rateLimiter = RateLimiter.create(10);

    /**
     * 下单接口：先删除缓存，再更新数据库，缓存延时双删
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV3/{sid}")
    @ResponseBody
    public String createOrderWithCacheV3(@PathVariable int sid) {
        int count;
        try {
            // 删除库存缓存
            stockService.delStockCountCache(sid);
            // 完成扣库存下单事务
            count = stockService.createWrongOrder(sid);
            // 延时指定时间后再次删除缓存
            threadPool.execut(new DelCacheByThread(sid));
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }
```

> 删除缓存重试机制

上文提到了，要解决删除失败的问题，需要用到消息队列，进行删除操作的重试。这里我们为了达到效果，接入了RabbitMq，并且需要在接口中写发送消息，并且需要消费者常驻来消费消息。

- RabbitMqConfig

```java
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-20 15:38
 **/
@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue delCacheQueue(){
        return new Queue("delCache");
    }
}
```

添加一个消费者：

- DelCacheReceiver

```java
@Component
@RabbitListener(queues = "delCache")
public class DelCacheReceiver {
    private static final Logger LOGGER= LoggerFactory.getLogger(DelCacheReceiver.class);

    @Resource
    private StockService stockService;

    @RabbitHandler
    public void process(String message){
        LOGGER.info("DelCacheReceiver收到消息: " + message);
        LOGGER.info("DelCacheReceiver开始删除缓存: " + message);
        stockService.delStockCountCache(Integer.parseInt(message));
    }
}
```

- OrderController

```java
    @Resource
    private RabbitTemplate rabbitTemplate;
    /**
     * 下单接口：先更新数据库，再删缓存，删除缓存重试机制
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV4/{sid}")
    @ResponseBody
    public String createOrderWithCacheV4(@PathVariable int sid) {
        int count;
        try {
            // 完成扣库存下单事务
            count = stockService.createWrongOrder(sid);
            // 删除库存缓存
            stockService.delStockCountCache(sid);
            // 延时指定时间后再次删除缓存
            // threadPool.execut(new DelCacheByThread(sid));
            // 假设上述再次删除缓存没成功，通知消息队列进行删除缓存
            sendToDelCache(String.valueOf(sid));
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }
    private void sendToDelCache(String message){
        LOGGER.info("这就去通知消息队列开始重试删除缓存：[{}]", message);
        rabbitTemplate.convertAndSend("delCache",message);
    }
```

访问createOrderWithCacheV4：

![image-20230720160847724](https://zcandyyj.oss-cn-hangzhou.aliyuncs.com/typora/images/image-20230720160847724.png)

可以看到，我们先完成了下单，然后删除了缓存，并且假设延迟删除缓存失败了，发送给消息队列重试的消息，消息队列收到消息后再去删除缓存。

> 读取binlog异步删除





## 6、如何优雅的实现订单异步处理





