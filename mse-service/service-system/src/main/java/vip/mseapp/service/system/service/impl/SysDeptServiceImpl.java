package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysDept;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.mapper.SysDeptMapper;
import vip.mseapp.service.system.mapper.SysUserMapper;
import vip.mseapp.service.system.service.SysDeptService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SysDeptServiceImpl extends CrudRepository<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper userMapper;

    public SysDeptServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<DeptTreeNode> getDeptTree() {
        List<SysDept> all = list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 0)
                .orderByAsc(SysDept::getSort));
        // 负责人姓名批量映射（列表展示用，查无此人/未设置则无键）
        List<Long> leaderIds = all.stream().map(SysDept::getLeaderUserId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> leaderNames = new java.util.HashMap<>();
        if (!leaderIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(leaderIds)) {
                leaderNames.put(u.getId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
            }
        }
        return buildTree(all, leaderNames);
    }

    @Override
    public Long createDept(Long parentId, String name, Integer sort) {
        return createDept(parentId, name, sort, null);
    }

    @Override
    public Long createDept(Long parentId, String name, Integer sort, Long leaderUserId) {
        SysDept dept = new SysDept();
        dept.setParentId(parentId == null ? 0L : parentId);
        dept.setAncestors(buildAncestors(dept.getParentId()));
        dept.setName(name);
        dept.setSort(sort == null ? 0 : sort);
        dept.setStatus(0);
        dept.setLeaderUserId(checkLeader(leaderUserId));
        save(dept);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long id, Long parentId, String name, Integer sort, Integer status) {
        updateDept(id, parentId, name, sort, status, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long id, Long parentId, String name, Integer sort, Integer status, Long leaderUserId) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new ServiceException(1220, "部门不存在");
        }
        if (leaderUserId != null) {
            // null=不改动（兼容旧签名）；0=清空；其余=校验后设置
            dept.setLeaderUserId(leaderUserId == 0L ? null : checkLeader(leaderUserId));
        }
        Long newParentId = parentId == null ? dept.getParentId() : parentId;
        if (!dept.getParentId().equals(newParentId)) {
            checkNoCycle(id, newParentId);
            String oldPrefix = dept.getAncestors();
            String newPrefix = buildAncestors(newParentId);
            dept.setAncestors(newPrefix);
            // 级联刷新子树：子部门 ancestors 前缀从「旧链,本部门」替换为「新链,本部门」
            String oldSelfPrefix = oldPrefix + "," + id;
            String newSelfPrefix = newPrefix + "," + id;
            List<SysDept> descendants = new java.util.ArrayList<>();
            for (SysDept child : baseMapper.selectChildren(id)) {
                if (child.getAncestors() != null && child.getAncestors().startsWith(oldSelfPrefix)) {
                    child.setAncestors(newSelfPrefix + child.getAncestors().substring(oldSelfPrefix.length()));
                    descendants.add(child);
                }
            }
            if (!descendants.isEmpty()) {
                updateBatchById(descendants, descendants.size());
            }
        }
        dept.setParentId(newParentId);
        dept.setName(name);
        dept.setSort(sort == null ? dept.getSort() : sort);
        dept.setStatus(status == null ? dept.getStatus() : status);
        updateById(dept);
        if (leaderUserId != null && leaderUserId == 0L) {
            // 0=清空负责人：updateById 默认 NOT_NULL 策略忽略 null 字段，须显式 set null 落库
            lambdaUpdate().eq(SysDept::getId, id).set(SysDept::getLeaderUserId, null).update();
        }
    }

    @Override
    public void deleteDept(Long id) {
        Long children = lambdaQuery().eq(SysDept::getParentId, id).count();
        if (children != null && children > 0) {
            throw new ServiceException(1221, "存在子部门，无法删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users != null && users > 0) {
            throw new ServiceException(1222, "部门下存在用户，无法删除");
        }
        removeById(id);
    }

    /** 负责人校验：须为本域启用用户（0 启用 1 停用），返回规范化值（null 透传） */
    private Long checkLeader(Long leaderUserId) {
        if (leaderUserId == null || leaderUserId == 0L) {
            return null;
        }
        SysUser leader = userMapper.selectById(leaderUserId);
        if (leader == null || leader.getStatus() == null || leader.getStatus() != 0) {
            throw new ServiceException(1224, "部门负责人不存在或已停用");
        }
        return leaderUserId;
    }

    /** ancestors = 父部门的 ancestors 链 + 父部门编号（根部门为 "0"） */
    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            throw new ServiceException(1220, "父部门不存在");
        }
        return parent.getAncestors() + "," + parentId;
    }

    private void checkNoCycle(Long id, Long newParentId) {
        if (id.equals(newParentId)) {
            throw new ServiceException(1223, "父部门不能是自身");
        }
        SysDept parent = getById(newParentId);
        while (parent != null && StringUtils.hasText(parent.getAncestors())) {
            for (String ancestor : parent.getAncestors().split(",")) {
                if (String.valueOf(id).equals(ancestor)) {
                    throw new ServiceException(1223, "不能将部门的子部门设为父部门");
                }
            }
            parent = parent.getParentId() == null || parent.getParentId() == 0L
                    ? null : getById(parent.getParentId());
        }
    }

    private List<DeptTreeNode> buildTree(List<SysDept> depts, Map<Long, String> leaderNames) {
        Map<Long, List<SysDept>> byParent = depts.stream()
                .sorted(Comparator.comparing(SysDept::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        return byParent.getOrDefault(0L, List.of()).stream()
                .map(dept -> new DeptTreeNode(dept, buildChildren(dept.getId(), byParent, leaderNames),
                        leaderNames.get(dept.getLeaderUserId())))
                .toList();
    }

    private List<DeptTreeNode> buildChildren(Long parentId, Map<Long, List<SysDept>> byParent, Map<Long, String> leaderNames) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(dept -> new DeptTreeNode(dept, buildChildren(dept.getId(), byParent, leaderNames),
                        leaderNames.get(dept.getLeaderUserId())))
                .toList();
    }
}
