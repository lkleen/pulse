package com.zutubi.pulse.web.user;

import com.zutubi.pulse.model.User;
import com.zutubi.pulse.util.StringUtils;
import org.acegisecurity.providers.encoding.PasswordEncoder;

/**
 * Allow a user to edit there password.
 *
 */
public class EditPasswordAction extends UserActionSupport
{
    private String current;
    private String password;
    private String confirm;

    private PasswordEncoder passwordEncoder;

    public String getCurrent()
    {
        return current;
    }

    public void setCurrent(String current)
    {
        this.current = current;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirm()
    {
        return confirm;
    }

    public void setConfirm(String confirm)
    {
        this.confirm = confirm;
    }

    public void validate()
    {
        // ensure that the current password is correct.
        User user = getUser();
        if (user == null)
        {
            addUnknownUserActionError();
            return;
        }

        // ensure that the new password and the confirmation match
        if (!StringUtils.equals(password, confirm))
        {
            addFieldError("confirm", getText("password.confirm.mismatch"));
        }

        if (!passwordEncoder.isPasswordValid(user.getPassword(), current, null))
        {
            addFieldError("current", getText("password.current.mismatch"));
        }
    }

    public String doInput() throws Exception
    {
        return super.doInput();
    }

    public String execute() throws Exception
    {
        User user = getUser();
        getUserManager().setPassword(user, password);
        getUserManager().save(user);
        return SUCCESS;
    }

    /**
     * Required resource.
     *
     * @param passwordEncoder
     */
    public void setPasswordEncoder(PasswordEncoder passwordEncoder)
    {
        this.passwordEncoder = passwordEncoder;
    }
}