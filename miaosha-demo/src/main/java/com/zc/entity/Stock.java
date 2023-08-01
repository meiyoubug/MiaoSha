package com.zc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
@Data
  @EqualsAndHashCode(callSuper = false)
    public class Stock implements Serializable {

    private static final long serialVersionUID=1L;

      @TableId(value = "id", type = IdType.AUTO)
      private Integer id;

      /**
     * 名称
     */
      private String name;

      /**
     * 库存
     */
      private Integer count;

      /**
     * 已售
     */
      private Integer sale;

      /**
     * 乐观锁，版本号
     */
      private Integer version;

}
