package com.ubs.wma.aat.rampuppack.domain;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain entity mapped to the {@code ramp_up_pack} table, modelled as an immutable Java record.
 *
 * <p>Spring Data R2DBC instantiates and "mutates" records by rebuilding them, so a {@code null}
 * {@link #id()} marks the entity as new — {@code save} performs an INSERT (the database assigns the
 * identity) versus an UPDATE. The {@code createdAt}/{@code updatedAt} timestamps are managed by
 * Spring Data auditing ({@link CreatedDate}/{@link LastModifiedDate}; enabled in {@code R2dbcConfig}).
 */
@Table("ramp_up_pack")
public record RampUpPack(
        @Id Long id,
        @Column("name") String name,
        @Column("description") String description,
        @Column("status") RampUpPackStatus status,
        @Column("created_at") @CreatedDate Instant createdAt,
        @Column("updated_at") @LastModifiedDate Instant updatedAt) {

    /** A new, transient pack: no id and no timestamps (assigned on persist). */
    public static RampUpPack newPack(String name, String description, RampUpPackStatus status) {
        return new RampUpPack(null, name, description, status, null, null);
    }

    /** A copy with the editable fields replaced, preserving id and audit timestamps. */
    public RampUpPack withChanges(String name, String description, RampUpPackStatus status) {
        return new RampUpPack(this.id, name, description, status, this.createdAt, this.updatedAt);
    }
}
