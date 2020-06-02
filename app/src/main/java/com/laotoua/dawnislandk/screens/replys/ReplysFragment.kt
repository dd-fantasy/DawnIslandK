package com.laotoua.dawnislandk.screens.replys


import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.databinding.FragmentReplyBinding
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber
import javax.inject.Inject


class ReplysFragment : DaggerFragment() {

    //TODO: maintain reply fragment when pressing back, such that progress can be remembered
    private var _binding: FragmentReplyBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ReplysViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels()

    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            setOnClickListener {
                binding.recyclerView.layoutManager?.scrollToPosition(0)
            }
        }

        val imageLoader = ImageLoader()
        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext()) }
        val jumpPopup: JumpPopup by lazyOnMainOnly { JumpPopup(requireContext()) }

        val mAdapter = QuickAdapter<Reply>(R.layout.list_item_reply).apply {
            setReferenceClickListener { quote ->
                // TODO: get Po based on Thread
                QuotePopup.showQuote(
                    this@ReplysFragment,
                    requireContext(),
                    quote,
                    viewModel.po
                )
            }
            /*** connect SharedVm and adapter
             *  may have better way of getting runtime data
             */
            setSharedVM(sharedVM)

            setOnItemClickListener { _, _, _ ->
                hideMenu()
            }

            // image
            addChildClickViewIds(
                R.id.attachedImage,
                R.id.refId
            )

            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    hideMenu()
                    Timber.i("clicked on image at $position")

                    val url = getItem(position).getImgUrl()
                    // TODO support multiple image
                    val viewerPopup = ImageViewerPopup(this@ReplysFragment, requireContext(), url)
                    viewerPopup.setXPopupImageLoader(imageLoader)
                    viewerPopup.setSingleSrcView(view as ImageView?, url)

                    XPopup.Builder(context)
                        .asCustom(viewerPopup)
                        .show()
                } else if (view.id == R.id.refId) {
                    val content = ">>No.${(view as TextView).text}\n"
                    postPopup.setupAndShow(
                        viewModel.currentThreadId,
                        quote = content
                    )
                }
            }

            // load more
            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }
        }

        binding.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.getPreviousPage()
                }
            })
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        binding.fabMenu.hide()
                        hideMenu()
                        binding.fabMenu.isClickable = false
                    } else if (dy < 0) {
                        binding.fabMenu.show()
                        binding.fabMenu.isClickable = true
                        if ((layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() <= 2
                            && !binding.refreshLayout.isRefreshing
                        ) {
                            viewModel.getPreviousPage()
                        }
                    }
                }
            })
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
            }
        })

        viewModel.replys.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            mAdapter.setPo(viewModel.po)
            Timber.i("${this.javaClass.simpleName} Adapter will have ${mAdapter.data.size} threads")
        })

        sharedVM.selectedThreadId.observe(viewLifecycleOwner, Observer {
            viewModel.setThreadId(it)
            updateTitle()
            updateSubtitle()
        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.copyId.setOnClickListener {
            copyId(">>No.${viewModel.currentThreadId}")
            hideMenu()
        }

        binding.post.setOnClickListener {
            hideMenu()
            val page = getCurrentPage(mAdapter)
            postPopup.setupAndShow(viewModel.currentThreadId) {
                if (page == viewModel.maxPage) {
                    mAdapter.loadMoreModule.loadMoreToLoading()
                }
            }
        }


        binding.jump.setOnClickListener {
            hideMenu()
            val page = getCurrentPage(mAdapter)
            XPopup.Builder(context)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        super.beforeShow()
                        jumpPopup.updatePages(page, viewModel.maxPage)
                    }
                })
                .asCustom(jumpPopup)
                .show()
                .dismissWith {
                    if (jumpPopup.submit) {
                        mAdapter.setList(emptyList())
                        Timber.i("Jumping to ${jumpPopup.targetPage}...")
                        viewModel.jumpTo(jumpPopup.targetPage)
                    }
                }
        }

        binding.onlyPo.setOnClickListener {
            Timber.i("Clicked on onlyPo")
            Toast.makeText(context, "还没写嘿嘿嘿。。", Toast.LENGTH_SHORT).show()
            hideMenu()
        }

        binding.addFeed.setOnClickListener {
            hideMenu()
            viewModel.addFeed(applicationDataStore.feedId, viewModel.currentThreadId!!)
        }

        viewModel.addFeedResponse.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { eventPayload ->
                Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun copyId(text: String) {
        val clipboard = getSystemService(
            requireContext(),
            ClipboardManager::class.java
        )
        val clip: ClipData =
            ClipData.newPlainText("currentThreadId", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, R.string.thread_id_copied, Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentPage(adapter: QuickAdapter<Reply>): Int {
        val pos = (binding.recyclerView.layoutManager as LinearLayoutManager)
            .findLastCompletelyVisibleItemPosition()
            .coerceAtLeast(0)
            .coerceAtMost(adapter.data.lastIndex)
        return adapter.getItem(pos).page
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotePopup.clearQuotePopups()
    }

    private fun hideMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.post.hide()
        binding.jump.hide()
        binding.copyId.hide()
        binding.onlyPo.hide()
        binding.addFeed.hide()
        isFabOpen = false
    }

    private fun showMenu() {
        val rotateForward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)
        binding.post.show()
        binding.jump.show()
        binding.copyId.show()
        binding.onlyPo.show()
        binding.addFeed.show()
        isFabOpen = true
    }

    private fun toggleMenu() {
        if (isFabOpen) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun updateTitle() {
        binding.toolbarLayout.toolbar.title = "A岛 • ${sharedVM.getSelectedThreadForumName()}"
    }

    private fun updateSubtitle() {
        binding.toolbarLayout.toolbar.subtitle = "No.${viewModel.currentThreadId} • adnmb.com"
    }
}