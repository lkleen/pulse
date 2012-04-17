package com.zutubi.pulse.master.notifications.renderer;

import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.notifications.NotificationAttachment;

import java.util.List;
import java.util.Map;

/**
 * Abstracts services that can render a build result for notifications.  This
 * service sits on top of a {@link BuildResultRenderer}.
 */
public interface RenderService
{
    /**
     * Returns the data map that would be used to render the given buildResult.
     *
     * @param buildResult  the result to be rendered
     * @param baseUrl      the configured base url for the pulse instance
     * @return the data map that would be used for rendering a template for
     *         this build result
     */
    Map<String, Object> getDataMap(BuildResult buildResult, String baseUrl);

    /**
     * Renders the given build result using the given details.
     *
     * @param buildResult         the result to be rendered
     * @param baseUrl             configured base url for the pulse instance
     * @param template            name of the template to use
     * @return a rendered result
     */
    RenderedResult renderResult(BuildResult buildResult, String baseUrl, String template);

    /**
     * Returns a set of build stage log file attachments based on the given configuration.
     *
     *
     * @param buildResult          the build to get the logs from
     * @param attachLogs           a flag to indicate if logs should be attached (if false the empty list is returned)
     * @param logLineLimit         a limit on the lines per log, if the log is larger only the tail is attached
     * @param includeBuildLog      if true, include the build's own log along with the stage logs
     * @return a list of log file attachments for the build
     */
    List<NotificationAttachment> getAttachments(BuildResult buildResult, boolean attachLogs, int logLineLimit, boolean includeBuildLog);
}
