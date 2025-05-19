package com.codingtracker.security;

import com.codingtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;  // 你的用户数据访问层接口

    @Autowired
    private PasswordEncoder passwordEncoder;  // 用于密码加密的Bean

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库中获取用户信息
        Optional<com.codingtracker.model.User> userEntity = userRepository.findByUsername(username);

        if (userEntity.isEmpty()) {  // 使用 isEmpty() 方法，避免 null 判断
            throw new UsernameNotFoundException("用户不存在");
        }

        // 返回一个 UserDetails 实现类，这里使用 Spring Security 提供的 User 类
        com.codingtracker.model.User user = userEntity.get();
        return new User(
                user.getUsername(),
                user.getPassword(),  // 从数据库获取的密码
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))  // 将角色转换为 GrantedAuthority
                        .collect(Collectors.toList())  // 用户角色
        );
    }
}
