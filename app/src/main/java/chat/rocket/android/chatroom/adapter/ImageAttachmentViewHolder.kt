package chat.rocket.android.chatroom.adapter

import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.chatroom.viewmodel.ImageAttachmentViewModel
import chat.rocket.android.widget.emoji.EmojiReactionListener
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.cache.common.CacheKey
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imageformat.ImageFormatChecker
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.stfalcon.frescoimageviewer.ImageViewer
import kotlinx.android.synthetic.main.message_attachment.view.*
import timber.log.Timber
import java.io.File


class ImageAttachmentViewHolder(itemView: View,
                                listener: ActionsListener,
                                reactionListener: EmojiReactionListener? = null)
    : BaseViewHolder<ImageAttachmentViewModel>(itemView, listener, reactionListener) {

    private var cacheKey: CacheKey? = null

    init {
        with(itemView) {
            setupActionMenu(attachment_container)
            setupActionMenu(image_attachment)
        }
    }

    override fun bindViews(data: ImageAttachmentViewModel) {
        with(itemView) {
            val controller = Fresco.newDraweeControllerBuilder().apply {
                setUri(data.attachmentUrl)
                autoPlayAnimations = true
                oldController = image_attachment.controller
            }.build()
            image_attachment.controller = controller
            file_name.text = data.attachmentTitle
            image_attachment.setOnClickListener { view ->
                // TODO - implement a proper image viewer with a proper Transition


                val request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(data.attachmentUrl))
                    .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
                    .build()
                cacheKey = DefaultCacheKeyFactory.getInstance()
                    .getEncodedCacheKey(request, null)

                val toolbar = Toolbar(itemView.context).also {
                    it.inflateMenu(R.menu.image_actions)
                    it.overflowIcon?.setTint(Color.WHITE)
                    it.setOnMenuItemClickListener {
                        return@setOnMenuItemClickListener when (it.itemId) {
                            R.id.action_save_image -> saveImage()
                            else -> super.onMenuItemClick(it)
                        }
                    }
                }

                val builder = ImageViewer.createPipelineDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setAutoPlayAnimations(true)
                ImageViewer.Builder(view.context, listOf(data.attachmentUrl))
                    .setOverlayView(toolbar)
                    .setStartPosition(0)
                    .hideStatusBar(false)
                    .setCustomDraweeControllerBuilder(builder)
                    .show()
            }
        }
    }

    private fun saveImage(): Boolean {
        if (ImagePipelineFactory.getInstance().mainFileCache.hasKey(cacheKey)) {
            val context = itemView.context
            val resource = ImagePipelineFactory.getInstance().mainFileCache.getResource(cacheKey)
            val cachedFile = (resource as FileBinaryResource).file
            val imageFormat = ImageFormatChecker.getImageFormat(resource.openStream())
            val imageDir = "${Environment.DIRECTORY_PICTURES}/Rocket.Chat Images/"
            val imagePath = Environment.getExternalStoragePublicDirectory(imageDir)
            val imageFile = File(imagePath, "${cachedFile.nameWithoutExtension}.${imageFormat.fileExtension}")
            imagePath.mkdirs()
            imageFile.createNewFile()
            try {
                cachedFile.copyTo(imageFile, true)
                MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), null) { path, uri ->
                    Timber.i("Scanned $path:")
                    Timber.i("-> uri=$uri")
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                val message = context.getString(R.string.msg_image_saved_failed)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } finally {
                val message = context.getString(R.string.msg_image_saved_successfully)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }
}