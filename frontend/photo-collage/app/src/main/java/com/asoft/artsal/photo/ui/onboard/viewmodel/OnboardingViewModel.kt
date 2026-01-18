package com.asoft.artsal.photo.ui.onboard.viewmodel

import android.app.Application
import android.content.res.Resources
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.Language
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    override val app: Application,
    private val sharePreference: SharePreference,
) : BaseViewModel(app = app) {

    val languages = SingleLiveEvent<List<Language>>()
    val codeLanguage = SingleLiveEvent<String>()

    val redirectNextScreen = SingleLiveEvent<Boolean>()

    private val dataLanguages = mutableListOf(
        Language(R.drawable.img_us, "English (US)", "en-rUS"),
        Language(R.drawable.img_uk, "English (UK)", "en-rGB"),
        Language(R.drawable.img_hindi, "Hindi (हिंद)", "hi"),
        Language(R.drawable.img_bangla, "Bangla (বাংলা)", "bn"),
        Language(R.drawable.img_brasil, "Portuguese (brasileiro)", "pt-rBR"),
        Language(R.drawable.img_saudi_arabia, "Saudi Arabia (عربي)", "ar-rSA"),
        Language(R.drawable.img_tunisia, "Tunisia (عربي)", "ar-rTN"),
        Language(R.drawable.img_oman, "Oman (عربي)", "ar-rOM"),
        Language(R.drawable.img_indo, "Indonesian (bahasa Indo)", "in"),
        Language(R.drawable.img_potugues, "Portuguese (português)", "pt-rPT"),
        Language(R.drawable.img_spainish, "Spanish (español)", "es"),
        Language(R.drawable.img_marathi, "Marathi (मराठी)", "mr"),
        Language(R.drawable.img_marathi, "Tegulu (తెలుగు)", "te"),
        Language(R.drawable.img_marathi, "Tamil (தமிழ்)", "ta"),
        Language(R.drawable.img_russian, "Russian (русский)", "ru"),
        Language(R.drawable.img_french, "French (français)", "fr"),
        Language(R.drawable.img_vietnamese, "Vietnamese (Tiếng Việt)", "vi"),
        Language(R.drawable.img_german, "German (Deutsch)", "de-rDE"),
        Language(R.drawable.img_korean, "Korean (한국인)", "ko"),
        Language(R.drawable.img_japan, "Japanese (日本語)", "ja"),
        Language(R.drawable.img_turkish, "Turkish (Türkçe)", "tr-rTR"),
        Language(R.drawable.img_china, "Chinese (中国人)", "zh"),
        Language(R.drawable.img_italy, "Italian (italiano)", "it-rIT"),
        Language(R.drawable.img_thai, "Thai (คนไทย)", "th"),
        Language(R.drawable.img_dutch, "Dutch (Nederlands)", "nl-rNL"),
        Language(R.drawable.img_danish, "Danish (dansk)", "da-rDK"),
        Language(R.drawable.img_irish, "Irish (Gaeilge)", "ga"),
        Language(R.drawable.img_polish, "Polish (polski)", "pl"),
        Language(R.drawable.img_zulu, "Zulu", "zu"),
    )

    fun getLanguages() {
        // Get the device's current language code
        val deviceLanguageCode = getDeviceLanguageCode()

        // Look for a match in our language list
        val foundIndex = dataLanguages.indexOfFirst { language ->
            language.code == deviceLanguageCode ||
                    language.code.startsWith(deviceLanguageCode.substringBefore('-')) ||
                    deviceLanguageCode.startsWith(language.code.substringBefore('-'))
        }

        // If found, move the language to the top of the list
        if (foundIndex > 1) {
            val foundLanguage = dataLanguages.removeAt(foundIndex)
            dataLanguages.add(3, foundLanguage)
        }

        val languageCodeSaved = sharePreference.get<String>(SharePreference.LANGUAGE_APP).orEmpty()
        if (languageCodeSaved.isEmpty()) {
            languages.call(dataLanguages)
        } else {
            val newList = dataLanguages.map {
                it.copy(isChecked = it.code == languageCodeSaved)
            }
            languages.call(newList)
        }

    }

    fun getLanguagesSetting() {
        val deviceLanguageCode = sharePreference.get<String>(SharePreference.LANGUAGE_APP) ?: "en"

        // Look for a match in our language list
        val foundIndex = dataLanguages.indexOfFirst { language ->
            language.code == deviceLanguageCode ||
                    language.code.startsWith(deviceLanguageCode.substringBefore('-')) ||
                    deviceLanguageCode.startsWith(language.code.substringBefore('-'))
        }

        // If found, move the language to the top of the list
        if (foundIndex >= 0) {
            val foundLanguage = dataLanguages.removeAt(foundIndex)
            dataLanguages.add(0, foundLanguage.copy(isChecked = true))
        }

        languages.call(dataLanguages)
    }

    private fun getDeviceLanguageCode(): String {
        val locales = Resources.getSystem().configuration.locales
        return if (locales.isEmpty) {
            Locale.getDefault().toString().replace('_', '-')
        } else {
            locales[0].toString().replace('_', '-')
        }
    }

    fun selectLanguage(code: String) {
        val newList = languages.value?.map {
            it.copy(isChecked = it.code == code)
        }
        languages.call(newList ?: return)
        codeLanguage.call(code)
    }

}

