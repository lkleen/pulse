package com.zutubi.pulse.core.commands.api;

import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorConfiguration;

import java.io.File;
import java.util.List;

/**
 * Context in which a command executes.  Provides a high-level interface for
 * recording command result information and registering captured output.
 */
public interface CommandContext
{
    /**
     * Returns the execution context for the build in which this command is
     * running.
     *
     * @return the current build's context
     */
    ExecutionContext getExecutionContext();

    /**
     * Indicates the current status of the executing command.
     *
     * @return the command's current status
     */
    ResultState getResultState();

    /**
     * Fails the current command, and records the given message as an error
     * feature.  Failures are used to report normal build problems, e.g.
     * a compile error.
     *
     * @param message error message indicating the failure reason
     */
    void failure(String message);

    /**
     * Errors the current command, and records the given message as an error
     * feature.  Errors are used for circumstances outside normal build
     * failure, e.g. resource exhaustion.
     *
     * @param message message indicating the error reason
     */
    void error(String message);

    /**
     * Adds the given feature to the command result.  If the feature indicates
     * a terminal problem, consider using {@link #failure(String)} or
     * {@link #error(String)} instead.
     *
     * @param feature the feature to add
     */
    void addFeature(Feature feature);

    /**
     * Adds a custom property to the command result, which may be reported in
     * a detailed view of the result.  For example, for a GNU make build a
     * property "makefile" may be added with a value of the makefile path.
     * <p/>
     * These properties are intended to help the end user understand all
     * details of the command that was executed.
     *
     * @param name  name of the property to add
     * @param value the value of the property
     */
    void addCommandProperty(String name, String value);

    /**
     * Registers a link output (a link to an external resource) with the
     * command result.
     *
     * @param name name of the output
     * @param url  url of the external resource to link to
     */
    void registerLink(String name, String url);

    /**
     * Registers a local output with the given name, returning the directory
     * to which all files in the output should be stored.  This is used, for
     * example, to capture an output file from the build for post-processing
     * and/or permanent storage.  All files written under the returned
     * directory will be captured as part of the output.
     *
     * @param name the name of the output, e.g. "my report"
     * @param type the MIME type of the captured files, or null if their types
     *             are unknown and should be guessed
     * @return the directory to which all files that are part of this output
     *         should be captured
     *
     * @see #registerProcessors(String, java.util.List)
     * @see #setOutputIndex(String, String)
     */
    File registerOutput(String name, String type);

    /**
     * Registers post-processors that should be applied to all files in the
     * output with the given name.  The output should have been previously
     * registered via {@link #registerOutput(String, String)}.  The processors
     * are applied after the command completes to all files captured under the
     * given output.
     *
     * @param name           name of the output to process files from
     * @param postProcessors the post-processors to apply to the given output
     */
    void registerProcessors(String name, List<PostProcessorConfiguration> postProcessors);

    /**
     * Sets the index file for a registered output.  When an index file is
     * set, the output will be treated as an HTML report, with the index
     * being the default file to view.  For common cases, e.g. where an
     * index file of "index.html" is present, it is not necessary to
     * explicitly register the index (it will be guessed automatically).
     *
     * @param name  name of the output to set the index file for
     * @param index path of the index file, relative to the output's directory
     */
    void setOutputIndex(String name, String index);
}
