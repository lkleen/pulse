package com.zutubi.pulse.core;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.validation.Validateable;
import com.zutubi.validation.ValidationContext;
import com.zutubi.validation.annotations.Required;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * 
 *
 */
public class CommandGroup extends CommandSupport implements Validateable
{
    private Command command = null;
    private List<Artifact> artifacts = new LinkedList<Artifact>();

    public void add(Command cmd) throws FileLoadException
    {
        if (this.command != null)
        {
            throw new FileLoadException("A 'command' tag may only contain a single nested command.");
        }
        this.command = cmd;

        if (!TextUtils.stringSet(this.command.getName()))
        {
            this.command.setName(getName());
        }
    }

    public boolean isForce()
    {
        return super.isForce() || command.isForce();
    }

    public void terminate()
    {
        command.terminate();
    }

    public FileArtifact createArtifact()
    {
        FileArtifact customArtifact = new FileArtifact();
        artifacts.add(customArtifact);
        return customArtifact;
    }

    public DirectoryArtifact createDirArtifact()
    {
        DirectoryArtifact customArtifact = new DirectoryArtifact();
        artifacts.add(customArtifact);
        return customArtifact;
    }

    public LinkArtifact createLinkArtifact()
    {
        LinkArtifact linkArtifact = new LinkArtifact();
        artifacts.add(linkArtifact);
        return linkArtifact;
    }

    public void execute(CommandContext context, CommandResult result)
    {
        command.execute(context, result);
        for(Artifact artifact: artifacts)
        {
            artifact.capture(result, context);
        }
    }

    // For testing
    List<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public List<String> getArtifactNames()
    {
        List<String> names = new LinkedList<String>();
        for (Artifact artifact : artifacts)
        {
            names.add(artifact.getName());
        }

        // dont forget about our nested friend. he may have some artifacts as well.
        names.addAll(getCommand().getArtifactNames());

        return names;
    }

    @Required public Command getCommand()
    {
        return command;
    }

    public void validate(ValidationContext context)
    {
        // ensure that our artifacts have unique names.
        List<String> artifactNames = getArtifactNames();
        Set<String> names = new TreeSet<String>();
        for (String name : artifactNames)
        {
            if (names.contains(name))
            {
                context.addFieldError("name", "A duplicate artifact name '" + name + "' has been detected. Please only " +
                        "use unique names for artifacts within a command group.");
            }
            names.add(name);
        }
    }
}