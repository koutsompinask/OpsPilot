package com.opspilot.tenant.repository;

import com.opspilot.tenant.domain.entity.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link com.opspilot.tenant.domain.entity.Tenant} entities.
 *
 * <p>Provides standard CRUD operations keyed by the tenant's UUID. The tenant ID is always
 * supplied externally (assigned by auth-service during bootstrap) rather than generated
 * by the database, so {@code existsById} is used to guard against duplicate bootstrap calls.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
