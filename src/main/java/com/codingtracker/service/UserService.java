package com.codingtracker.service;

import com.codingtracker.dto.UserInfoDTO;
import com.codingtracker.init.SystemStatsLoader;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.UserOJ;
import com.codingtracker.repository.UserOJRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codingtracker.model.User;
import com.codingtracker.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserOJRepository userOJRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;
    private final SystemStatsLoader statsLoader;  // 统计加载器

    @Autowired
    public UserService(UserRepository userRepository,
                       UserOJRepository userOJRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       AvatarStorageService avatarStorageService,
                       SystemStatsLoader statsLoader) {
        this.userRepository = userRepository;
        this.userOJRepository = userOJRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
        this.statsLoader = statsLoader;
    }

    /**
     * 用户注册
     */
    public boolean registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return false;
        }
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        user.getRoles().add(User.Type.NEW);
        userRepository.save(user);

        updateUserCountStat();
        return true;
    }

    /**
     * 用户登录验证
     */
    public User valid(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * 修改用户信息
     */
    @Transactional
    public void modifyUser(UserInfoDTO userInfoDTO) {
        User existingUser = userRepository.findByUsername(userInfoDTO.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userInfoDTO.getRealName() != null) {
            existingUser.setRealName(userInfoDTO.getRealName());
        }
        if (userInfoDTO.getMajor() != null) {
            existingUser.setMajor(userInfoDTO.getMajor());
        }
        if (userInfoDTO.getEmail() != null) {
            existingUser.setEmail(userInfoDTO.getEmail());
        }
        if (userInfoDTO.getAvatar() != null) {
            existingUser.setAvatar(userInfoDTO.getAvatar());
        }

        Map<String, String> ojAccountsMap = userInfoDTO.getOjAccounts();
        if (ojAccountsMap != null) {
            List<UserOJ> existingOJList = existingUser.getOjAccounts();

            Map<String, Set<String>> existingMap = new HashMap<>();
            for (UserOJ oj : existingOJList) {
                existingMap.computeIfAbsent(oj.getPlatform().name(), k -> new HashSet<>())
                        .add(oj.getAccountName());
            }

            Map<String, Set<String>> incomingMap = new HashMap<>();
            for (Map.Entry<String, String> entry : ojAccountsMap.entrySet()) {
                String platformName = entry.getKey();
                String accountsStr = entry.getValue();
                if (accountsStr == null || accountsStr.trim().isEmpty()) continue;

                Set<String> acctSet = Arrays.stream(accountsStr.split("[；;]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                incomingMap.put(platformName, acctSet);
            }

            Iterator<UserOJ> it = existingOJList.iterator();
            while (it.hasNext()) {
                UserOJ oj = it.next();
                String platform = oj.getPlatform().name();
                String acct = oj.getAccountName();
                Set<String> incomingAccts = incomingMap.get(platform);
                if (incomingAccts == null || !incomingAccts.contains(acct)) {
                    it.remove();
                    userOJRepository.delete(oj);
                }
            }

            for (Map.Entry<String, Set<String>> entry : incomingMap.entrySet()) {
                String platformName = entry.getKey();
                Set<String> accounts = entry.getValue();

                Set<String> existAccts = existingMap.getOrDefault(platformName, Collections.emptySet());
                for (String acctName : accounts) {
                    if (!existAccts.contains(acctName)) {
                        UserOJ userOJ = new UserOJ();
                        userOJ.setPlatform(OJPlatform.valueOf(platformName));
                        userOJ.setAccountName(acctName);
                        userOJ.setUser(existingUser);
                        existingOJList.add(userOJ);
                    }
                }
            }
        }

        userRepository.save(existingUser);
    }

    /**
     * 保存用户头像文件
     */
    public boolean storeAvatar(String username, MultipartFile file) {
        try {
            String avatarUrl = avatarStorageService.store(file);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 修改用户密码
     */
    @Transactional
    public User modifyUserPassword(Integer userId, String password) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        userRepository.save(user);
        return user;
    }

    /**
     * 获取所有用户（不包括管理员）
     */
    public List<User> allUser() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isAdmin())
                .collect(Collectors.toList());
    }

    /**
     * 判断用户名是否存在
     */
    public boolean hasUser(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 根据真实姓名查找用户
     */
    public Optional<User> findByRealName(String realName) {
        return userRepository.findByRealName(realName);
    }

    /**
     * 根据角色查找用户
     */
    public List<User> findByRole(User.Type role) {
        return userRepository.findByRolesContains(role);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 添加 OJ 账号
     */
    @Transactional
    public boolean addOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            UserOJ ojAccount = new UserOJ();
            ojAccount.setUser(user);
            ojAccount.setPlatform(OJPlatform.fromName(platform));
            ojAccount.setAccountName(accountName);

            user.getOjAccounts().add(ojAccount);
            userRepository.save(user);

            return true;
        }
        return false;
    }

    /**
     * 获取指定用户名的所有 OJ 账号
     */
    public List<UserOJ> getOJAccounts(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        return userOptional.map(User::getOjAccounts).orElse(null);
    }

    /**
     * 删除指定用户的 OJ 账号
     */
    @Transactional
    public boolean deleteOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String ojName = OJPlatform.fromName(platform).toString();
            List<UserOJ> ojAccounts = user.getOjAccounts();
            for (UserOJ ojAccount : ojAccounts) {
                if (ojAccount.getPlatform().toString().equals(ojName) && ojAccount.getAccountName().equals(accountName)) {
                    ojAccounts.remove(ojAccount);
                    userOJRepository.delete(ojAccount);
                    userRepository.save(user);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询所有用户
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 根据 ID 查找用户
     */
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * 创建新用户
     */
    public User createUser(User u) {
        User savedUser = userRepository.save(u);

        updateUserCountStat();

        return savedUser;
    }

    /**
     * 更新已有用户
     */
    public User updateUser(User u) {
        return userRepository.save(u);
    }

    /**
     * 判断用户是否存在（根据 ID）
     */
    public boolean existsById(Integer id) {
        return userRepository.existsById(id);
    }

    /**
     * 删除用户
     */
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * 根据用户名获取所有 OJ 账号
     */
    public List<UserOJ> getOJAccountsByUsername(String username) {
        return userOJRepository.findByUserUsername(username);
    }

    /**
     * 更新统计文件中的用户总数等数据
     */
    private void updateUserCountStat() {
        long userCount = userRepository.count();
        long problemCount = statsLoader.getSumProblemCount();
        long tryCount = statsLoader.getSumTryCount();
        statsLoader.updateStats(userCount, problemCount, tryCount);
    }
}
