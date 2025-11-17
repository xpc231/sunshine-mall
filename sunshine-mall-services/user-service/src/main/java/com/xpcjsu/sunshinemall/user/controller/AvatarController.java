package com.xpcjsu.sunshinemall.user.controller;

import com.aliyun.oss.OSS;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import com.xpcjsu.sunshinemall.user.config.OssProperties;
import com.xpcjsu.sunshinemall.user.entity.User;
import com.xpcjsu.sunshinemall.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/avatar")
@RequiredArgsConstructor
public class AvatarController {
    private final OSS oss;
    private final OssProperties props;
    private final UserMapper userMapper;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        Long userId = UserContext.getUser();
        if (file == null || file.isEmpty()) {
            return Result.failure("FILE_EMPTY", "文件为空");
        }
        String ext = getExt(file.getOriginalFilename());
        String key = "user/avatar/" + userId + "/" + LocalDate.now().toString().replace("-", "") + "/" + UUID.randomUUID() + (ext == null ? "" : ("." + ext));
        try (InputStream in = file.getInputStream()) {
            oss.putObject(props.getBucket(), key, in);
        }
        String url = "https://" + props.getBucket() + "." + props.getEndpoint().replace("http://", "").replace("https://", "") + "/" + key;
        User u = userMapper.selectById(userId);
        u.setAvatar(url);
        userMapper.updateById(u);
        return Result.success(url, "上传成功");
    }

    private String getExt(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1) : null;
    }
}