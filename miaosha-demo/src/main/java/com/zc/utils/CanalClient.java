package com.zc.utils;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-20 21:18
 **/
@Component
public class CanalClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(CanalClient.class);
    @Resource
    private CanalConnector connector;


    /**
     * 通过canal删除Redis缓存
     */
    public synchronized void  delCacheByCanal(String schemaName, String tableName) {
        boolean isDeleted = false;
        //连接canal
        connector.connect();
        //订阅canal
        connector.subscribe();
        // 每次读取 1000 条
        Message message = connector.getWithoutAck(1000);
        long batchID = message.getId();
        int size = message.getEntries().size();
        if (batchID == -1 || size == 0) {
            return;
        } else {
            //处理日志
            dealEntrys(message.getEntries(), schemaName, tableName);
        }

        connector.ack(batchID);
    }

    /**
     * 处理日志
     *
     * @param entrys 条目
     */
    private void dealEntrys(List<CanalEntry.Entry> entrys, String schemaName, String tableName) {
        boolean isDeleted = false;
        AtomicInteger id = new AtomicInteger();
        CanalEntry.Header header = null;
        CanalEntry.EntryType entryType = null;
        CanalEntry.RowChange rowChange = null;
        for (CanalEntry.Entry entry : entrys) {
            header = entry.getHeader();
            entryType = entry.getEntryType();
            if (entryType == CanalEntry.EntryType.ROWDATA) {
                //获取表名
                String table = header.getTableName();
                //获取数据库名
                String schema = header.getSchemaName();
                //符合我们传入的数据库和表名
                if (schemaName.equals(schema) && tableName.equals(table)) {
                    try {
                        rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //获取事件类型 增删改查
                    CanalEntry.EventType eventType = rowChange.getEventType();

                    //追踪到columns级别
                    rowChange.getRowDatasList().forEach(rowData -> {
                        // 获取更新之后的 column 情况
                        List<CanalEntry.Column> afterColumns = rowData.getAfterColumnsList();
                        // 执行的是更新操作
                        if (eventType == CanalEntry.EventType.UPDATE) {
                            // 删除缓存
                            afterColumns.forEach(column -> {
                                String columnName = column.getName();
                                String columnValue = column.getValue();
                                if ("id".equals(columnName)) {
                                    id.set(Integer.parseInt(columnValue));
                                }
                                //删除缓存
                                String hashKey = CacheKey.GoodsKey.getKey() + "_" + id;
                                stringRedisTemplate.delete(hashKey);
                            });

                        }
                    });
                }
            }
        }
    }

}
