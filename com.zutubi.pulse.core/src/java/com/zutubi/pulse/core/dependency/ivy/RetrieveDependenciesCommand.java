package com.zutubi.pulse.core.dependency.ivy;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.commands.api.Command;
import com.zutubi.pulse.core.commands.api.CommandContext;
import com.zutubi.pulse.core.engine.api.BuildException;
import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import com.zutubi.util.NullaryFunctionE;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

import java.net.URL;

/**
 * A command that handles retrieving the dependencies for a build.  This
 * should run after the scm bootstrapping, but before the build.
 */
public class RetrieveDependenciesCommand  implements Command
{
    private IvyClient ivy;

    public RetrieveDependenciesCommand(RetrieveDependenciesCommandConfiguration config)
    {
        this.ivy = config.getIvy();
    }

    public void execute(CommandContext commandContext)
    {
        try
        {
            final PulseExecutionContext context = (PulseExecutionContext) commandContext.getExecutionContext();

            URL masterUrl = new URL(context.getString(NAMESPACE_INTERNAL, PROPERTY_MASTER_URL));
            String host = masterUrl.getHost();

            AuthenticatedAction.execute(host, context.getSecurityHash(), new NullaryFunctionE<Object, Exception>()
            {
                public Object process() throws Exception
                {
                    ModuleDescriptor descriptor = context.getValue(NAMESPACE_INTERNAL, PROPERTY_DEPENDENCY_DESCRIPTOR, ModuleDescriptor.class);
                    String retrievalPattern = context.resolveReferences(context.getString(NAMESPACE_INTERNAL, PROPERTY_RETRIEVAL_PATTERN));

                    if (!ivy.isResolved(descriptor.getModuleRevisionId()))
                    {
                        ivy.resolve(descriptor);
                    }
                    ivy.retrieve(descriptor.getModuleRevisionId(), context.getWorkingDir().getAbsolutePath() + "/" + retrievalPattern);
                    return null;
                }
            });
        }
        catch (Exception e)
        {
            throw new BuildException("Error running dependency retrieval: " + e.getMessage(), e);
        }
    }

    public void terminate()
    {
        
    }
}