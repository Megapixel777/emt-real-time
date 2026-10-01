package com.tomasperez.emtrealtime.storage

import android.content.Context

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.tomasperez.emtrealtime.data.Favorite


class FavoriteStorage(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "emt_favorites",
            Context.MODE_PRIVATE
        )

    private val gson =
        Gson()

    private val favoritesKey =
        "favorites"


    // ==================================================
    // OBTENER TODOS LOS FAVORITOS
    // ==================================================

    fun getFavorites(): List<Favorite> {

        val json =
            preferences.getString(
                favoritesKey,
                null
            )
                ?: return emptyList()

        return try {

            val type =
                object :
                    TypeToken<List<Favorite>>() {}.type

            val favorites =
                gson.fromJson<List<Favorite>>(
                    json,
                    type
                ) ?: emptyList()

            favorites

        } catch (e: Exception) {

            emptyList()
        }
    }


    // ==================================================
    // GUARDAR FAVORITO
    // ==================================================

    fun saveFavorite(
        favorite: Favorite
    ) {

        val favorites =
            getFavorites().toMutableList()

        // Evitar duplicados de la misma
        // parada + línea

        val alreadyExists =
            favorites.any {

                it.stopId == favorite.stopId &&
                        it.line.equals(
                            favorite.line,
                            ignoreCase = true
                        )
            }

        if (!alreadyExists) {

            favorites.add(
                favorite
            )

            saveFavorites(
                favorites
            )
        }
    }


    // ==================================================
    // ACTUALIZAR FAVORITO
    // ==================================================

    fun updateFavorite(
        updatedFavorite: Favorite
    ) {

        val favorites =
            getFavorites()
                .map { favorite ->

                    if (
                        favorite.stopId ==
                        updatedFavorite.stopId &&
                        favorite.line.equals(
                            updatedFavorite.line,
                            ignoreCase = true
                        )
                    ) {

                        updatedFavorite

                    } else {

                        favorite
                    }
                }

        saveFavorites(
            favorites
        )
    }


    // ==================================================
    // ELIMINAR FAVORITO
    // ==================================================

    fun deleteFavorite(
        favorite: Favorite
    ) {

        val favorites =
            getFavorites()
                .filterNot {

                    it.stopId ==
                            favorite.stopId &&
                            it.line.equals(
                                favorite.line,
                                ignoreCase = true
                            )
                }

        saveFavorites(
            favorites
        )
    }


    // ==================================================
    // GUARDAR LISTA COMPLETA
    // ==================================================

    private fun saveFavorites(
        favorites: List<Favorite>
    ) {

        val json =
            gson.toJson(
                favorites
            )

        preferences.edit()
            .putString(
                favoritesKey,
                json
            )
            .apply()
    }


    // ==================================================
    // BORRAR TODOS
    // ==================================================

    fun deleteAllFavorites() {

        preferences.edit()
            .remove(
                favoritesKey
            )
            .apply()
    }
}