package com.groops.fairsquare.activities_and_fragments.activities_home.profile

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import com.groops.fairsquare.BuildConfig
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.utility.AnalyticsUtils
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.MediaHelper
import kotlinx.android.synthetic.main.custom_layout_rating_badge.view.*
import kotlinx.android.synthetic.main.fragment_interest_detail_and_products.*
import kotlinx.android.synthetic.main.layout_header_interest_profile.*

class InterestProfileFragmentWebview: HLFragment() {

    enum class Type { DETAIL, PRODUCTS }

    companion object {
        @JvmStatic val LOG_TAG = InterestProfileFragmentWebview::class.qualifiedName

        @JvmStatic fun newInstance(type: Type, companyID: String, avatarUrl: String?,
                                   wallPicture: String?, score: String?):
                InterestProfileFragmentWebview {

            val fragment = InterestProfileFragmentWebview()
            fragment.arguments = Bundle().apply {
                this.putSerializable(Constants.EXTRA_PARAM_1, type)
                this.putString(Constants.EXTRA_PARAM_2, companyID)
                this.putString(Constants.EXTRA_PARAM_3, avatarUrl)
                this.putString(Constants.EXTRA_PARAM_4, wallPicture)
                this.putString(Constants.EXTRA_PARAM_5, score)
            }

            return fragment
        }

        private val URL_DETAIL = if (BuildConfig.DEBUG) Constants.GF_DETAIL_URL_DEV else Constants.GF_DETAIL_URL_PROD
        private val URL_PRODUCTS = if (BuildConfig.DEBUG) Constants.GF_PRODUCTS_URL_DEV else Constants.GF_PRODUCTS_URL_PROD
    }

    private var type: Type? = null
    private var companyID: String? = null
    private var avatarURL: String? = null
    private var wallURL: String? = null
    private var score: String? = null

    /**
     * The URL to be loaded into the WebView
     */
    private var urlToLoad: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_interest_detail_and_products, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeBtn.setOnClickListener { (view.context as? Activity)?.onBackPressed() }

        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.builtInZoomControls = true
        webView?.settings?.displayZoomControls = false
        webView?.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                (activity as? HLActivity)?.setProgressMessage(
                        if (type == Type.DETAIL) R.string.interest_webview_load_detail else R.string.interest_webview_load_products
                )
                (activity as? HLActivity)?.handleProgress(true)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                (activity as? HLActivity)?.handleProgress(false)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

//                    (activity as? HLActivity)?.showGenericError()
                LogUtils.e(LOG_TAG, "GENERIC Error loading url: $urlToLoad with error: ${error.toString()}")
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                // method deprecated in Java
//                    super.onReceivedError(view, errorCode, description, failingUrl)

//                    (activity as? HLActivity)?.showGenericError()
                LogUtils.e(LOG_TAG, "GENERIC Error loading url: $urlToLoad with error: $errorCode ($description)")
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)

//                    (activity as? HLActivity)?.showGenericError()
                LogUtils.e(LOG_TAG, "HTTP Error loading url: $urlToLoad with error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)

//                    (activity as? HLActivity)?.showGenericError()
                LogUtils.e(LOG_TAG, "SSL Error loading url: $urlToLoad with error: ${error.toString()}")
            }
        }

        if (!urlToLoad.isNullOrBlank() && !companyID.isNullOrBlank()) {
            webView?.loadUrl(urlToLoad, mutableMapOf("x-k" to Constants.HTTP_KEY, "x-id" to companyID!!))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (Utils.hasKitKat() && activity != null) {
            if (0 != activity!!.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }
    }


    override fun onStart() {
        super.onStart()

        MediaHelper.loadPictureWithGlide(context, wallURL, backgroundPicture as ImageView)
        MediaHelper.loadProfilePictureWithPlaceholder(context, avatarURL, profilePicture as ImageView, true)

        ratingBadge.visibility = if (!score.isNullOrBlank()) View.VISIBLE else View.GONE
        ratingBadge.score.text = score
    }

    override fun onResume() {
        super.onResume()

        if (type != null) {
            AnalyticsUtils.trackScreen(
                    context,
                    if (type == Type.DETAIL) AnalyticsUtils.INTEREST_DETAIL else AnalyticsUtils.INTEREST_PRODUCTS
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(Constants.EXTRA_PARAM_1, type)
        outState.putString(Constants.EXTRA_PARAM_2, companyID)
        outState.putString(Constants.EXTRA_PARAM_3, avatarURL)
        outState.putString(Constants.EXTRA_PARAM_4, wallURL)
        outState.putString(Constants.EXTRA_PARAM_5, score)
        outState.putString(Constants.EXTRA_PARAM_7, urlToLoad)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        type = savedInstanceState?.getSerializable(Constants.EXTRA_PARAM_1) as? Type
        companyID = savedInstanceState?.getString(Constants.EXTRA_PARAM_2)
        avatarURL = savedInstanceState?.getString(Constants.EXTRA_PARAM_3)
        wallURL = savedInstanceState?.getString(Constants.EXTRA_PARAM_4)
        score = savedInstanceState?.getString(Constants.EXTRA_PARAM_5)

        urlToLoad = savedInstanceState?.getString(Constants.EXTRA_PARAM_6)
        if (urlToLoad.isNullOrBlank()) {
            urlToLoad = if (type == Type.DETAIL) URL_DETAIL else URL_PRODUCTS
        }
    }


    /* NO OPS */
    override fun configureResponseReceiver() {}
    override fun configureLayout(view: View) {}
    override fun setLayout() {}


}