package com.zutubi.pulse;

import com.zutubi.pulse.hessian.CustomHessianProxyFactory;
import com.zutubi.pulse.model.Slave;
import com.zutubi.pulse.services.SlaveService;
import com.zutubi.pulse.services.UncontactableSlaveService;

import java.net.MalformedURLException;
import java.net.URL;

/**
 */
public class SlaveProxyFactory
{
    private CustomHessianProxyFactory hessianProxyFactory;

    public SlaveService createProxy(Slave slave)
    {
        try
        {
            return unsafeCreateProxy(slave);
        }
        catch (MalformedURLException e)
        {
            return new UncontactableSlaveService(e.getMessage());
        }
    }

    public SlaveService unsafeCreateProxy(Slave slave) throws MalformedURLException
    {
        URL url = new URL("http", slave.getHost(), slave.getPort(), "/hessian");
        return (SlaveService) hessianProxyFactory.create(SlaveService.class, url.toString());
    }

    public void setHessianProxyFactory(CustomHessianProxyFactory hessianProxyFactory)
    {
        this.hessianProxyFactory = hessianProxyFactory;
    }
}