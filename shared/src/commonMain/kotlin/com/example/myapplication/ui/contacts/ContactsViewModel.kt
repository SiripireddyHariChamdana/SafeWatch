package com.example.myapplication.ui.contacts

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.EmergencyContact
import com.example.myapplication.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _contacts = mutableStateOf<List<EmergencyContact>>(emptyList())
    val contacts: State<List<EmergencyContact>> = _contacts

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun fetchContacts() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        
        // Setup realtime listener if not already done
        startRealtimeSync(userId)

        viewModelScope.launch {
            _loading.value = true
            try {
                val result = client.postgrest["emergency_contacts"]
                    .select(columns = Columns.ALL) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<EmergencyContact>()
                _contacts.value = result
            } catch (e: Exception) {
                _error.value = "Failed to fetch contacts: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addContact(name: String, phone: String) {
        val userId = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                println("🚀 Contacts: Adding contact '$name' for $userId")
                // Use a Map instead of the model temporarily to verify if it's a serialization issue
                client.postgrest["emergency_contacts"].insert(
                    mapOf(
                        "user_id" to userId,
                        "contact_name" to name,
                        "contact_phone" to phone
                    )
                )
                println("✅ Contacts: Save success")
                fetchContacts() // Refresh list
            } catch (e: Exception) {
                println("❌ Contacts: Save Failed: ${e.message}")
                _error.value = "Failed to add contact. Please try again."
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                client.postgrest["emergency_contacts"].delete {
                    filter { eq("id", contactId) }
                }
                fetchContacts() // Refresh list
            } catch (e: Exception) {
                _error.value = "Failed to delete contact: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private var isRealtimeStarted = false
    private fun startRealtimeSync(userId: String) {
        if (isRealtimeStarted) return
        isRealtimeStarted = true

        val channel = client.realtime.channel("contacts_sync_$userId")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "emergency_contacts"
        }.onEach {
            println("🔄 Realtime: Contact change detected")
            fetchContacts()
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                client.realtime.connect()
                channel.subscribe()
            } catch (e: Exception) {
                println("❌ Realtime Contacts Error: ${e.message}")
            }
        }
    }
}
