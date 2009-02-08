package com.zutubi.pulse.personal;

import com.zutubi.pulse.command.BootContext;
import com.zutubi.pulse.command.Command;
import com.zutubi.pulse.config.CommandLineConfig;
import com.zutubi.pulse.config.CompositeConfig;
import com.zutubi.pulse.config.PropertiesConfig;
import com.zutubi.pulse.scm.WorkingCopy;
import com.zutubi.pulse.scm.WorkingCopyStatus;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 */
@SuppressWarnings({ "AccessStaticViaInstance" })
public class PersonalBuildCommand implements Command
{
    private File base;
    private String[] files;
    private CommandLineConfig switchConfig;
    private PropertiesConfig defineConfig;
    private String patchFilename;
    private boolean noRequest = false;
    private boolean statusOnly = false;
    private ConsoleUI console = new ConsoleUI();

    public void processArguments(String... argv) throws ParseException
    {
        switchConfig = new CommandLineConfig();
        Options options = new Options();

        options.addOption(OptionBuilder.withLongOpt("quiet")
                .create('q'));
        options.addOption(OptionBuilder.withLongOpt("verbose")
                .create('v'));
        options.addOption(OptionBuilder.withLongOpt("status")
                .create('t'));
        options.addOption(OptionBuilder.withLongOpt("define")
                .hasArg()
                .create('d'));
        options.addOption(OptionBuilder.withLongOpt("file")
                .hasArg()
                .create('f'));
        options.addOption(OptionBuilder.withLongOpt("no-request")
                .create('n'));

        addPropertyOption(options, 's', "server", PersonalBuildConfig.PROPERTY_PULSE_URL);
        addPropertyOption(options, 'u', "user", PersonalBuildConfig.PROPERTY_PULSE_USER);
        addPropertyOption(options, 'p', "password", PersonalBuildConfig.PROPERTY_PULSE_PASSWORD);
        addPropertyOption(options, 'r', "project", PersonalBuildConfig.PROPERTY_PROJECT);
        addPropertyOption(options, 'b', "specification", PersonalBuildConfig.PROPERTY_SPECIFICATION);

        CommandLineParser parser = new PosixParser();

        CommandLine commandLine = parser.parse(options, argv, true);
        Properties defines = new Properties();

        if (commandLine.hasOption('d'))
        {
            addDefinedOption(defines, commandLine.getOptionValue('d'));
        }
        if (commandLine.hasOption('q'))
        {
            console.setVerbosity(PersonalBuildUI.Verbosity.QUIET);
        }
        if (commandLine.hasOption('v'))
        {
            console.setVerbosity(PersonalBuildUI.Verbosity.VERBOSE);
        }
        if (commandLine.hasOption('t'))
        {
            statusOnly = true;
        }
        if (commandLine.hasOption('n'))
        {
            noRequest = true;
        }
        if(commandLine.hasOption('f'))
        {
            patchFilename = commandLine.getOptionValue('f');
        }

        switchConfig.setCommandLine(commandLine);
        defineConfig = new PropertiesConfig(defines);
        base = new File(System.getProperty("user.dir"));
        files = commandLine.getArgs();
    }

    private void addDefinedOption(Properties defines, String value) throws ParseException
    {
        int index = value.indexOf('=');
        if (index <= 0 || index >= value.length() - 1)
        {
            throw new ParseException("Invalid property definition syntax '" + value + "' (expected name=value)");
        }

        String propertyName = value.substring(0, index);
        String propertyValue = value.substring(index + 1);

        defines.put(propertyName, propertyValue);
    }

    private void addPropertyOption(Options options, char shortOption, String longOption, String property)
    {
        options.addOption(OptionBuilder.withLongOpt(longOption)
                .hasArg()
                .create(shortOption));
        switchConfig.mapSwitch(Character.toString(shortOption), property);
    }

    private int execute(String[] argv) throws ParseException
    {
        processArguments(argv);

        CompositeConfig uiConfig = new CompositeConfig(switchConfig, defineConfig);
        PersonalBuildConfig config = new PersonalBuildConfig(base, uiConfig);
        PersonalBuildClient client = new PersonalBuildClient(config);
        client.setUI(console);

        try
        {
            WorkingCopy wc = client.checkConfiguration();

            if (statusOnly)
            {
                WorkingCopyStatus wcs = client.getStatus(wc, files);
                if(!wcs.hasChanges())
                {
                    console.status("No changes found.");
                }
            }
            else
            {
                File patchFile;

                if(patchFilename == null)
                {
                    try
                    {
                        patchFile = File.createTempFile("pulse.patch.", ".zip");
                        patchFile.deleteOnExit();
                    }
                    catch (IOException e)
                    {
                        console.error("Unable to create temporary patch file: " + e.getMessage(), e);
                        return 1;
                    }
                }
                else
                {
                    patchFile = new File(patchFilename);
                }

                PatchArchive patch = client.preparePatch(wc, patchFile, files);
                if(patch == null)
                {
                    console.status("No changes found.");
                }
                else if(!noRequest)
                {
                    client.sendRequest(patch);
                }
            }
        }
        catch (UserAbortException e)
        {
            return 2;
        }
        catch (PersonalBuildException e)
        {
            console.error(e.getMessage(), e);
            return 1;
        }

        return 0;
    }

    public int execute(BootContext context) throws ParseException
    {
        return execute(context.getCommandArgv());
    }

    public String getHelp()
    {
        return "request a personal build";
    }

    public String getDetailedHelp()
    {
        return "Sends a personal build request to a pulse server.  This involves updating\n" +
               "the current working copy, analysing any outstanding changes, forming a patch\n" +
               "file and sending the patch to the pulse server to execute a build.\n\n" +
               "Configuration is defined via properties files or command line arguments.  The\n" +
               "configuration specifies connection details for the pulse server, along with\n" +
               "information about the project and build specification you wish to execute.\n" +
               "The SCM configuration of the project must match the working copy.";
    }

    public List<String> getUsages()
    {
        return Arrays.asList(new String[] { "", "<file> ...", ":<changelist>" });
    }

    public List<String> getAliases()
    {
        return Arrays.asList(new String[] { "pe", "per", "pers" });
    }

    public Map<String, String> getOptions()
    {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("-r [--project] project", "set project to build");
        options.put("-b [--specification] spec", "set build specification to build");
        options.put("-s [--server] url", "set pulse server url");
        options.put("-u [--user] name", "set pulse user name");
        options.put("-p [--password] password", "set pulse password");
        options.put("-f [--file] filename", "set patch file name");
        options.put("-d [--define] name=value", "set named property to given value");
        options.put("-q [--quiet]", "suppress unnecessary output");
        options.put("-v [--verbose]", "show verbose output");
        options.put("-n [--no-request]", "create patch but do not request build");
        options.put("-t [--status]", "show status only, do not update or build");
        return options;
    }

    public boolean isDefault()
    {
        return false;
    }

    public static void main(String[] argv)
    {
        PersonalBuildCommand command = new PersonalBuildCommand();
        try
        {
            System.exit(command.execute(argv));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}