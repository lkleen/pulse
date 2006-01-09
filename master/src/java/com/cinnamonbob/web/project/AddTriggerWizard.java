package com.cinnamonbob.web.project;

import com.cinnamonbob.model.Project;
import com.cinnamonbob.model.ProjectManager;
import com.cinnamonbob.scheduling.*;
import com.cinnamonbob.scheduling.tasks.BuildProjectTask;
import com.cinnamonbob.scm.SCMChangeEvent;
import com.cinnamonbob.util.logging.Logger;
import com.cinnamonbob.web.wizard.BaseWizard;
import com.cinnamonbob.web.wizard.BaseWizardState;
import com.cinnamonbob.web.wizard.Wizard;
import com.opensymphony.util.TextUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * <class-comment/>
 */
public class AddTriggerWizard extends BaseWizard
{
    private static final String MONITOR_STATE = "monitor";
    private static final String CRON_STATE = "cron";

    private static final Logger LOG = Logger.getLogger(AddTriggerWizard.class);

    private ProjectManager projectManager;
    private Scheduler scheduler;

    private SelectTriggerType selectState;
    private ConfigureCronTrigger configCron;
    private ConfigureMonitorTrigger configMonitor;

    public AddTriggerWizard()
    {
        selectState = new SelectTriggerType(this, "select");
        configCron = new ConfigureCronTrigger(this, "cron");
        configMonitor = new ConfigureMonitorTrigger(this, "monitor");

        setCurrentState(selectState);

        addState(selectState);
        addState(configCron);
        addState(configMonitor);
    }

    public long getProject()
    {
        return selectState.getProject();
    }

    public void process()
    {
        // wizard is finished, now we create the appropriate trigger.

        Project project = projectManager.getProject(getProject());

        Trigger trigger = null;
        if (CRON_STATE.equals(selectState.getType()))
        {
            trigger = new CronTrigger(configCron.cron, configCron.name);
            trigger.getDataMap().put(BuildProjectTask.PARAM_SPEC, configCron.spec);
        }
        else if (MONITOR_STATE.equals(selectState.getType()))
        {
            trigger = new EventTrigger(SCMChangeEvent.class, configMonitor.name);
            trigger.getDataMap().put(BuildProjectTask.PARAM_SPEC, configMonitor.spec);
        }

        trigger.setProject(project.getId());
        trigger.setTaskClass(BuildProjectTask.class);
        trigger.getDataMap().put(BuildProjectTask.PARAM_PROJECT, project.getId());

        try
        {
            scheduler.schedule(trigger);
        }
        catch (SchedulingException e)
        {
            LOG.severe(e.getMessage(), e);
        }
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public class SelectTriggerType extends BaseWizardState
    {
        private Map<String, String> types;

        private long project;

        private String type;

        public SelectTriggerType(Wizard wizard, String name)
        {
            super(wizard, name);
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public long getProject()
        {
            return project;
        }

        public void setProject(long project)
        {
            this.project = project;
        }

        public Map<String, String> getTypes()
        {
            if (types == null)
            {
                types = new TreeMap<String, String>();
                types.put(MONITOR_STATE, "monitor scm trigger");
                types.put(CRON_STATE, "cron trigger");
            }
            return types;
        }

        public void validate()
        {
            if (!TextUtils.stringSet(type) || !types.containsKey(type))
            {
                addFieldError("type", "Invalid type '" + type + "' specified. ");
            }
        }

        public String getNextState()
        {
            if (TextUtils.stringSet(type))
            {
                return type;
            }
            return super.getStateName();
        }
    }

    public class ConfigureCronTrigger extends BaseWizardState
    {
        private String name;
        private String spec;
        private String cron;

        public ConfigureCronTrigger(Wizard wizard, String stateName)
        {
            super(wizard, stateName);
        }

        public String getNextState()
        {
            return null;
        }

        public String getCron()
        {
            return cron;
        }

        public void setCron(String cron)
        {
            this.cron = cron;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getSpec()
        {
            return spec;
        }

        public void setSpec(String spec)
        {
            this.spec = spec;
        }
    }

    public class ConfigureMonitorTrigger extends BaseWizardState
    {
        private String name;
        private String spec;

        public ConfigureMonitorTrigger(Wizard wizard, String stateName)
        {
            super(wizard, stateName);
        }

        public String getNextState()
        {
            return null;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public void setSpec(String spec)
        {
            this.spec = spec;
        }

        public String getSpec()
        {
            return spec;
        }
    }

}