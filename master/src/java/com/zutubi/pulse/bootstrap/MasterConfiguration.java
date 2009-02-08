package com.zutubi.pulse.bootstrap;

import com.zutubi.pulse.logging.LogConfiguration;
import com.zutubi.pulse.model.Slave;

/**
 * 
 *
 */
public interface MasterConfiguration extends LogConfiguration
{
    //---( server configuration )---
    public static final String ADMIN_LOGIN = "admin.login";

    public static final String BASE_URL = "webapp.base.url";

    public static final String AGENT_HOST = "agent.url";

    public static final String MASTER_ENABLED = "server.agent.enabled";

    //---( help configuration )---
    public static final String HELP_URL = "help.url";

    //---( queue configuration )---
    public static final String UNSATISFIABLE_RECIPE_TIMEOUT = "unsatisfiable.recipe.timeout";
    public static final long UNSATISFIABLE_RECIPE_TIMEOUT_DEFAULT = 15;

    //---( mail configuration )---

    public static final String SMTP_HOST = "mail.smtp.host";

    public static final String SMTP_SSL = "mail.smtp.ssl";

    public static final String SMTP_PORT = "mail.smtp.port";

    public static final String SMTP_FROM = "mail.smtp.from";

    public static final String SMTP_PREFIX = "mail.smtp.prefix";

    public static final String SMTP_USERNAME = "mail.smtp.username";

    public static final String SMTP_PASSWORD = "mail.smtp.password";

    public static final String SMTP_LOCAL_HOSTNAME = "mail.smtp.localhost";

    //---( jabber configuration )---

    public static final String JABBER_HOST = "jabber.host";

    public static final String JABBER_PORT = "jabber.port";
    public static final int JABBER_PORT_DEFAULT = 5222;

    public static final String JABBER_USERNAME = "jabber.username";

    public static final String JABBER_PASSWORD = "jabber.password";

    public static final String JABBER_FORCE_SSL = "jabber.force.ssl";

    //---( rss configuration )---

    public static final String RSS_ENABLED = "rss.enabled";

    //---( anonymous access )---

    public static final String ANONYMOUS_ACCESS_ENABLED = "anon.enabled";

    public static final String ANONYMOUS_SIGNUP_ENABLED = "signup.enabled";

    //---( scm integration )---
    public static final String SCM_POLLING_INTERVAL = "scm.polling.interval";

    //--- ( ldap integration )---

    public static final String LDAP_ENABLED = "ldap.enabled";
    public static final String LDAP_HOST_URL = "ldap.host";
    public static final String LDAP_BASE_DN = "ldap.base.dn";
    public static final String LDAP_MANAGER_DN = "ldap.manager.dn";
    public static final String LDAP_MANAGER_PASSWORD = "ldap.manager.password";
    public static final String LDAP_USER_BASE = "ldap.user.base";
    public static final String LDAP_USER_FILTER = "ldap.user.filter";
    public static final String LDAP_GROUP_BASE_DN = "ldap.group.base.dn";
    public static final String LDAP_GROUP_SEARCH_SUBTREE = "ldap.group.search.subtree";
    public static final String LDAP_GROUP_FILTER = "ldap.group.filter";
    public static final String LDAP_GROUP_ROLE_ATTRIBUTE = "ldap.group.role.attribute";
    public static final String LDAP_AUTO_ADD = "ldap.auto.add";
    public static final String LDAP_EMAIL_ATTRIBUTE = "ldap.email.attribute";
    public static final String LDAP_ESCAPE_SPACES = "ldap.escape";
    public static final String LDAP_FOLLOW_REFERRALS = "ldap.follow";

    String getAdminLogin();

    void setAdminLogin(String login);

    String getBaseUrl();

    void setBaseUrl(String host);

    String getAgentHost();

    void setAgentHost(String url);

    String getHelpUrl();

    void setHelpUrl(String helpUrl);

    String getSmtpHost();

    void setSmtpHost(String host);

    int getSmtpPort();

    void setSmtpPort(int port);

    boolean getSmtpSSL();

    void setSmtpSSL(boolean ssl);

    String getSmtpFrom();

    void setSmtpFrom(String from);

    String getSmtpPrefix();

    void setSmtpPrefix(String prefix);

    String getSmtpUsername();

    void setSmtpUsername(String username);

    String getSmtpPassword();

    String getSmtpLocalhost();

    void setSmtpLocalhost(String host);

    void setSmtpPassword(String password);

    String getJabberHost();

    void setJabberHost(String host);

    int getJabberPort();

    void setJabberPort(int port);

    String getJabberUsername();

    void setJabberUsername(String username);

    String getJabberPassword();

    void setJabberPassword(String password);

    Boolean getJabberForceSSL();

    void setJabberForceSSL(Boolean forceSSL);

    Boolean getRssEnabled();

    void setRssEnabled(Boolean rssEnabled);

    Boolean getAnonymousAccessEnabled();

    void setAnonymousAccessEnabled(Boolean anonEnabled);

    Boolean getAnonymousSignupEnabled();

    void setAnonymousSignupEnabled(Boolean signupEnabled);

    Boolean getLdapEnabled();

    void setLdapEnabled(Boolean enabled);

    String getLdapHostUrl();

    void setLdapHostUrl(String hostUrl);

    String getLdapBaseDn();

    void setLdapBaseDn(String baseDn);

    String getLdapManagerDn();

    void setLdapManagerDn(String managerDn);

    String getLdapManagerPassword();

    void setLdapManagerPassword(String managerPassword);

    String getLdapUserBase();

    void setLdapUserBase(String userBase);

    String getLdapUserFilter();

    void setLdapUserFilter(String userFilter);

    Boolean getLdapAutoAdd();

    void setLdapAutoAdd(Boolean autoAdd);

    String getLdapEmailAttribute();

    void setLdapEmailAttribute(String attribute);

    Boolean getLdapEscapeSpaces();

    void setLdapEscapeSpaces(Boolean escape);

    boolean getLdapFollowReferrals();

    void setLdapFollowReferrals(boolean follow);

    String getLdapGroupBaseDn();

    void setLdapGroupBaseDn(String dn);

    boolean getLdapGroupSearchSubtree();

    void setLdapGroupSearchSubtree(boolean search);

    String getLdapGroupFilter();

    void setLdapGroupFilter(String groupFilter);

    String getLdapGroupRoleAttribute();

    void setLdapGroupRoleAttribute(String attribute);

    Integer getScmPollingInterval();

    void setScmPollingInterval(Integer interval);

    long getUnsatisfiableRecipeTimeout();

    void setUnsatisfiableRecipeTimeout(Long timeout);

    Slave.EnableState getMasterEnableState();

    void setMasterEnableState(Slave.EnableState state);
}