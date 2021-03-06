/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.screens.history

import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.BasePagerFragment

class HistoryPagerFragment:BasePagerFragment() {
    private val pageIndices = DawnApp.applicationDataStore.getHistoryPagerPageIndices()
    override val pageTitleResIds = mutableMapOf<Int,Int>().apply {
        put(pageIndices.first, R.string.browsing_history)
        put(pageIndices.second, R.string.post_history)
    }

    override val pageFragmentClass = mutableMapOf<Int, Class<out BaseNavFragment>>().apply {
        put(pageIndices.first, BrowsingHistoryFragment::class.java)
        put(pageIndices.second, PostHistoryFragment::class.java)
    }

    override val pageEditorClickListener: View.OnClickListener = View.OnClickListener {
        val items = listOf(
            requireContext().getString(R.string.browsing_history),
            requireContext().getString(R.string.post_history)
        )
        MaterialDialog(requireContext()).show {
            title(R.string.edit_default_page)
            listItemsSingleChoice(items = items) { _, index, _ ->
                DawnApp.applicationDataStore.setHistoryPagerDefaultPage(index, 1 - index)
                Toast.makeText(context, R.string.restart_to_apply_setting, Toast.LENGTH_SHORT)
                    .show()
            }
            positiveButton(R.string.submit)
            negativeButton(R.string.cancel)
        }
    }
}