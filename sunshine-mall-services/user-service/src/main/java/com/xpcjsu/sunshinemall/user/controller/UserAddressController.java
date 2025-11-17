package com.xpcjsu.sunshinemall.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import com.xpcjsu.sunshinemall.user.dto.UserAddressDTO;
import com.xpcjsu.sunshinemall.user.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService service;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserAddressDTO dto) {
        Long userId = UserContext.getUser();
        dto.setUserId(userId);
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserAddressDTO dto) {
        Long userId = UserContext.getUser();
        dto.setId(id);
        dto.setUserId(userId);
        return service.update(dto) ? Result.success(null) : Result.failure("UPDATE_FAILED", "更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUser();
        return service.delete(id, userId) ? Result.success(null) : Result.failure("DELETE_FAILED", "删除失败");
    }

    @GetMapping("/{id}")
    public Result<UserAddressDTO> get(@PathVariable Long id) {
        Long userId = UserContext.getUser();
        return Result.success(service.getById(id, userId));
    }

    @GetMapping
    public Result<List<UserAddressDTO>> list() {
        Long userId = UserContext.getUser();
        return Result.success(service.listByUser(userId));
    }

    @GetMapping("/page")
    public Result<Page<UserAddressDTO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContext.getUser();
        return Result.success(service.pageByUser(userId, pageNum, pageSize));
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = UserContext.getUser();
        return service.setDefault(id, userId) ? Result.success(null) : Result.failure("UPDATE_FAILED", "更新失败");
    }
}