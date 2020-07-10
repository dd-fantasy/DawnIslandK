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

package com.laotoua.dawnislandk.screens.comments

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformContent
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformCookie
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformTime
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widgets.spans.ReferenceSpan
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.GlideApp
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.XPopupUtils
import dagger.android.support.DaggerFragment

@SuppressLint("ViewConstructor")
// uses caller fragment's context, should not live without fragment
class QuotePopup(
    private val caller: DaggerFragment,
    private val commentVM: CommentsViewModel,
    private val quoteId: String,
    private val po: String
) : CenterPopupView(caller.requireContext()) {

    override fun getImplLayoutId(): Int = R.layout.popup_quote

    override fun getMaxWidth(): Int = (XPopupUtils.getWindowWidth(context) * .9f).toInt()

    private val liveQuote: LiveData<Comment> = commentVM.getQuote(quoteId)

    private val liveQuoteObs = Observer<Comment> {
        if (it != null) {
            convertQuote(it, po)
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.quote).visibility = View.VISIBLE
        }
    }

    private val quoteDownloadStatusObs = Observer<EventPayload<String>> {
        if (it.loadingStatus == LoadingStatus.ERROR && it.payload == quoteId) {
            dismiss()
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    fun listenToLiveQuote() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.quote).visibility = View.GONE
        // observe quote live quote, loading Status
        liveQuote.observe(caller.viewLifecycleOwner, liveQuoteObs)
        commentVM.quoteLoadingStatus.observe(caller.viewLifecycleOwner, quoteDownloadStatusObs)
    }

    private fun convertQuote(quote: Comment, po: String) {
        // remove observers when data has come back
        liveQuote.removeObserver(liveQuoteObs)
        commentVM.quoteLoadingStatus.removeObserver(quoteDownloadStatusObs)

        findViewById<TextView>(R.id.userId).text =
            transformCookie(
                quote.userid,
                quote.admin,
                po
            )

        findViewById<TextView>(R.id.timestamp).text = transformTime(quote.now)

        findViewById<TextView>(R.id.refId).text =
            context.resources.getString(R.string.ref_id_formatted, quote.id)

        findViewById<TextView>(R.id.sage).run {
            visibility = if (quote.sage == "1") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val title = quote.getSimplifiedTitle()
        findViewById<TextView>(R.id.title).run {
            if (title.isNotBlank()) {
                text = title
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        val name = quote.getSimplifiedName()
        findViewById<TextView>(R.id.name).run {
            if (name.isNotBlank()) {
                text = name
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // load image
        findViewById<ImageView>(R.id.attachedImage).run {
            visibility = if (quote.img != "") {
                GlideApp.with(context)
                    .load(DawnConstants.thumbCDN + quote.img + quote.ext)
//                    .override(250, 250)
                    .fitCenter()
                    .into(this)
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener { imageView ->
                val url = quote.getImgUrl()
                val viewerPopup =
                    ImageViewerPopup(
                        url,
                        caller.requireContext()
                    )
                viewerPopup.setSingleSrcView(imageView as ImageView?, url)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        val referenceClickListener = object : ReferenceSpan.ReferenceClickHandler {
            override fun handleReference(id: String) {
                showQuote(
                    caller,
                    commentVM,
                    context,
                    id,
                    po
                )
            }
        }

        findViewById<TextView>(R.id.content).run {
            /** when TextView is scrolled, resetting text does not reset scroll position
             *  WITHOUT scroll reset, text is not shown
             */
            maxLines = 15
            scrollY = 0
            movementMethod = LinkMovementMethod.getInstance()
            text = transformContent(
                context,
                quote.content,
                DawnApp.applicationDataStore.lineHeight,
                DawnApp.applicationDataStore.segGap, referenceClickListener
            )
            textSize = DawnApp.applicationDataStore.textSize
            letterSpacing = DawnApp.applicationDataStore.letterSpace
        }

        findViewById<Button>(R.id.jumpToQuotedPost).run {
            visibility = if (quote.parentId != commentVM.currentPostId) View.VISIBLE else View.GONE
            setOnClickListener {
                val parentId = quote.parentId
                Toast.makeText(context, "TODO $parentId", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun showQuote(
            caller: DaggerFragment,
            replyVM: CommentsViewModel,
            context: Context,
            id: String,
            po: String
        ) {
            val top = QuotePopup(caller, replyVM, id, po)
            XPopup.Builder(context)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        super.beforeShow()
                        top.listenToLiveQuote()
                    }
                })
                .asCustom(top)
                .show()
        }
    }
}