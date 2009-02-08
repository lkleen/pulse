package com.zutubi.pulse.slave.api;

import com.zutubi.pulse.api.AuthenticationException;
import com.zutubi.pulse.api.AdminTokenManager;
import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.ShutdownManager;
import com.zutubi.pulse.Version;

/**
 */
public class RemoteApi
{
    private AdminTokenManager tokenManager;
    private ShutdownManager shutdownManager;

    public RemoteApi()
    {
        ComponentContext.autowire(this);
    }

    public boolean shutdown(String token, boolean force, boolean exitJvm) throws AuthenticationException
    {
        // Sigh ... this is tricky, because if we shutdown here Jetty dies
        // before this request is complete and the client gets an error :-|.
        checkToken(token);
        shutdownManager.delayedShutdown(force, exitJvm);
        return true;
    }

    public boolean stopService(String token) throws AuthenticationException
    {
        checkToken(token);
        shutdownManager.delayedStop();
        return true;
    }

    public int getBuildNumber(String token) throws AuthenticationException
    {
        checkToken(token);
        return Version.getVersion().getBuildNumberAsInt();
    }

    private void checkToken(String token) throws AuthenticationException
    {
        if(!tokenManager.checkAdminToken(token))
        {
            throw new AuthenticationException("Invalid token");
        }

    }
    public void setTokenManager(AdminTokenManager tokenManager)
    {
        this.tokenManager = tokenManager;
    }

    public void setShutdownManager(ShutdownManager shutdownManager)
    {
        this.shutdownManager = shutdownManager;
    }
}