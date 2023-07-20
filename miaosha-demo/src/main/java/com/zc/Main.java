package com.zc;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: Default (Template) Project
 * @description: ${description}
 * @author: ZC
 * @create: 2023-07-14 18:08
 **/
public class Main {
    public static void main(String[] args) {
        AutoGenerator autoGenerator = new AutoGenerator();

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDbType(DbType.MYSQL);
        dataSourceConfig.setDriverName("com.mysql.cj.jdbc.Driver");
        dataSourceConfig.setUsername("root");
        dataSourceConfig.setPassword("86739043");
        dataSourceConfig.setUrl("jdbc:mysql://localhost:3306/m4a_miaosha?characterEncoding=utf-8");
        autoGenerator.setDataSource(dataSourceConfig);
        GlobalConfig globalConfig = new GlobalConfig();
        //生成文件是否在磁盘中打开
        globalConfig.setOpen(false);
        //生成代码的路径
        globalConfig.setOutputDir(System.getProperty("user.dir") + "/src/main/java");
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
//        strategyConfig.setInclude("t_user");
        autoGenerator.setStrategy(strategyConfig);
        autoGenerator.execute();
    }
}
