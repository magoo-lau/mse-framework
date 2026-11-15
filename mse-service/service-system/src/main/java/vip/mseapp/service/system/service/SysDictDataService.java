package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysDictData;

import java.util.List;

/**
 * 字典数据管理（租户隔离，按 dict_type 归组）。
 */
public interface SysDictDataService extends IRepository<SysDictData> {

    PageResult<SysDictData> pageDictData(long pageNum, long pageSize, String dictType, String dictLabel, Integer status);

    /** 业务下拉用：某类型下全部启用字典数据（仅登录即可，无权限码要求） */
    List<SysDictData> listEnabledByType(String dictType);

    Long createDictData(SysDictData dictData);

    void updateDictData(SysDictData dictData);

    void deleteDictData(Long id);
}
