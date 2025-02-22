package com.ion606.installer

import android.util.Log
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


data class Release(
    val tag_name: String, // Release tag
    val name: String, // Release name
    val body: String, // Release notes
    val assets: List<Asset> // List of assets (APK files)
)

data class Asset(
    val browser_download_url: String
)


interface GitHubService {
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<Release>>  // Use Response to check HTTP status
}


object GitHubAPI {
    private const val BASE_URL = "https://api.github.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GitHubService = retrofit.create(GitHubService::class.java)

    suspend fun getLatestRelease(): List<Release>? {
        return try {
            val response = service.getReleases("ION-WorkoutApp", "app")
            if (response.isSuccessful) {
                response.body()?.let { releases ->
                    if (releases.isNotEmpty()) {
                        // Log details using Android Log
                        Log.d("GitHubAPI", "Latest release: ${releases[0].name}")
                    }
                    releases
                }
            } else {
                Log.e("GitHubAPI", "Failed to fetch releases: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("GitHubAPI", "Error fetching releases: ${e.message}")
            null
        }
    }
}