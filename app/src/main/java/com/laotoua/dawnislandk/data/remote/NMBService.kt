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

package com.laotoua.dawnislandk.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface NMBService {

    @GET("https://reed.mfweb.top/Functions/Pictures/GetRandomPicture")
    fun getRandomReedPicture(): Call<ResponseBody>

    @GET("https://api.github.com/repos/fishballzzz/DawnIslandK/releases/latest")
    fun getLatestRelease(): Call<ResponseBody>

    @GET("https://cover.acfunwiki.org/nmb-notice.json")
    fun getNMBNotice(): Call<ResponseBody>

    @GET("https://cover.acfunwiki.org/luwei.json")
    fun getLuweiNotice(): Call<ResponseBody>

    @GET("Api/getForumList")
    fun getNMBForumList(): Call<ResponseBody>

    @GET("Api/showf")
    fun getNMBPosts(@Query("id") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/feed")
    fun getNMBFeeds(@Query("uuid") uuid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/addFeed")
    fun addNMBFeed(@Query("uuid") uuid: String, @Query("tid") tid: String): Call<ResponseBody>

    @GET("Api/delFeed")
    fun delNMBFeed(@Query("uuid") uuid: String, @Query("tid") tid: String): Call<ResponseBody>

    @GET("Api/timeline")
    fun getNMBTimeLine(@Query("page") page: Int): Call<ResponseBody>

    @GET("Api/thread")
    fun getNMBComments(
        @Header("Cookie") hash: String?,
        @Query("id") id: String,
        @Query("page") page: Int
    ): Call<ResponseBody>

    @GET("Api/ref")
    fun getNMBQuote(@Query("id") id: String): Call<ResponseBody>

    @Multipart
    @POST("Home/Forum/doReplyThread.html")
    fun postComment(
        @Part("resto") resto: RequestBody, @Part("name") name: RequestBody?,
        @Part("email") email: RequestBody?, @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody?, @Part("water") water: RequestBody?,
        @Part image: MultipartBody.Part?, @Header("Cookie") hash: String
    ): Call<ResponseBody>

    @Multipart
    @POST("Home/Forum/doPostThread.html")
    fun postNewPost(
        @Part("fid") fid: RequestBody, @Part("name") name: RequestBody?,
        @Part("email") email: RequestBody?, @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody?, @Part("water") water: RequestBody?,
        @Part image: MultipartBody.Part?, @Header("Cookie") hash: String
    ): Call<ResponseBody>
}

