package com.zutubi.pulse.dev.personal;

import com.zutubi.pulse.core.scm.api.MenuChoice;
import com.zutubi.pulse.core.scm.api.MenuOption;
import com.zutubi.pulse.core.scm.api.PersonalBuildUI;
import com.zutubi.pulse.core.scm.api.YesNoResponse;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;
import com.zutubi.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import static java.util.Arrays.asList;
import java.util.LinkedList;
import java.util.List;

/**
 * A personal build UI that uses the console to interact with the user.
 */
public class ConsoleUI implements PersonalBuildUI
{
    private static final String ECHO_PROPERTY = "pulse.echo.passwords";

    public enum Verbosity
    {
        QUIET,
        NORMAL,
        VERBOSE
    }

    private Console console;
    private Verbosity verbosity = Verbosity.NORMAL;
    private String indent = "";

    public ConsoleUI()
    {
        console = new DefaultConsole();
    }

    private void fatal(String message)
    {
        print("Error: " + message, true);
        System.exit(1);
    }

    public Verbosity getVerbosity()
    {
        return verbosity;
    }

    public void setVerbosity(Verbosity verbosity)
    {
        this.verbosity = verbosity;
    }

    public boolean isDebugEnabled()
    {
        return verbosity == Verbosity.VERBOSE;
    }

    public void debug(String message)
    {
        if (verbosity == Verbosity.VERBOSE)
        {
            print(message, false);
        }
    }

    public void status(String message)
    {
        if (verbosity != Verbosity.QUIET)
        {
            print(message, false);
        }
    }

    public void warning(String message)
    {
        print("Warning: " + message, true);
    }

    public void error(String message)
    {
        print("Error: " + message, true);
    }

    public void error(String message, Throwable throwable)
    {
        if (verbosity == Verbosity.VERBOSE)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            throwable.printStackTrace(new PrintStream(baos));
            console.printError(baos.toString());
        }

        error(message);
    }

    private void print(String message, boolean error)
    {
        if (error)
        {
            console.printErrorLine(indent + message);
        }
        else
        {
            console.printOutputLine(indent + message);
        }
    }

    public void enterContext()
    {
        indent += "  ";
    }

    public void exitContext()
    {
        if(indent.length() >= 2)
        {
            indent = indent.substring(2);
        }
    }

    public String inputPrompt(String question)
    {
        console.printOutput(question + ": ");
        try
        {
            return console.readInputLine();
        }
        catch (IOException e)
        {
            fatal("Unable to prompt for input: " + e.getMessage());
            return null;
        }
    }

    public String inputPrompt(String prompt, String defaultResponse)
    {
        console.printOutput(prompt + " [default: " + defaultResponse + "]: ");
        try
        {
            String response = console.readInputLine();
            if(response.length() == 0)
            {
                response = defaultResponse;
            }

            return response;
        }
        catch (IOException e)
        {
            fatal("Unable to prompt for input: " + e.getMessage());
            return defaultResponse;
        }
    }

    public String passwordPrompt(String question)
    {
        String result = console.readPassword(question + ": ", Boolean.getBoolean(ECHO_PROPERTY));
        if (result == null)
        {
            fatal("Unable to prompt for password");
        }

        return result;
    }

    public YesNoResponse yesNoPrompt(String question, boolean showAlways, boolean showNever, YesNoResponse defaultResponse)
    {
        List<YesNoResponse> validResponses = new LinkedList<YesNoResponse>(asList(YesNoResponse.YES, YesNoResponse.NO));
        if (showAlways)
        {
            validResponses.add(YesNoResponse.ALWAYS);
        }

        if (showNever)
        {
            validResponses.add(YesNoResponse.NEVER);
        }

        String prompt = StringUtils.join("/", CollectionUtils.map(validResponses, new Mapping<YesNoResponse, String>()
        {
            public String map(YesNoResponse response)
            {
                return response.getPrompt();
            }
        }));

        prompt += String.format(" [default: %s]> ", defaultResponse.getPrompt());

        try
        {
            console.printOutputLine(question);

            YesNoResponse response;
            do
            {
                console.printOutput(prompt);
                String input = console.readInputLine();
                response = YesNoResponse.fromInput(input, defaultResponse, validResponses.toArray(new YesNoResponse[validResponses.size()]));
            }
            while (response == null);

            return response;
        }
        catch (IOException e)
        {
            fatal("Unable to prompt for input: " + e.getMessage());
            return null;
        }
    }

    public <T> MenuChoice<T> menuPrompt(String question, List<MenuOption<T>> options)
    {
        console.printOutputLine(question + ":");
        int defaultOption = 0;
        int i = 1;
        for (MenuOption<T> option: options)
        {
            console.printOutputLine("  " + i + ") " + option.getText());
            if (option.isDefaultOption())
            {
                defaultOption = i;
            }
            i++;
        }

        String prompt = "Choose a number (append '!' to save this selection)";
        if (defaultOption != 0)
        {
            prompt += " [default: " + defaultOption + "]";
        }
        prompt += "> ";

        MenuChoice<T> choice;
        do
        {
            console.printOutput(prompt);
            try
            {
                choice = decodeChoice(console.readInputLine(), options, defaultOption);
            }
            catch (IOException e)
            {
                fatal("Unable to prompt for input: " + e.getMessage());
                return null;
            }
        }
        while (choice == null);

        return choice;
    }

    private <T> MenuChoice<T> decodeChoice(String input, List<MenuOption<T>> options, int defaultOption)
    {
        input = input.trim();
        boolean persistent;
        if (input.endsWith("!"))
        {
            persistent = true;
            input = input.substring(0, input.length() - 1);
        }
        else
        {
            persistent = false;
        }

        if (input.length() == 0)
        {
            if (defaultOption == 0)
            {
                return null;
            }
            else
            {
                return new MenuChoice<T>(options.get(defaultOption - 1).getValue(), persistent);
            }
        }

        try
        {
            int intValue = Integer.parseInt(input);
            if (intValue <= 0 || intValue > options.size())
            {
                return null;
            }

            return new MenuChoice<T>(options.get(intValue - 1).getValue(), persistent);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public void setConsole(Console console)
    {
        this.console = console;
    }
}
