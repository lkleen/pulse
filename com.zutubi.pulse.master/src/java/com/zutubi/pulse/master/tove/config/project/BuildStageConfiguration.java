package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.master.AgentService;
import com.zutubi.pulse.master.RecipeAssignmentRequest;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.tove.annotations.*;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A build stage is a component of a build that represents the execution of a
 * single recipe on an agent.  Stages execute independently and in parallel
 * where possible, and the build result is the aggregation of all stage
 * results.
 */
@SymbolicName("zutubi.stageConfig")
@Form(fieldOrder = {"name", "recipe", "agent"})
@Table(columns = {"name", "recipe"})
@Wire
public class BuildStageConfiguration extends AbstractNamedConfiguration
{
    private static final Logger LOG = Logger.getLogger(BuildStageConfiguration.class);

    @Reference(optionProvider = "BuildStageAgentOptionProvider")
    private AgentConfiguration agent;
    @Select(optionProvider = "BuildStageRecipeOptionProvider", editable = true)
    private String recipe;
    @Ordered
    private Map<String, ResourcePropertyConfiguration> properties = new LinkedHashMap<String, ResourcePropertyConfiguration>();
    private List<ResourceRequirementConfiguration> requirements = new LinkedList<ResourceRequirementConfiguration>();

    @Transient
    private ObjectFactory objectFactory;

    private String publicationPattern = "build/[artifact].[ext]";
    private String retrievalPattern = "lib/[artifact].[ext]";

    private List<PublicationConfiguration> publications = new LinkedList<PublicationConfiguration>();

    public BuildStageConfiguration()
    {
    }

    public BuildStageConfiguration(String name)
    {
        super(name);
    }

    public AgentConfiguration getAgent()
    {
        return agent;
    }

    public void setAgent(AgentConfiguration agent)
    {
        this.agent = agent;
    }

    public String getRecipe()
    {
        return this.recipe;
    }

    public void setRecipe(String recipe)
    {
        this.recipe = recipe;
    }

    public Map<String, ResourcePropertyConfiguration> getProperties()
    {
        return this.properties;
    }

    public void setProperties(Map<String, ResourcePropertyConfiguration> properties)
    {
        this.properties = properties;
    }

    public ResourcePropertyConfiguration getProperty(String name)
    {
        return this.properties.get(name);
    }

    public List<ResourceRequirementConfiguration> getRequirements()
    {
        return requirements;
    }

    public void setRequirements(List<ResourceRequirementConfiguration> requirements)
    {
        this.requirements = requirements;
    }

    public String getPublicationPattern()
    {
        return publicationPattern;
    }

    public void setPublicationPattern(String publicationPattern)
    {
        this.publicationPattern = publicationPattern;
    }

    public String getRetrievalPattern()
    {
        return retrievalPattern;
    }

    public void setRetrievalPattern(String retrievalPattern)
    {
        this.retrievalPattern = retrievalPattern;
    }

    public List<PublicationConfiguration> getPublications()
    {
        return publications;
    }

    public void setPublications(List<PublicationConfiguration> publications)
    {
        this.publications = publications;
    }

    @Transient
    public AgentRequirements getAgentRequirements()
    {
        try
        {
            if(agent == null)
            {
                return objectFactory.buildBean(AnyCapableAgentRequirements.class);
            }
            else
            {
                return objectFactory.buildBean(SpecificAgentRequirements.class, new Class[]{ AgentConfiguration.class }, new Object[]{ agent });
            }
        }
        catch (Exception e)
        {
            LOG.severe(e);
            return new AgentRequirements()
            {
                public String getSummary()
                {
                    return "[none]";
                }

                public boolean fulfilledBy(RecipeAssignmentRequest request, AgentService service)
                {
                    return false;
                }
            };
        }
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }
}
