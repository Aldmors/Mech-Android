package com.mech.carexpensetracker.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.ui.components.AppCard
import com.mech.carexpensetracker.ui.components.EventTypeBadge
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.theme.DesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var search by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md),
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
                singleLine = true,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                EventFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
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
                items(state.events) { event ->
                    EventRow(event = event, formatCost = viewModel::formatCost)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CarEventEntity, formatCost: (CarEventEntity) -> String) {
    AppCard {
        Text(
            text = formatCost(event),
            modifier = Modifier.padding(DesignTokens.Spacing.md),
        )
        EventTypeBadge(
            type = EventType.fromRaw(event.typeRaw),
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md),
        )
        event.comment?.let {
            Text(text = it, modifier = Modifier.padding(DesignTokens.Spacing.md))
        }
        if (event.secondaryFuelTypeRaw != null) {
            Text(
                text = "Dual fuel: ${event.fuelTypeRaw} + ${event.secondaryFuelTypeRaw}",
                modifier = Modifier.padding(DesignTokens.Spacing.md),
            )
        }
    }
}

@Composable
private fun NoteRow(note: CarNoteEntity) {
    AppCard {
        Text(text = note.title, modifier = Modifier.padding(DesignTokens.Spacing.md))
        note.details?.let { Text(text = it, modifier = Modifier.padding(DesignTokens.Spacing.md)) }
    }
}
