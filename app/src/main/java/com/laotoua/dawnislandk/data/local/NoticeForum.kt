package com.laotoua.dawnislandk.data.local

import com.squareup.moshi.JsonClass

// from http://cover.acfunwiki.org/luwei.json, differs from Forum
@JsonClass(generateAdapter = true)
data class NoticeForum(
    val id: String,
    val sort: String = "",
    val name: String,
    val showName: String = "",
    val fgroup: String,
    val rule: String = "请遵守总版规" // default rule
){
    fun getDisplayName():String = if (showName.isNotBlank()) showName else name
}
