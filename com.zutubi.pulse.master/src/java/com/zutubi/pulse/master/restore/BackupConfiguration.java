package com.zutubi.pulse.master.restore;

import com.zutubi.tove.annotations.ControllingCheckbox;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.validation.annotations.Constraint;
import com.zutubi.validation.annotations.Required;

/**
 * Settings for automatic backups.  By default backups are enabled and run
 * daily at 5am.
 */
@SymbolicName("zutubi.backupConfig")
@Form(fieldOrder = {"enabled", "cronSchedule"})
public class BackupConfiguration extends AbstractConfiguration
{
    public static final String DEFAULT_CRON_SCHEDULE = "0 0 5 * * ?";

    @Required
    @Constraint("com.zutubi.pulse.master.tove.config.project.triggers.CronExpressionValidator")
    private String cronSchedule = DEFAULT_CRON_SCHEDULE;

    @ControllingCheckbox(dependentFields = {"cronSchedule"})
    private boolean enabled = true;

    public BackupConfiguration()
    {
        setPermanent(true);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getCronSchedule()
    {
        return cronSchedule;
    }

    public void setCronSchedule(String cronSchedule)
    {
        this.cronSchedule = cronSchedule;
    }
}