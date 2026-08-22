package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.IsicTopAppBar
import com.example.ui.screens.AdRepositionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataLoadScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RegisterExitScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IsicViewModel
import com.example.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: IsicViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button to return to Dashboard if inside any sub-module
    if (currentScreen != ScreenDestination.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.DASHBOARD)
        }
    }

    val (screenTitle, screenSubtitle) = when (currentScreen) {
        ScreenDestination.DASHBOARD -> Pair("iSiC", "Sistema Integrado de Controle Técnico ADT")
        ScreenDestination.REGISTER_EXIT -> Pair("Registrar Saída", "Baixa de materiais por Cliente / O.S.")
        ScreenDestination.AD_REPOSITION -> Pair("Reposição AD", "Controle de Retorno e Devolução")
        ScreenDestination.INVENTORY -> Pair("Inventário Geral", "Poder de Terceiro & Saldo Físico")
        ScreenDestination.DATA_LOAD -> Pair("Carga de Dados", "Importação, Exportação e Backup")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            IsicTopAppBar(
                title = screenTitle,
                subtitle = screenSubtitle,
                showBackButton = currentScreen != ScreenDestination.DASHBOARD,
                onBackClick = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransition"
        ) { destination ->
            when (destination) {
                ScreenDestination.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                ScreenDestination.REGISTER_EXIT -> RegisterExitScreen(viewModel = viewModel)
                ScreenDestination.AD_REPOSITION -> AdRepositionScreen(viewModel = viewModel)
                ScreenDestination.INVENTORY -> InventoryScreen(viewModel = viewModel)
                ScreenDestination.DATA_LOAD -> DataLoadScreen(viewModel = viewModel)
            }
        }
    }
}
