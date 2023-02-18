package com.bnyro.contacts.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bnyro.contacts.R
import com.bnyro.contacts.enums.SortOrder
import com.bnyro.contacts.ext.rememberPreference
import com.bnyro.contacts.obj.ContactData
import com.bnyro.contacts.ui.components.base.ClickableIcon
import com.bnyro.contacts.ui.components.base.OptionMenu
import com.bnyro.contacts.ui.components.base.SearchBar
import com.bnyro.contacts.ui.components.dialogs.ConfirmationDialog
import com.bnyro.contacts.ui.components.modifier.scrollbar
import com.bnyro.contacts.ui.models.ContactsModel
import com.bnyro.contacts.ui.screens.AboutScreen
import com.bnyro.contacts.ui.screens.EditorScreen
import com.bnyro.contacts.ui.screens.SettingsScreen
import com.bnyro.contacts.util.PermissionHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsPage(
    contacts: List<ContactData>?,
    showEditorDefault: Boolean
) {
    val viewModel: ContactsModel = viewModel()
    val context = LocalContext.current

    val selectedContacts = remember {
        mutableStateListOf<ContactData>()
    }

    var showEditor by remember {
        mutableStateOf(showEditorDefault)
    }

    var showDelete by remember {
        mutableStateOf(false)
    }

    var sortOrderPref by rememberPreference("sortOrder", SortOrder.FIRSTNAME.value.toString())
    val sortOrder by rememberUpdatedState(SortOrder.fromInt(sortOrderPref.toInt()))

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    val importVcard =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importVcf(context, it) }
        }

    val exportVcard = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        uri?.let { viewModel.exportVcf(context, it) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            val searchQuery = remember {
                mutableStateOf(TextFieldValue())
            }

            Crossfade(targetState = selectedContacts.isEmpty()) { state ->
                when (state) {
                    true -> {
                        SearchBar(
                            modifier = Modifier.padding(horizontal = 10.dp).padding(top = 15.dp),
                            state = searchQuery
                        ) {
                            Box(
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                var expandedSort by remember {
                                    mutableStateOf(false)
                                }
                                var expandedOptions by remember {
                                    mutableStateOf(false)
                                }

                                Row {
                                    ClickableIcon(
                                        icon = Icons.Default.Sort
                                    ) {
                                        expandedSort = !expandedSort
                                    }
                                    ClickableIcon(
                                        icon = Icons.Default.MoreVert
                                    ) {
                                        expandedOptions = !expandedOptions
                                    }
                                }

                                OptionMenu(
                                    expanded = expandedSort,
                                    options = listOf(
                                        stringResource(R.string.first_name),
                                        stringResource(R.string.last_name)
                                    ),
                                    onDismissRequest = {
                                        expandedSort = false
                                    },
                                    onSelect = {
                                        sortOrderPref = it.toString()
                                        expandedSort = false
                                    }
                                )
                                OptionMenu(
                                    expanded = expandedOptions,
                                    options = listOf(
                                        stringResource(R.string.import_vcf),
                                        stringResource(R.string.export_vcf),
                                        stringResource(R.string.settings),
                                        stringResource(R.string.about)
                                    ),
                                    onDismissRequest = {
                                        expandedOptions = false
                                    },
                                    onSelect = {
                                        when (it) {
                                            0 -> {
                                                importVcard.launch(
                                                    arrayOf(
                                                        "text/vcard",
                                                        "text/v-card",
                                                        "text/x-vcard"
                                                    )
                                                )
                                            }

                                            1 -> {
                                                exportVcard.launch("contacts.vcf")
                                            }

                                            2 -> {
                                                showSettings = true
                                            }

                                            3 -> {
                                                showAbout = true
                                            }
                                        }
                                        expandedOptions = false
                                    }
                                )
                            }
                        }
                    }
                    false -> {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.app_name))
                            },
                            actions = {
                                ClickableIcon(icon = Icons.Default.CopyAll) {
                                    viewModel.copyContacts(context, selectedContacts.toList())
                                    selectedContacts.clear()
                                }
                                ClickableIcon(icon = Icons.Default.MoveToInbox) {
                                    viewModel.moveContacts(context, selectedContacts.toList())
                                    selectedContacts.clear()
                                }
                                ClickableIcon(icon = Icons.Default.Delete) {
                                    showDelete = true
                                }
                            }
                        )
                    }
                }
            }

            if (contacts == null) {
                LaunchedEffect(Unit) {
                    if (PermissionHelper.hasPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        )
                    ) {
                        return@LaunchedEffect
                    }
                    while (!PermissionHelper.hasPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        )
                    ) {
                        delay(100)
                    }
                    viewModel.loadContacts()
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier.size(120.dp),
                        imageVector = Icons.Default.ImportContacts,
                        contentDescription = null
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.nothing_here)
                    )
                }
            } else {
                val state = rememberLazyListState()
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .scrollbar(state, false)
                ) {
                    val contactGroups = contacts.filter {
                        it.displayName.orEmpty().lowercase().contains(
                            searchQuery.value.text.lowercase()
                        )
                    }.sortedBy {
                        when (sortOrder) {
                            SortOrder.FIRSTNAME -> it.firstName
                            SortOrder.LASTNAME -> it.surName
                        }
                    }.groupBy {
                        when (sortOrder) {
                            SortOrder.FIRSTNAME -> it.firstName?.firstOrNull()?.uppercase()
                            SortOrder.LASTNAME -> it.surName?.firstOrNull()?.uppercase()
                        }
                    }

                    contactGroups.forEach { (firstLetter, groupedContacts) ->
                        stickyHeader {
                            CharacterHeader(firstLetter.orEmpty())
                        }
                        items(groupedContacts) {
                            ContactItem(
                                contact = it,
                                sortOrder = sortOrder,
                                selected = selectedContacts.contains(it),
                                onSinglePress = {
                                    if (selectedContacts.isEmpty()) {
                                        false
                                    } else {
                                        if (selectedContacts.contains(it)) {
                                            selectedContacts.add(it)
                                        } else {
                                            selectedContacts.remove(it)
                                        }
                                        true
                                    }
                                },
                                onLongPress = {
                                    if (!selectedContacts.contains(it)) selectedContacts.add(it)
                                }
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd),
            onClick = {
                showEditor = true
            }
        ) {
            Icon(Icons.Default.Create, null)
        }
    }

    if (showEditor) {
        EditorScreen(
            onClose = {
                showEditor = false
            },
            onSave = {
                viewModel.createContact(context, it)
            }
        )
    }

    if (showSettings) {
        SettingsScreen {
            showSettings = false
        }
    }

    if (showAbout) {
        AboutScreen {
            showAbout = false
        }
    }

    if (showDelete) {
        ConfirmationDialog(
            onDismissRequest = {
                showDelete = false
            },
            title = stringResource(R.string.delete_contact),
            text = stringResource(R.string.irreversible)
        ) {
            viewModel.deleteContacts(context, selectedContacts.toList())
            selectedContacts.clear()
        }
    }
}
