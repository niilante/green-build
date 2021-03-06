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

package com.kevalpatel2106.ci.greenbuild.base.ciInterface.entities

import android.content.Context
import com.kevalpatel2106.ci.greenbuild.base.R

/**
 * Created by Kevalpatel2106 on 18-Apr-18.
 *
 * @author <a href="https://github.com/kevalpatel2106">kevalpatel2106</a>
 */
enum class TriggerType {
    PUSH,
    PULL_REQUEST,
    CRON,
    API
}

fun TriggerType.getTriggerTypeText(context: Context): String {
    return when (this) {
        TriggerType.PUSH -> context.getString(R.string.trigger_type_commit)
        TriggerType.PULL_REQUEST -> context.getString(R.string.trigger_type_pull_request)
        TriggerType.CRON -> context.getString(R.string.trigger_type_cron)
        TriggerType.API -> context.getString(R.string.trigger_type_api)
    }
}
