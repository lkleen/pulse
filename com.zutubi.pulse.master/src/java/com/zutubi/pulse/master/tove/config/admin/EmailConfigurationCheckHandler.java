package com.zutubi.pulse.master.tove.config.admin;

import com.zutubi.pulse.master.notifications.email.EmailService;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Wire;
import com.zutubi.tove.config.api.AbstractConfigurationCheckHandler;
import com.zutubi.validation.annotations.Required;

import java.util.Arrays;

/**
 */
@SymbolicName("zutubi.emailConfigurationCheckHandler")
@Wire
public class EmailConfigurationCheckHandler extends AbstractConfigurationCheckHandler<EmailConfiguration>
{
    @Required
    private String emailAddress;
    
    private EmailService emailService;

    public void test(EmailConfiguration configuration) throws Exception
    {
        emailService.sendMail(Arrays.asList(emailAddress), "Test Email", "text/plain", "Welcome to Zutubi Pulse!", configuration);
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

    public void setEmailService(EmailService emailService)
    {
        this.emailService = emailService;
    }
}
