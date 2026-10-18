package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val SAM_OPTIMIZATION_JS = """
(function() {
  // 1. Ekran o'lchamini to'liq egallash (Full screen canvas & 100% viewport)
  function fixCanvasDimensions() {
    try {
      var canvas = document.getElementById('wallpaperCanvas');
      var w = window.innerWidth || document.documentElement.clientWidth;
      var h = window.innerHeight || document.documentElement.clientHeight;

      if (canvas && (canvas.width !== w || canvas.height !== h)) {
        canvas.width = w;
        canvas.height = h;
        canvas.style.width = '100vw';
        canvas.style.height = '100vh';
        if (typeof initParticles === 'function') {
          initParticles();
        }
      }

      document.documentElement.style.width = '100%';
      document.documentElement.style.height = '100%';
      document.documentElement.style.overflow = 'hidden';
      document.body.style.width = '100%';
      document.body.style.height = '100%';
      document.body.style.overflow = 'hidden';
    } catch(e) {
      console.log('Canvas fix error', e);
    }
  }

  fixCanvasDimensions();
  window.addEventListener('resize', fixCanvasDimensions);
  window.addEventListener('orientationchange', function() {
    setTimeout(fixCanvasDimensions, 100);
    setTimeout(fixCanvasDimensions, 400);
  });

  // 2. Soat vidjetini to'g'ridan-to'g'ri sensor orqali qotmasdan surish (Smooth Touch Drag)
  var widget = document.getElementById('clockWidget');
  if (widget && !widget.__touchAttached) {
    widget.__touchAttached = true;
    var isDraggingWidget = false;
    var touchOffsetX = 0;
    var touchOffsetY = 0;

    widget.addEventListener('touchstart', function(e) {
      if (e.touches.length === 1) {
        var t = e.touches[0];
        var rect = widget.getBoundingClientRect();
        touchOffsetX = t.clientX - rect.left;
        touchOffsetY = t.clientY - rect.top;
        isDraggingWidget = true;
        e.stopPropagation();
      }
    }, { passive: false });

    window.addEventListener('touchmove', function(e) {
      if (isDraggingWidget && e.touches.length === 1) {
        var t = e.touches[0];
        widget.style.left = (t.clientX - touchOffsetX) + 'px';
        widget.style.top = (t.clientY - touchOffsetY) + 'px';
        widget.style.transform = 'none';
        if (e.cancelable) {
          e.preventDefault();
        }
      }
    }, { passive: false });

    function stopDragging() {
      isDraggingWidget = false;
    }
    window.addEventListener('touchend', stopDragging, { passive: true });
    window.addEventListener('touchcancel', stopDragging, { passive: true });
  }

  // 3. Zarrachalar animatsiyasini GPU uchun optimallashtirish (Qotishni yo'qotish va 60 FPS)
  var canvas = document.getElementById('wallpaperCanvas');
  var modeSelect = document.getElementById('modeSelect');
  var speedSlider = document.getElementById('speedSlider');
  var particleCountInput = document.getElementById('particleCount');

  if (canvas && typeof particles !== 'undefined') {
    var ctx = canvas.getContext('2d');

    // Mobil telefon protsessorini qizitmaslik va silliq ishlash uchun sonini 70 ga moslashtirish
    if (particleCountInput && parseInt(particleCountInput.value) > 75) {
      particleCountInput.value = '70';
      var countVal = document.getElementById('countVal');
      if (countVal) countVal.innerText = '70';
      if (typeof initParticles === 'function') {
        initParticles();
      }
    }

    // Canvas render qilishni bitta paketga yig'ish (Batched drawing)
    window.renderWallpaper = function() {
      var w = canvas.width;
      var h = canvas.height;
      ctx.fillStyle = 'rgba(3, 7, 18, 0.28)';
      ctx.fillRect(0, 0, w, h);

      var mode = modeSelect ? modeSelect.value : 'particles';
      var speed = speedSlider ? parseFloat(speedSlider.value) : 2;

      if (mode === 'particles') {
        var pLen = particles.length;

        // Barcha zarrachalarni bitta siklda chizish
        ctx.fillStyle = '#00f0ff';
        ctx.beginPath();
        for (var i = 0; i < pLen; i++) {
          var p = particles[i];
          p.x += p.vx;
          p.y += p.vy;
          if (p.x < 0 || p.x > w) p.vx *= -1;
          if (p.y < 0 || p.y > h) p.vy *= -1;
          ctx.moveTo(p.x + p.radius, p.y);
          ctx.arc(p.x, p.y, p.radius, 0, 6.2831853);
        }
        ctx.fill();

        // Chiziqlarni bitta paketda tez chizish (Math.hypot o'rniga masofa kvadrati)
        ctx.strokeStyle = 'rgba(0, 240, 255, 0.12)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        var maxDistSq = 12100; // 110 * 110
        for (var i = 0; i < pLen; i++) {
          var p1 = particles[i];
          for (var j = i + 1; j < pLen; j++) {
            var p2 = particles[j];
            var dx = p1.x - p2.x;
            var dy = p1.y - p2.y;
            var distSq = dx * dx + dy * dy;
            if (distSq < maxDistSq) {
              ctx.moveTo(p1.x, p1.y);
              ctx.lineTo(p2.x, p2.y);
            }
          }
        }
        ctx.stroke();

      } else if (mode === 'starfield') {
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        for (var i = 0; i < particles.length; i++) {
          var p = particles[i];
          p.y += p.radius * speed;
          if (p.y > h) p.y = 0;
          ctx.moveTo(p.x + p.radius, p.y);
          ctx.arc(p.x, p.y, p.radius, 0, 6.2831853);
        }
        ctx.fill();
      }

      requestAnimationFrame(renderWallpaper);
    };
  }
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SamWebView(
    url: String,
    isDesktopMode: Boolean,
    touchDragEnabled: Boolean,
    zoomScale: Float,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onErrorOccurred: (String?) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Software rendering to prevent MESA DRM rendernode probing in container
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xFF030712.toInt())
        }
    }

    LaunchedEffect(webView) {
        onWebViewCreated(webView)
    }

    // Optimized WebView settings for high performance and full screen scaling
    LaunchedEffect(isDesktopMode) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true

            // Respect viewport meta tag: do NOT scale down to desktop viewport
            useWideViewPort = isDesktopMode
            loadWithOverviewMode = isDesktopMode

            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    LaunchedEffect(url) {
        webView.loadUrl(url)
    }

    DisposableEffect(Unit) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
                if (newProgress >= 100) {
                    onLoadingChanged(false)
                    // Inject optimization script as soon as loaded
                    view?.evaluateJavascript(SAM_OPTIMIZATION_JS, null)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoadingChanged(true)
                onErrorOccurred(null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoadingChanged(false)
                // Execute optimizations for full screen canvas and smooth dragging
                view?.evaluateJavascript(SAM_OPTIMIZATION_JS, null)
                // Re-check after layout stabilization
                view?.postDelayed({
                    view.evaluateJavascript(SAM_OPTIMIZATION_JS, null)
                }, 300)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    onErrorOccurred(error?.description?.toString() ?: "Tarmoq xatosi yuz berdi")
                }
            }
        }

        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
