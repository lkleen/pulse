package com.zutubi.pulse.model.persistence;

import com.zutubi.pulse.model.User;
import com.zutubi.pulse.model.Project;

import java.util.List;

/**
 * 
 *
 */
public interface UserDao extends EntityDao<User>
{
    User findByLogin(String login);

    List<User> findByLikeLogin(String login);

    List<Project> getProjects(User user);
}
