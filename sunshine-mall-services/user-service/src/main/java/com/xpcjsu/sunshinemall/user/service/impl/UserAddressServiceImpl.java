package com.xpcjsu.sunshinemall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.user.dto.UserAddressDTO;
import com.xpcjsu.sunshinemall.user.entity.UserAddress;
import com.xpcjsu.sunshinemall.user.mapper.UserAddressMapper;
import com.xpcjsu.sunshinemall.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserAddressDTO dto) {
        UserAddress entity = new UserAddress();
        BeanUtils.copyProperties(dto, entity);
        entity.setDeleted(0);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAddress> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            uw.eq(UserAddress::getUserId, entity.getUserId()).set(UserAddress::getIsDefault, 0);
            mapper.update(null, uw);
        }
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UserAddressDTO dto) {
        if (dto.getId() == null || dto.getUserId() == null) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数错误");
        }
        UserAddress old = mapper.selectById(dto.getId());
        if (old == null || !old.getUserId().equals(dto.getUserId())) {
            throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "无权限");
        }
        UserAddress entity = new UserAddress();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAddress> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            uw.eq(UserAddress::getUserId, entity.getUserId()).ne(UserAddress::getId, entity.getId()).set(UserAddress::getIsDefault, 0);
            mapper.update(null, uw);
        }
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id, Long userId) {
        UserAddress old = mapper.selectById(id);
        if (old == null || !old.getUserId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "无权限");
        }
        return mapper.deleteById(id) > 0;
    }

    @Override
    public UserAddressDTO getById(Long id, Long userId) {
        UserAddress entity = mapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "无权限");
        }
        UserAddressDTO dto = new UserAddressDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    @Override
    public List<UserAddressDTO> listByUser(Long userId) {
        List<UserAddress> list = mapper.selectList(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        return list.stream().map(e -> {
            UserAddressDTO dto = new UserAddressDTO();
            BeanUtils.copyProperties(e, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<UserAddressDTO> pageByUser(Long userId, int pageNum, int pageSize) {
        Page<UserAddress> page = mapper.selectPage(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        Page<UserAddressDTO> dtoPage = new Page<>(pageNum, pageSize, page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(e -> {
            UserAddressDTO dto = new UserAddressDTO();
            BeanUtils.copyProperties(e, dto);
            return dto;
        }).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id, Long userId) {
        UserAddress entity = mapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "无权限");
        }
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAddress> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(UserAddress::getUserId, userId).set(UserAddress::getIsDefault, 0);
        mapper.update(null, uw);
        entity.setIsDefault(1);
        return mapper.updateById(entity) > 0;
    }
}