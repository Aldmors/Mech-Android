package com.mech.carexpensetracker.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.ui.components.AppCard
import com.mech.carexpensetracker.ui.components.AppIcons
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.EventRecordRow
import com.mech.carexpensetracker.ui.theme.DesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var search by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }

    AppLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    viewModel.setSearchQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_events)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                EventFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        leadingIcon = {
                            Icon(
                                imageVector = filterIcon(filter),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(
                                when (filter) {
                                    EventFilter.All -> stringResource(R.string.filter_all)
                                    EventFilter.Fuel -> stringResource(R.string.filter_fuel)
                                    EventFilter.Service -> stringResource(R.string.filter_service)
                                    EventFilter.Documents -> stringResource(R.string.filter_documents)
                                    EventFilter.Notes -> stringResource(R.string.filter_notes)
                                },
                            )
                        },
                    )
                }
            }
        }
        if (state.showingNotes) {
            if (state.notes.isEmpty()) {
                item { EmptyStateCard(message = stringResource(R.string.no_events)) }
            } else {
                items(state.notes) { note -> NoteRow(note = note) }
            }
        } else {
            if (state.events.isEmpty()) {
                item { EmptyStateCard(message = stringResource(R.string.no_events)) }
            } else {
                items(state.events, key = { it.externalId }) { event ->
                    EventRecordRow(
                        event = event,
                        previousFuelEvent = state.previousFuelById[event.externalId],
                        units = state.vehicleUnits,
                    )
                }
            }
        }
    }
}

@Composable
private fun filterIcon(filter: EventFilter) = when (filter) {
    EventFilter.All -> Icons.Default.FilterList
    EventFilter.Fuel -> AppIcons.eventType(EventType.Fuel)
    EventFilter.Service -> AppIcons.eventType(EventType.Repair)
    EventFilter.Documents -> AppIcons.eventType(EventType.Papers)
    EventFilter.Notes -> Icons.AutoMirrored.Filled.Notes
}

@Composable
private fun NoteRow(note: CarNoteEntity) {
    AppCard {
        Text(text = note.title, modifier = Modifier.padding(DesignTokens.Spacing.md))
        note.details?.let { Text(text = it, modifier = Modifier.padding(DesignTokens.Spacing.md)) }
    }
}
