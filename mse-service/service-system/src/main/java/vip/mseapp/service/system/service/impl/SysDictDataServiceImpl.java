package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysDictData;
import vip.mseapp.service.system.entity.SysDictType;
import vip.mseapp.service.system.mapper.SysDictDataMapper;
import vip.mseapp.service.system.mapper.SysDictTypeMapper;
import vip.mseapp.service.system.service.SysDictDataService;
import org.springframework.util.StringUtils;

import java.util.List;

public class SysDictDataServiceImpl extends CrudRepository<SysDictDataMapper, SysDictData> implements SysDictDataService {

    private final SysDictTypeMapper dictTypeMapper;

    public SysDictDataServiceImpl(SysDictTypeMapper dictTypeMapper) {
        this.dictTypeMapper = dictTypeMapper;
    }

    @Override
    public PageResult<SysDictData> pageDictData(long pageNum, long pageSize, String dictType, String dictLabel, Integer status) {
        Page<SysDictData> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysDictData>()
                        .eq(StringUtils.hasText(dictType), SysDictData::getDictType, dictType)
                        .like(StringUtils.hasText(dictLabel), SysDictData::getDictLabel, dictLabel)
                        .eq(status != null, SysDictData::getStatus, status)
                        .orderByAsc(SysDictData::getDictSort));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    /**
     * 按类型取启用字典项（业务表单下拉数据源）：本租户行 → 本库默认模板行（tenant_id=0 同库副本，
     * 与 sys_config 生效值语义一致）。两端独立单世界生效值：独立库模式模板行整库可见已随首查返回；
     * 共享模式模板行被租户插件条件挡住，回退查本库 0 行补齐基础枚举（正常/停用等种子模板），
     * 均查不到返回空（租户未定义且本库无模板）。平台会话（tenant 0）即平台库自身行，无回退概念。
     */
    @Override
    public List<SysDictData> listEnabledByType(String dictType) {
        // 类型停用 = 该类型下拉整体不可用（数据行启停只管单行，类型状态在此处统一门控；
        // 类型行同样遵循 本租户行 → 模板行 的生效值口径）
        SysDictType type = dictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType));
        if (type == null && TenantContext.getRequiredTenantId() != MseConstants.PLATFORM_TENANT_ID) {
            type = TenantContext.runIgnore(() -> dictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                    .eq(SysDictType::getDictType, dictType)
                    .eq(SysDictType::getTenantId, MseConstants.PLATFORM_TENANT_ID)));
        }
        if (type != null && type.getStatus() != null && type.getStatus() != 0) {
            return List.of();
        }
        List<SysDictData> own = list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 0)
                .orderByAsc(SysDictData::getDictSort));
        if (!own.isEmpty()) {
            return own;
        }
        if (TenantContext.getRequiredTenantId() == MseConstants.PLATFORM_TENANT_ID) {
            return own;
        }
        return TenantContext.runIgnore(() -> list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 0)
                .eq(SysDictData::getTenantId, MseConstants.PLATFORM_TENANT_ID)
                .orderByAsc(SysDictData::getDictSort)));
    }

    @Override
    public Long createDictData(SysDictData dictData) {
        dictData.setId(null);
        save(dictData);
        return dictData.getId();
    }

    @Override
    public void updateDictData(SysDictData dictData) {
        updateById(dictData);
    }

    @Override
    public void deleteDictData(Long id) {
        removeById(id);
    }
}
