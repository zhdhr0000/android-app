package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.isGroup
import org.jetbrains.anko.notificationManager
import org.threeten.bp.Instant

class NotificationJob(val message: Message) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("notification_group")) {

    companion object {
        private const val serialVersionUID = 1L
        const val CHANNEL_GROUP = "channel_group"
        const val CHANNEL_MESSAGE = "channel_message"
    }

    override fun onRun() {
        notifyMessage(message)
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.notificationManager
    }

    @SuppressLint("NewApi")
    private fun notifyMessage(message: Message) {
        val context = MixinApplication.appContext
        val user = syncUser(message) ?: return
        if (user.relationship == UserRelationship.BLOCKING.name) {
            return
        }
        user.muteUntil?.let {
            if (Instant.now().isBefore(Instant.parse(it))) {
                return
            }
        }
        val conversation = conversationDao.getConversation(message.conversationId)
        conversation?.muteUntil?.let {
            if (it.isNotBlank() && Instant.now().isBefore(Instant.parse(it))) {
                return
            }
        }
        val mainIntent = MainActivity.getSingleIntent(context)
        val conversationIntent = ConversationActivity
            .putIntent(context, message.conversationId, isGroup = notNullElse(conversation, { it.isGroup() }, false))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = if (conversation?.isGroup() == true) {
                notificationBuilder = NotificationCompat.Builder(context, CHANNEL_GROUP)
                NotificationChannel(CHANNEL_GROUP,
                    MixinApplication.get().getString(R.string.notification_group), NotificationManager.IMPORTANCE_HIGH)
            } else {
                notificationBuilder = NotificationCompat.Builder(context, CHANNEL_MESSAGE)
                NotificationChannel(CHANNEL_MESSAGE,
                    MixinApplication.get().getString(R.string.notification_message),
                    NotificationManager.IMPORTANCE_HIGH)
            }
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        } else {
            notificationBuilder = NotificationCompat.Builder(context, CHANNEL_MESSAGE)
        }

        notificationBuilder.setContentIntent(
            PendingIntent.getActivities(context, message.id.hashCode(),
                arrayOf(mainIntent, conversationIntent), PendingIntent.FLAG_UPDATE_CURRENT))
        when (message.category) {
            MessageCategory.SIGNAL_TEXT.name, MessageCategory.PLAIN_TEXT.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_text_message, user.fullName))
                    notificationBuilder.setContentText("${user.fullName} : ${message.content}")
                } else {
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                    notificationBuilder.setContentText(message.content)
                }
            }
            MessageCategory.SIGNAL_IMAGE.name, MessageCategory.PLAIN_IMAGE.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_image_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_image_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_image_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_image_message))
                }
            }
            MessageCategory.SIGNAL_VIDEO.name, MessageCategory.PLAIN_VIDEO.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_video_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_video_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_video_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_video_message))
                }
            }
            MessageCategory.SIGNAL_DATA.name, MessageCategory.PLAIN_DATA.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_data_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_data_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_data_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_data_message))
                }
            }
            MessageCategory.SIGNAL_AUDIO.name, MessageCategory.PLAIN_AUDIO.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_audio_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_audio_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_audio_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_audio_message))
                }
            }
            MessageCategory.SIGNAL_STICKER.name, MessageCategory.PLAIN_STICKER.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_sticker_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_sticker_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_sticker_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_sticker_message))
                }
            }
            MessageCategory.SIGNAL_CONTACT.name, MessageCategory.PLAIN_CONTACT.name -> {
                if (conversation?.isGroup() == true) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_contact_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.name)
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_contact_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_contact_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_contact_message))
                }
            }
            MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                if (message.action == SnapshotType.transfer.name) {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_transfer_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_transfer_message))
                }
            }
            MessageCategory.SYSTEM_CONVERSATION.name -> {
                notificationBuilder.setContentTitle(context.getString(R.string.app_name))
            }
            else -> {
                // No support
                return
            }
        }
        notificationBuilder.setSmallIcon(R.drawable.ic_msg_default)
        notificationBuilder.setWhen(System.currentTimeMillis())

        notificationBuilder.setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.mixin))
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        notNullElse(user, {
            context.mainThread {
                val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

                Glide.with(context)
                    .asBitmap()
                    .load(it.avatarUrl)
                    .apply(RequestOptions().fitCenter().circleCrop())
                    .listener(object : RequestListener<Bitmap> {
                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            notificationBuilder.setLargeIcon(resource)
                            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ):
                            Boolean {
                            notificationBuilder.setLargeIcon(
                                BitmapFactory.decodeResource(context.resources, R.drawable.default_avatar))
                            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                            return false
                        }
                    }).submit(width, height)
            }
        }, {
            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
        })
    }

    private fun syncUser(message: Message): User? {
        val u = userDao.findUser(message.userId)
        if (u == null) {
            val response = userService.getUsers(arrayListOf(message.userId)).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { data ->
                    for (user in data) {
                        userRepo.upsert(user)
                    }
                }
            }
        }
        return u
    }
}