package com.zutubi.pulse.license;

/**
 * <class-comment/>
 */
public interface LicenseEnforcer
{
    /**
     * Returns true if the installed license enables pulse.
     *
     */
    boolean isLicensed();

    /**
     * Returns true if the installed license allows another project to be created.
     *
     */
    boolean canAddProject();

    /**
     * Returns true if the installed license allows another user to be created.
     *
     */
    boolean canAddUser();

    /**
     * Returns true if the installed license allows another agent to be configured.
     *
     */
    boolean canAddAgent();
}
