package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysDictData;
import vip.mseapp.service.system.entity.SysDictType;
import vip.mseapp.service.system.mapper.SysDictDataMapper;
import vip.mseapp.service.system.mapper.SysDictTypeMapper;
import vip.mseapp.service.system.service.SysDictTypeService;
import org.springframework.util.StringUtils;

public class SysDictTypeServiceImpl extends CrudRepository<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    private final SysDictDataMapper dictDataMapper;

    public SysDictTypeServiceImpl(SysDictDataMapper dictDataMapper) {
        this.dictDataMapper = dictDataMapper;
    }

    @Override
    public PageResult<SysDictType> pageDictTypes(long pageNum, long pageSize, String dictName, String dictType, Integer status) {
        Page<SysDictType> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysDictType>()
                        .like(StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
                        .like(StringUtils.hasText(dictType), SysDictType::getDictType, dictType)
                        .eq(status != null, SysDictType::getStatus, status)
                        .orderByDesc(SysDictType::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public Long createDictType(SysDictType dictType) {
        checkTypeUnique(dictType.getDictType(), null);
        dictType.setId(null);
        save(dictType);
        return dictType.getId();
    }

    @Override
    public void updateDictType(SysDictType dictType) {
        SysDictType db = getById(dictType.getId());
        if (db == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        // 类型标识变更需同步数据行的 dict_type（数据按类型字符串归组）
        String newType = StringUtils.hasText(dictType.getDictType()) ? dictType.getDictType() : db.getDictType();
        checkTypeUnique(newType, dictType.getId());
        if (!newType.equals(db.getDictType())) {
            SysDictData update = new SysDictData();
            update.setDictType(newType);
            dictDataMapper.update(update, new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, db.getDictType()));
        }
        dictType.setDictType(newType);
        updateById(dictType);
    }

    @Override
    public void deleteDictType(Long id) {
        SysDictType dictType = getById(id);
        if (dictType == null) {
            return;
        }
        Long dataCount = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType.getDictType()));
        if (dataCount != null && dataCount > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_DICT_TYPE_HAS_DATA);
        }
        removeById(id);
    }

    private void checkTypeUnique(String dictType, Long excludeId) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }
        Long count = lambdaQuery().eq(SysDictType::getDictType, dictType).ne(excludeId != null, SysDictType::getId, excludeId).count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_DICT_TYPE_DUPLICATE);
        }
    }
}
