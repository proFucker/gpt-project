package com.xfd.common.mapper;

import com.xfd.common.dao.ChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordMapper {
    List<ChatRecord> selectCurrentChat(@Param("userId") String userId, @Param("contextKey") String contextKey);

    boolean batchSaveChatRecord(List<ChatRecord> chatRecords);
}
