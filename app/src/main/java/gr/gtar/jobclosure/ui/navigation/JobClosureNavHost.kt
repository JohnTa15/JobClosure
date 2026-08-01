package gr.gtar.jobclosure.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.gtar.jobclosure.JobClosureApp
import gr.gtar.jobclosure.ui.bookingdetail.BookingDetailScreen
import gr.gtar.jobclosure.ui.bookingdetail.BookingDetailViewModel
import gr.gtar.jobclosure.ui.bookingedit.BookingEditScreen
import gr.gtar.jobclosure.ui.bookingedit.BookingEditViewModel
import gr.gtar.jobclosure.ui.bookinglist.BookingListScreen
import gr.gtar.jobclosure.ui.bookinglist.BookingListViewModel
import gr.gtar.jobclosure.ui.settings.SettingsScreen
import gr.gtar.jobclosure.ui.settings.SettingsViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_SETTINGS = "settings"
private const val ARG_BOOKING_ID = "bookingId"
private const val ROUTE_EDIT = "edit/{$ARG_BOOKING_ID}"
private const val ROUTE_DETAIL = "detail/{$ARG_BOOKING_ID}"
private const val NEW_BOOKING_ID = -1L

@Composable
fun JobClosureNavHost(app: JobClosureApp) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 5 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 5 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut() },
    ) {
        composable(ROUTE_LIST) {
            val viewModel: BookingListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { BookingListViewModel(app, app.bookingRepository, app.settingsRepository) }
                },
            )
            BookingListScreen(
                viewModel = viewModel,
                onAddBooking = { navController.navigate("edit/$NEW_BOOKING_ID") },
                onOpenBooking = { id -> navController.navigate("detail/$id") },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable(
            ROUTE_EDIT,
            arguments = listOf(navArgument(ARG_BOOKING_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(ARG_BOOKING_ID) ?: NEW_BOOKING_ID
            val isNew = bookingId == NEW_BOOKING_ID
            val viewModel: BookingEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BookingEditViewModel(
                            application = app,
                            repository = app.bookingRepository,
                            settingsRepository = app.settingsRepository,
                            placeSearchRepository = app.placeSearchRepository,
                            bookingId = if (isNew) null else bookingId,
                        )
                    }
                },
            )
            BookingEditScreen(
                viewModel = viewModel,
                isNew = isNew,
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_BOOKING_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(ARG_BOOKING_ID) ?: NEW_BOOKING_ID
            val viewModel: BookingDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BookingDetailViewModel(
                            application = app,
                            bookingRepository = app.bookingRepository,
                            settingsRepository = app.settingsRepository,
                            travelTimeRepository = app.travelTimeRepository,
                            droneConditionsRepository = app.droneConditionsRepository,
                            placeSearchRepository = app.placeSearchRepository,
                            bookingId = bookingId,
                        )
                    }
                },
            )
            BookingDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("edit/$id") },
            )
        }

        composable(ROUTE_SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(app.settingsRepository, app.updateRepository, app.placeSearchRepository)
                    }
                },
            )
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
