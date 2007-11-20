package com.zutubi.pulse.api;

import com.zutubi.prototype.config.ConfigurationSecurityManager;
import com.zutubi.prototype.config.ConfigurationTemplateManager;
import com.zutubi.prototype.security.AccessManager;
import com.zutubi.prototype.type.*;
import com.zutubi.prototype.type.record.*;
import com.zutubi.pulse.ShutdownManager;
import com.zutubi.pulse.Version;
import com.zutubi.pulse.util.TimeStamps;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.core.config.Configuration;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.core.model.ResultState;
import com.zutubi.pulse.core.scm.ScmClient;
import com.zutubi.pulse.core.scm.ScmException;
import com.zutubi.pulse.core.scm.ScmClientUtils;
import com.zutubi.pulse.core.scm.ScmClientFactory;
import com.zutubi.pulse.core.scm.config.ScmConfiguration;
import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.system.SystemStartedEvent;
import com.zutubi.util.logging.Logger;
import com.zutubi.util.TextUtils;

import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import java.util.Date;

/**
 * Implements a simple API for remote monitoring and control.
 */
// FIXME
public class RemoteApi implements com.zutubi.pulse.events.EventListener
{
    private static final Logger LOG = Logger.getLogger(RemoteApi.class);

    private TokenManager tokenManager;
    private EventManager eventManager;
    private ShutdownManager shutdownManager;
    private ConfigurationTemplateManager configurationTemplateManager;
    private ConfigurationSecurityManager configurationSecurityManager;
    private TypeRegistry typeRegistry;
    private RecordManager recordManager;

    private BuildManager buildManager;
    private ProjectManager projectManager;
    private ScmClientFactory<ScmConfiguration> scmClientFactory;
    //    private UserManager userManager;
//    private AgentManager agentManager;
//    private AuthenticationManager authenticationManager;
//
//    private ValidationManager validationManager;
//
//    //---( Define the properties that are visible in structs in the remote api. )---
//    private static final Map<Class, String[]> structDefs = new HashMap<Class, String[]>();
//    private CommitMessageTransformerManager transformerManager;
//
//    {
//        structDefs.put(Project.class, new String[]{"id", "name", "description", "url"});
//    }

    //
    public RemoteApi()
    {
        // can remove this call when we sort out autowiring from the XmlRpcServlet.
        ComponentContext.autowire(this);
    }

    public int getVersion()
    {
        Version v = Version.getVersion();
        return v.getBuildNumberAsInt();
    }

    public String login(String username, String password) throws AuthenticationException
    {
        return tokenManager.login(username, password);
    }

    public boolean logout(String token)
    {
        return tokenManager.logout(token);
    }

    public String ping()
    {
        return "pong";
    }

    public Boolean configPathExists(String token, String path) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            return configurationTemplateManager.pathExists(path) && configurationSecurityManager.hasPermission(path, AccessManager.ACTION_VIEW);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public Vector<String> getConfigListing(String token, String path) throws AuthenticationException, TypeException
    {
        tokenManager.loginUser(token);
        try
        {
            Vector<String> result;

            if (path.length() == 0)
            {
                result = new Vector<String>(configurationTemplateManager.getRootListing());
            }
            else
            {
                Type type = configurationTemplateManager.getType(path);
                if (type instanceof CollectionType)
                {
                    CollectionType collectionType = (CollectionType) type;
                    if (collectionType.getCollectionType() instanceof ComplexType)
                    {
                        Record record = configurationTemplateManager.getRecord(path);
                        result = new Vector<String>(collectionType.getOrder(record));
                    }
                    else
                    {
                        throw new IllegalArgumentException("Path refers to simple collection");
                    }
                }
                else
                {
                    CompositeType compositeType = (CompositeType) type;
                    result = new Vector<String>(compositeType.getNestedPropertyNames());
                }
            }

            configurationSecurityManager.filterPaths(path, result, AccessManager.ACTION_VIEW);
            return result;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public Hashtable<String, Object> createDefaultConfig(String token, String symbolicName) throws AuthenticationException, TypeException
    {
        tokenManager.verifyUser(token);
        CompositeType type = typeRegistry.getType(symbolicName);
        if (type == null)
        {
            throw new IllegalArgumentException("Unrecognised symbolic name '" + symbolicName + "'");
        }

        MutableRecord record = type.createNewRecord(true);
        return type.toXmlRpc(record);
    }

    public Object getConfig(String token, String path) throws AuthenticationException, TypeException
    {
        tokenManager.loginUser(token);

        try
        {
            Object instance = configurationTemplateManager.getInstance(path);
            if (instance == null)
            {
                throw new IllegalArgumentException("Path '" + path + "' does not exist");
            }

            configurationSecurityManager.ensurePermission(path, AccessManager.ACTION_VIEW);

            Type t = configurationTemplateManager.getType(path);
            return t.toXmlRpc(t.unstantiate(instance));
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public String getConfigHandle(String token, String path) throws AuthenticationException, TypeException
    {
        tokenManager.loginUser(token);

        try
        {
            configurationSecurityManager.ensurePermission(path, AccessManager.ACTION_VIEW);

            Record record = configurationTemplateManager.getRecord(path);
            if (record == null)
            {
                throw new IllegalArgumentException("Path '" + path + "' does not exist");
            }

            return Long.toString(record.getHandle());
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public Object getRawConfig(String token, String path) throws AuthenticationException, TypeException
    {
        tokenManager.loginUser(token);
        try
        {
            Record record = recordManager.select(path);
            if (record == null)
            {
                throw new IllegalArgumentException("Path '" + path + "' does not exist");
            }

            configurationSecurityManager.ensurePermission(path, AccessManager.ACTION_VIEW);

            Type t = configurationTemplateManager.getType(path);
            return t.toXmlRpc(record);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public boolean isConfigValid(String token, String path) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            Configuration instance = configurationTemplateManager.getInstance(path);
            if(instance == null)
            {
                throw new IllegalArgumentException("Path '" + path + "' does not exist");
            }

            configurationSecurityManager.ensurePermission(path, AccessManager.ACTION_VIEW);
            return instance.isValid();
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public String insertConfig(String token, String path, Hashtable config) throws AuthenticationException, TypeException, ValidationException
    {
        tokenManager.loginUser(token);
        try
        {
            ComplexType pathType = configurationTemplateManager.getType(path);
            CompositeType expectedType = (CompositeType) pathType.getTargetType();

            String parentPath;
            String baseName;
            if (pathType instanceof CollectionType)
            {
                parentPath = path;
                baseName = null;
            }
            else
            {
                parentPath = PathUtils.getParentPath(path);
                baseName = PathUtils.getBaseName(path);
            }

            if (configurationTemplateManager.isTemplatedCollection(parentPath))
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': use insertTemplatedConfig to insert into templated collections");
            }

            String symbolicName = CompositeType.getTypeFromXmlRpc(config);
            CompositeType type = configurationTemplateManager.typeCheck(expectedType, symbolicName);
            MutableRecord record = type.fromXmlRpc(config);

            Configuration instance = configurationTemplateManager.validate(parentPath, baseName, record, true);
            if (!type.isValid(instance))
            {
                throw new ValidationException(type, instance);
            }

            return configurationTemplateManager.insertRecord(path, record);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public String insertTemplatedConfig(String token, String templateParentPath, Hashtable config, boolean template) throws AuthenticationException, TypeException, ValidationException
    {
        tokenManager.loginUser(token);
        try
        {
            String insertPath = PathUtils.getParentPath(templateParentPath);
            if (insertPath == null)
            {
                throw new IllegalArgumentException("Invalid templateParentPath '" + templateParentPath + "': no parent path");
            }

            if (!configurationTemplateManager.isTemplatedCollection(insertPath))
            {
                throw new IllegalArgumentException("Invalid templateParentPath '" + templateParentPath + "': parent path '" + insertPath + "' is not a templated collection, use insertConfig instead");
            }

            TemplateRecord templateParent = (TemplateRecord) configurationTemplateManager.getRecord(templateParentPath);
            if (templateParent == null)
            {
                throw new IllegalArgumentException("Invalid templateParentPath '" + templateParentPath + "': template parent does not exist");
            }

            if (configurationTemplateManager.isConcrete(insertPath, templateParent))
            {
                throw new IllegalArgumentException("Invalid templateParentPath '" + templateParentPath + "': template parent is concrete and thus cannot be inherited from");
            }

            CompositeType expectedType = (CompositeType) templateParent.getType();

            String symbolicName = CompositeType.getTypeFromXmlRpc(config);
            CompositeType type = configurationTemplateManager.typeCheck(expectedType, symbolicName);
            MutableRecord record = type.fromXmlRpc(config);
            configurationTemplateManager.setParentTemplate(record, templateParent.getHandle());
            if (template)
            {
                configurationTemplateManager.markAsTemplate(record);
            }

            Configuration instance = configurationTemplateManager.validate(insertPath, null, record, true);
            if (!type.isValid(instance))
            {
                throw new ValidationException(type, instance);
            }

            return configurationTemplateManager.insertRecord(insertPath, record);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public String saveConfig(String token, String path, Hashtable config, boolean deep) throws AuthenticationException, TypeException, ValidationException
    {
        tokenManager.loginUser(token);
        try
        {
            Record existingRecord = configurationTemplateManager.getRecord(path);
            if (existingRecord == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': no existing record found (use insert to create new records)");
            }

            String existingSymbolicName = existingRecord.getSymbolicName();
            if (existingSymbolicName == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': path refers to a collection (manipulate collections using insert and delete)");
            }

            String symbolicName = CompositeType.getTypeFromXmlRpc(config);
            if (!existingSymbolicName.equals(symbolicName))
            {
                throw new IllegalArgumentException("Expecting type '" + existingSymbolicName + "', found '" + symbolicName + "' (type cannot be changed by saving)");
            }

            CompositeType type = typeRegistry.getType(existingSymbolicName);
            MutableRecord record = type.fromXmlRpc(config);

            Configuration instance = configurationTemplateManager.validate(PathUtils.getParentPath(path), PathUtils.getBaseName(path), record, deep);
            if ((deep && !type.isValid(instance)) || !instance.isValid())
            {
                throw new ValidationException(type, instance);
            }

            return configurationTemplateManager.saveRecord(path, record, deep);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public boolean deleteConfig(String token, String path) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            if (configurationTemplateManager.getRecord(path) == null)
            {
                return false;
            }

            configurationTemplateManager.delete(path);
            return true;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public int deleteAllConfigs(String token, String pathPattern) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            return configurationTemplateManager.deleteAll(pathPattern);
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public boolean restoreConfig(String token, String path) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            configurationTemplateManager.restore(path);
            return true;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public boolean setConfigOrder(String token, String path, Vector<String> order) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            configurationTemplateManager.setOrder(path, order);
            return true;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public boolean logError(String token, String message) throws AuthenticationException
    {
        tokenManager.verifyAdmin(token);
        LOG.severe(message);
        return true;
    }

    public boolean logWarning(String token, String message) throws AuthenticationException
    {
        tokenManager.verifyAdmin(token);
        LOG.warning(message);
        return true;
    }

//    public Vector<String> getAllUserLogins(String token) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//        List<User> users = userManager.getAllUsers();
//        Vector<String> result = new Vector<String>(users.size());
//        for (User user : users)
//        {
//            result.add(user.getLogin());
//        }
//
//        return result;
//    }
//
//    public Vector<String> getAllProjectNames(String token) throws AuthenticationException
//    {
//        //@Secured({"ROLE_USER"})
//        tokenManager.verifyUser(token);
//
//        List<Project> projects = projectManager.getAllProjects();
//        return getNames(projects);
//    }
//
//    public Vector<String> getAllProjectGroups(String token) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//
//        List<ProjectGroup> groups = projectManager.getAllProjectGroups();
//        Vector<String> result = new Vector<String>(groups.size());
//        for (ProjectGroup g : groups)
//        {
//            result.add(g.getName());
//        }
//
//        return result;
//    }
//
//    public Hashtable<String, Object> getProjectGroup(String token, String name) throws AuthenticationException, IllegalArgumentException
//    {
//        tokenManager.verifyUser(token);
//
//        ProjectGroup group = projectManager.getProjectGroup(name);
//        if(group == null)
//        {
//            throw new IllegalArgumentException(String.format("Unknown project group: '%s'", name));
//        }
//
//        Hashtable<String, Object> result = new Hashtable<String, Object>();
//        result.put("name", group.getName());
//        result.put("projects", getNames(group.getProjects()));
//        return result;
//    }
//
//    public boolean createProjectGroup(String token, String name, Vector<String> projects) throws AuthenticationException, IllegalArgumentException
//    {
//        tokenManager.verifyAdmin(token);
//
//        if(!TextUtils.stringSet(name))
//        {
//            throw new IllegalArgumentException("Name is required");
//        }
//
//        if(projectManager.getProjectGroup(name) != null)
//        {
//            throw new IllegalArgumentException(String.format("A project group with name '%s' already exists", name));
//        }
//
//        List<Project> members = getProjectList(projects);
//
//        ProjectGroup group = new ProjectGroup(name);
//        group.setProjects(members);
//
//        try
//        {
//            tokenManager.loginUser(token);
//            projectManager.save(group);
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean editProjectGroup(String token, String name, String newName, Vector<String> projects) throws AuthenticationException, IllegalArgumentException
//    {
//        tokenManager.verifyAdmin(token);
//
//        ProjectGroup group = projectManager.getProjectGroup(name);
//        if(group == null)
//        {
//            throw new IllegalArgumentException(String.format("Unknown project group '%s'", name));
//        }
//
//        if(!TextUtils.stringSet(newName))
//        {
//            throw new IllegalArgumentException("Name is required");
//        }
//
//        if(!newName.equals(name) && projectManager.getProjectGroup(newName) != null)
//        {
//            throw new IllegalArgumentException(String.format("A project group with name '%s' already exists", newName));
//        }
//
//        group.setName(newName);
//        List<Project> members = getProjectList(projects);
//        group.setProjects(members);
//
//        try
//        {
//            tokenManager.loginUser(token);
//            projectManager.save(group);
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean deleteProjectGroup(String token, String name) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//
//        ProjectGroup group = projectManager.getProjectGroup(name);
//        if(group == null)
//        {
//            return false;
//        }
//        else
//        {
//            projectManager.delete(group);
//            return true;
//        }
//    }
//
//    public Vector<String> getMyProjectNames(String token) throws AuthenticationException
//    {
//        User user = tokenManager.verifyUser(token);
//        List<Project> projects = new LinkedList<Project>();
//
//        if (user != null)
//        {
//            projects.addAll(userManager.getUserProjects(user, projectManager));
//        }
//
//        return getNames(projects);
//    }

    public Vector<Hashtable<String, Object>> getBuild(String token, String projectName, int id) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(1);
            Project project = internalGetProject(projectName, true);
            BuildResult build = buildManager.getByProjectAndNumber(project, id);
            if (build == null)
            {
                return result;
            }

            result.add(convertResult(build));
            return result;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

//    public boolean deleteBuild(String token, String projectName, int id) throws AuthenticationException
//    {
//        tokenManager.loginUser(token);
//        try
//        {
//            Project project = internalGetProject(projectName);
//            projectManager.checkWrite(project);
//            BuildResult build = buildManager.getByProjectAndNumber(project, id);
//            if (build == null)
//            {
//                return false;
//            }
//
//            buildManager.delete(build);
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public Vector<Hashtable<String, Object>> queryBuildsForProject(String token, String projectName, String[] resultStates, int firstResult, int maxResults, boolean mostRecentFirst) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        Project project = internalGetProject(projectName);
//
//        List<BuildResult> builds = buildManager.queryBuilds(new Project[]{project}, mapStates(resultStates), -1, -1, null, firstResult, maxResults, mostRecentFirst);
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(builds.size());
//        for (BuildResult build : builds)
//        {
//            Hashtable<String, Object> buildDetails = convertResult(build);
//            result.add(buildDetails);
//        }
//
//        return result;
//    }
//
//    public Vector<Hashtable<String, Object>> getBuildRange(String token, String projectName, String buildSpecification, int afterBuild, int toBuild) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//
//        Project project = internalGetProject(projectName);
//        BuildSpecification spec = getBuildSpecification(project, buildSpecification);
//        List<BuildResult> buildRange = buildManager.querySpecificationBuilds(project, spec.getPname(), ResultState.getCompletedStates(), afterBuild + 1, toBuild, 0, -1, false, false);
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(buildRange.size());
//        for(BuildResult r: buildRange)
//        {
//            result.add(convertResult(r));
//        }
//
//        return result;
//    }
//
//    public Vector<Hashtable<String, Object>> getPreviousBuild(String token, String projectName, int id) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//
//        Project project = internalGetProject(projectName);
//        BuildResult buildResult = internalGetBuild(project, id);
//        buildResult = buildManager.getPreviousBuildResult(buildResult);
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>();
//        if(buildResult != null)
//        {
//            result.add(convertResult(buildResult));
//        }
//
//        return result;
//    }
//    
//    private ResultState[] mapStates(String[] stateNames)
//    {
//        if(stateNames.length > 0)
//        {
//            ResultState[] states = new ResultState[stateNames.length];
//            for(int i = 0; i < stateNames.length; i++)
//            {
//                states[i] = ResultState.fromPrettyString(stateNames[i]);
//            }
//
//            return states;
//        }
//        else
//        {
//            return null;
//        }
//    }

    public int getNextBuildNumber(String token, String projectName) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            Project project = internalGetProject(projectName, true);
            return (int) project.getNextBuildNumber();
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public Vector<Hashtable<String, Object>> getLatestBuildsForProject(String token, String projectName, boolean completedOnly, int maxResults) throws AuthenticationException
    {
        tokenManager.loginUser(token);
        try
        {
            Project project = internalGetProject(projectName, true);

            ResultState[] states = null;
            if (completedOnly)
            {
                states = ResultState.getCompletedStates();
            }

            List<BuildResult> builds = buildManager.queryBuilds(new Project[]{project}, states, -1, -1, null, 0, maxResults, true);
            Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(builds.size());
            for (BuildResult build : builds)
            {
                Hashtable<String, Object> buildDetails = convertResult(build);
                result.add(buildDetails);
            }

            return result;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

    public Vector<Hashtable<String, Object>> getLatestBuildForProject(String token, String projectName, boolean completedOnly) throws AuthenticationException
    {
        return getLatestBuildsForProject(token, projectName, completedOnly, 1);
    }

//    public Vector<Hashtable<String, Object>> getPersonalBuild(String token, int id) throws AuthenticationException
//    {
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(1);
//
//        User user = tokenManager.verifyUser(token);
//        BuildResult build = buildManager.getByUserAndNumber(user, id);
//        if (build == null)
//        {
//            return result;
//        }
//
//        result.add(convertResult(build));
//        return result;
//    }
//
//    public Vector<Hashtable<String, Object>> getLatestPersonalBuilds(String token, boolean completedOnly, int maxResults) throws AuthenticationException
//    {
//        User user = tokenManager.verifyUser(token);
//
//        List<BuildResult> builds = buildManager.getPersonalBuilds(user);
//        if (completedOnly)
//        {
//            Iterator<BuildResult> it = builds.iterator();
//            while (it.hasNext())
//            {
//                BuildResult b = it.next();
//                if (!b.completed())
//                {
//                    it.remove();
//                }
//            }
//        }
//
//        if (maxResults >= 0 && builds.size() > maxResults)
//        {
//            builds = builds.subList(0, maxResults);
//        }
//
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(builds.size());
//        for (BuildResult build : builds)
//        {
//            Hashtable<String, Object> buildDetails = convertResult(build);
//            result.add(buildDetails);
//        }
//
//        return result;
//    }
//
//    public Vector<Hashtable<String, Object>> getLatestPersonalBuild(String token, boolean completedOnly) throws AuthenticationException
//    {
//        return getLatestPersonalBuilds(token, completedOnly, 1);
//    }

    private Hashtable<String, Object> convertResult(BuildResult build)
    {
        Hashtable<String, Object> buildDetails = new Hashtable<String, Object>();
        buildDetails.put("id", (int) build.getNumber());
        buildDetails.put("project", build.getProject().getName());
        buildDetails.put("revision", getBuildRevision(build));
        buildDetails.put("status", build.getState().getPrettyString());
        buildDetails.put("completed", build.completed());
        buildDetails.put("succeeded", build.succeeded());

        TimeStamps timeStamps = build.getStamps();
        buildDetails.put("startTime", new Date(timeStamps.getStartTime()));
        buildDetails.put("endTime", new Date(timeStamps.getEndTime()));
        if (timeStamps.hasEstimatedTimeRemaining())
        {
            buildDetails.put("progress", timeStamps.getEstimatedPercentComplete());
        }
        else
        {
            buildDetails.put("progress", -1);
        }

        return buildDetails;
    }

    private String getBuildRevision(BuildResult build)
 	{
        Revision revision = build.getRevision();
        if(revision != null)
        {
            return revision.getRevisionString();
        }

         return "";
 	}

//    public Vector<Hashtable<String, Object>> getChangesInBuild(String token, String projectName, int id) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        Project project = internalGetProject(projectName);
//        BuildResult build = buildManager.getByProjectAndNumber(project, id);
//        if (build == null)
//        {
//            throw new IllegalArgumentException("Unknown build '" + id + "' for project '" + projectName + "'");
//        }
//
//        List<Changelist> changelists = buildManager.getChangesForBuild(build);
//        Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>(changelists.size());
//        for(Changelist change: changelists)
//        {
//            result.add(convertChangelist(change));
//        }
//
//        return result;
//    }
//
//    private Hashtable<String, Object> convertChangelist(Changelist change)
//    {
//        Hashtable<String, Object> result = new Hashtable<String, Object>();
//        if(change.getRevision() != null && change.getRevision().getRevisionString() != null)
//        {
//            result.put("revision", change.getRevision().getRevisionString());
//        }
//        if(change.getUser() != null)
//        {
//            result.put("author", change.getUser());
//        }
//        if(change.getDate() != null)
//        {
//            result.put("date", change.getDate());
//        }
//        if(change.getComment() != null)
//        {
//            result.put("comment", change.getComment());
//        }
//
//        Vector<Hashtable<String, Object>> files = new Vector<Hashtable<String, Object>>(change.getChanges().size());
//        for(Change file: change.getChanges())
//        {
//            files.add(convertChange(file));
//        }
//        result.put("files", files);
//
//        return result;
//    }
//
//    private Hashtable<String, Object> convertChange(Change change)
//    {
//        Hashtable<String, Object> result = new Hashtable<String, Object>();
//        if(change.getFilename() != null)
//        {
//            result.put("file", change.getFilename());
//        }
//        if(change.getRevision() != null)
//        {
//            result.put("revision", change.getRevision().getRevisionString());
//        }
//        if(change.getAction() != null)
//        {
//            result.put("action", change.getAction().toString().toLowerCase());
//        }
//
//        return result;
//    }
//
//    public Vector<Hashtable<String, Object>> getArtifactsInBuild(String token, final String projectName, final int id) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        final Project project = internalGetProject(projectName);
//        final Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>();
//
//        buildManager.executeInTransaction(new Runnable()
//        {
//            public void run()
//            {
//                final BuildResult build = internalGetBuild(project, id);
//
//                build.getRoot().forEachNode(new UnaryFunction<RecipeResultNode>()
//                {
//                    public void process(RecipeResultNode recipeResultNode)
//                    {
//                        RecipeResult recipeResult = recipeResultNode.getBuildResult();
//                        if(recipeResult != null)
//                        {
//                            String stage = recipeResultNode.getStage();
//                            for(CommandResult commandResult: recipeResult.getCommandResults())
//                            {
//                                String command = commandResult.getCommandName();
//                                for(StoredArtifact artifact: commandResult.getArtifacts())
//                                {
//                                    result.add(convertArtifact(artifact, projectName, build, stage, command));
//                                }
//                            }
//                        }
//                    }
//                });
//            }
//        });
//
//        return result;
//    }
//
//    private Hashtable<String, Object> convertArtifact(StoredArtifact artifact, String project, BuildResult build, String stage, String command)
//    {
//        Hashtable<String, Object> result = new Hashtable<String, Object>();
//        result.put("stage", stage);
//        result.put("command", command);
//        result.put("name", artifact.getName());
//        result.put("permalink", StringUtils.join("/", "display/projects",
//                                                 StringUtils.uriComponentEncode(project),
//                                                 "builds", Long.toString(build.getNumber()),
//                                                 StringUtils.uriComponentEncode(stage),
//                                                 StringUtils.uriComponentEncode(command),
//                                                 StringUtils.uriComponentEncode(artifact.getName())));
//        return result;
//    }
//
//    public Vector<Hashtable<String, String>> getMessagesInBuild(String token, String projectName, final int id) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        final Project project = internalGetProject(projectName);
//        final Vector<Hashtable<String, String>> result = new Vector<Hashtable<String, String>>();
//
//        buildManager.executeInTransaction(new Runnable()
//        {
//            public void run()
//            {
//                final BuildResult build = internalGetBuild(project, id);
//                build.loadFeatures(configurationManager.getDataDirectory());
//                for(Feature f: build.getFeatures())
//                {
//                    result.add(convertFeature(null, null, null, null, f));
//                }
//
//                build.getRoot().forEachNode(new UnaryFunction<RecipeResultNode>()
//                {
//                    public void process(RecipeResultNode recipeResultNode)
//                    {
//                        RecipeResult recipeResult = recipeResultNode.getResult();
//                        if(recipeResult != null)
//                        {
//                            String stage = recipeResultNode.getStage();
//                            for(Feature f: recipeResult.getFeatures())
//                            {
//                                result.add(convertFeature(stage, null, null, null, f));
//                            }
//
//                            for(CommandResult commandResult: recipeResult.getCommandResults())
//                            {
//                                String command = commandResult.getCommandName();
//                                for(Feature f: commandResult.getFeatures())
//                                {
//                                    result.add(convertFeature(stage, command, null, null, f));
//                                }
//
//                                for(StoredArtifact artifact: commandResult.getArtifacts())
//                                {
//                                    String artifactName = artifact.getName();
//                                    for(StoredFileArtifact fileArtifact: artifact.getChildren())
//                                    {
//                                        String artifactPath = fileArtifact.getPath();
//                                        for(Feature f: fileArtifact.getFeatures())
//                                        {
//                                            result.add(convertFeature(stage, command, artifactName, artifactPath, f));
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                });
//            }
//        });
//
//        return result;
//    }
//
//    public Vector<Hashtable<String, String>> getErrorMessagesInBuild(String token, String projectName, final int id) throws AuthenticationException
//    {
//        return getMessagesOfLevel(token, projectName, id, Feature.Level.ERROR);
//    }
//
//    public Vector<Hashtable<String, String>> getWarningMessagesInBuild(String token, String projectName, final int id) throws AuthenticationException
//    {
//        return getMessagesOfLevel(token, projectName, id, Feature.Level.WARNING);
//    }
//
//    public Vector<Hashtable<String, String>> getInfoMessagesInBuild(String token, String projectName, final int id) throws AuthenticationException
//    {
//        return getMessagesOfLevel(token, projectName, id, Feature.Level.INFO);
//    }
//
//    private Vector<Hashtable<String, String>> getMessagesOfLevel(String token, String projectName, int id, Feature.Level level) throws AuthenticationException
//    {
//        Vector<Hashtable<String, String>> result = getMessagesInBuild(token, projectName, id);
//        Iterator<Hashtable<String, String>> it = result.iterator();
//        String levelString = level.getPrettyString();
//        while(it.hasNext())
//        {
//            Hashtable<String, String> feature = it.next();
//            if(!levelString.equals(feature.get("level")))
//            {
//                it.remove();
//            }
//        }
//
//        return result;
//    }
//
//    private Hashtable<String, String> convertFeature(String stageName, String commandName, String artifactName, String artifactPath, Feature feature)
//    {
//        Hashtable<String, String> result = new Hashtable<String, String>();
//        if(stageName != null)
//        {
//            result.put("stage", stageName);
//        }
//
//        if(commandName != null)
//        {
//            result.put("command", commandName);
//        }
//
//        if(artifactName != null)
//        {
//            result.put("artifact", artifactName);
//        }
//
//        if(artifactPath != null)
//        {
//            result.put("path", artifactPath);
//        }
//
//        result.put("level", feature.getLevel().getPrettyString());
//        result.put("message", feature.getSummary());
//        return result;
//    }
//
    public boolean triggerBuild(String token, String projectName) throws AuthenticationException
    {
        return triggerBuild(token, projectName, null);
    }

    public boolean triggerBuild(String token, String projectName, String revision) throws AuthenticationException
    {
        try
        {
            tokenManager.loginUser(token);
            Project project = internalGetProject(projectName, false);

            Revision r = null;
            if(TextUtils.stringSet(revision))
            {
                ScmClient client = null;
                try
                {
                    client = scmClientFactory.createClient(project.getConfig().getScm());
                    r = client.getRevision(revision);
                }
                catch (ScmException e)
                {
                    throw new IllegalArgumentException("Unable to verify revision: " + e.getMessage());
                }
                finally
                {
                    ScmClientUtils.close(client);
                }
            }

            projectManager.triggerBuild(project.getConfig(), new RemoteTriggerBuildReason(), r, true);
            return true;
        }
        finally
        {
            tokenManager.logoutUser();
        }
    }

//    public Vector<String> getProjectBuildSpecifications(String token, String projectName) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        Project project = internalGetProject(projectName);
//        return getNames(project.getBuildSpecifications());
//    }
//
//    public String getProjectState(String token, String projectName) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//        Project project = internalGetProject(projectName);
//        return project.getState().toString().toLowerCase();
//    }
//
//    public Hashtable<String, String> preparePersonalBuild(String token, String projectName, String buildSpecification) throws AuthenticationException
//    {
//        tokenManager.verifyRoleIn(token, GrantedAuthority.PERSONAL);
//        Project project = internalGetProject(projectName);
//        getBuildSpecification(project, buildSpecification);
//
//        Hashtable<String, String> scmDetails = new Hashtable<String, String>();
//        scmDetails.put(SCMConfiguration.PROPERTY_TYPE, project.getScm().getType());
//        scmDetails.putAll(project.getScm().getRepositoryProperties());
//        return scmDetails;
//    }
//
//    public boolean pauseProject(String token, String projectName) throws AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//            Project project = internalGetProject(projectName);
//            if (project.isPaused())
//            {
//                return false;
//            }
//            else
//            {
//                projectManager.pauseProject(project);
//                return true;
//            }
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean resumeProject(String token, String projectName) throws AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//            Project project = internalGetProject(projectName);
//            if (project.isPaused())
//            {
//                projectManager.resumeProject(project);
//                return true;
//            }
//            else
//            {
//                return false;
//            }
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean createAgent(String token, String name, String host, int port) throws AuthenticationException, LicenseException
//    {
//        tokenManager.verifyAdmin(token);
//        LicenseHolder.ensureAuthorization(LicenseHolder.AUTH_ADD_AGENT);
//
//        Slave newSlave = new Slave(name, host, port);
//        validate(newSlave);
//
//        if (!TextUtils.stringSet(name) || agentManager.agentExists(name))
//        {
//            throw new IllegalArgumentException(String.format("An agent by the name '%s' already exists. Please select a different name.", name));
//        }
//
//        agentManager.addSlave(new Slave(name, host, port));
//        return true;
//    }
//
///* Refactor DeleteAgentAction, but first, understand what is going on with agents and slaves.
//    public boolean deleteAgent(String token, String name) throws AuthenticationException
//    {
//        try
//        {
//            AcegiUtils.loginAs(tokenManager.verifyAdmin(token));
//            if (!agentManager.agentExists(name))
//            {
//                throw new IllegalArgumentException(String.format("No agent by the name '%s' exists. Please select a different name.", name));
//            }
//
//        }
//        finally
//        {
//            AcegiUtils.logout();
//        }
//    }
//*/
//
//    public String getAgentStatus(String token, String name) throws AuthenticationException
//    {
//        tokenManager.verifyUser(token);
//
//        Agent agent = agentManager.getAgent(name);
//        if (agent == null)
//        {
//            return "";
//        }
//
//        return agent.getStatus().getPrettyString();
//    }
//
//    public boolean enableAgent(String token, String name) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//
//        Agent agent = agentManager.getAgent(name);
//        if (agent == null)
//        {
//            throw new IllegalArgumentException("Unknown agent '" + name + "'");
//        }
//
//        if(agent.isSlave())
//        {
//            agentManager.enableSlave(agent.getId());
//        }
//        else
//        {
//            agentManager.enableMasterAgent();
//        }
//
//        return true;
//    }
//
//    public boolean disableAgent(String token, String name) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//
//        Agent agent = agentManager.getAgent(name);
//        if (agent == null)
//        {
//            throw new IllegalArgumentException("Unknown agent '" + name + "'");
//        }
//
//        if(agent.isSlave())
//        {
//            agentManager.disableSlave(agent.getId());
//        }
//        else
//        {
//            agentManager.disableMasterAgent();
//        }
//
//        return true;
//    }

    public boolean shutdown(String token, boolean force, boolean exitJvm) throws AuthenticationException
    {
        tokenManager.verifyAdmin(token);

        // Sigh ... this is tricky, because if we shutdown here Jetty dies
        // before this request is complete and the client gets an error :-|.
        shutdownManager.delayedShutdown(force, exitJvm);
        return true;
    }

    public boolean stopService(String token) throws AuthenticationException
    {
        tokenManager.verifyAdmin(token);
        shutdownManager.delayedStop();
        return true;
    }

//    /**
//     * Updates the specified users password.
//     *
//     * @param token    used to authenticate the request.
//     * @param login    name identifying the user whose password is being set.
//     * @param password is the new password.
//     * @return true if the request was successful, false otherwise.
//     * @throws AuthenticationException if the token does not authorise administrator access.
//     */
//    public boolean setPassword(String token, String login, String password) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//
//        User user = userManager.getUser(login);
//        if (user == null)
//        {
//            throw new IllegalArgumentException(String.format("Unknown username '%s'", login));
//        }
//        userManager.setPassword(user, password);
//        userManager.save(user);
//        return true;
//    }
//
//    /**
//     * Create a new user.
//     *
//     * @param user      is a map of containing the users details.
//     * @param token     used to authenticate the request
//     *
//     * @return true if the request is successful
//     *
//     * @throws AuthenticationException if you are not authorised to execute this action.
//     * @throws LicenseException        if you are not licensed to execute this action.
//     */
//    public boolean createUser(String token, Hashtable<String, Object> user) throws AuthenticationException, LicenseException
//    {
//        tokenManager.verifyAdmin(token);
//        LicenseHolder.ensureAuthorization(LicenseHolder.AUTH_ADD_USER);
//
//        // validate the user details.
//        String login = (String) user.get("login");
//        User existingUser = userManager.getUser(login);
//        if (existingUser != null)
//        {
//            throw new IllegalArgumentException(String.format("A user with the login '%s' already exists. Please select a different login.", login));
//        }
//
//        User instance = new User();
//        instance.setLogin(login);
//        instance.setName((String) user.get("name"));
//
//        userManager.addUser(instance, false, false);
//        return true;
//    }
//
//    /**
//     * Delete the specified user.
//     *
//     * @param token used to authenticate the request.
//     * @param login identifies the user to be deleted.
//     * @return true if the request is successful, false otherwise.
//     * @throws AuthenticationException is you are not authorised to execute this request.
//     *
//     * @throws IllegalArgumentException if no user with the specified login exists.
//     */
//    public boolean deleteUser(String token, String login) throws AuthenticationException, IllegalArgumentException
//    {
//        tokenManager.verifyAdmin(token);
//
//        User user = userManager.getUser(login);
//        if (user == null)
//        {
//            throw new IllegalArgumentException(String.format("Unknown user login: '%s'", login));
//        }
//        userManager.delete(user);
//        return true;
//    }
//
//    /**
//     * Create a new project.
//     *
//     * @param token     used to authenticate the request.
//     * @param project   the project details
//     * @param scm       the scm details
//     * @param type      the project type details
//     *
//     * @return true if the request is successful, false otherwise.
//     *
//     * @throws AuthenticationException  if you are not authorised to execute this action.
//     * @throws LicenseException         if you are not licensed to execute this action.
//     * @throws IllegalArgumentException      if a validation error is detected.
//     */
//    public boolean createProject(String token, Hashtable<String, Object> project, Hashtable<String, Object> scm, Hashtable<String, Object> type) throws AuthenticationException, LicenseException, IllegalArgumentException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            String name = (String) project.get("name");
//            Project existingProject = projectManager.getProject(name);
//            if (existingProject != null)
//            {
//                throw new IllegalArgumentException(String.format("A project with the name '%s' already exists. Please use a different name.", name));
//            }
//
//            Project newProject = new Project();
//            setProperties(project, newProject);
//            validate(newProject);
//
//            // lookup scm.
//            Scm newScm = createScm(scm);
//            validate(newScm);
//            newProject.setScm(newScm);
//
//            // set the details.
//            PulseFileDetails newType = createFileDetails(type);
//            validate(newType);
//            newProject.setPulseFileDetails(newType);
//
//            // set the details.
//            projectManager.create(newProject);
//
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    /**
//     * Delete the specified project.
//     *
//     * @param token used to authenticate the request.
//     * @param name  the name of the project to be deleted.
//     * @return true if the request is successful, false otherwise.
//     *
//     * @throws AuthenticationException if you are not authorised to execute this request.
//     *
//     * @throws IllegalArgumentException if no project with the specified name exists.
//     */
//    public boolean deleteProject(String token, String name) throws AuthenticationException, IllegalArgumentException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            projectManager.delete(project);
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean editProject(String token, String name, Hashtable<String, Object> projectDetails) throws AuthenticationException, IllegalArgumentException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            // are we changing the name of the project? if so, then we need to check that the new name is not already in use.
//            if (projectDetails.containsKey("name"))
//            {
//                String newName = (String) projectDetails.get("name");
//                if (!name.equals(newName) && projectManager.getProject(newName) != null)
//                {
//                    throw new IllegalArgumentException(String.format("The name '%s' is already in use by another project. Please select a different name.", newName));
//                }
//            }
//
//            setProperties(projectDetails, project);
//            validate(project);
//
//            projectManager.save(project);
//
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    /**
//     * Get the details for the specified project.
//     *
//     * @param name is the name of the project to be retrieved.
//     * @param token used to authenticate the request.
//     *
//     * @return a mapping of the projects details.
//     *
//     * @throws IllegalArgumentException if the specified name does not reference a project.
//     * @throws AuthenticationException if you are not authorised to execute this request.
//     */
//    public Hashtable<String, Object> getProject(String token, String name) throws IllegalArgumentException, AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            Hashtable<String, Object> details = extractDetails(project);
//            // add the scm and type details.
//            details.put("scm", project.getScm().getType());
//            details.put("type", project.getPulseFileDetails().getType());
//
//            return details;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public Hashtable<String, Object> cloneProject(String token, String name, String cloneName, String cloneDescription) throws IllegalArgumentException, AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            if(projectManager.getProject(cloneName) != null)
//            {
//                throw new IllegalArgumentException(String.format("The name '%s' is already in use by another project. Please select a different name.", name));
//            }
//
//            Project clone = projectManager.cloneProject(project, cloneName, cloneDescription);
//            Hashtable<String, Object> details = extractDetails(clone);
//            // add the scm and type details.
//            details.put("scm", clone.getScm().getType());
//            details.put("type", clone.getPulseFileDetails().getType());
//
//            return details;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//
//    }
//
//    /**
//     * Retrieve the scm details for the specified project.
//     *
//     * @param token         used to authorise this request.
//     * @param projectName   the name of the project for which the scm details are being retrieved.
//     *
//     * @return a scm structure. The contents of this are specific to the type of scm.
//     *
//     * @throws AuthenticationException if you are not authorised to execute this request.
//     */
//    public Hashtable<String, Object> getScm(String token, String projectName) throws AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(projectName);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", projectName));
//            }
//
//            return extractDetails(project.getScm());
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean editScm(String token, String name, Hashtable<String, Object> scmDetails) throws AuthenticationException, IllegalArgumentException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            setProperties(scmDetails, project.getScm());
//            validate(project.getScm());
//
//            projectManager.save(project);
//
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public Hashtable<String, Object> getProjectType(String token, String name) throws AuthenticationException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            return extractDetails(project.getPulseFileDetails());
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    public boolean editProjectType(String token, String name, Hashtable<String, Object> specifics) throws AuthenticationException, IllegalArgumentException
//    {
//        try
//        {
//            tokenManager.loginUser(token);
//
//            Project project = projectManager.getProject(name);
//            if (project == null)
//            {
//                throw new IllegalArgumentException(String.format("Unknown project name: '%s'", name));
//            }
//
//            setProperties(specifics, project.getPulseFileDetails());
//            validate(project.getPulseFileDetails());
//
//            projectManager.save(project);
//
//            return true;
//        }
//        finally
//        {
//            tokenManager.logoutUser();
//        }
//    }
//
//    /**
//     * Deletes all commit message links, primarily for testing purposes.
//     *
//     * @param token used to authenticate the request
//     * @return the number of commit message links deleted
//     * @throws AuthenticationException if the token does not authorise administrator access
//     */
//    public int deleteAllCommitMessageLinks(String token) throws AuthenticationException
//    {
//        tokenManager.verifyAdmin(token);
//        List<CommitMessageTransformer> transformers = transformerManager.getCommitMessageTransformers();
//        int result = transformers.size();
//        for (CommitMessageTransformer t : transformers)
//        {
//            transformerManager.delete(t);
//        }
//        return result;
//    }
//
//    private <T extends NamedEntity> Vector<String> getNames(Collection<T> entities)
//    {
//        Vector<String> result = new Vector<String>(entities.size());
//        for (NamedEntity t: entities)
//        {
//            result.add(t.getName());
//        }
//        return result;
//    }
//
//    private Hashtable<String, Object> extractDetails(Object obj)
//    {
//        if (!structDefs.containsKey(obj.getClass()))
//        {
//            throw new RuntimeException(String.format("Object of type '%s' is not supported by the remote interface.", obj.getClass().getName()));
//        }
//
//        Hashtable<String, Object> details = new Hashtable<String, Object>();
//        for (String property : structDefs.get(obj.getClass()))
//        {
//            try
//            {
//                Object value = Ognl.getValue(property, obj);
//                if (value != null)
//                {
//                    TypeSqueezer squeezer = Squeezers.findSqueezer(value.getClass());
//                    details.put(property, squeezer.squeeze(value));
//                }
//            }
//            catch (OgnlException e)
//            {
//                e.printStackTrace();
//            }
//            catch (SqueezeException e)
//            {
//                e.printStackTrace();
//            }
//        }
//        return details;
//    }
//
//    private void validate(Object o) throws IllegalArgumentException
//    {
//        ValidationContext ctx = new PulseValidationContext(o);
//        try
//        {
//            validationManager.validate(o, ctx);
//        }
//        catch (ValidationException e)
//        {
//            throw new IllegalArgumentException(e.getMessage());
//        }
//        if (ctx.hasErrors())
//        {
//            if (ctx.hasFieldErrors())
//            {
//                String field = ctx.getFieldErrors().keySet().iterator().next();
//                String message = ctx.getFieldErrors(field).iterator().next();
//                throw new IllegalArgumentException(String.format("Field %s is invalid. Reason: %s", field, message));
//            }
//            if (ctx.hasActionErrors())
//            {
//                String message = ctx.getActionErrors().iterator().next();
//                throw new IllegalArgumentException(String.format("The following error occured validating your request: %s", message));
//            }
//        }
//    }
//
//
//    private void setProperties(Hashtable<String, Object> scmDetails, Object object)
//    {
//        OgnlUtils.setProperties(scmDetails, object);
//    }

    private Project internalGetProject(String projectName, boolean allownIvalid)
    {
        Project project = projectManager.getProject(projectName, allownIvalid);
        if (project == null)
        {
            throw new IllegalArgumentException("Unknown project '" + projectName + "'");
        }
        return project;
    }

//    private ProjectConfiguration internalGetProjectConfig(String projectName)
//    {
//        ProjectConfiguration project = projectManager.getProjectConfig(projectName);
//        if (project == null)
//        {
//            throw new IllegalArgumentException("Unknown project '" + projectName + "'");
//        }
//        return project;
//    }
//
//    private List<Project> getProjectList(Vector<String> projects)
//    {
//        List<Project> members = new ArrayList<Project>(projects.size());
//        for(String s: projects)
//        {
//            members.add(internalGetProject(s));
//        }
//        return members;
//    }
//
//    private BuildSpecification getBuildSpecification(Project project, String buildSpecification)
//    {
//        BuildSpecification spec;
//        if(TextUtils.stringSet(buildSpecification))
//        {
//             spec = project.getBuildSpecification(buildSpecification);
//        }
//        else
//        {
//            spec = project.getDefaultSpecification();
//        }
//
//        if (spec == null)
//        {
//            throw new IllegalArgumentException("Unknown build specification '" + buildSpecification + "'");
//        }
//
//        return spec;
//    }

    public void setTokenManager(TokenManager tokenManager)
    {
        this.tokenManager = tokenManager;
    }

    public void setShutdownManager(ShutdownManager shutdownManager)
    {
        this.shutdownManager = shutdownManager;
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }


    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }

//    public void setUserManager(UserManager userManager)
//    {
//        this.userManager = userManager;
//    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

//    public void setAgentManager(AgentManager agentManager)
//    {
//        this.agentManager = agentManager;
//    }
//
//    public void setValidationManager(ValidationManager validationManager)
//    {
//        this.validationManager = validationManager;
//    }
//
//    public void setCommitMessageTransformerManager(CommitMessageTransformerManager manager)
//    {
//        this.transformerManager = manager;
//    }
//
//    public void setAuthenticationManager(AuthenticationManager authenticationManager)
//    {
//        this.authenticationManager = authenticationManager;
//    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
        eventManager.register(this);
    }

    public void handleEvent(Event evt)
    {
        // Rewire on startup to get the full token manager.
        ComponentContext.autowire(this);
        eventManager.unregister(this);
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{SystemStartedEvent.class};
    }

    public void setRecordManager(RecordManager recordManager)
    {
        this.recordManager = recordManager;
    }

    public void setConfigurationSecurityManager(ConfigurationSecurityManager configurationSecurityManager)
    {
        this.configurationSecurityManager = configurationSecurityManager;
    }

    public void setScmClientFactory(ScmClientFactory<ScmConfiguration> scmClientFactory)
    {
        this.scmClientFactory = scmClientFactory;
    }
}
