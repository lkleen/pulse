package com.zutubi.pulse.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Internal command used to stop when running under Java Service Wrapper.
 */
public class StopServiceCommand extends AdminCommand
{
    public String getHelp()
    {
        // Internal command
        return null;
    }

    public String getDetailedHelp()
    {
        return null;
    }

    public List<String> getUsages()
    {
        return null;
    }

    public List<String> getAliases()
    {
        return null;
    }

    public Map<String, String> getOptions()
    {
        return null;
    }

    public boolean isDefault()
    {
        return false;
    }

    public int doExecute(BootContext context) throws XmlRpcException, IOException, ParseException
    {
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine = parser.parse(getSharedOptions(), context.getCommandArgv(), true);
        super.processSharedOptions(commandLine);
        xmlRpcClient.execute("RemoteApi.stopService", new Vector<Object>(Arrays.asList(new Object[]{ adminToken })));
        return 0;
    }
}
