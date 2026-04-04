package com.opspilot.tenant.repository;

import com.opspilot.tenant.domain.entity.UserProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link com.opspilot.tenant.domain.entity.UserProfile} entities.
 *
 * <p>Extends standard CRUD with a derived query that enforces tenant isolation: every
 * multi-row read is scoped to a single tenant by filtering on the {@code tenant_id} foreign key.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Returns all user profiles whose owning tenant matches the given ID.
     *
     * <p>Spring Data translates the method name into a {@code WHERE tenant_id = ?} clause,
     * ensuring that callers only ever receive profiles from their own tenant.
     *
     * @param tenantId the tenant to filter by
     * @return all profiles belonging to that tenant, or an empty list if none exist
     */
    List<UserProfile> findAllByTenant_Id(UUID tenantId);

    /**
     * Returns a page of user profiles for the given tenant; sort and page size are controlled
     * by the caller via {@code pageable}.
     *
     * @param tenantId the tenant to filter by
     * @param pageable pagination and sort parameters
     * @return a page of user profiles
     */
    Page<UserProfile> findAllByTenant_Id(UUID tenantId, Pageable pageable);
}
