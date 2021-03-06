/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.model.persistence;

import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.pulse.master.model.User;

import java.util.Date;
import java.util.List;

/**
 * Provides access to build results and the sub results contained within them.
 */
public interface BuildResultDao extends EntityDao<BuildResult>
{
    void save(RecipeResultNode node);

    void save(RecipeResult result);

    void save(CommandResult result);

    List<BuildResult> queryBuilds(Project[] projects, ResultState[] states, String[] statuses, long earliestStartTime, long latestStartTime, int first, int max, boolean mostRecentFirst, boolean includePinned);

    List<BuildResult> queryBuilds(Project[] projects, ResultState[] states, long earliestStartTime, long latestStartTime, int first, int max, boolean mostRecentFirst);

    List<BuildResult> queryBuildsWithMessages(Project[] projects, Feature.Level level, int max);

    List<BuildResult> findLatestByProject(Project project, int max);

    List<BuildResult> findSinceByProject(Project project, Date since);

    List<BuildResult> findLatestByProject(Project project, int first, int max);

    List<BuildResult> findLatestByProject(Project project, ResultState[] states, int first, int max);

    List<BuildResult> findLatestCompleted(Project project, int first, int max);

    /**
     * Returns all builds for the given projects that completed after the given
     * time stamp.
     *
     * @param projects  the projects to find builds of
     * @param sinceTime time in milliseconds since the epoch to restrict
     *                  completed times to
     * @return all completed builds of the given projects with completion times
     *         after the given time
     */
    List<BuildResult> findCompletedSince(Project[] projects, long sinceTime);

    BuildResult findPreviousBuildResult(BuildResult result);

    BuildResult findPreviousBuildResultWithRevision(final BuildResult result, final ResultState[] states);

    List<BuildResult> findOldestByProject(Project project, ResultState[] states, int max, boolean includePersonal);

    BuildResult findByProjectAndNumber(long projectId, final long number);

    BuildResult findByUserAndNumber(User user, long id);

    CommandResult findCommandResult(long id);

    RecipeResult findRecipeResult(long id);

    long getBuildCount(Project project, ResultState[] states);

    long getBuildCount(Project[] projects, ResultState[] states);

    long getBuildCount(Project project, ResultState[] states, String[] statuses, boolean includePinned);

    long getBuildCount(Project project, long after, long upTo);

    List<BuildResult> queryBuilds(Project project, ResultState[] states, long lowestNumber, long highestNumber, int first, int max, boolean mostRecentFirst, boolean initialise);

    List<BuildResult> findByUser(User user);

    List<BuildResult> getLatestByUser(User user, ResultState[] states, int max);

    int getCompletedResultCount(User user);

    /**
     * Retrieve the oldest completed builds for the specified user.  The limit indicates how many
     * builds should be skipped before we start returning builds.
     * <p/>
     * For example, if a user has 10 builds, and the limit is 3, then the 7 oldest builds are
     * returned by this method.
     *
     * @param user      that triggered the builds.
     * @param offset     the number of the newest builds to skip from the resulting list.
     *
     * @return a list of old personal build results. 
     */
    List<BuildResult> getOldestCompletedBuilds(User user, int offset);

    RecipeResultNode findResultNodeByResultId(long id);

    BuildResult findLatest(ResultState... inStates);

    CommandResult findCommandResultByArtifact(long artifactId);

    BuildResult findLatestByProject(Project project, boolean initialise, ResultState... inStates);

    int getBuildCountByAgentName(String agent, Project[] projects, ResultState[] states);

    List<BuildResult> findLatestByAgentName(String agent, Project[] projects, ResultState[] states, int first, int max);

    /**
     * Finds the build result that contains the recipe with the given id.
     *
     * @param id id of the recipe result to find the build for
     * @return the build containing the recipe, or null if not found
     */
    BuildResult findByRecipeId(long id);

    /**
     * Find the build of the specified project that was part of the
     * metabuild.
     *
     * @param project       the project associated with the requested build
     * @param metaBuildId   the metabuild id of the requested build.
     * @param states        the allowed states of the builds.  If no states are specified,
     * all states are returned.
     * @return the build result, or null if none could be located.
     */
    BuildResult findByProjectAndMetabuildId(Project project, long metaBuildId, ResultState... states);

    /**
     * Find the build results preceeding the specified build id.  All of the builds
     * will belong to the same project.
     *
     * @param buildId       the build id
     * @param maxResults    the maximum number of results to be returned.
     * @param states        filter the build results to include builds with the specified states.
     *                      Not specifying any states is equivalent to specifying all states.
     * 
     * @return a list of build results, in order of ascending ids.
     */
    List<BuildResult> findByBeforeBuild(final long buildId, final int maxResults, final ResultState... states);

    /**
     * Find the build results immediately after the specified build id.  All of the
     * builds will belong to the same project.
     *
     * @param buildId       the build id
     * @param maxResults    the maximum number of results to be returned.
     * @param states        filter the build results to include builds with the specified states.
     *                      Not specifying any states is equivalent to specifying all states.
     *
     * @return a list of build results, in order of ascending ids.
     */
    List<BuildResult> findByAfterBuild(final long buildId, final int maxResults, final ResultState... states);

    /**
     * Find the latest build result that belongs to the same project as the specified build.
     *
     * @param buildId   the build id
     * @param states        filter the build results to include builds with the specified states.
     *                      Not specifying any states is equivalent to specifying all states.
     * 
     * @return a build result for the latest build.
     */
    BuildResult findByLatestBuild(final long buildId, final ResultState... states);
}
