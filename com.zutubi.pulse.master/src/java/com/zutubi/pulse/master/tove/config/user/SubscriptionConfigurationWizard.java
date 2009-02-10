package com.zutubi.pulse.master.tove.config.user;

import com.zutubi.pulse.master.tove.wizard.AbstractChainableState;
import com.zutubi.pulse.master.tove.wizard.AbstractTypeWizard;
import com.zutubi.pulse.master.tove.wizard.TypeWizardState;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.Type;
import com.zutubi.tove.type.record.MutableRecord;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;

import java.util.List;

/**
 *
 */
public class SubscriptionConfigurationWizard extends AbstractTypeWizard
{
    private CompositeType subscriptionType;
    private CompositeType projectSubscriptionType;
    private CompositeType conditionType;

    public void initialise()
    {
        subscriptionType = typeRegistry.getType(SubscriptionConfiguration.class);
        projectSubscriptionType = typeRegistry.getType(ProjectSubscriptionConfiguration.class);
        conditionType = typeRegistry.getType(SubscriptionConditionConfiguration.class);

        List<AbstractChainableState> states = addWizardStates(null, parentPath, subscriptionType, null);
        addWizardStates(CollectionUtils.filter(states, new Predicate<AbstractChainableState>()
        {
            public boolean satisfied(AbstractChainableState state)
            {
                return state.getType().equals(projectSubscriptionType);
            }
        }), null, conditionType, null);
    }

    public void doFinish()
    {
        super.doFinish();

        MutableRecord record = getCompletedStateForType(subscriptionType).getDataRecord();
        TypeWizardState conditionState = getCompletedStateForType(conditionType);
        if (conditionState != null)
        {
            record.put("condition", conditionState.getDataRecord());
        }

        successPath = configurationTemplateManager.insertRecord(insertPath, record);
    }

    public Type getType()
    {
        return subscriptionType;
    }
}