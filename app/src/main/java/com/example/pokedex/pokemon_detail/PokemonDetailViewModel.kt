package com.example.pokedex.pokemon_detail

import androidx.lifecycle.ViewModel
import com.example.pokedex.data.remote.responses.Pokemon
import com.example.pokedex.repository.PokemonRepository
import com.example.pokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val repository: PokemonRepository
) : ViewModel() {


    suspend fun getPokemonInfo(pokemonName: String) : Resource<Pokemon> {
        return repository.getPokemonInfo(pokemonName)
    }


}