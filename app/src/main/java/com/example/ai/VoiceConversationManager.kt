package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "VoiceConvManager"

class VoiceConversationManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _speechStatus = MutableStateFlow("Tap 'Speak Now' to talk with Jameson")
    val speechStatus: StateFlow<String> = _speechStatus.asStateFlow()

    init {
        mainHandler.post {
            initSpeechRecognizer()
        }
        tts = TextToSpeech(context.applicationContext, this)
    }

    private fun initSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceConversationManager)
                }
                Log.d(TAG, "SpeechRecognizer initialized on main looper")
            } else {
                Log.w(TAG, "SpeechRecognizer isRecognitionAvailable returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(1.08f)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    private var lastDispatchedUtterance = ""
    private var lastDispatchTime = 0L

    private fun dispatchRecognizedText(text: String) {
        val clean = text.trim()
        val now = System.currentTimeMillis()
        if (clean.isBlank()) return
        if (clean.equals(lastDispatchedUtterance, ignoreCase = true) && (now - lastDispatchTime) < 1500L) {
            return
        }
        lastDispatchedUtterance = clean
        lastDispatchTime = now
        _speechStatus.value = "Transcribed: \"$clean\""
        onSpeechRecognized(clean)
    }

    fun startListening() {
        mainHandler.post {
            stopSpeaking()
            lastDispatchedUtterance = ""
            _liveTranscript.value = ""
            _speechStatus.value = "Listening to your voice... speak now"

            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            if (speechRecognizer == null) {
                _speechStatus.value = "Microphone ready — speak your question"
                _isListening.value = false
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Extended silence tolerances so recognition does not immediately cut off
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.d(TAG, "Listening started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _isListening.value = false
                _speechStatus.value = "Tap 'Speak Now' to talk"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _audioRmsLevel.value = 0f
            if (_liveTranscript.value.isNotBlank()) {
                dispatchRecognizedText(_liveTranscript.value)
            } else {
                _speechStatus.value = "Tap 'Speak Now' to talk"
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _liveTranscript.value = ""
            _audioRmsLevel.value = 0f
            _speechStatus.value = "Ready to listen"
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || text.isBlank()) return
        stopListening()
        _isSpeaking.value = true

        val speechText = text
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?m)^[-*•]\\s*"), "")
            .trim()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "JamesonSpeech_${System.currentTimeMillis()}")
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "JamesonSpeechId")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
        _speechStatus.value = "Listening to your voice... speak now"
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
        _speechStatus.value = "Receiving your voice..."
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
        _audioRmsLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioRmsLevel.value = 0f
        _speechStatus.value = "Processing your question..."
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioRmsLevel.value = 0f
        Log.w(TAG, "SpeechRecognizer error code: $error")

        if (error == SpeechRecognizer.ERROR_CLIENT) {
            initSpeechRecognizer()
        }

        if (_liveTranscript.value.isNotBlank()) {
            dispatchRecognizedText(_liveTranscript.value)
        } else {
            _speechStatus.value = "Ready — tap 'Speak Now' to talk"
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _audioRmsLevel.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim()
        if (!text.isNullOrBlank()) {
            _liveTranscript.value = text
            dispatchRecognizedText(text)
        } else if (_liveTranscript.value.isNotBlank()) {
            dispatchRecognizedText(_liveTranscript.value)
        } else {
            _speechStatus.value = "Ready to listen"
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim()
        if (!partial.isNullOrBlank()) {
            _liveTranscript.value = partial
            _speechStatus.value = "Heard: \"$partial\"..."
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
