package com.codingtracker.service;

import com.codingtracker.model.CFUserInfo;
import com.codingtracker.repository.CFUserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CFUserInfoService {

    private final CFUserInfoRepository cfUserInfoRepository;

    @Autowired
    public CFUserInfoService(CFUserInfoRepository cfUserInfoRepository) {
        this.cfUserInfoRepository = cfUserInfoRepository;
    }

    // 保存CFUserInfo
    public void saveCFUserInfo(CFUserInfo cfUserInfo) {
        if (cfUserInfo != null) {
            cfUserInfoRepository.save(cfUserInfo);
        }
    }

    // 根据用户名获取用户信息
    public CFUserInfo getCFUserInfoByUsername(String username) {
        return cfUserInfoRepository.findById(username).orElse(null);
    }

    // 删除指定用户名的用户信息
    public void deleteCFUserInfoByUsername(String username) {
        cfUserInfoRepository.deleteById(username);
    }
}
