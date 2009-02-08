package com.zutubi.pulse.web.project;

import com.zutubi.pulse.committransformers.CommitMessageBuilder;
import com.zutubi.pulse.core.model.Changelist;
import com.zutubi.pulse.model.CommitMessageTransformer;
import com.zutubi.pulse.model.persistence.ChangelistDao;
import com.zutubi.pulse.util.logging.Logger;

import java.util.List;

/**
 */
public class CommitMessageHelper
{
    private static final Logger LOG = Logger.getLogger(CommitMessageHelper.class);
    
    private List<CommitMessageTransformer> transformers;
    private ChangelistDao changelistDao;

    public CommitMessageHelper(List<CommitMessageTransformer> transformers, ChangelistDao changelistDao)
    {
        this.transformers = transformers;
        this.changelistDao = changelistDao;
    }

    public String applyTransforms(Changelist changelist)
    {
        return applyTransforms(changelist, 0);
    }
    
    public String applyTransforms(Changelist changelist, int limit)
    {
        String s = changelist.getComment();
        try
        {
            CommitMessageBuilder builder = new CommitMessageBuilder(s);

            for(CommitMessageTransformer transformer: transformers)
            {
                if(transformer.appliesToChangelist(changelistDao.getAllAffectedProjectIds(changelist)))
                {
                    builder = transformer.transform(builder);
                }
            }

            if (limit > 0)
            {
                builder.trim(limit);
            }
            else
            {
                builder.wrap(80);
            }

            builder.encode();

            return builder.toString();
        }
        catch (Exception e)
        {
            LOG.warning(String.format("Failed to process changelist comment. Cause: %s", e.getMessage()), e);
            return s;
        }
    }
}