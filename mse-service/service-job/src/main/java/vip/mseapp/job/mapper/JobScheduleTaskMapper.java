package vip.mseapp.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mseapp.job.entity.JobScheduleTask;

/** 调度任务登记 mapper（mse-job 派发通道：只读清单 + 置 trigger_pending） */
@Mapper
public interface JobScheduleTaskMapper extends BaseMapper<JobScheduleTask> {
}
