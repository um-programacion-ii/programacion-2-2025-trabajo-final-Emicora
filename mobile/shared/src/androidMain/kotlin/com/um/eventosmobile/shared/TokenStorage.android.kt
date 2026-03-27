package com.um.eventosmobile.shared

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementación Android de TokenStorage que almacena el token solo en memoria.
 * El token NO es persistente y se pierde cuando la aplicación se cierra.
 * 
 * Esta clase implementa la funcionalidad de TokenStorage para Android
 * usando una variable en memoria para almacenamiento no persistente.
 */
class TokenStorageAndroid(private val context: Context) : TokenStorage {
    // Variable en memoria para almacenar el token (no persistente)
    @Volatile
    private var tokenInMemory: String? = null
    
    // Mutex para sincronización thread-safe
    private val mutex = Mutex()
    
    override suspend fun saveToken(token: String) {
        mutex.withLock {
            tokenInMemory = token
        }
    }
    
    override suspend fun getToken(): String? {
        return mutex.withLock {
            tokenInMemory
        }
    }
    
    override suspend fun clearToken() {
        mutex.withLock {
            tokenInMemory = null
        }
    }
}

