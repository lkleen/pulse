package com.zutubi.pulse.core.scm.p4;

import com.zutubi.pulse.core.scm.api.*;
import static com.zutubi.pulse.core.scm.p4.PerforceConstants.*;
import com.zutubi.util.config.Config;
import com.zutubi.util.config.ConfigSupport;

/**
 */
public class PerforceWorkingCopy extends PersonalBuildUIAwareSupport implements WorkingCopy
{
    // Note that the preferred way to set these standard Perforce properties is
    // to just use regular p4 configuration (e.g. environment, P4CONFIG, etc).
    // They are supported by this implementation for completeness and testing.
    public static final String PROPERTY_CLIENT = "p4.client";
    public static final String PROPERTY_PORT   = "p4.port";
    public static final String PROPERTY_USER   = "p4.user";

    // Pulse-specific perforce configuration properties.
    public static final String PROPERTY_CONFIRM_RESOLVE = "p4.confirm.resolve";
    public static final String PROPERTY_PRE_2004_2 = "p4.pre.2004.2";

    public boolean matchesLocation(WorkingCopyContext context, String location) throws ScmException
    {
        // Location is <template workspace>@<port>
        String[] pieces = location.split("@");

        // $ p4 set
        // P4EDITOR=C:\WINDOWS\System32\notepad.exe (set)
        // P4JOURNAL=journal (set -s)
        // P4LOG=log (set -s)
        // P4PORT=10.0.0.3:1666
        // P4ROOT=C:\Program Files\Perforce (set -s)
        // P4USER=Jason (set)
        PerforceCore core = createCore(context);
        PerforceCore.P4Result result = core.runP4(null, getP4Command(COMMAND_SET), COMMAND_SET);
        String[] lines = core.splitLines(result);
        for(String line: lines)
        {
            int index = line.indexOf('=');
            if(index > 0 && index < line.length() - 1)
            {
                String key = line.substring(0, index);
                if(key.equals(ENV_PORT))
                {
                    String value = line.substring(index + 1);
                    value = value.split(" ")[0];

                    if(!value.equals(pieces[1]))
                    {
                        getUI().warning("P4PORT setting '" + value + "' does not match Pulse project's P4PORT '" + pieces[1] + "'");
                        return false;
                    }
                }
            }
        }

        // TODO: check the client mapping?  This is difficult...many false positives methinks
        
        return true;
    }

    public WorkingCopyStatus getLocalStatus(WorkingCopyContext context, String... spec) throws ScmException
    {
        PerforceCore core = createCore(context);
        WorkingCopyStatus status = new WorkingCopyStatus(core.getClientRoot());
        PerforceFStatHandler handler = new PerforceFStatHandler(getUI(), status);

        ConfigSupport configSupport = new ConfigSupport(context.getConfig());
        boolean pre2004_2 = configSupport.getBooleanProperty(PROPERTY_PRE_2004_2, false);

        // Spec can be either a changelist # or a list of files
        String changelist;
        if(spec.length == 1 && spec[0].startsWith(":"))
        {
            if (pre2004_2)
            {
                throw new ScmException("Unable to specify a changelist with configuration property '" + PROPERTY_PRE_2004_2 + "' set to true");
            }

            // It's a changelist
            changelist = spec[0].substring(1);
            if(changelist.length() == 0)
            {
                throw new ScmException("Empty changelist name specified (" + spec[0] + ")");
            }

            core.runP4WithHandler(handler, null, getP4Command(COMMAND_FSTAT), COMMAND_FSTAT, FLAG_PATH_IN_DEPOT_FORMAT, FLAG_FILES_OPENED, FLAG_AFFECTED_CHANGELIST, changelist, "//...");
        }
        else if(spec.length > 0)
        {
            // Then it is a list of files
            String[] commands = new String[spec.length + 4];
            commands[0] = getP4Command(COMMAND_FSTAT);
            commands[1] = COMMAND_FSTAT;
            commands[2] = FLAG_PATH_IN_DEPOT_FORMAT;
            commands[3] = FLAG_FILES_OPENED;
            System.arraycopy(spec, 0, commands, 4, spec.length);

            core.runP4WithHandler(handler, null, commands);
        }
        else
        {
            // Emulate submit behaviour: default changelist
            if (pre2004_2)
            {
                core.runP4WithHandler(handler, null, getP4Command(COMMAND_FSTAT), COMMAND_FSTAT, FLAG_PATH_IN_DEPOT_FORMAT, FLAG_FILES_OPENED, "//...");
            }
            else
            {
                core.runP4WithHandler(handler, null, getP4Command(COMMAND_FSTAT), COMMAND_FSTAT, FLAG_PATH_IN_DEPOT_FORMAT, FLAG_FILES_OPENED, FLAG_AFFECTED_CHANGELIST, "default", "//...");
            }
        }

        return status;
    }

    public Revision update(WorkingCopyContext context, Revision revision) throws ScmException
    {
        PerforceCore core = createCore(context);
        revision = revision == null ? core.getLatestRevisionForFiles(null) : revision;

        PerforceSyncHandler syncHandler = new PerforceSyncHandler(getUI());
        core.runP4WithHandler(syncHandler, null, getP4Command(COMMAND_SYNC), COMMAND_SYNC, "@" + revision.getRevisionString());

        if(syncHandler.isResolveRequired())
        {
            ConfigSupport configSupport = new ConfigSupport(context.getConfig());
            if(configSupport.getBooleanProperty(PROPERTY_CONFIRM_RESOLVE, true))
            {
                PersonalBuildUI.Response response = getUI().ynaPrompt("Some files must be resolved.  Auto-resolve now?", PersonalBuildUI.Response.YES);
                if(response.isPersistent())
                {
                    configSupport.setBooleanProperty(PROPERTY_CONFIRM_RESOLVE, !response.isAffirmative());
                }

                if(!response.isAffirmative())
                {
                    return revision;
                }
            }

            getUI().status("Running auto-resolve...");
            getUI().enterContext();
            try
            {
                core.runP4WithHandler(new PerforceProgressPrintingHandler(getUI(), false), null, getP4Command(COMMAND_RESOLVE), COMMAND_RESOLVE, FLAG_AUTO_MERGE);
            }
            finally
            {
                getUI().exitContext();
            }
            getUI().status("Resolve complete.");
        }

        return revision;
    }

    private PerforceCore createCore(WorkingCopyContext context)
    {
        PerforceCore core = new PerforceCore();
        Config config = context.getConfig();
        transferPropertyIfSet(config, core, PROPERTY_CLIENT, ENV_CLIENT);
        transferPropertyIfSet(config, core, PROPERTY_PASSWORD, ENV_PASSWORD);
        transferPropertyIfSet(config, core, PROPERTY_PORT, ENV_PORT);
        transferPropertyIfSet(config, core, PROPERTY_USER, ENV_USER);
        return core;
    }

    private void transferPropertyIfSet(Config config, PerforceCore core, String property, String environmentVariable)
    {
        core.setEnv(environmentVariable, config.getProperty(property));
    }
}