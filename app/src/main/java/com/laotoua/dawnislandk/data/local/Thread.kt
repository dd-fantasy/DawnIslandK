package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class Thread(
    @PrimaryKey val id: String, //	该串的id
    var fid: String = "", //	该串的fid, 非时间线的串会被设置
    var category: String = "",
    val img: String, //	该串的图片相对地址
    val ext: String, // 	该串图片的后缀
    val now: String, // 	该串的可视化发言时间
    val userid: String, //userid 	该串的饼干
    val name: String, //name 	你懂得
    val email: String, //email 	你懂得
    val title: String, //title 	你还是懂的(:з」∠)
    val content: String, //content 	....这个你也懂
    val sage: String = "", // sage
    val admin: String = "0", //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    val status: String = "", //
    @Ignore var replys: List<Reply> = emptyList(), //replys 	主页展示回复的帖子
    val replyCount: String = "", //replyCount 	总共有多少个回复
    var readingProgress: Int = 1, // 记录上次看到的进度
    var lastUpdatedAt: Long = 0
) {
    // Room uses this
    constructor(
        id: String,
        fid: String,
        category: String,
        img: String,
        ext: String,
        now: String,
        userid: String,
        name: String,
        email: String,
        title: String,
        content: String,
        sage: String,
        admin: String,
        status: String,
        replyCount: String,
        readingProgress: Int,
        lastUpdatedAt: Long
    ) : this(
        id,
        fid,
        category,
        img,
        ext,
        now,
        userid,
        name,
        email,
        title,
        content,
        sage,
        admin,
        status,
        emptyList(),
        replyCount,
        readingProgress,
        lastUpdatedAt
    )

    // convert threadList to Reply
    fun toReply() = Reply(
        id = id,
        userid = userid,
        name = name,
        sage = sage,
        admin = admin,
        status = status,
        title = title,
        email = email,
        now = now,
        content = content,
        img = img,
        ext = ext,
        page = 1
    )

    fun getImgUrl() = (img + ext)
    fun getSimplifiedTitle(): String = if (title.isNotBlank() && title != "无标题") "标题：$title" else ""
    fun getSimplifiedName(): String = if (name.isNotBlank() && name != "无名氏") "名称：$name" else ""

    fun equalsWithServerData(target: Thread?): Boolean =
        if (target == null) false
        else id == target.id && fid == target.fid
                && category == target.category && img == target.img
                && ext == target.ext && now == target.now
                && userid == target.userid && name == target.name
                && email == target.email && title == target.title
                && content == target.content && sage == target.sage
                && admin == target.admin && status == target.status
                && replyCount == target.replyCount

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }

    fun stripCopy():Thread =
        copy(
            id = id,
            fid = fid,
            category = category,
            img = img,
            ext = ext,
            now = now,
            userid = userid,
            name = name,
            email = email,
            title = title,
            content = content,
            sage = sage,
            admin = admin,
            status = status,
            replys = emptyList(),
            replyCount = replyCount,
            readingProgress = readingProgress,
            lastUpdatedAt = lastUpdatedAt)

}