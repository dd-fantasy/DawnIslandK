package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedViewModel : ViewModel() {
    private val feedsList = mutableListOf<Thread>()
    private val feedsIds = mutableSetOf<String>()
    private var _feeds = MutableLiveData<List<Thread>>()
    val feeds: LiveData<List<Thread>> get() = _feeds
    private var nextPage = 1
    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<EventPayload<Int>>> get() = _delFeedResponse

    private var tryAgain = false

    fun getNextPage() {
        getFeedOnPage(nextPage)
    }

    private fun getFeedOnPage(page: Int) {
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            Timber.i("Downloading Feeds on page $page...")
            DataResource.create(NMBServiceClient.getFeeds(AppState.feedId, page)).run {
                when (this) {
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "无法读取订阅...\n$message"
                            )
                        )
                    }
                    is DataResource.Success -> {
                        val res = convertFeedData(data!!)
                        if (res) {
                            _feeds.postValue(feedsList)
                            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
                        } else {
                            if (tryAgain) {
                                getFeedOnPage(nextPage)
                            } else {
                                _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.NODATA))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun convertFeedData(data: List<Thread>): Boolean {
        if (data.isEmpty()) {
            nextPage -= 1
            tryAgain = true
            return false
        }
        tryAgain = false
        val noDuplicates = data.filterNot { feedsIds.contains(it.id) }
        return if (noDuplicates.isNotEmpty()) {
            feedsIds.addAll(noDuplicates.map { it.id })
            feedsList.addAll(noDuplicates)
            Timber.i(
                "feedsList now has ${feedsList.size} feeds"
            )
            nextPage += 1
            true
        } else {
            false
        }

    }

    fun deleteFeed(id: String, position: Int) {
        Timber.i("Deleting Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            NMBServiceClient.delFeed(AppState.feedId, id).run {
                when (this) {
                    is APISuccessMessageResponse -> {
                        feedsList.removeAt(position)
                        feedsIds.remove(id)
                        _delFeedResponse.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.SUCCESS,
                                message,
                                position
                            )
                        )
                    }
                    else -> {
                        Timber.e("Response type: ${this.javaClass.simpleName}")
                        Timber.e(message)
                        _delFeedResponse.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "删除订阅失败"
                            )
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        feedsList.clear()
        feedsIds.clear()
        nextPage = 1
        getFeedOnPage(nextPage)
    }
}
