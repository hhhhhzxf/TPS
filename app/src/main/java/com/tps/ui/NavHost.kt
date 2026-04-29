package com.tps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tps.ui.admin.AdminMainScreen
import com.tps.ui.auth.LoginScreen
import com.tps.ui.auth.RegisterScreen
import com.tps.ui.home.MainScreen
import com.tps.ui.message.ChatScreen
import com.tps.ui.product.ProductDetailScreen
import com.tps.util.TokenManager

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object AdminMain : Screen("admin_main")
    object ProductDetail : Screen("product/{id}") {
        fun createRoute(id: Long) = "product/$id"
    }
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(id: Long) = "chat/$id"
    }
    object Publish : Screen("publish")
    object MyProducts : Screen("my_products")
    object Favorites : Screen("favorites")
    object History : Screen("history")
    object Feedback : Screen("feedback")
}

@Composable
fun TpsNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val tokenManager = remember { TokenManager(context) }
    val startDestination = when {
        tokenManager.isLoggedIn() && tokenManager.isAdmin() -> Screen.AdminMain.route
        tokenManager.isLoggedIn() -> Screen.Main.route
        else -> Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    val dest = if (isAdmin) Screen.AdminMain.route else Screen.Main.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToProductDetail = { id ->
                    navController.navigate(Screen.ProductDetail.createRoute(id))
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToPublish = {
                    navController.navigate(Screen.Publish.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToMyProducts = { navController.navigate(Screen.MyProducts.route) },
                onNavigateToOrdersRole = { role -> 
                    navController.navigate("orders?role=$role") {
                        popUpTo(Screen.Main.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToFeedback = { navController.navigate(Screen.Feedback.route) }
            )
        }

        composable(Screen.Publish.route) {
            com.tps.ui.product.PublishProductScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.MyProducts.route) {
            com.tps.ui.profile.MyProductsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) }
            )
        }

        composable(Screen.Favorites.route) {
            com.tps.ui.profile.FavoritesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) }
            )
        }

        composable(Screen.History.route) {
            com.tps.ui.profile.BrowsingHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) }
            )
        }

        composable(Screen.Feedback.route) {
            com.tps.ui.profile.FeedbackScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminMain.route) {
            AdminMainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            ProductDetailScreen(
                productId = id,
                onBack = { navController.popBackStack() },
                onChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) }
            )
        }
    }
}
