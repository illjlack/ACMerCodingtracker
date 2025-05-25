package com.codingtracker.repository;

import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.UserTryProblemOptimized;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserTryProblemOptimizedRepository extends JpaRepository<UserTryProblemOptimized, Long> {

    // 删除所有冗余表数据
    @Transactional
    void deleteAll();

    // 保存冗余表数据
    @Transactional
    <S extends UserTryProblemOptimized> S save(S entity);

    Page<UserTryProblemOptimized> findByUsername(String username, Pageable pageable);
}
