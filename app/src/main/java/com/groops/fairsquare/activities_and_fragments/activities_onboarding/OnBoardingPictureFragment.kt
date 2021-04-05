package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.utility.EditPictureMenuItemClickListenerKt
import com.groops.fairsquare.utility.helpers.HLMediaType
import com.groops.fairsquare.utility.helpers.MEDIA_UPLOAD_NEW_PIC_PROFILE_SIGNUP
import com.groops.fairsquare.utility.helpers.MediaHelper
import com.groops.fairsquare.utility.helpers.MediaUploadManager
import kotlinx.android.synthetic.main.fragment_onboarding_picture.*
import java.io.File

class OnBoardingPictureFragment: HLFragment(), MediaUploadManager.OnUploadCompleteListener {

    companion object {

        val LOG_TAG = OnBoardingPictureFragment::class.qualifiedName

        fun newInstance(): OnBoardingPictureFragment {
            return OnBoardingPictureFragment()
        }
    }

    private lateinit var uploadManager: MediaUploadManager

    private val editPictureClick = View.OnClickListener {
        MediaHelper.openPopupMenu(
                it.context,
                R.menu.edit_users_picture,
                it,
                EditPictureMenuItemClickListenerKt(
                        (it.context as HLActivity),
                        HLMediaType.PHOTO,
                        this,
                        uploadManager
                )
        )
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_picture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        uploadManager = MediaUploadManager(activity!!, this, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        uploadManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        uploadManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onStart() {
        super.onStart()

        setLayout()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {}

    override fun configureResponseReceiver() {}

    override fun configureLayout(view: View) {
        addPicture.setOnClickListener(editPictureClick)
        actionChange.setOnClickListener(editPictureClick)
    }

    override fun setLayout() {

        with(onBoardingActivityListener.getAvatar()) {
            if (!isNullOrBlank()) {
                MediaHelper.loadProfilePictureWithPlaceholder(context, this, profilePicture as ImageView)
                groupPicture.visibility = View.VISIBLE
            }
            else groupPicture.visibility = View.GONE
        }

    }

    override fun handlePictureResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?) {

        if (file != null) {
            uploadManager.uploadMedia(
                    context,
                    file = file,
                    type = MEDIA_UPLOAD_NEW_PIC_PROFILE_SIGNUP,
                    mediaType = HLMediaType.PHOTO,
                    userId = onBoardingActivityListener.getUserID(),
                    path = file.absolutePath,
                    uploadCompleteListener = this
            )
        }

    }

    override fun handleVideoResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?, mediaFileUri: String?) {}
    override fun handleMediaFinalOps(file: File?) {}

    override fun onUploadComplete(path: String?, mediaLink: String?) {
        if (!mediaLink.isNullOrBlank()) {
            onBoardingActivityListener.setAvatar(mediaLink)

            profilePicture.post {
                MediaHelper.loadProfilePictureWithPlaceholder(context, mediaLink, profilePicture as ImageView)
                groupPicture.visibility = View.VISIBLE
            }
        }
    }


}