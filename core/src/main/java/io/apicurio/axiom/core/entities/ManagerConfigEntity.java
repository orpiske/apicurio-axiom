package io.apicurio.axiom.core.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Stores the Manager's configurable system prompt and prompt template.
 * This is a single-row table — there is only one Manager configuration.
 */
@Entity
@Table(name = "manager_config")
public class ManagerConfigEntity extends PanacheEntity {

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    public String systemPrompt;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    public String promptTemplate;
}
