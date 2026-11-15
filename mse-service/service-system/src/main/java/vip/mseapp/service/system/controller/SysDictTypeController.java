package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysDictType;
import vip.mseapp.service.system.service.SysDictTypeService;
import vip.mseapp.web.log.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典类型管理（仅租户端）：/system/dict/type/**；字典数据见 SysDictDataController。
 */
@Validated
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController {

    private final SysDictTypeService dictTypeService;

    public SysDictTypeController(SysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @SaCheckPermission(value = "system:dict:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysDictType>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String dictName,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(dictTypeService.pageDictTypes(pageNum, pageSize, dictName, dictType, status));
    }

    @OperLog(module = "system:dict", type = "新增")
    @SaCheckPermission(value = "system:dict:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated DictTypeSaveRequest request) {
        SysDictType dictType = new SysDictType();
        dictType.setDictName(request.dictName());
        dictType.setDictType(request.dictType());
        dictType.setStatus(request.status() == null ? 0 : request.status());
        dictType.setRemark(request.remark());
        return Result.ok(dictTypeService.createDictType(dictType));
    }

    @OperLog(module = "system:dict", type = "修改")
    @SaCheckPermission(value = "system:dict:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated DictTypeUpdateRequest request) {
        SysDictType dictType = new SysDictType();
        dictType.setId(request.id());
        dictType.setDictName(request.dictName());
        dictType.setDictType(request.dictType());
        dictType.setStatus(request.status());
        dictType.setRemark(request.remark());
        dictTypeService.updateDictType(dictType);
        return Result.ok();
    }

    @OperLog(module = "system:dict", type = "删除")
    @SaCheckPermission(value = "system:dict:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictTypeService.deleteDictType(id);
        return Result.ok();
    }

    // -------------------- 请求体 --------------------

    public record DictTypeSaveRequest(
            @NotBlank(message = "字典名称不能为空") String dictName,
            @NotBlank(message = "字典类型不能为空") String dictType,
            Integer status,
            String remark) {
    }

    public record DictTypeUpdateRequest(
            @NotNull(message = "字典编号不能为空") Long id,
            @NotBlank(message = "字典名称不能为空") String dictName,
            @NotBlank(message = "字典类型不能为空") String dictType,
            Integer status,
            String remark) {
    }
}
