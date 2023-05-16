package com.example.pokedex.pokemon_list

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.pokedex.data.models.PokedexListEntry
import com.example.pokedex.repository.PokemonRepository
import com.example.pokedex.util.Resource
import com.example.pokedex.util.constants.Constants.PAGE_SIZE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import android.graphics.Color as AndroidColor

@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val repository: PokemonRepository
) : ViewModel() {

    private var curPage = 0

    var pokemonList = mutableStateOf<List<PokedexListEntry>>(listOf())
    var loadError = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var endReached = mutableStateOf(false)

    private var cachedPokemonList = listOf<PokedexListEntry>()
    private var isSearchStarting = true
    var isSearching = mutableStateOf(false)


    init {
        loadPokemonPaginated()
    }

    fun searchPokemonList(query: String){
        val listToSearch = if(isSearchStarting){
            pokemonList.value
        }else{
            cachedPokemonList
        }
        viewModelScope.launch(Dispatchers.Default) {
            if(query.isEmpty()){
                pokemonList.value = cachedPokemonList
                isSearching.value = false
                isSearchStarting = true
                return@launch
            }
            val results = listToSearch.filter {
                it.pokemonName.contains(query.trim(), ignoreCase = true) ||
                        it.number.toString() == query.trim()
            }
            if(isSearchStarting){
                cachedPokemonList = pokemonList.value
                isSearchStarting = false
            }
            pokemonList.value = results
            isSearching.value = true
        }
    }


    fun loadPokemonPaginated(){
        viewModelScope.launch {
            isLoading.value = true

            val result = repository.getPokemonList(PAGE_SIZE, curPage * PAGE_SIZE)

            when(result){
                is Resource.Success -> {
                    endReached.value = curPage * PAGE_SIZE >= result.data!!.count
                    val pokedexEntries = result.data.results.mapIndexed { index, entry ->
                        val number = if(entry.url.endsWith("/")){
                            entry.url.dropLast(1).takeLastWhile { it.isDigit() }
                        }else{
                            entry.url.takeLastWhile { it.isDigit() }
                        }
                        val url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${number}.png"
                        PokedexListEntry(entry.name.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT
                            ) else it.toString()
                        }, url, number.toInt())
                    }
                    curPage++
                    loadError.value = ""
                    isLoading.value = false
                    pokemonList.value += pokedexEntries
                }
                is Resource.Error -> {
                    loadError.value = result.message!!
                    isLoading.value = false
                }

                else -> {}
            }
        }
    }


    fun generateColorFromImage(imageUrl: String, context: Context, onFinish: (Color) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .build()
                val result = (imageLoader.execute(request).drawable as BitmapDrawable).bitmap
                val softwareBitmap = result.copy(Bitmap.Config.ARGB_8888, true)  // convert to software bitmap
                val color = getProminentColor(softwareBitmap)
                softwareBitmap.recycle()  // recycle the software bitmap
                onFinish(Color(color))
            }
        }
    }

    suspend fun getProminentColor(bitmap: Bitmap): Int {
        return withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()
            palette.getDominantColor(AndroidColor.GRAY)
        }
    }

}