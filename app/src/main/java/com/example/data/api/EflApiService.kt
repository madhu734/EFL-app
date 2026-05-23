package com.example.data.api

import com.example.data.model.PocketBaseResponse
import com.example.data.model.Team
import com.example.data.model.Player
import com.example.data.model.Fixture
import com.example.data.model.Result
import com.example.data.model.Supporter
import com.example.data.model.SystemSetting
import com.example.data.model.FplUser
import com.example.data.model.FplMatchData
import retrofit2.http.GET
import retrofit2.http.Url

interface EflApiService {
    @GET
    suspend fun getTeams(@Url url: String): PocketBaseResponse<Team>

    @GET
    suspend fun getPlayers(@Url url: String): PocketBaseResponse<Player>

    @GET
    suspend fun getFixtures(@Url url: String): PocketBaseResponse<Fixture>

    @GET
    suspend fun getResults(@Url url: String): PocketBaseResponse<Result>

    @GET
    suspend fun getSupporters(@Url url: String): PocketBaseResponse<Supporter>

    @GET
    suspend fun getSystemSettings(@Url url: String): PocketBaseResponse<SystemSetting>

    @GET
    suspend fun getFplUsers(@Url url: String): PocketBaseResponse<FplUser>

    @GET
    suspend fun getFplMatchData(@Url url: String): PocketBaseResponse<FplMatchData>

    @retrofit2.http.POST
    suspend fun executePost(
        @Url url: String,
        @retrofit2.http.Body body: okhttp3.RequestBody
    ): okhttp3.ResponseBody

    @retrofit2.http.PATCH
    suspend fun executePatch(
        @Url url: String,
        @retrofit2.http.Body body: okhttp3.RequestBody
    ): okhttp3.ResponseBody
}
