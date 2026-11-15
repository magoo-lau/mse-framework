package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import vip.mseapp.service.platform.entity.SysScheduleTaskLog;

import java.util.List;

@Mapper
public interface SysScheduleTaskLogMapper extends BaseMapper<SysScheduleTaskLog> {

    /** 每任务保留最近 N 条：删除时间早于第 201 新的记录（不足 201 条时子查询为空、不删） */
    int KEEP_RECENT = 200;

    @Select("SELECT id FROM sys_schedule_task_log WHERE log_key = #{logKey} LIMIT 1")
    Long selectIdByLogKey(@Param("logKey") String logKey);

    @Delete("""
            DELETE FROM sys_schedule_task_log WHERE task_key = #{taskKey} AND start_time < (
                SELECT t.start_time FROM (
                    SELECT start_time FROM sys_schedule_task_log WHERE task_key = #{taskKey}
                    ORDER BY start_time DESC LIMIT 1 OFFSET 200) t)
            """)
    int pruneKeepingRecent(@Param("taskKey") String taskKey);
}
