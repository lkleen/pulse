package com.zutubi.pulse.core.postprocessors.test;

import com.zutubi.pulse.core.postprocessors.api.PostProcessor;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorConfiguration;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;

/**
 */
@SymbolicName("zutubi.testPostProcessorConfig")
public class TestPostProcessorConfiguration extends AbstractNamedConfiguration implements PostProcessorConfiguration
{
    public Class<? extends PostProcessor> processorType()
    {
        return TestPostProcessor.class;
    }
}