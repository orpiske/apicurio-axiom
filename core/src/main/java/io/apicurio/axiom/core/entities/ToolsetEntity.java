package io.apicurio.axiom.core.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A named collection of tools that can be referenced in Allowed Tools
 * configurations using the {@code @ToolsetName} syntax. When resolved
 * at execution time, the toolset reference is expanded into its
 * constituent tool strings.
 */
@Entity
@Table(name = "toolset")
public class ToolsetEntity extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    /**
     * Comma-separated list of tool strings in this toolset.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    public String tools;
}
