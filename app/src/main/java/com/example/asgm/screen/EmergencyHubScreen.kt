// #member3
// SOS screen: the resident's own private emergency contacts, the community contacts, and
// safety guides in one page. The private section (and its divider/header) only appears when
// the resident has added at least one; otherwise the page looks exactly as before.
package com.example.asgm.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.viewmodel.PersonalContactViewModel
import com.example.asgm.viewmodel.PersonalContactViewModelFactory
import com.example.asgm.viewmodel.SafetyGuideViewModel
import com.example.asgm.viewmodel.SafetyGuideViewModelFactory
import kotlinx.coroutines.launch

private const val MAX_PERSONAL_CONTACTS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHubScreen(navController: NavHostController) {
    // no session (e.g. restored after process death) -> the nav guard sends us to login
    val userId = UserSession.currentUserId ?: return
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val contactViewModel: PersonalContactViewModel =
        viewModel(factory = PersonalContactViewModelFactory(db.emergencyContactDao(), userId))
    val guideViewModel: SafetyGuideViewModel =
        viewModel(factory = SafetyGuideViewModelFactory(db.safetyGuideDao()))
    val personal by contactViewModel.personal.collectAsState()
    val community by contactViewModel.community.collectAsState()
    val guides by guideViewModel.guides.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var contactPendingDelete by remember { mutableStateOf<EmergencyContactEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SOS")
                        Text(
                            "Emergency contacts & safety guides",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (personal.size >= MAX_PERSONAL_CONTACTS) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Maximum $MAX_PERSONAL_CONTACTS personal contacts"
                                    )
                                }
                            } else {
                                navController.navigate("personal_contact_form?contactId=-1")
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add your emergency contact")
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (personal.isNotEmpty()) {
                sectionHeader("Your Personal Emergency Contacts (Max $MAX_PERSONAL_CONTACTS)")
                contactCards(personal, keyPrefix = "personal") { contact, cardModifier ->
                    ContactCard(
                        contact = contact,
                        modifier = cardModifier,
                        onEdit = {
                            navController.navigate("personal_contact_form?contactId=${contact.serviceId}")
                        },
                        onDelete = { contactPendingDelete = contact }
                    )
                }
                fullSpanDivider()
                sectionHeader("General Emergency Contacts")
            }

            contactCards(community, keyPrefix = "community") { contact, cardModifier ->
                ContactCard(contact = contact, modifier = cardModifier)
            }

            if (guides.isNotEmpty()) {
                fullSpanDivider()
                sectionHeader("Safety Guides")
                items(guides, key = { "guide_${it.guideId}" }) { guide ->
                    GuideCard(guide) { navController.navigate("guide_detail/${guide.guideId}") }
                }
            }
        }

        contactPendingDelete?.let { contact ->
            AlertDialog(
                onDismissRequest = { contactPendingDelete = null },
                title = { Text("Delete contact?") },
                text = { Text("Remove \"${contact.name}\" from your personal contacts. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        contactViewModel.delete(contact)
                        contactPendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { contactPendingDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

// A full-width section label inside the 2-column grid.
private fun LazyGridScope.sectionHeader(text: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun LazyGridScope.fullSpanDivider() {
    item(span = { GridItemSpan(maxLineSpan) }) {
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

// Lays a list of contacts into the grid two-per-row; an odd last one is centered on its own
// row instead of being stranded in the left column.
private fun LazyGridScope.contactCards(
    contacts: List<EmergencyContactEntity>,
    keyPrefix: String,
    card: @Composable (EmergencyContactEntity, Modifier) -> Unit
) {
    val hasLoneLast = contacts.size % 2 == 1
    val paired = if (hasLoneLast) contacts.dropLast(1) else contacts
    items(paired, key = { "${keyPrefix}_${it.serviceId}" }) { contact ->
        card(contact, Modifier.fillMaxWidth())
    }
    if (hasLoneLast) {
        val last = contacts.last()
        item(key = "${keyPrefix}_${last.serviceId}", span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                card(last, Modifier.fillMaxWidth(0.5f))
            }
        }
    }
}

private fun dialContact(context: android.content.Context, phoneNo: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNo")))
}

@Composable
private fun ContactCard(
    contact: EmergencyContactEntity,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clickable { dialContact(context, contact.phoneNo) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            // Capped at a couple lines each -- an admin-typed name/detail with no length limit
            // would otherwise grow this card past its grid row partner's height.
            Text(
                contact.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Private contacts have no category line -- only community contacts fill this in.
            if (contact.categoryEmergency.isNotBlank()) {
                Text(
                    contact.categoryEmergency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                contact.phoneNo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onEdit != null || onDelete != null) {
                Row {
                    onEdit?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit contact",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete contact",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconForGuide(category: String): ImageVector = when (category) {
    "Fire" -> Icons.Filled.LocalFireDepartment
    "Flood" -> Icons.Filled.Water
    "Power Outage" -> Icons.Filled.PowerOff
    "Earthquake" -> Icons.Filled.Vibration
    else -> Icons.AutoMirrored.Filled.MenuBook
}

@Composable
private fun GuideCard(guide: SafetyGuideEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = iconForGuide(guide.categorySafety),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                guide.categorySafety,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Procedures",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
