package com.laotoua.dawnislandk.screens.subscriptions

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionFeedBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widget.BaseNavFragment
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber


class FeedsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = FeedsFragment()
    }

    private var _binding: FragmentSubscriptionFeedBinding? = null
    private val binding: FragmentSubscriptionFeedBinding get() = _binding!!

    private val viewModel: FeedsViewModel by viewModels { viewModelFactory }

    private var mHandler: Handler? = null
    private val mDelayedLoad = Runnable {
        viewModel.getNextPage()
    }
    private var delayedLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initial load
        if (viewModel.feeds.value.isNullOrEmpty() && !delayedLoading) {
            binding.srlAndRv.refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }

        val mAdapter = QuickAdapter<Post>(R.layout.list_item_post, sharedVM).apply {
            setOnItemClickListener { _, _, position ->
                getItem(position).run {
                    sharedVM.setPost(id, fid)
                }
                (requireActivity() as MainActivity).showComment()
            }

            // long click to delete
            setOnItemLongClickListener { _, _, position ->
                val id = getItem(position).id
                MaterialDialog(requireContext()).show {
                    title(text = "删除订阅 $id?")
                    positiveButton(R.string.delete) {
                        viewModel.deleteFeed(id, position)
                    }
                    negativeButton(R.string.cancel)
                }

                true
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    val url = getItem(position).getImgUrl()

                    val viewerPopup =
                        ImageViewerPopup(
                            url,
                            fragment = this@FeedsFragment
                        )
                    viewerPopup.setSingleSrcView(view as ImageView?, url)

                    XPopup.Builder(context)
                        .asCustom(viewerPopup)
                        .show()
                }
            }

            // load more
            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }
        }

        binding.srlAndRv.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        binding.srlAndRv.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refresh()
                }
            })
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { eventPayload ->
                Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
                if (eventPayload.loadingStatus == LoadingStatus.SUCCESS) mAdapter.removeAt(
                    eventPayload.payload!!
                )
            }
        })

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })

        viewModel.feeds.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                if (!mAdapter.hasEmptyView()) mAdapter.setEmptyView(R.layout.view_no_data)
                mAdapter.setDiffNewData(null)
                return@Observer
            }
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })
    }

    override fun onResume() {
        super.onResume()
        (parentFragment as SubscriptionPagerFragment).setToolbarClickListener {
            binding.srlAndRv.recyclerView.layoutManager?.scrollToPosition(0)
        }
    }

    override fun onPause() {
        super.onPause()
        mHandler?.removeCallbacks(mDelayedLoad)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHandler = null
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}
