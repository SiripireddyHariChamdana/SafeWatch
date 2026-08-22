package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.contacts.ContactsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.EmergencyContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    viewModel: ContactsViewModel = viewModel { ContactsViewModel() }
) {
    val contacts by viewModel.contacts
    val loading by viewModel.loading
    val error by viewModel.error

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchContacts()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Emergency Circle", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (loading && contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts) { contact ->
                        ContactItem(
                            contact = contact,
                            onDelete = { contact.id?.let { viewModel.deleteContact(it) } }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        if (!loading) {
                            GradientButton(
                                text = "Add New Member", 
                                onClick = { showAddDialog = true }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddContactDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, phone ->
                    viewModel.addContact(name, phone)
                    showAddDialog = false
                }
            )
        }

        if (error != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = Color.Red
            ) {
                Text(error!!)
            }
        }
    }
}

@Composable
fun ContactItem(contact: EmergencyContact, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NeonBlue.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = NeonBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(contact.contact_name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(contact.contact_phone, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { 
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f)) 
            }
        }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact", color = Color.White) },
        text = {
            Column {
                SafeWatchTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Full Name",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
                SafeWatchTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "Phone Number",
                    icon = Icons.Default.Phone,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onAdd(name, phone) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add", color = NeonBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = DarkNavy,
        shape = RoundedCornerShape(24.dp)
    )
}
