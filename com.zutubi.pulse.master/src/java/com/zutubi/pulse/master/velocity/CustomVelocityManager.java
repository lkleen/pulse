package com.zutubi.pulse.master.velocity;

import com.opensymphony.webwork.views.velocity.VelocityManager;
import com.opensymphony.xwork.util.OgnlValueStack;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.Version;
import com.zutubi.pulse.core.spring.SpringComponentContext;
import com.zutubi.pulse.master.agent.AgentManager;
import com.zutubi.pulse.master.license.License;
import com.zutubi.pulse.master.license.LicenseHolder;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.model.UserManager;
import com.zutubi.pulse.master.security.AcegiUtils;
import com.zutubi.pulse.master.tove.config.admin.GlobalConfiguration;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.pulse.servercore.events.system.SystemStartedListener;
import com.zutubi.tove.config.ConfigurationProvider;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
public class CustomVelocityManager extends VelocityManager
{
    private ProjectManager projectManager;
    private AgentManager agentManager;
    private UserManager userManager;
    private ConfigurationProvider configurationProvider;
    private boolean systemStarted = false;

    public CustomVelocityManager()
    {
        SpringComponentContext.autowire(this);
    }

    public Context createContext(OgnlValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        Context context = super.createContext(stack, req, res);
        context.put("urls", new Urls((String) context.get("base")));
        
        // add version strings.
        Version v = Version.getVersion();
        context.put("version_number", v.getVersionNumber());
        context.put("build_date", v.getBuildDate());
        context.put("build_number", v.getBuildNumber());

        if(systemStarted)
        {
            GlobalConfiguration config = configurationProvider.get(GlobalConfiguration.class);
            if (config != null)
            {
                context.put("helpUrl", config.getBaseHelpUrl());
                context.put("rssEnabled", config.isRssEnabled());
                context.put("config", config);
            }

            String login = AcegiUtils.getLoggedInUsername();
            if (login != null)
            {
                User user = userManager.getUser(login);
                context.put("principle", user);
                context.put("canLogout", AcegiUtils.canLogout());
            }

            License license = LicenseHolder.getLicense();
            if (license != null)
            {
                context.put("license", license);
                context.put("licenseExceeded", license.isExceeded(projectManager.getProjectCount(), agentManager.getAgentCount(), userManager.getUserCount()));
                context.put("licenseCanRunVersion", license.canRunVersion(v));
            }
        }

        return context;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
        systemStarted = true;
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(new SystemStartedListener()
        {
            public void systemStarted()
            {
                SpringComponentContext.autowire(CustomVelocityManager.this);
            }
        });
    }
}