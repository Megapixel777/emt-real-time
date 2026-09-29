package com.tomasperez.emtrealtime.storage

import android.content.Context
import com.tomasperez.emtrealtime.data.Favorite

class FavoriteStorage(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "emt_favorites",
            Context.MODE_PRIVATE
        )

    fun saveFavorite(favorite: Favorite) {

        preferences.edit()
            .putInt("stop_id", favorite.stopId)
            .putString("line", favorite.line)
            .putString("destination", favorite.destination)
            .apply()
    }

    fun getFavorite(): Favorite? {

        if (!preferences.contains("stop_id")) {
            return null
        }

        return Favorite(
            stopId = preferences.getInt("stop_id", 0),
            line = preferences.getString("line", "") ?: "",
            destination = preferences.getString("destination", "") ?: ""
        )
    }

    fun deleteFavorite() {

        preferences.edit()
            .clear()
            .apply()
    }
}