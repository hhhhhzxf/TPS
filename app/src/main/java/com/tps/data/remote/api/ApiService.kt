package com.tps.data.remote.api

import com.tps.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/code")
    suspend fun sendCode(@Query("phone") phone: String): ApiResponse<String>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): ApiResponse<LoginResponse>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): ApiResponse<LoginResponse>

    // User
    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") id: Long): ApiResponse<UserProfile>

    @GET("api/users/me")
    suspend fun getMyProfile(): ApiResponse<UserProfile>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body req: UpdateProfileRequest): ApiResponse<UserProfile>

    @PUT("api/users/me/avatar")
    suspend fun updateAvatar(@Query("avatarUrl") avatarUrl: String): ApiResponse<UserProfile>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ApiResponse<UploadResponse>

    // Products
    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("condition") condition: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("sort") sort: String? = null
    ): ApiResponse<PageResponse<ProductDto>>

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): ApiResponse<ProductDto>

    @POST("api/products")
    suspend fun createProduct(@Body req: ProductRequest): ApiResponse<ProductDto>

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body req: ProductRequest): ApiResponse<ProductDto>

    @PATCH("api/products/{id}/status")
    suspend fun updateProductStatus(@Path("id") id: Long, @Query("status") status: String): ApiResponse<ProductDto>

    @POST("api/products/{id}/bump")
    suspend fun bumpProduct(@Path("id") id: Long): ApiResponse<ProductDto>

    @GET("api/users/{id}/products")
    suspend fun getUserProducts(@Path("id") id: Long): ApiResponse<List<ProductDto>>

    @GET("api/products/my")
    suspend fun getMyProducts(): ApiResponse<List<ProductDto>>

    // Favorites
    @POST("api/favorites/{productId}")
    suspend fun toggleFavorite(@Path("productId") productId: Long): ApiResponse<Boolean>

    @GET("api/favorites")
    suspend fun getFavorites(): ApiResponse<List<ProductDto>>

    // Orders
    @POST("api/orders")
    suspend fun createOrder(
        @Query("productId") productId: Long,
        @Query("finalPrice") finalPrice: Double
    ): ApiResponse<OrderDto>

    @GET("api/orders/my")
    suspend fun getMyOrders(
        @Query("role") role: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<OrderDto>>

    @GET("api/orders/{id}")
    suspend fun getOrder(@Path("id") id: Long): ApiResponse<OrderDto>

    @PUT("api/orders/{id}/pay")
    suspend fun payOrder(@Path("id") id: Long): ApiResponse<Unit>

    @PUT("api/orders/{id}/ship")
    suspend fun shipOrder(
        @Path("id") id: Long,
        @Query("trackingNumber") trackingNumber: String? = null
    ): ApiResponse<Unit>

    @PUT("api/orders/{id}/confirm")
    suspend fun confirmOrder(@Path("id") id: Long): ApiResponse<Unit>

    @PUT("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Long): ApiResponse<Unit>

    // Messages
    @GET("api/messages/conversations")
    suspend fun getConversations(): ApiResponse<PageResponse<ConversationDto>>

    @GET("api/messages/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: Long
    ): ApiResponse<List<MessageDto>>

    @POST("api/messages/conversation")
    suspend fun getOrCreateConversation(
        @Query("targetUserId") targetUserId: Long,
        @Query("productId") productId: Long
    ): ApiResponse<ConversationDto>

    // Notifications
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("type") type: String? = null
    ): ApiResponse<PageResponse<NotificationDto>>

    @PUT("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): ApiResponse<Unit>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): ApiResponse<Unit>

    // History
    @POST("api/history/products/{productId}")
    suspend fun recordHistory(@Path("productId") productId: Long): ApiResponse<Unit>

    @GET("api/history/products")
    suspend fun getHistory(): ApiResponse<List<ProductDto>>

    @DELETE("api/history/products")
    suspend fun clearHistory(): ApiResponse<Unit>

    // Feedback
    @POST("api/feedback")
    suspend fun submitFeedback(@Body req: FeedbackRequest): ApiResponse<FeedbackDto>

    @GET("api/feedback/my")
    suspend fun getMyFeedback(): ApiResponse<List<FeedbackDto>>

    // Admin
    @GET("api/admin/users")
    suspend fun adminGetUsers(@Query("page") page: Int = 0): ApiResponse<PageResponse<UserProfile>>

    @PUT("api/admin/users/{id}/ban")
    suspend fun adminBanUser(@Path("id") id: Long): ApiResponse<Unit>

    @PUT("api/admin/users/{id}/unban")
    suspend fun adminUnbanUser(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/admin/reports")
    suspend fun adminGetReports(@Query("page") page: Int = 0): ApiResponse<PageResponse<ReportDto>>

    @PUT("api/admin/products/{id}/takedown")
    suspend fun adminTakedownProduct(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/admin/orders")
    suspend fun adminGetOrders(@Query("page") page: Int = 0): ApiResponse<PageResponse<OrderDto>>

    @GET("api/admin/stats")
    suspend fun adminGetStats(): ApiResponse<AdminStats>
}
