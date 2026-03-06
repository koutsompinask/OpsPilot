package com.opspilot.tenant.repository;

import com.opspilot.tenant.entity.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
