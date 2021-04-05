/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.webkit.*
import android.webkit.WebView
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.Utils
import kotlinx.android.synthetic.main.activity_webview_empty.*


/**
 * A screen dedicated to the visualization of web content.
 * @author mbaldrighi on 3/20/2019.
 */

class EmptyWebViewActivity : HLActivity() {

    companion object {
        val LOG_TAG = EmptyWebViewActivity::class.qualifiedName
    }

    private var intentUrl: String? = null
    private var urlToLoad: String? = null
    private var title: String? = null
    private var messageID: String? = null


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

        webView.loadUrl(urlToLoad)
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
//        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_1))
//            intentUrl = intent.getStringExtra(Constants.EXTRA_PARAM_1)
//        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_2))
//            title = intent.getStringExtra(Constants.EXTRA_PARAM_2)
//        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_3))
//            messageID = intent.getStringExtra(Constants.EXTRA_PARAM_3)
//
//        urlToLoad = getEditedUrl(intentUrl)
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

}

