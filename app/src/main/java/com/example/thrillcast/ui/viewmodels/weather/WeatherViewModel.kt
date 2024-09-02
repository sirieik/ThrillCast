package com.example.thrillcast.ui.viewmodels.weather

import com.example.thrillcast.data.datamodels.WeatherForecast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thrillcast.data.datamodels.Wind
import com.example.thrillcast.data.repositories.HolfuyRepository
import com.example.thrillcast.data.repositories.MetRepository
import com.example.thrillcast.data.repositories.WindyRepository
import com.example.thrillcast.ui.common.WindyWinds
import com.example.thrillcast.ui.common.Takeoff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * En ViewModel-klasse for å håndtere og oppdatere værdata for forskjellige steder.
 *
 * @param holfuyRepository Injisert repository for å hente data fra Holfuy-API.
 * @param metRepository Injisert repository for å hente data fra MET-API.
 * @param windyRepository Injisert repository for å hente data fra Windy-API.
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val holfuyRepository: HolfuyRepository,
    private val metRepository: MetRepository,
    private val windyRepository: WindyRepository
    ) : ViewModel() {

    private val _currentWeatherUiState = MutableStateFlow(
        CurrentWeatherUiState(
            null,
            null
        )
    )

    val currentWeatherUiState: StateFlow<CurrentWeatherUiState> = _currentWeatherUiState.asStateFlow()

    private val _forecastUiState = MutableStateFlow(
        ForecastUiState(
            null
        )
    )

    val forecastWeatherUiState: StateFlow<ForecastUiState> = _forecastUiState.asStateFlow()

    private val _heightWindUiState = MutableStateFlow(
        HeightWindUiState(
            null
        )
    )

    val heightWindUiState: StateFlow<HeightWindUiState> = _heightWindUiState.asStateFlow()

    private val _takeoffUiState = MutableStateFlow(
        TakeoffUiState(
            null
        )
    )

    val takeoffUiState: StateFlow<TakeoffUiState> = _takeoffUiState.asStateFlow()

    private val _multiCurrentWeatherUiState = MutableStateFlow(
        MultiCurrentWeatherUiState(
            listOf()
        )
    )

    val multiCurrentWeatherUiState: StateFlow<MultiCurrentWeatherUiState> = _multiCurrentWeatherUiState.asStateFlow()


    private val _locationsWindUiState = MutableStateFlow(
        LocationsWindUiState(
            listOf()
        )
    )
    val locationsWindUiState: StateFlow<LocationsWindUiState> = _locationsWindUiState.asStateFlow()

    /**
     * Henter nåværende værdata for et gitt takeoff-sted ved hjelp av Holfuy og MET-APIene.
     *
     * @param takeoff Takeoff-stedet der været skal hentes.
     */
    fun retrieveCurrentWeather(takeoff: Takeoff) {
        viewModelScope.launch {
            takeoff.id.let {
                val stationWind: Wind? = holfuyRepository.fetchHolfuyStationWeather(takeoff.id)
                val weather = metRepository.fetchNowCastObject(
                    takeoff.coordinates.latitude,
                    takeoff.coordinates.longitude
                )?.properties?.timeseries?.firstOrNull()

                _currentWeatherUiState.value = CurrentWeatherUiState(wind = stationWind, nowCastObject = weather)
            }
        }
    }

    /**
     * Henter værprognose for et gitt takeoff-sted ved hjelp av MET-API.
     *
     * @param takeoff Takeoff-stedet der værprognosen skal hentes.
     */
    fun retrieveForecastWeather(takeoff: Takeoff) {
        viewModelScope.launch {
            val locationForecast: List<WeatherForecast>? = metRepository.fetchLocationForecast(takeoff.coordinates.latitude, takeoff.coordinates.longitude)
            _forecastUiState.value = ForecastUiState(locationForecast = locationForecast)
        }
    }

    /**
     * Henter høydevind-data for et gitt takeoff-sted ved hjelp av Windy-API.
     *
     * @param takeoff Takeoff-stedet der høydevinden skal hentes.
     */
    fun retrieveHeightWind(takeoff: Takeoff) {
        viewModelScope.launch {
            val windyWinds: List<WindyWinds> = windyRepository.fetchWindyWindsList(takeoff.coordinates.latitude, takeoff.coordinates.longitude)
            _heightWindUiState.value = HeightWindUiState(windyWindsList = windyWinds)
        }
    }

    /**
     * Oppdaterer valgt takeoff-sted i ViewModel.
     *
     * @param takeoff Takeoff-stedet som er valgt.
     */
    fun updateChosenTakeoff(takeoff: Takeoff) {
        viewModelScope.launch {
            _takeoffUiState.value = TakeoffUiState(takeoff = takeoff)
        }
    }

    /**
     * Henter vinddata for en liste med takeoff-steder ved hjelp av Holfuy-API.
     *
     * @param locations Listen med takeoff-steder.
     */
    fun retrieveLocationsWind(locations: List<Takeoff>) {
        viewModelScope.launch {
            val locationWinds = mutableListOf<Wind>()
            locations.forEach{ location ->
                val wind = holfuyRepository.fetchHolfuyStationWeather(location.id)
                locationWinds.add(wind ?: Wind(0,0.0,0.0,0.0,"m/s"))
            }
            _locationsWindUiState.value = LocationsWindUiState(locationWinds)
        }
    }

    /**
     * Henter værdata for en liste med favorittsteder ved hjelp av Holfuy og MET-APIene.
     *
     * @param favorites Listen med favorittsteder.
     */
    fun retrieveFavoritesWeather(favorites: List<Takeoff?>) {
        viewModelScope.launch {
            val currentWeatherList = mutableListOf<CurrentWeatherUiState>()

            favorites.forEach { favorite ->
                favorite?.id?.let {
                    val wind = holfuyRepository.fetchHolfuyStationWeather(favorite.id)
                    val weather = metRepository.fetchNowCastObject(
                        favorite.coordinates.latitude,
                        favorite.coordinates.longitude
                    )
                    currentWeatherList.add(
                        CurrentWeatherUiState(
                            weather?.properties?.timeseries?.firstOrNull(),
                            wind
                        )
                    )
                }
            }
            _multiCurrentWeatherUiState.value = MultiCurrentWeatherUiState(currentWeatherList)
        }
    }
}