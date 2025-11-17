package com.xpcjsu.sunshinemall.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.user.dto.UserAddressDTO;

import java.util.List;

public interface UserAddressService {
    Long create(UserAddressDTO dto);
    boolean update(UserAddressDTO dto);
    boolean delete(Long id, Long userId);
    UserAddressDTO getById(Long id, Long userId);
    List<UserAddressDTO> listByUser(Long userId);
    Page<UserAddressDTO> pageByUser(Long userId, int pageNum, int pageSize);
    boolean setDefault(Long id, Long userId);
}