package com.example.data.api

import com.example.data.model.PocketBaseResponse
import com.example.data.model.Team
import com.example.data.model.Player
import com.example.data.model.Fixture
import com.example.data.model.Result
import com.example.data.model.Supporter
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
}
