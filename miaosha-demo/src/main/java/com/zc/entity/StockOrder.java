package com.zc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
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
    public class StockOrder implements Serializable {

    private static final long serialVersionUID=1L;

      @TableId(value = "id", type = IdType.AUTO)
      private Integer id;

      /**
     * 库存ID
     */
      private Integer sid;

      /**
     * 商品名称
     */
      private String name;

    private Integer userId;

      /**
     * 创建时间
     */
      private LocalDateTime createTime;


}
