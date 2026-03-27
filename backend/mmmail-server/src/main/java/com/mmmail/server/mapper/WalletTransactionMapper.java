package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
