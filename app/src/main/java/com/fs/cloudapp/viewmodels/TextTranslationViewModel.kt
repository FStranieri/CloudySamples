package com.fs.cloudapp.viewmodels

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.MutableLiveData
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator
import kotlinx.coroutines.flow.MutableStateFlow

class TextTranslationViewModel : GenericViewModel<TextTranslationViewModel.TextTranslationState>() {

    private lateinit var mlRemoteTranslator: MLRemoteTranslator

    var translation: String? = null

    init {
        super.mState = MutableStateFlow(TextTranslationState())
        initializeMLRemoteTranslator()
    }

    //using the cloud, ignoring the source language
    private fun initializeMLRemoteTranslator() {
        // Create a text translator using custom parameter settings.
        val setting = MLRemoteTranslateSetting.Factory()
            // Set the source language code.
            // The BCP-47 standard is used for Traditional Chinese,
            // and the ISO 639-1 standard is used for other languages.
            // This parameter is optional. If this parameter is not set,
            // the system automatically detects the language.
            .setSourceLangCode(null)
            // Set the target language code.
            // The BCP-47 standard is used for Traditional Chinese,
            // and the ISO 639-1 standard is used for other languages.
            .setTargetLangCode(Locale.current.language)
            .create()
        this.mlRemoteTranslator = MLTranslatorFactory.getInstance().getRemoteTranslator(setting)
    }

    fun translate(text: String) {
        mlRemoteTranslator.asyncTranslate(text).addOnSuccessListener {
            translation = it
            updateState(mState.value.copy(showTranslation = true, failureOutput = null))
        }.addOnFailureListener {
            translation = null
            updateState(mState.value.copy(failureOutput = it, showTranslation = false))
        }
    }

    fun resetTranslationVisibility() {
        updateState(mState.value.copy(showTranslation = false, failureOutput = null))
    }

    companion object {
        const val TAG = "TextTranslationViewModel"
    }

    data class TextTranslationState(
        val showTranslation: Boolean = false,
        override val failureOutput: Exception? = null
    ) : GenericState
}