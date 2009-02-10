package com.zutubi.pulse.acceptance.pages;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.SeleniumUtils;
import com.zutubi.pulse.acceptance.forms.SignupForm;
import com.zutubi.pulse.master.webwork.Urls;

/**
 */
public class LoginPage extends SeleniumPage
{
    private static final String SIGNUP_ID = "signup";

    public LoginPage(Selenium selenium, Urls urls)
    {
        super(selenium, urls, "login-logo", "login");
    }

    public String getUrl()
    {
        return urls.login();
    }

    public void login(String username, String password)
    {
        selenium.type("zfid.j_username", username);
        selenium.type("zfid.j_password", password);
        selenium.click("zfid.login");
        selenium.waitForPageToLoad("30000");
    }

    public boolean isSignupPresent()
    {
        return selenium.isElementPresent(SIGNUP_ID);
    }

    public void waitForSignup()
    {
        SeleniumUtils.waitForElementId(selenium, SIGNUP_ID);
    }

    public SignupForm clickSignup()
    {
        selenium.click(SIGNUP_ID);
        selenium.waitForPageToLoad("30000");
        return new SignupForm(selenium);
    }
}