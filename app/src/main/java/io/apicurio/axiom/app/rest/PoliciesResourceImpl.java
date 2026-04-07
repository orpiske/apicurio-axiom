package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.api.PoliciesResource;
import io.apicurio.axiom.api.beans.NewPolicy;
import io.apicurio.axiom.api.beans.Policy;
import io.apicurio.axiom.core.entities.PolicyEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.util.List;

/**
 * Implementation of the Policies REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class PoliciesResourceImpl implements PoliciesResource {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Policy> listPolicies() {
        return PolicyEntity.<PolicyEntity>listAll()
                .stream()
                .map(this::toBean)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Policy createPolicy(NewPolicy data) {
        PolicyEntity entity = new PolicyEntity();
        applyFields(entity, data);
        entity.persist();
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Policy getPolicy(long policyId) {
        return toBean(findOrThrow(policyId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Policy updatePolicy(long policyId, NewPolicy data) {
        PolicyEntity entity = findOrThrow(policyId);
        applyFields(entity, data);
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deletePolicy(long policyId) {
        PolicyEntity entity = findOrThrow(policyId);
        entity.delete();
    }

    private void applyFields(PolicyEntity entity, NewPolicy data) {
        entity.name = data.getName();
        entity.guideline = data.getGuideline();
        entity.actionType = data.getActionType();
        entity.actorHint = data.getActorHint();
    }

    private PolicyEntity findOrThrow(long id) {
        PolicyEntity entity = PolicyEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Policy not found: " + id, 404);
        }
        return entity;
    }

    private Policy toBean(PolicyEntity entity) {
        Policy policy = new Policy();
        policy.setId(entity.id);
        policy.setName(entity.name);
        policy.setGuideline(entity.guideline);
        policy.setActionType(entity.actionType);
        policy.setActorHint(entity.actorHint);
        return policy;
    }
}
