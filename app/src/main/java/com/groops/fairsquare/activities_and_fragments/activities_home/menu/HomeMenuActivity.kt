/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.webkit.*
import android.webkit.WebView
import com.groops.fairsquare.BuildConfig
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.utility.AnalyticsUtils
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.Utils
import kotlinx.android.synthetic.main.activity_webview_empty.*


/**
 * A screen dedicated to the visualization of the main Home MENU in form of web content.
 * @author mbaldrighi on 3/20/2019.
 */

const val MENU_DEFAULT_URL_DEV = "http://ec2-18-205-151-197.compute-1.amazonaws.com/HamburgerAndroid.aspx"
const val MENU_DEFAULT_URL_PROD = "https://genderfair.highlanders.app/HamburgerAndroid.aspx"

class HomeMenuActivity : HLActivity() {

    companion object {
        val LOG_TAG = HomeMenuActivity::class.qualifiedName
        private var urlToLoad: String? = if (BuildConfig.DEBUG) MENU_DEFAULT_URL_DEV else MENU_DEFAULT_URL_PROD
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_webview_empty)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        manageIntent()

        if (Utils.hasKitKat()) {
            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                setProgressMessage(R.string.webview_page_loading)
                handleProgress(true)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                handleProgress(false)

//                webView!!.loadUrl(
//                        "javascript:(function() { " +
//                                "var element = document.getElementsByClassName('ndfHFb-c4YZDc-Wrql6b')[0];"
//                                + "element.parentNode.removeChild(element);" +
//                                "})()"
//                )

            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

//                showGenericError()
                LogUtils.e(LOG_TAG, "GENERIC Error loading url: $urlToLoad with error: ${error.toString()}")
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                // method deprecated in Java
//                    super.onReceivedError(view, errorCode, description, failingUrl)

//                showGenericError()
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: $description")
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)

//                showGenericError()
                LogUtils.e(LOG_TAG, "HTTP Error loading url: $urlToLoad with error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)

//                showGenericError()
                LogUtils.e(LOG_TAG, "SSL Error loading url: $urlToLoad with error: ${error.toString()}")
            }
        }
        webView.addJavascriptInterface(GFActionsMenuInterface(this), "Android")

        webView.loadUrl(urlToLoad)
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_MENU)
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
    }

    override fun onBackPressed() {

        // TODO: 3/20/19    understand what's wanted for back pressed
        
        if (webView!!.canGoBack())
            webView!!.goBack()
        else {
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top)
        }
    }



    /** Instantiate the interface and set the context  */
    class GFActionsMenuInterface(private val mContext: Context) {

        /** Close activity  */
        @JavascriptInterface
        fun closePage() {
            if (mContext is Activity && Utils.isContextValid(mContext)) {
                mContext.finish()
                mContext.overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top)
            }
        }

        /** Open action "SHOP" */
        @JavascriptInterface
        fun openShop() {
            LogUtils.d(LOG_TAG, "Open Shop")
            MenuActivity.openMenuLandingFragment(mContext, MenuActivity.FlowType.SHOP)
        }

        /** Open action "INVEST" */
        @JavascriptInterface
        fun openInvest() {
            LogUtils.d(LOG_TAG, "Open Invest")
            MenuActivity.openMenuLandingFragment(mContext, MenuActivity.FlowType.INVEST)
        }

        /** Open action "WORK" */
        @JavascriptInterface
        fun openWork() {
            LogUtils.d(LOG_TAG, "Open Work")
            MenuActivity.openMenuLandingFragment(mContext, MenuActivity.FlowType.WORK)
        }

    }



}

