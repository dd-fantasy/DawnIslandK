package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BrowsingHistoryAndPost(
    @Embedded val browsingHistory: BrowsingHistory,
    @Relation(
        parentColumn = "postId",
        entityColumn = "id"
    )
    // post can be deleted hence not exist in database
    val post: Post?
)
