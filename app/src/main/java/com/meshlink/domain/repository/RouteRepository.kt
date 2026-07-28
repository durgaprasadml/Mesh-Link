package com.meshlink.domain.repository

import com.meshlink.domain.model.RouteEntry

interface RouteRepository {
    suspend fun getRoutes(): List<RouteEntry>
    suspend fun getRoute(destinationId: String): RouteEntry?
    suspend fun updateRoute(route: RouteEntry)
}
