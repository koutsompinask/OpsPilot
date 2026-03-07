package com.opspilot.tenant.repository;

import com.opspilot.tenant.domain.entity.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
