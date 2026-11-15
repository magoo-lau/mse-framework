package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysPost;
import vip.mseapp.service.system.entity.SysUserPost;
import vip.mseapp.service.system.mapper.SysPostMapper;
import vip.mseapp.service.system.mapper.SysUserPostMapper;
import vip.mseapp.service.system.service.SysPostService;
import org.springframework.util.StringUtils;

import java.util.List;

public class SysPostServiceImpl extends CrudRepository<SysPostMapper, SysPost> implements SysPostService {

    private final SysUserPostMapper userPostMapper;

    public SysPostServiceImpl(SysUserPostMapper userPostMapper) {
        this.userPostMapper = userPostMapper;
    }

    @Override
    public PageResult<SysPost> pagePosts(long pageNum, long pageSize, String postCode, String postName, Integer status) {
        Page<SysPost> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysPost>()
                        .like(StringUtils.hasText(postCode), SysPost::getPostCode, postCode)
                        .like(StringUtils.hasText(postName), SysPost::getPostName, postName)
                        .eq(status != null, SysPost::getStatus, status)
                        .orderByAsc(SysPost::getPostSort));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public List<SysPost> listEnabled() {
        return list(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, 0)
                .orderByAsc(SysPost::getPostSort));
    }

    @Override
    public Long createPost(SysPost post) {
        checkCodeUnique(post.getPostCode(), null);
        post.setId(null);
        save(post);
        return post.getId();
    }

    @Override
    public void updatePost(SysPost post) {
        checkCodeUnique(post.getPostCode(), post.getId());
        updateById(post);
    }

    @Override
    public void deletePost(Long id) {
        Long users = userPostMapper.selectCount(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getPostId, id));
        if (users != null && users > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "岗位已分配用户，无法删除");
        }
        removeById(id);
    }

    private void checkCodeUnique(String postCode, Long excludeId) {
        if (!StringUtils.hasText(postCode)) {
            return;
        }
        Long count = lambdaQuery().eq(SysPost::getPostCode, postCode).ne(excludeId != null, SysPost::getId, excludeId).count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_POST_CODE_DUPLICATE);
        }
    }
}
