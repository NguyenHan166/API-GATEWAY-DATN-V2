package com.asoft.artsal.photo.ui.home.viewmodel


import android.app.Application
import androidx.lifecycle.viewModelScope
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.data.model.Collage
import com.asoft.artsal.photo.data.model.Template
import com.asoft.artsal.photo.data.model.TemplateType
import com.asoft.artsal.photo.data.model.TypeEditor
import com.asoft.artsal.photo.data.model.TypeView
import com.asoft.artsal.photo.extensions.internal.checkingTypeCollage
import com.asoft.artsal.photo.extensions.internal.checkingTypeTemplate
import com.asoft.artsal.photo.extensions.internal.getBorderByName
import com.asoft.artsal.photo.extensions.internal.getLayerByFileName
import com.asoft.artsal.photo.extensions.internal.getLayoutByFileName
import com.asoft.artsal.photo.extensions.internal.getPhotoNumbers
import com.asoft.artsal.photo.extensions.internal.getRadio
import com.asoft.artsal.photo.extensions.internal.getTemplateViewType
import com.asoft.artsal.photo.extensions.internal.getTypeView
import com.asoft.artsal.photo.ui.collage.adapter.CollageType
import com.asoft.artsal.photo.ui.collage.adapter.SelectView
import com.asoft.artsal.photo.ui.template.adapter.SelectTemplateView
import com.asoft.artsal.photo.utils.AdsIdUtils
import com.asoft.artsal.photo.utils.AssetsPackManager
import com.asoft.artsal.photo.utils.Const.COLLAGE_PATH
import com.asoft.artsal.photo.utils.Const.FILTER_PATH
import com.asoft.artsal.photo.utils.Const.LAYER_FIRST
import com.asoft.artsal.photo.utils.Const.LAYER_SECOND
import com.asoft.artsal.photo.utils.Const.PREVIEW_PATH
import com.asoft.artsal.photo.utils.Const.ROOT_PATH
import com.asoft.artsal.photo.utils.Const.TEMPLATE_PATH
import com.asoft.artsal.photo.utils.RemoteConfigAdsConst
import com.asoft.artsal.photo.utils.SharePreference
import com.asoft.artsal.photo.utils.SingleLiveEvent
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.minsap.ad.ads.MinSapNativeAd
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.AdError
import com.minsap.ad.config.NativeAdValue
import com.minsap.ad.listener.NativeAdCallBack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    override val app: Application,
    private val assetsPackManager: AssetsPackManager,
    private val sharePreference: SharePreference
) : BaseViewModel(app) {

    fun shouldShowReviewDialog(): Boolean {
        Timber.i("Checking enable show rate...")
        val isEnableShow = sharePreference.get<Boolean>(SharePreference.IS_ENABLE_SHOW_RATE, false) ?: false
        if(!isEnableShow) return false

        Timber.i("Checking rate showed")
        val ratingShowed = sharePreference.get<Boolean>(SharePreference.IS_RATE_APP, false) ?: false
        if (ratingShowed) return false

        Timber.i("Checking rate last time...")
        return checkRateLastTime()
    }

    private fun checkRateLastTime(): Boolean {
        val lastShowTime = sharePreference.get<Long>(SharePreference.LAST_RATE_TIME, 0L)
        if (lastShowTime == 0L) {
            return true
        }

        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDayOfMonth = currentCalendar.get(Calendar.DAY_OF_MONTH)

        val lastShownCalendar = Calendar.getInstance().apply {
            timeInMillis = lastShowTime ?: 0L
        }
        val lastShownYear = lastShownCalendar.get(Calendar.YEAR)
        val lastShownMonth = lastShownCalendar.get(Calendar.MONTH)
        val lastShownDayOfMonth = lastShownCalendar.get(Calendar.DAY_OF_MONTH)

        val isSameDay = currentYear == lastShownYear &&
                currentMonth == lastShownMonth &&
                currentDayOfMonth == lastShownDayOfMonth

        return !isSameDay
    }

    fun saveTimeShowDialog() {
        sharePreference.save(SharePreference.LAST_RATE_TIME, System.currentTimeMillis())
    }

    fun initDataAsset() {
        Timber.i("loading data for collage...")
        val listModels = assetsPackManager.getAllModels(COLLAGE_PATH)
        if (listModels.isNotEmpty()) {
            setupCollageFromAssets(listModels)
        }

        Timber.i("loading data for filter...")
        val imagePaths = assetsPackManager.getAllModels(FILTER_PATH)
        viewModelScope.launch(Dispatchers.IO) {
            assetsPackManager.copyLutFilesToInternalStorage()
        }

        Timber.i("loading data for template...")
        val listTemplatePreview = assetsPackManager.getAllModels(TEMPLATE_PATH + PREVIEW_PATH)
        val listTemplateFirstLayer = assetsPackManager.getAllModels(TEMPLATE_PATH + LAYER_FIRST)
        val listTemplateSecondLayer = assetsPackManager.getAllModels(TEMPLATE_PATH + LAYER_SECOND)

        Timber.d("loading preview : ${listTemplatePreview}")
        Timber.d("loading first layer : ${listTemplateFirstLayer}")
        Timber.d("loading second layer : ${listTemplateSecondLayer}")

        if (listTemplatePreview.isNotEmpty() && listTemplateFirstLayer.isNotEmpty() && listTemplateSecondLayer.isNotEmpty()) {
            setupTemplateFromAssets(
                listTemplatePreview,
                listTemplateFirstLayer,
                listTemplateSecondLayer
            )
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  Variable Collage
    //////////////////////////////////////////////////////////////////
    val dataAllPhotos = mutableListOf<Collage>()
    val data2Photos = mutableListOf<Collage>()
    val data3Photos = mutableListOf<Collage>()
    val data4Photos = mutableListOf<Collage>()
    val data5Photos = mutableListOf<Collage>()

    private val _selectViewAll = MutableStateFlow<List<SelectView>?>(null)
    val selectViewAll: StateFlow<List<SelectView>?> = _selectViewAll.asStateFlow()
    private val _selectView2 = MutableStateFlow<List<SelectView>?>(null)
    val selectView2: StateFlow<List<SelectView>?> = _selectView2.asStateFlow()

    private val _selectView3 = MutableStateFlow<List<SelectView>?>(null)
    val selectView3: StateFlow<List<SelectView>?> = _selectView3.asStateFlow()

    private val _selectView4 = MutableStateFlow<List<SelectView>?>(null)
    val selectView4: StateFlow<List<SelectView>?> = _selectView4.asStateFlow()

    private val _selectView5 = MutableStateFlow<List<SelectView>?>(null)
    val selectView5: StateFlow<List<SelectView>?> = _selectView5.asStateFlow()

    ///////////////////////////////////////////////////////////////////
    //                  FUNCTION COLLAGE
    //////////////////////////////////////////////////////////////////
    fun setupDataByType(type: CollageType) {
        Timber.i("Setup data by type...: ${type.name}")
        when (type) {
            CollageType.Collage2 -> setupPhotos(data2Photos, CollageType.Collage2)

            CollageType.Collage3 -> setupPhotos(data3Photos, CollageType.Collage3)

            CollageType.Collage4 -> setupPhotos(data4Photos, CollageType.Collage4)

            CollageType.Collage5 -> setupPhotos(data5Photos, CollageType.Collage5)

            CollageType.All -> setupPhotos(dataAllPhotos, CollageType.All)
        }
    }

    fun setupPhotos(photos: List<Collage> = data5Photos, type: CollageType = CollageType.All) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<SelectView>()
            photos.forEachIndexed { index, collage ->
                list.add(
                    SelectView.Content(
                        collage
                    )
                )
                if (index == 2 && type != CollageType.All) {
                    list.add(SelectView.NativeAds())
                }
                if (index == 1 && type == CollageType.All) {
                    list.add(SelectView.NativeAds())
                }
            }
            when (type) {
                CollageType.Collage2 -> _selectView2.value = list
                CollageType.Collage3 -> _selectView3.value = list
                CollageType.Collage4 -> _selectView4.value = list
                CollageType.Collage5 -> _selectView5.value = list
                CollageType.All -> _selectViewAll.value = list
            }
        }
    }

    private fun setupCollageFromAssets(imagePaths: List<String>) {
        Timber.i("starting setup photo Collage FromAssets...")
        viewModelScope.launch(Dispatchers.IO) {
            imagePaths.forEach { imageName ->
                val typeView = imageName.getTypeView()
                val radio = typeView.getRadio()
                val photoNumbers = imageName.getPhotoNumbers()
                val collageType = imageName.checkingTypeCollage()
                val border = typeView.getBorderByName(imageName)

                val collage = Collage(
                    id = UUID.randomUUID().toString().hashCode(),
                    imagePath = "$ROOT_PATH$COLLAGE_PATH/$imageName",
                    collageName = imageName,
                    photoNumbers = photoNumbers,
                    type = TypeEditor.Collage,
                    radio = radio,
                    collageType = collageType,
                    typeView = typeView,
                    border = border
                )
                addCollage(collage, collageType)
                Timber.i("$ROOT_PATH$COLLAGE_PATH/$imageName")
            }
            val allData = mutableListOf<Collage>()
            dataAllPhotos.add(data5Photos[0])
            dataAllPhotos.add(data2Photos[2])
            allData.arrangeView(data2Photos.filter { it.id != data2Photos[2].id }.toMutableList())
            allData.arrangeView(data3Photos.toMutableList())
            allData.arrangeView(data4Photos.toMutableList())
            allData.arrangeView(data5Photos.filter { it.id != data5Photos[0].id }.toMutableList())

            while (allData.isNotEmpty()) {
                val randomIndex = allData.indices.random()
                val collage = allData[randomIndex]
                dataAllPhotos.add(collage)
                allData.removeAt(randomIndex)
            }
        }
    }

    private fun MutableList<Collage>.arrangeView(data: MutableList<Collage>): MutableList<Collage> {

        data.filter { it.typeView == TypeView.Landscape && it.id != data5Photos[0].id }.forEach {
            dataAllPhotos.add(2, it)
            data.remove(it)
        }
        this.addAll(data)
        return this
    }

    private fun addCollage(collage: Collage, type: CollageType) {
        Timber.i("Add collage type...: ${type.name}...")
        when (type) {
            CollageType.Collage2 -> data2Photos.add(collage)

            CollageType.Collage3 -> data3Photos.add(collage)

            CollageType.Collage4 -> data4Photos.add(collage)

            CollageType.Collage5 -> data5Photos.add(collage)

            CollageType.All -> {}
        }
    }

    ///////////////////////////////////////////////////////////////////
    //                  Variable Template
    ///////////////////////////////////////////////////////////////////
    val dataTempAll = mutableListOf<Template>()
    val dataTempTrend = mutableListOf<Template>()
    val dataTempBirth = mutableListOf<Template>()
    val dataTempHoli = mutableListOf<Template>()
    val dataTempSpec = mutableListOf<Template>()
    val dataTempMoment = mutableListOf<Template>()
    val dataTempTravel = mutableListOf<Template>()
    val dataTempSea = mutableListOf<Template>()
    val dataTempMile = mutableListOf<Template>()

    private val _currentTabSelected = MutableStateFlow<TemplateType?>(null)
    val currentTabSelected: StateFlow<TemplateType?> = _currentTabSelected.asStateFlow()
    private val _mutableTabSelected = MutableStateFlow<TemplateType?>(TemplateType.Birthdays)
    val mutableTabSelected: StateFlow<TemplateType?> = _mutableTabSelected.asStateFlow()
    private val _selectView = MutableStateFlow<List<SelectTemplateView>?>(null)
    val selectView: StateFlow<List<SelectTemplateView>?> = _selectView.asStateFlow()
    private val _selectTempTrend = MutableStateFlow<List<SelectTemplateView>?>(null)
    val selectTempTrend: StateFlow<List<SelectTemplateView>?> = _selectTempTrend.asStateFlow()
    private val _selectTempViewAll = MutableStateFlow<List<SelectTemplateView>?>(null)
    val selectTempViewAll: StateFlow<List<SelectTemplateView>?> = _selectTempViewAll.asStateFlow()

    private val _selectTempViewSpec = MutableStateFlow<List<SelectTemplateView>?>(null)
    val selectTempViewSpec: StateFlow<List<SelectTemplateView>?> = _selectTempViewSpec.asStateFlow()

    ///////////////////////////////////////////////////////////////////
    //                  FUNCTION TEMPLATE
    ///////////////////////////////////////////////////////////////////

    fun getMutableTypeSelected(): TemplateType {
        return mutableTabSelected.value ?: TemplateType.Birthdays
    }

    fun setupTemplatePhotos(
        photos: List<Template> = dataTempBirth, type: TemplateType = TemplateType.All
    ) {
        val list = mutableListOf<SelectTemplateView>()
        photos.forEachIndexed { index, template ->
            list.add(
                SelectTemplateView.Content(
                    template
                )
            )
            if (index == 2) {
                list.add(SelectTemplateView.NativeAds())
            }
        }
        when (type) {
            TemplateType.Birthdays -> _selectView.value = list
            TemplateType.Holidays -> _selectView.value = list
            TemplateType.SpecialDays -> _selectTempViewSpec.value = list
            TemplateType.Seasons -> _selectView.value = list
            TemplateType.Milestones -> _selectView.value = list
            TemplateType.Moments -> _selectView.value = list
            TemplateType.Travel -> _selectView.value = list
            TemplateType.All -> _selectTempViewAll.value = list
            TemplateType.Trending -> _selectTempTrend.value = list
        }
    }

    fun setupDataTemplateByType(type: TemplateType) {
        Timber.i("Setup data by type...: ${type.name}")
        when (type) {
            TemplateType.Trending -> setupTemplatePhotos(dataTempTrend, TemplateType.Trending)
            TemplateType.All -> setupTemplatePhotos(dataTempAll, TemplateType.All)
            TemplateType.Birthdays -> setupTemplatePhotos(dataTempBirth, TemplateType.Birthdays)
            TemplateType.SpecialDays -> setupTemplatePhotos(dataTempSpec, TemplateType.SpecialDays)
            TemplateType.Holidays -> setupTemplatePhotos(dataTempHoli, TemplateType.Holidays)
            TemplateType.Travel -> setupTemplatePhotos(dataTempTravel, TemplateType.Travel)
            TemplateType.Seasons -> setupTemplatePhotos(dataTempSea, TemplateType.Seasons)
            TemplateType.Milestones -> setupTemplatePhotos(dataTempMile, TemplateType.Milestones)
            TemplateType.Moments -> setupTemplatePhotos(dataTempMoment, TemplateType.Moments)
        }
    }

    private fun addTemplate(template: Template, type: TemplateType) {
        Timber.i("Add template type...: ${type.name}...")
        when (type) {
            TemplateType.Birthdays -> dataTempBirth.add(template)
            TemplateType.Holidays -> dataTempHoli.add(template)
            TemplateType.Travel -> dataTempTravel.add(template)
            TemplateType.Seasons -> dataTempSea.add(template)
            TemplateType.Moments -> dataTempMoment.add(template)
            TemplateType.Milestones -> dataTempMile.add(template)
            TemplateType.SpecialDays -> dataTempSpec.add(template)
            else -> {}
        }
    }

    fun setupTemplateFromAssets(
        imagePaths: List<String>,
        firstLayer: List<String>,
        secondLayer: List<String>
    ) {
        Timber.i("starting setup photo Collage FromAssets...")
        viewModelScope.launch(Dispatchers.IO) {
            imagePaths.forEach { imageName ->
                val typeView = imageName.getTemplateViewType()
                val radio = typeView.getRadio()
                val templateType = imageName.checkingTypeTemplate()
                val layoutId = imageName.getLayoutByFileName()
                val layerFirstId = imageName.getLayerByFileName(firstLayer)
                val layerSecondId = imageName.getLayerByFileName(secondLayer)

                val template = Template(
                    id = UUID.randomUUID().toString().hashCode(),
                    imagePath = "$ROOT_PATH$TEMPLATE_PATH$PREVIEW_PATH/$imageName",
                    name = imageName,
                    radio = radio,
                    typeView = typeView,
                    layoutId = layoutId,
                    layerFirstId = "$ROOT_PATH$TEMPLATE_PATH$LAYER_FIRST/$layerFirstId",
                    layerSecondId = "$ROOT_PATH$TEMPLATE_PATH$LAYER_SECOND/$layerSecondId"
                )
                addTemplate(template, templateType)
                Timber.i("$ROOT_PATH$TEMPLATE_PATH$PREVIEW_PATH/$imageName")
            }
            val trendingData = mutableListOf<Template>()
            trendingData.addAll(dataTempTravel)
            trendingData.addAll(dataTempMoment)
            dataTempTrend.addAll(trendingData)

            val allData = mutableListOf<Template>()
            allData.addAll(dataTempBirth)
            allData.addAll(dataTempHoli)
            allData.addAll(dataTempSpec)
            allData.addAll(dataTempMoment)
            allData.addAll(dataTempTravel)
            allData.addAll(dataTempSea)
            allData.addAll(dataTempMile)

            while (allData.isNotEmpty()) {
                val randomIndex = allData.indices.random()
                val template = allData[randomIndex]
                dataTempAll.add(template)
                allData.removeAt(randomIndex)
            }
        }
    }

    fun updateCurrentTab(templateType: TemplateType) {
        if (templateType != TemplateType.All && templateType != TemplateType.Trending && templateType != TemplateType.SpecialDays) {
            if (templateType != mutableTabSelected.value) {
                _mutableTabSelected.value = templateType
                setupDataTemplateByType(templateType)
            }
        }
        _currentTabSelected.value = templateType
    }

    ///////////////////////////////////////////////////////////////////
    //                  SETUP ADS
    ///////////////////////////////////////////////////////////////////

    private val _adNativeTempHome = MutableStateFlow<NativeAdState?>(null)
    val adNativeTempHome: StateFlow<NativeAdState?> = _adNativeTempHome.asStateFlow()
    private val _createAdNativeTempHome by lazy { MinSapNativeAd.create() }
    val createAdNativeTempHome: MinSapNativeAd = _createAdNativeTempHome

    fun getAdNativeTempHome() = _adNativeTempHome
    fun preloadNativeAdTempHome() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_TEMPLATE_SHOW)) {
            if (_adNativeTempHome.value != null) return
            createAdNativeTempHome.loadNativeAdResult(
                context = app,
                adUnitId = AdsIdUtils.NATIVE_TEMPLATE,
                nameAd = "Home Template",
                callbackResult = object : NativeAdCallBack() {
                    override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                        _adNativeTempHome.value =
                            NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                    }

                    override fun onAdFailedToLoad(error: AdError) {
                        _adNativeTempHome.value = NativeAdState.NativeAdError(adError = error)
                    }

                    override fun onAdClicked() {
                        preloadNativeAdTempHome()
                    }
                })
        }
    }

    private val _adNativeCollage = MutableStateFlow<NativeAdState?>(null)
    val adNativeCollage: StateFlow<NativeAdState?> = _adNativeCollage.asStateFlow()
    private val _createAdNativeCollage by lazy { MinSapNativeAd.create() }
    val createAdNativeCollage: MinSapNativeAd = _createAdNativeCollage

    fun getAdNativeCollage() = _adNativeCollage
    fun preloadNativeAdCollage() {
        if (firebaseRemoteConfig.getBoolean(RemoteConfigAdsConst.NATIVE_COLLAGE_SHOW)) {
            if (_adNativeCollage.value != null) return
            viewModelScope.launch(Dispatchers.IO) {
                _createAdNativeCollage.loadNativeAdResult(
                    context = app,
                    adUnitId = AdsIdUtils.NATIVE_COLLAGE,
                    nameAd = "Home Collage",
                    callbackResult = object : NativeAdCallBack() {
                        override fun onAdLoaded(nativeAdValue: NativeAdValue) {
                            _adNativeCollage.value =
                                NativeAdState.NativeAdData(nativeAdValue = nativeAdValue)
                        }

                        override fun onAdFailedToLoad(error: AdError) {
                            _adNativeCollage.value = NativeAdState.NativeAdError(adError = error)
                        }

                        override fun onAdClicked() {
                            preloadNativeAdCollage()
                        }
                    })
            }
        }
    }

    companion object {
        const val TAG = "Home_VM"
    }
}


