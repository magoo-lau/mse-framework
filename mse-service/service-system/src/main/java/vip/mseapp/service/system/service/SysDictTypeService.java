package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysDictType;

/**
 * 字典类型管理（租户隔离；字典数据操作见 {@link SysDictDataService}）。
 */
public interface SysDictTypeService extends IRepository<SysDictType> {

    PageResult<SysDictType> pageDictTypes(long pageNum, long pageSize, String dictName, String dictType, Integer status);

    Long createDictType(SysDictType dictType);

    void updateDictType(SysDictType dictType);

    /** 类型下存在数据时不可删除；删除类型不级联删数据（先清数据再删类型） */
    void deleteDictType(Long id);
}
