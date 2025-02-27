package com.example.repondeur

import android.telecom.Call
import android.telecom.InCallService
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class CallHandlerService : InCallService() {
    private var currentCall: Call? = null
    private var tts: TextToSpeech? = null
    private var messageRepeatCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    
    private val MESSAGE = "Pour transférer l'appel à Daniel Grosleau, veuillez appuyer sur le 8"
    private val DTMF_WAIT_TIME = 5000L // 5 secondes
    private val MAX_REPEATS = 2

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        
        // Initialiser Text-to-Speech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
                handleIncomingCall(call)
            }
        }

        // Initialiser le générateur de tonalité DTMF
        toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
        
        // Observer les changements d'état DTMF
        call.registerCallback(object : Call.Callback() {
            override fun onDigit(digit: Char, event: Int) {
                if (digit == '8') {
                    // Logique pour transférer l'appel
                    Log.d("CallHandlerService", "Touche 8 détectée - Transfert demandé")
                    // Ici, vous implémenteriez la logique de transfert d'appel
                    handler.removeCallbacksAndMessages(null)
                }
            }
        })
    }

    private fun handleIncomingCall(call: Call) {
        when (call.state) {
            Call.STATE_RINGING -> {
                // Répondre automatiquement à l'appel
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
                messageRepeatCount = 0
                playMessage()
            }
        }
    }

    private fun playMessage() {
        if (messageRepeatCount < MAX_REPEATS) {
            tts?.speak(MESSAGE, TextToSpeech.QUEUE_FLUSH, null, "message_id")
            messageRepeatCount++
            
            // Attendre 5 secondes pour la réponse
            handler.postDelayed({
                if (currentCall?.state == Call.STATE_ACTIVE) {
                    playMessage()
                }
            }, DTMF_WAIT_TIME)
        } else {
            // Raccrocher après deux tentatives
            currentCall?.disconnect()
        }
    }

    override fun onCallRemoved(call: Call) {
        if (currentCall == call) {
            handler.removeCallbacksAndMessages(null)
            tts?.shutdown()
            toneGenerator?.release()
            currentCall = null
        }
        super.onCallRemoved(call)
    }
} 