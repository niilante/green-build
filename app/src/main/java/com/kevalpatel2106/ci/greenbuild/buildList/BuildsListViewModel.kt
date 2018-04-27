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

package com.kevalpatel2106.ci.greenbuild.buildList

import android.arch.lifecycle.MutableLiveData
import com.kevalpatel2106.ci.greenbuild.base.arch.BaseViewModel
import com.kevalpatel2106.ci.greenbuild.base.arch.SingleLiveEvent
import com.kevalpatel2106.ci.greenbuild.base.arch.recall
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.CompatibilityCheck
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.ServerInterface
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.Build
import com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities.BuildSortBy
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by Keval on 18/04/18.
 *
 * @author <a href="https://github.com/kevalpatel2106">kevalpatel2106</a>
 */
internal class BuildsListViewModel @Inject constructor(
        private val serverInterface: ServerInterface,
        compatibilityCheck: CompatibilityCheck
) : BaseViewModel() {
    internal val buildsList = MutableLiveData<ArrayList<Build>>()

    internal val errorLoadingList = SingleLiveEvent<String>()

    internal var isLoadingList = MutableLiveData<Boolean>()

    internal var isLoadingFirstTime = MutableLiveData<Boolean>()

    internal var hasModeData = MutableLiveData<Boolean>()

    init {
        if (!compatibilityCheck.isBuildsListByRepoSupported())
            throw IllegalStateException("Builds listing by repository is not supported for this CI.")

        hasModeData.value = true
        isLoadingList.value = false
        isLoadingFirstTime.value = false

        buildsList.value = ArrayList()
    }

    fun loadBuildsList(repoId: String, page: Int) {
        serverInterface.getBuildList(page, repoId, BuildSortBy.FINISHED_AT_DESC)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    isLoadingList.value = true
                    buildsList.value?.let { if (it.isEmpty()) isLoadingFirstTime.value = true }
                }
                .doOnTerminate {
                    isLoadingList.value = false
                    isLoadingFirstTime.value = false
                }
                .map {
                    hasModeData.value = it.hasNext

                    if (page == 1) buildsList.value!!.clear()
                    buildsList.value!!.addAll(it.list)

                    return@map buildsList.value
                }
                .subscribe({
                    buildsList.recall()
                }, {
                    errorLoadingList.value = it.message
                })
    }
}
