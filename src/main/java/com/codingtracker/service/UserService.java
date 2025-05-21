package com.codingtracker.service;

import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.UserOJ;
import com.codingtracker.repository.UserOJRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codingtracker.model.User;
import com.codingtracker.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository; // 用于与数据库交互的 Repository

    private final UserOJRepository userOJRepository;

    private final BCryptPasswordEncoder passwordEncoder; // bean注入


    // 构造函数注入方式
    @Autowired
    public UserService(UserRepository userRepository,
                       UserOJRepository userOJRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userOJRepository = userOJRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户注册
     * 
     * @param user 注册的用户信息
     * @return     如果注册成功，返回 true；否则返回 false
     */
    public boolean registerUser(User user) {
        // 检查是否已有相同用户名
        if (userRepository.existsByUsername(user.getUsername())) {
            return false; // 如果用户名已存在，返回 false
        }

        // 对密码进行加密
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        // 设置其他用户信息（如默认类型等）
        user.getRoles().add(User.Type.NEW);

        // 保存用户到数据库
        userRepository.save(user);
        return true; // 注册成功
    }

    /**
     * 用户登录验证
     * 
     * @param username 用户名
     * @param password 用户密码
     * @return         如果验证成功，返回用户信息；否则返回 null
     */
    public User valid(String username, String password) {
        // 从数据库中获取用户
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user; // 密码匹配，返回用户
            }
        }

        return null; // 验证失败
    }

    /**
     * 修改用户信息
     * 
     * @param user 修改后的用户信息
     */
    @Transactional
    public void modifyUser(User user) {
        // 根据 ID 查找用户并更新信息
        User existingUser = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        // 更新字段（可根据实际需求选择更新字段）
        existingUser.setRealName(user.getRealName());
        existingUser.setMajor(user.getMajor());
        
        userRepository.save(existingUser); // 保存更新的用户信息
    }

    /**
     * 修改用户密码
     *
     * @param userId  用户ID
     * @param password 新密码
     * @return        更新后的用户信息
     */
    @Transactional
    public User modifyUserPassword(Integer userId, String password) {
        // 根据 ID 查找用户
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // 加密新密码
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword); // 更新密码

        userRepository.save(user); // 保存用户信息

        return user; // 返回更新后的用户信息
    }

    /**
     * 获取所有用户信息（不包括管理员）
     * 
     * @return 用户列表
     */
    public List<User> allUser() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isAdmin()) // 过滤掉管理员
                .collect(Collectors.toList());
    }

    /**
     * 判断是否已存在某用户名
     * 
     * @param username 用户名
     * @return         如果用户名已存在，返回 true；否则返回 false
     */
    public boolean hasUser(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 根据真实姓名查找用户
     * 
     * @param realName 真实姓名
     * @return          查找结果，返回用户信息，如果不存在则返回空
     */
    public Optional<User> findByRealName(String realName) {
        return userRepository.findByRealName(realName); // 根据真实姓名查找用户
    }

    /**
     * 根据角色查找用户
     * 
     * @param role 用户角色
     * @return     符合角色的用户列表
     */
    public List<User> findByRole(User.Type role) {
        return userRepository.findByRolesContains(role);  // 查找指定角色的用户
    }


    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    /**
     * 为用户添加 OJ 账号
     *
     * @param username 用户名
     * @param platform 平台
     * @param accountName 用户名
     * @return 更新后的用户信息（包括新增的 OJ 账号）
     */
    @Transactional
    public boolean addOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);  // 根据用户名查找用户
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 创建新的 OJ 账号实体
            UserOJ ojAccount = new UserOJ();
            ojAccount.setUser(user);  // 设置 OJ 账号的所属用户
            ojAccount.setPlatform(OJPlatform.fromName(platform));
            ojAccount.setAccountName(accountName);  // 设置 OJ 账号名称

            // 将新创建的 OJ 账号添加到用户的 OJ 账户列表中
            user.getOjAccounts().add(ojAccount);

            // 保存更新后的用户实体和新增的 OJ 账号
            userRepository.save(user);  // 保存用户（OJ 账号会级联保存）

            return true;  // 添加成功，返回 true
        }
        return false;  // 如果用户不存在，返回 false
    }

    /**
     * 获取指定用户名的所有 OJ 账号
     *
     * @param username 用户名
     * @return 用户的 OJ 账号列表
     */
    public List<UserOJ> getOJAccounts(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);  // 根据用户名查找用户
        return userOptional.map(User::getOjAccounts).orElse(null);  // 如果用户存在，返回其 OJ 账号列表
    }

    /**
     * 删除指定用户的某个 OJ 账号
     * 通过 OJ 平台、账号名称和用户名来查找 OJ 账号并删除
     *
     * @param username 用户名
     * @param platform OJ 平台
     * @param accountName OJ 账号名称
     * @return 删除是否成功
     */
    @Transactional
    public boolean deleteOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);  // 根据用户名查找用户
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String ojName = OJPlatform.fromName(platform).toString();
            List<UserOJ> ojAccounts = user.getOjAccounts();
            for (UserOJ ojAccount : ojAccounts) {
                // 根据 OJ 平台和账号名称匹配
                if (ojAccount.getPlatform().toString().equals(ojName) && ojAccount.getAccountName().equals(accountName)) {
                    ojAccounts.remove(ojAccount);  // 从用户的 OJ 账号列表中移除该 OJ 账号
                    userOJRepository.delete(ojAccount);  // 从数据库中删除该 OJ 账号
                    userRepository.save(user);  // 保存更新后的用户实体
                    return true;  // 删除成功，返回 true
                }
            }
        }
        return false;  // 如果没有找到符合条件的 OJ 账号，返回 false
    }

    /**
     * 根据用户名获取所有 OJ 账号
     *
     * @param username 用户名
     * @return 用户的所有 OJ 账号列表
     */
    public List<UserOJ> getOJAccountsByUsername(String username) {
        return userOJRepository.findByUserUsername(username);  // 查找所有与用户名关联的 OJ 账号
    }
}
