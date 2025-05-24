package com.codingtracker.service;

import com.codingtracker.dto.UserStatsDTO;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.ProblemResult;
import com.codingtracker.model.User;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserTryProblemService {

    private final UserTryProblemRepository repository;
    private final UserRepository userRepository; // 你需要注入UserRepository来查用户名

    public UserTryProblemService(UserTryProblemRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<UserStatsDTO> getTryCounts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawList = repository.countTryByUserAndPlatformBetween(start, end);
        return convertRawListToDTOWithName(rawList);
    }

    public List<UserStatsDTO> getAcCounts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawList = repository.countAcByUserAndPlatformBetween(start, end, ProblemResult.AC);
        return convertRawListToDTOWithName(rawList);
    }

    private List<UserStatsDTO> convertRawListToDTOWithName(List<Object[]> rawList) {
        Map<Integer, UserStatsDTO> map = new LinkedHashMap<>();

        // 收集所有userId
        Set<Integer> userIds = new HashSet<>();
        for (Object[] row : rawList) {
            Number userIdNum = (Number) row[0];
            int userId = userIdNum.intValue();
            userIds.add(userId);
        }

        // 批量查用户信息
        Iterable<User> users = userRepository.findAllById(userIds);
        Map<Integer, String> userIdToName = new HashMap<>();
        Map<Integer, String> userIdToRealName = new HashMap<>();
        for (User user : users) {
            userIdToName.put(user.getId(), user.getUsername());
            userIdToRealName.put(user.getId(), user.getRealName());  // 真实姓名
        }

        // 构造 DTO
        for (Object[] row : rawList) {
            Number userIdNum = (Number) row[0];
            Integer userId = userIdNum.intValue();
            OJPlatform platform = (OJPlatform) row[1];
            Long count = (Long) row[2];

            String username = userIdToName.getOrDefault(userId, "未知用户");
            String realName = userIdToRealName.getOrDefault(userId, "未知姓名");

            UserStatsDTO userStats = map.computeIfAbsent(userId, id -> new UserStatsDTO(userId, username, realName));
            userStats.addCount(platform, count);
        }

        return new ArrayList<>(map.values());
    }
}

