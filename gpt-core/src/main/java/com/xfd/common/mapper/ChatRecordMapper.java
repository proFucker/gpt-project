package com.xfd.common.mapper;

import com.xfd.common.dao.ChatRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatRecordMapper {
    List<ChatRecord> selectCurrentChat(String wxId, String contextKey);

    boolean batchSaveChatRecord(List<ChatRecord> chatRecords);
}
