/*
 * Copyright 2018 Keval Patel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kevalpatel2106.greenbuild.travisInterface

import android.content.Context
import com.kevalpatel2106.ci.greenbuild.base.account.Account
import com.kevalpatel2106.ci.greenbuild.base.application.BaseApplication
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Branch
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.CiServer
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.Page
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.ServerInterface
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Build
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.BuildSortBy
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.BuildState
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Cache
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Cron
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.EnvVars
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Repo
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.RepoSortBy
import com.kevalpatel2106.ci.greenbuild.base.network.NetworkApi
import com.kevalpatel2106.greenbuild.travisInterface.authentication.TravisAuthenticationActivity
import io.reactivex.Observable
import timber.log.Timber

/**
 * Created by Keval on 16/04/18.
 *
 * @author <a href="https://github.com/kevalpatel2106">kevalpatel2106</a>
 */
class TravisServerInterface internal constructor(
        private val application: BaseApplication,
        private val baseUrl: String,
        accessToken: String
) : ServerInterface(accessToken) {

    private val travisEndpoints = NetworkApi(application, accessToken)
            .getRetrofitClient(baseUrl)
            .create(TravisEndpoints::class.java)

    companion object {

        fun get(application: BaseApplication, baseUrl: String, accessToken: String): TravisServerInterface? {

            return when {
                baseUrl == Constants.TRAVIS_CI_ORG -> {
                    TravisServerInterface(
                            application = application,
                            baseUrl = Constants.TRAVIS_CI_ORG,
                            accessToken = accessToken
                    )
                }
                baseUrl == Constants.TRAVIS_CI_COM -> {
                    TravisServerInterface(
                            application = application,
                            baseUrl = Constants.TRAVIS_CI_COM,
                            accessToken = accessToken
                    )
                }
                baseUrl.startsWith("https://travis.") && baseUrl.endsWith("/api/") -> {
                    TravisServerInterface(
                            application = application,
                            baseUrl = baseUrl,
                            accessToken = accessToken
                    )
                }
                else -> {
                    Timber.i("Not a travis ci server: $baseUrl")
                    null
                }
            }
        }

        fun getCiServers(activity: Context): ArrayList<CiServer> {
            val ciServers = ArrayList<CiServer>(3)

            ciServers.add(CiServer(
                    icon = R.drawable.logo_travis_ci_org,
                    name = activity.getString(R.string.ci_provider_name_travis_org),
                    description = activity.getString(R.string.ci_provider_description_travis_org),
                    domain = activity.getString(R.string.travis_org_domain),
                    onClick = {
                        TravisAuthenticationActivity.launch(
                                context = activity,
                                options = it,
                                ciName = "Travis CI",
                                ciIcon = R.drawable.logo_travis_ci_org,
                                baseUrl = Constants.TRAVIS_CI_ORG
                        )
                    }))
            ciServers.add(CiServer(
                    icon = R.drawable.logo_travis_ci_com,
                    name = activity.getString(R.string.ci_provider_name_travis_com),
                    description = activity.getString(R.string.ci_provider_description_travis_com),
                    domain = activity.getString(R.string.travis_com_domain),
                    onClick = {
                        TravisAuthenticationActivity.launch(
                                context = activity,
                                options = it,
                                ciName = "Travis CI",
                                ciIcon = R.drawable.logo_travis_ci_com,
                                baseUrl = Constants.TRAVIS_CI_COM
                        )
                    }))
            ciServers.add(CiServer(
                    icon = R.drawable.logo_travis_ci_enterprice,
                    name = activity.getString(R.string.ci_provider_name_travis_enterprice),
                    description = activity.getString(R.string.ci_provider_description_travis_enterprice),
                    domain = null,
                    onClick = {
                        TravisAuthenticationActivity.launch(
                                context = activity,
                                options = it,
                                ciName = "Travis CI (Enterprise)",
                                ciIcon = R.drawable.logo_travis_ci_enterprice,
                                baseUrl = null
                        )
                    }))

            return ciServers
        }
    }

    override fun getBaseUrl(): String {
        return baseUrl
    }

    /**
     * Get the information of the user based on the [accessToken] provided. This method will use
     * [TravisEndpoints.getMyProfile] endpoint to get the user information.
     *
     * @see [TravisEndpoints.getMyProfile]
     */
    override fun getMyAccount(): Observable<Account> {
        return travisEndpoints
                .getMyProfile()
                .map { it.getAccount(baseUrl, accessToken) }
    }

    /**
     * Get the branches for the repo.
     */
    override fun getBranches(page: Int,
                             repoId: String): Observable<Page<Branch>> {
        return travisEndpoints
                .getBranches(
                        repoId = repoId,
                        offset = (page - 1) * PAGE_SIZE
                ).map {
                    val branchList = ArrayList<Branch>(it.branches.size)
                    it.branches.forEach { branchList.add(it.toBranch()) }
                    return@map Page(
                            hasNext = !it.pagination.isLast,
                            list = branchList
                    )
                }
    }

    override fun getRepoList(page: Int,
                             repoSortBy: RepoSortBy,
                             showOnlyPrivate: Boolean): Observable<Page<Repo>> {

        val sortByQuery = when (repoSortBy) {
            RepoSortBy.NAME_ASC -> "name"
            RepoSortBy.NAME_DESC -> "name:desc"
            RepoSortBy.LAST_BUILD_TIME_ASC -> "default_branch.last_build"
            RepoSortBy.LAST_BUILD_TIME_DESC -> "default_branch.last_build:desc"
        }

        return travisEndpoints
                .getMyRepos(
                        sortBy = sortByQuery,
                        onlyActive = true,
                        offset = (page - 1) * PAGE_SIZE)
                .map {
                    val repoList = ArrayList<Repo>(it.repositories.size)
                    it.repositories.forEach {
                        repoList.add(it.apply {
                            lastBuild?.repoName = null
                            lastBuild?.ownerName = null
                        }.toRepo())
                    }
                    return@map Page(
                            hasNext = !it.pagination.isLast,
                            list = repoList
                    )
                }
    }

    /**
     * Get the list of recent [Build].
     */
    override fun getRecentBuildsList(
            page: Int,
            repoSortBy: BuildSortBy,
            buildState: BuildState?
    ): Observable<Page<Build>> {
        val sortByQuery = when (repoSortBy) {
            BuildSortBy.STARTED_AT_ASC -> "started_at"
            BuildSortBy.STARTED_AT_DESC -> "started_at:desc"
            BuildSortBy.FINISHED_AT_ASC -> "finished_at"
            BuildSortBy.FINISHED_AT_DESC -> "finished_at:desc"
        }

        val state = when (buildState) {
            BuildState.CANCELED -> Constants.CANCEL_BUILD
            BuildState.PASSED -> Constants.PASSED_BUILD
            BuildState.RUNNING -> Constants.RUNNING_BUILD
            BuildState.FAILED -> Constants.FAILED_BUILD
            BuildState.ERRORED -> Constants.ERRORED_BUILD
            BuildState.BOOTING -> Constants.BOOTING_BUILD
            null -> null
            else -> null
        }

        return travisEndpoints
                .getRecentBuilds(
                        sortBy = sortByQuery,
                        offset = (page - 1) * PAGE_SIZE,
                        state = state
                ).map {
                    val buildList = ArrayList<Build>(it.builds.size)
                    it.builds.forEach {
                        buildList.add(it.apply {
                            repoName = it.repository.name
                            ownerName = it.createdBy.login  //TODO
                        }.toBuild())
                    }
                    return@map Page(
                            hasNext = !it.pagination.isLast,
                            list = buildList
                    )
                }
    }

    override fun getBuildList(page: Int,
                              repoId: String,
                              repoSortBy: BuildSortBy,
                              buildState: BuildState?): Observable<Page<Build>> {
        val sortByQuery = when (repoSortBy) {
            BuildSortBy.STARTED_AT_ASC -> "started_at"
            BuildSortBy.STARTED_AT_DESC -> "started_at:desc"
            BuildSortBy.FINISHED_AT_ASC -> "finished_at"
            BuildSortBy.FINISHED_AT_DESC -> "finished_at:desc"
        }

        val state = when (buildState) {
            BuildState.CANCELED -> Constants.CANCEL_BUILD
            BuildState.PASSED -> Constants.PASSED_BUILD
            BuildState.RUNNING -> Constants.RUNNING_BUILD
            BuildState.FAILED -> Constants.FAILED_BUILD
            BuildState.ERRORED -> Constants.ERRORED_BUILD
            BuildState.BOOTING -> Constants.BOOTING_BUILD
            null -> null
            else -> null
        }

        return travisEndpoints
                .getBuildsForRepo(
                        sortBy = sortByQuery,
                        offset = (page - 1) * PAGE_SIZE,
                        repoId = repoId,
                        state = state
                ).map {
                    val buildList = ArrayList<Build>(it.builds.size)
                    it.builds.forEach { buildList.add(it.toBuild()) }
                    return@map Page(
                            hasNext = !it.pagination.isLast,
                            list = buildList
                    )
                }
    }

    override fun getEnvironmentVariablesList(page: Int,
                                             repoId: String): Observable<Page<EnvVars>> {
        return travisEndpoints
                .getEnvVariablesForRepo(repoId = repoId)
                .map {
                    val buildList = ArrayList<EnvVars>(it.envVars.size)
                    it.envVars.forEach { buildList.add(it.toEnvVars()) }
                    return@map Page(
                            hasNext = false, /* This api is not designed for pagination */
                            list = buildList
                    )
                }
    }

    /**
     * Get the list of [Cache] for the given [Repo]. It will return the [Observable] with the
     * paginated list of [Cache].
     */
    override fun getCachesList(page: Int, repoId: String): Observable<Page<Cache>> {
        return travisEndpoints
                .getCachesForRepo(repoId = repoId)
                .map {
                    val buildList = ArrayList<Cache>(it.caches.size)
                    it.caches.forEach { buildList.add(it.toCache()) }
                    return@map Page(
                            hasNext = false, /* This api is not designed for pagination */
                            list = buildList
                    )
                }
    }

    /**
     * Delete [Cache] for [branchName] for the given [Repo]. It will return the [Observable] with the
     * count of deleted [Cache] items.
     */
    override fun deleteCache(repoId: String, branchName: String): Observable<Int> {
        return travisEndpoints
                .deleteCacheByBranch(branchName = branchName, repoId = repoId)
                .map { it.caches.size }
    }

    /**
     * Delete [EnvVars] with [envVarId] id for the given [Repo].It will return the [Observable] with the
     * count of deleted [EnvVars] items.
     */
    override fun deleteEnvironmentVariable(repoId: String, envVarId: String): Observable<Int> {
        return travisEndpoints
                .deleteEnvVariable(envVarId = envVarId, repoId = repoId)
                .map { 1 }
    }

    /**
     * Edit the [EnvVars] which has id [envVarId] and repository id [repoId]. This will return the
     * [Observable] with the updated [EnvVars].
     */
    override fun editEnvVariable(repoId: String,
                                 envVarId: String,
                                 newName: String,
                                 newValue: String,
                                 isPublic: Boolean
    ): Observable<EnvVars> {
        return travisEndpoints
                .editEnvVariable(
                        envVarId = envVarId,
                        repoId = repoId,
                        envName = newName,
                        envValue = newValue,
                        isPublic = isPublic
                )
                .map { it.toEnvVars() }
    }

    /**
     * Get the list of [Cron] for the given [Repo]. It will return the [Observable] with the
     * paginated list of [Cron].
     */
    override fun getCronsList(page: Int, repoId: String): Observable<Page<Cron>> {
        return travisEndpoints
                .getCronList(
                        offset = (page - 1) * PAGE_SIZE,
                        repoId = repoId
                ).map {
                    val cronList = ArrayList<Cron>(it.crons.size)
                    it.crons.forEach { cronList.add(it.toCron()) }
                    return@map Page(
                            hasNext = !it.pagination.isLast,
                            list = cronList
                    )
                }
    }

    /**
     * Start [Cron] with [cronId]. It will return the [Observable] with the id of the [Cron] that
     * is started.
     */
    override fun startCronManually(cronId: String, repoId: String): Observable<String> {
        throw IllegalStateException("Travis CI doesn't support manual run.")
    }

    /**
     * Delete [Cron] with [cronId]. It will return the [Observable] with the id of the [Cron] that
     * was deleted
     */
    override fun deleteCron(cronId: String, repoId: String): Observable<String> {
        return travisEndpoints.deleteCron(cronId).map { cronId }
    }

    /**
     * Method to provide the list of all the supported intervals.
     *
     * @return [ArrayList] of all the supported cron intervals.
     */
    override fun supportedCronIntervals(): Observable<ArrayList<String>> {
        return Observable.just(arrayListOf("daily", "weekly", "monthly"))
    }

    /**
     * Schedule new [Cron] for the branch with [branchName] and with [interval]. Use [dontRunIfRecentlyBuilt]
     * to don't run the cron if the build vor that branch recently completed.
     *
     * @return It will return the [Observable] with the id of the [Cron] that was deleted.
     */
    override fun scheduleCron(
            repoId: String,
            branchName: String,
            interval: String,
            dontRunIfRecentlyBuilt: Boolean
    ): Observable<Cron> {
        return travisEndpoints.scheduleNewCron(repoId, branchName, interval, dontRunIfRecentlyBuilt)
    }

    /**
     * Restart the [build]. You can only restart the build which has status other than
     * [BuildState.RUNNING] or [BuildState.BOOTING].
     *
     * @return It will return the [Observable] with the id of the new build.
     */
    override fun restartBuild(build: Build): Observable<String> {
        return travisEndpoints.restartBuild(build.id.toString())
                .map { return@map build.id.toString() /* Travis doesn't change the build id. */ }
    }

    /**
     * Abort the [build]. You can abort the build that are in [BuildState.RUNNING] only.
     *
     * @return It will return the [Observable] with the id of the new build.
     */
    override fun abortBuild(build: Build): Observable<String> {
        return travisEndpoints.abortBuild(build.id.toString())
                .map { return@map build.id.toString() /* Travis doesn't change the build id. */ }
    }
}
