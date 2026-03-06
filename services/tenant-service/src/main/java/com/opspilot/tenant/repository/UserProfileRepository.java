package com.opspilot.tenant.repository;

import com.opspilot.tenant.entity.UserProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    List<UserProfile> findAllByTenant_Id(UUID tenantId);
}
