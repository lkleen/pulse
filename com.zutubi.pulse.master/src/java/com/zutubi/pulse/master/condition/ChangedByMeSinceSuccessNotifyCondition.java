package com.zutubi.pulse.master.condition;

import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.model.PersistentChangelist;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.tove.config.user.UserConfiguration;
import com.zutubi.pulse.master.util.TransactionContext;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.NullaryFunction;

import java.util.List;

/**
 * A condition that is true if there have been changes between the last
 * successful build and this build.
 */
public class ChangedByMeSinceSuccessNotifyCondition implements NotifyCondition
{
    private BuildManager buildManager;
    private TransactionContext transactionContext;

    public ChangedByMeSinceSuccessNotifyCondition()
    {
    }

    public boolean satisfied(final BuildResult result, final UserConfiguration user)
    {
        if(result == null)
        {
            return false;
        }

        return transactionContext.executeInsideTransaction(new NullaryFunction<Boolean>()
        {
            public Boolean process()
            {
                // Check for direct changes on the build we have first.
                List<PersistentChangelist> changelists = buildManager.getChangesForBuild(result);
                ByMePredicate predicate = new ByMePredicate(user);
                if (CollectionUtils.contains(changelists, predicate))
                {
                    return true;
                }

                // OK, look back to the last success, and test all builds after
                // it but before our result.
                BuildResult previousSuccess = buildManager.getPreviousBuildResultWithRevision(result, new ResultState[]{ResultState.SUCCESS});
                long lowestNumber = previousSuccess == null ? 1 : previousSuccess.getNumber() + 1;

                List<BuildResult> resultRange = buildManager.queryBuilds(result.getProject(), ResultState.getCompletedStates(), lowestNumber, result.getNumber() - 1, 0, -1, false, false);
                for (BuildResult r: resultRange)
                {
                    changelists  = buildManager.getChangesForBuild(r);
                    if (CollectionUtils.contains(changelists, predicate))
                    {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setTransactionContext(TransactionContext transactionContext)
    {
        this.transactionContext = transactionContext;
    }
}